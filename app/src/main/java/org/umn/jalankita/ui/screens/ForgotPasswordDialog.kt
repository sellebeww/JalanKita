package org.umn.jalankita.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.umn.jalankita.data.repository.PasswordResetMode
import org.umn.jalankita.ui.components.AppPasswordField
import org.umn.jalankita.ui.components.AppTextField
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.TertiaryButton
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.viewmodel.ForgotPasswordUiState

/**
 * "Lupa Password".
 *
 * What it offers depends on where the account actually lives. Supabase owns the
 * credential, so the only honest action is to mail a reset link. A demo account
 * lives on this device, so it can be given a new password immediately — rather
 * than pretending to send an email that nothing would deliver.
 */
@Composable
fun ForgotPasswordDialog(
    state: ForgotPasswordUiState,
    mode: PasswordResetMode,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.visible) return
    var passwordVisible by remember { mutableStateOf(false) }
    val onDevice = mode == PasswordResetMode.ON_DEVICE

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.doneMessage != null) {
                    ResetDone(message = state.doneMessage, onClose = onDismiss)
                    return@Column
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockReset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.iconLg),
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Lupa Password",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (onDevice) {
                        "Akun mode demo tersimpan di perangkat ini, jadi password " +
                            "dapat langsung diatur ulang di sini."
                    } else {
                        "Masukkan email terdaftar. Kami akan mengirim tautan untuk " +
                            "mengatur ulang password Anda."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

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
                    imeAction = if (onDevice) ImeAction.Next else ImeAction.Done,
                )

                if (onDevice) {
                    Spacer(Modifier.height(Spacing.md))
                    AppPasswordField(
                        value = state.newPassword,
                        onValueChange = onPasswordChange,
                        label = "Password Baru",
                        placeholder = "Minimal 6 karakter",
                        visible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        errorText = state.passwordError,
                        enabled = !state.isSubmitting,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    AppPasswordField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmChange,
                        label = "Konfirmasi Password Baru",
                        visible = passwordVisible,
                        onToggleVisibility = { passwordVisible = !passwordVisible },
                        errorText = state.confirmError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                    )
                }

                if (state.error != null) {
                    Spacer(Modifier.height(Spacing.md))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.iconSm),
                        )
                        Spacer(Modifier.size(Spacing.xs))
                        Text(
                            text = state.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
                PrimaryButton(
                    text = if (onDevice) "Perbarui Password" else "Kirim Tautan",
                    onClick = onSubmit,
                    loading = state.isSubmitting,
                )
                Spacer(Modifier.height(Spacing.xs))
                TertiaryButton(text = "Batal", onClick = onDismiss, enabled = !state.isSubmitting)
            }
        }
    }
}

@Composable
private fun ResetDone(message: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = StatusGreen,
            modifier = Modifier.size(Dimens.iconLg),
        )
    }
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = "Berhasil",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.xl))
    PrimaryButton(text = "Tutup", onClick = onClose)
}
