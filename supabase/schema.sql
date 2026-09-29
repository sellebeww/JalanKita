-- ============================================================================
-- JalanKita — Supabase schema, policies and storage setup
-- ----------------------------------------------------------------------------
-- Run this once in the Supabase SQL Editor for your project.
-- It is idempotent: re-running it is safe.
--
-- Design notes
--   * reports.id is the UUID generated on the device. Reusing it on every retry
--     is what makes synchronisation idempotent (see SyncReportsWorker).
--   * report_status (what the authority is doing) lives here; sync_status (did
--     the upload reach us) is purely a device-side concern and is NOT stored.
--   * Citizens may never change report_status — that is enforced with column
--     level grants, because RLS policies alone cannot restrict columns.
-- ============================================================================

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
    id         uuid primary key references auth.users (id) on delete cascade,
    name       text        not null default '',
    email      text        not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- reports
-- ---------------------------------------------------------------------------
create table if not exists public.reports (
    id            uuid primary key,                  -- generated on the device
    user_id       uuid             not null references auth.users (id) on delete cascade,
    category      text             not null,
    description   text,
    latitude      double precision not null,
    longitude     double precision not null,
    street_name   text,
    photo_url     text,
    report_status text             not null default 'PENDING',
    created_at    timestamptz      not null default now(),
    updated_at    timestamptz      not null default now(),

    constraint reports_category_check check (
        category in ('POTHOLE', 'CRACK', 'DAMAGED_ASPHALT', 'SUBSIDENCE',
                     'DAMAGED_SHOULDER', 'FLOODING', 'OTHER')
    ),
    constraint reports_status_check check (
        report_status in ('PENDING', 'VERIFIED', 'IN_PROGRESS', 'COMPLETED', 'NEEDS_INFO')
    ),
    constraint reports_latitude_check  check (latitude between -90 and 90),
    constraint reports_longitude_check check (longitude between -180 and 180)
);

create index if not exists reports_user_id_idx    on public.reports (user_id);
create index if not exists reports_created_at_idx on public.reports (created_at desc);
create index if not exists reports_status_idx     on public.reports (report_status);
create index if not exists reports_location_idx   on public.reports (latitude, longitude);

-- ---------------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------------
create table if not exists public.notifications (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid        not null references auth.users (id) on delete cascade,
    title      text        not null,
    message    text        not null,
    report_id  uuid        references public.reports (id) on delete cascade,
    is_read    boolean     not null default false,
    created_at timestamptz not null default now()
);

create index if not exists notifications_user_id_idx    on public.notifications (user_id);
create index if not exists notifications_created_at_idx on public.notifications (created_at desc);
create index if not exists notifications_unread_idx     on public.notifications (user_id) where not is_read;

-- ---------------------------------------------------------------------------
-- Triggers: keep updated_at honest, and give every new user a profile row
-- ---------------------------------------------------------------------------
create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
    before update on public.profiles
    for each row execute function public.touch_updated_at();

drop trigger if exists reports_touch_updated_at on public.reports;
create trigger reports_touch_updated_at
    before update on public.reports
    for each row execute function public.touch_updated_at();

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.profiles (id, name, email)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'name', split_part(new.email, '@', 1)),
        coalesce(new.email, '')
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Notify the reporter whenever the authority moves a report along.
create or replace function public.notify_report_status_change()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    if new.report_status is distinct from old.report_status then
        insert into public.notifications (user_id, title, message, report_id)
        values (
            new.user_id,
            'Status laporan Anda berubah',
            'Laporan Anda kini berstatus ' || new.report_status || '.',
            new.id
        );
    end if;
    return new;
end;
$$;

drop trigger if exists reports_notify_status_change on public.reports;
create trigger reports_notify_status_change
    after update on public.reports
    for each row execute function public.notify_report_status_change();

-- ============================================================================
-- Row Level Security
-- ============================================================================
alter table public.profiles      enable row level security;
alter table public.reports       enable row level security;
alter table public.notifications enable row level security;

-- profiles: a user sees and edits only their own row
drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
    for select to authenticated using (auth.uid() = id);

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
    for insert to authenticated with check (auth.uid() = id);

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
    for update to authenticated using (auth.uid() = id) with check (auth.uid() = id);

-- reports: a user reads and creates only their own
drop policy if exists "reports_select_own" on public.reports;
create policy "reports_select_own" on public.reports
    for select to authenticated using (auth.uid() = user_id);

drop policy if exists "reports_insert_own" on public.reports;
create policy "reports_insert_own" on public.reports
    for insert to authenticated
    with check (auth.uid() = user_id and report_status = 'PENDING');

drop policy if exists "reports_update_own" on public.reports;
create policy "reports_update_own" on public.reports
    for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Column-level grants: citizens may correct their own description / photo /
-- street name, but report_status belongs to the authority alone.
revoke update on public.reports from authenticated;
grant update (description, street_name, photo_url, updated_at) on public.reports to authenticated;

-- notifications: read your own, and mark them read
drop policy if exists "notifications_select_own" on public.notifications;
create policy "notifications_select_own" on public.notifications
    for select to authenticated using (auth.uid() = user_id);

drop policy if exists "notifications_update_own" on public.notifications;
create policy "notifications_update_own" on public.notifications
    for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);

revoke update on public.notifications from authenticated;
grant update (is_read) on public.notifications to authenticated;

-- No public/anon write access anywhere.
revoke all on public.profiles      from anon;
revoke all on public.reports       from anon;
revoke all on public.notifications from anon;

-- ============================================================================
-- Storage: report-images bucket
-- ----------------------------------------------------------------------------
-- Photos are public to read (the app stores the public URL on the report) but
-- a user may only write inside their own folder: reports live at
--   report-images/{user_id}/{report_id}.jpg
-- ============================================================================
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('report-images', 'report-images', true, 5242880, array['image/jpeg', 'image/png'])
on conflict (id) do update
    set public = excluded.public,
        file_size_limit = excluded.file_size_limit,
        allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "report_images_public_read" on storage.objects;
create policy "report_images_public_read" on storage.objects
    for select using (bucket_id = 'report-images');

drop policy if exists "report_images_insert_own_folder" on storage.objects;
create policy "report_images_insert_own_folder" on storage.objects
    for insert to authenticated
    with check (
        bucket_id = 'report-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- Re-uploading after a partially failed sync must overwrite, not duplicate.
drop policy if exists "report_images_update_own_folder" on storage.objects;
create policy "report_images_update_own_folder" on storage.objects
    for update to authenticated
    using (
        bucket_id = 'report-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    )
    with check (
        bucket_id = 'report-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    );

drop policy if exists "report_images_delete_own_folder" on storage.objects;
create policy "report_images_delete_own_folder" on storage.objects
    for delete to authenticated
    using (
        bucket_id = 'report-images'
        and (storage.foldername(name))[1] = auth.uid()::text
    );
