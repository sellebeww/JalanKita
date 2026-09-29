package org.umn.jalankita.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.umn.jalankita.ui.theme.Dimens
import org.umn.jalankita.ui.theme.Spacing

/**
 * The app's three button roles, in one place.
 *
 * Screens call these instead of Material's `Button` so height, radius, label
 * style, icon size and the busy spinner are identical everywhere — previously
 * every screen re-declared them and they drifted apart.
 */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        colors = colors,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = Dimens.buttonHeight),
    ) {
        ButtonContent(
            text = text,
            icon = icon,
            loading = loading,
            spinnerColor = colors.contentColor,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fillWidth: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.small,
        border = ButtonDefaults.outlinedButtonBorder(enabled && !loading),
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = Dimens.buttonHeightCompact),
    ) {
        ButtonContent(
            text = text,
            icon = icon,
            loading = loading,
            spinnerColor = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Low-emphasis action — "Lihat Semua", "Batal", inline links. */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        ButtonContent(
            text = text,
            icon = icon,
            loading = false,
            spinnerColor = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    loading: Boolean,
    spinnerColor: Color,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.iconMd),
            strokeWidth = 2.dp,
            color = spinnerColor,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.iconMd))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}
