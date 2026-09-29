package org.umn.jalankita.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.components.AppCard
import org.umn.jalankita.ui.components.AppPasswordField
import org.umn.jalankita.ui.components.AppTextField
import org.umn.jalankita.ui.components.AppTopBar
import org.umn.jalankita.ui.components.PrimaryButton
import org.umn.jalankita.ui.components.TertiaryButton
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing
import org.umn.jalankita.ui.theme.StatusGreen
import org.umn.jalankita.ui.theme.StatusOrange
import org.umn.jalankita.ui.theme.StatusRed
import org.umn.jalankita.viewmodel.PasswordStrength
import org.umn.jalankita.viewmodel.RegisterUiState

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Daftar Akun", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .padding(horizontal = Dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Buat akun untuk mulai melaporkan kerusakan jalan di sekitar Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                )

                AppCard(shape = MaterialTheme.shapes.large) {
                    AppTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = "Nama Lengkap",
                        placeholder = "Nama sesuai identitas",
                        leadingIcon = Icons.Outlined.Person,
                        errorText = state.nameError,
                        enabled = !state.isSubmitting,
                        capitalization = KeyboardCapitalization.Words,
                    )
                    Spacer(Modifier.height(Spacing.md))
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
                        placeholder = "Minimal 6 karakter",
                        visible = state.passwordVisible,
                        onToggleVisibility = onTogglePasswordVisibility,
                        leadingIcon = Icons.Outlined.Lock,
                        errorText = state.passwordError,
                        enabled = !state.isSubmitting,
                    )
                    if (state.passwordStrength != PasswordStrength.NONE && state.passwordError == null) {
                        Spacer(Modifier.height(Spacing.sm))
                        PasswordStrengthMeter(state.passwordStrength)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    AppPasswordField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Konfirmasi Password",
                        placeholder = "Ulangi password",
                        visible = state.passwordVisible,
                        onToggleVisibility = onTogglePasswordVisibility,
                        leadingIcon = Icons.Outlined.Lock,
                        errorText = state.confirmPasswordError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                    )

                    if (state.formError != null) {
                        Spacer(Modifier.height(Spacing.md))
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
                                Text(state.formError, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.xl))
                    PrimaryButton(
                        text = "Daftar",
                        onClick = onSubmit,
                        loading = state.isSubmitting,
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Sudah punya akun?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TertiaryButton(text = "Masuk", onClick = onBack, enabled = !state.isSubmitting)
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

/** Immediate feedback beats an error after submit. */
@Composable
private fun PasswordStrengthMeter(strength: PasswordStrength) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordStrength.STRONG -> StatusGreen
            PasswordStrength.FAIR -> StatusOrange
            else -> StatusRed
        },
        label = "strengthColor",
    )
    val progress by animateFloatAsState(targetValue = strength.fraction, label = "strengthProgress")

    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.weight(1f).height(6.dp),
        )
        Text(
            text = strength.label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
