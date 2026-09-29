package org.umn.jalankita.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.umn.jalankita.data.demo.DemoCredentials
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppPasswordField
import org.umn.jalankita.ui.components.AppTextField
import org.umn.jalankita.ui.components.AuroraBackground
import org.umn.jalankita.ui.components.BrandLockup
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.SecondaryButton
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.viewmodel.LoginUiState

/**
 * Sign-in.
 *
 * A living brand header — the aurora gradient drifting behind the mark — and a
 * form card that overlaps it, so the first screen of the app looks like the rest
 * of it rather than a bare form. The header, card and footer fade in one after
 * the other: a single simultaneous fade reads as a slow screen, a stagger reads
 * as the screen assembling itself.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    isDemoMode: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onForgotPassword: () -> Unit,
    onUseDemoAccount: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val headerAlpha by fadeIn(entered, delayMillis = 0)
    val cardAlpha by fadeIn(entered, delayMillis = 160)
    val footerAlpha by fadeIn(entered, delayMillis = 320)

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader(alpha = headerAlpha)

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(horizontal = Dimens.screenPadding)
                    .offset(y = (-38).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.alpha(cardAlpha),
                ) {
                    Text(
                        text = "Masuk ke Akun Anda",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = "Gunakan email terdaftar untuk melanjutkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (isDemoMode) {
                        Spacer(Modifier.height(Spacing.lg))
                        DemoAccountHint(onUseDemoAccount = onUseDemoAccount, enabled = !state.isSubmitting)
                    }

                    Spacer(Modifier.height(Spacing.xl))
                    AppTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Email",
                        placeholder = "nama@email.com",
                        leadingIcon = Icons.Outlined.Email,
                        errorText = state.emailError,
                        enabled = !state.isSubmitting,
                        keyboardType = KeyboardType.Email,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    AppPasswordField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        placeholder = "Masukkan password",
                        visible = state.passwordVisible,
                        onToggleVisibility = onTogglePasswordVisibility,
                        leadingIcon = Icons.Outlined.Lock,
                        errorText = state.passwordError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                    )

                    Spacer(Modifier.height(Spacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.rememberMe,
                            onCheckedChange = onRememberMeChange,
                            enabled = !state.isSubmitting,
                        )
                        Text(
                            text = "Ingat Saya",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onForgotPassword, enabled = !state.isSubmitting) {
                            Text("Lupa Password?", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (state.formError != null) {
                        Spacer(Modifier.height(Spacing.md))
                        FormErrorBanner(state.formError)
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    PrimaryButton(
                        text = "Masuk",
                        onClick = onSubmit,
                        loading = state.isSubmitting,
                    )

                    Spacer(Modifier.height(Spacing.lg))
                    LabelledDivider("atau")
                    Spacer(Modifier.height(Spacing.lg))

                    SecondaryButton(
                        text = "Daftar Akun Baru",
                        onClick = onNavigateToRegister,
                        enabled = !state.isSubmitting,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))
                Column(
                    modifier = Modifier.alpha(footerAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ValueProps()
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        text = "Dengan masuk, Anda setuju laporan yang dikirim dapat " +
                            "digunakan untuk perbaikan jalan di wilayah Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

/** Living gradient behind the mark; the form card overlaps its lower edge. */
@Composable
private fun BrandHeader(alpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp),
    ) {
        AuroraBackground(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Dimens.screenPadding)
                .padding(top = Spacing.xxl, bottom = 64.dp)
                .alpha(alpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandLockup(
                logoSize = 72.dp,
                tagline = "Laporkan kerusakan jalan dengan mudah, cepat, dan tepat.",
                onBrandSurface = true,
            )
        }
    }
}

/** Three reasons to sign in, in the words of what the app actually does. */
@Composable
private fun ValueProps() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ValueProp(Icons.Outlined.NearMe, "Peringatan", "Jalan rusak di sekitar Anda", Modifier.weight(1f))
        ValueProp(Icons.Outlined.Verified, "Tepercaya", "Rekam jejak tiap pelapor", Modifier.weight(1f))
        ValueProp(Icons.Outlined.AutoAwesome, "Offline", "Tetap tersimpan tanpa sinyal", Modifier.weight(1f))
    }
}

@Composable
private fun ValueProp(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimens.iconMd),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Demo builds ship with a ready account; showing it beats making someone guess,
 * and the button fills the form so the app can be opened in one tap.
 */
@Composable
private fun DemoAccountHint(onUseDemoAccount: () -> Unit, enabled: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconMd),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(text = "Mode Demo", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Akun contoh sudah tersedia di perangkat ini:\n" +
                    "${DemoCredentials.EMAIL} · ${DemoCredentials.PASSWORD}",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(Spacing.sm))
            TextButton(
                onClick = onUseDemoAccount,
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text("Gunakan Akun Demo", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FormErrorBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconMd),
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(text = message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LabelledDivider(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun fadeIn(entered: Boolean, delayMillis: Int) = animateFloatAsState(
    targetValue = if (entered) 1f else 0f,
    animationSpec = tween(durationMillis = 480, delayMillis = delayMillis),
    label = "loginFade$delayMillis",
)
