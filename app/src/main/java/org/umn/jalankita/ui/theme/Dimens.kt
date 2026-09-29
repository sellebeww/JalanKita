package org.umn.jalankita.ui.theme

import androidx.compose.ui.unit.dp

/**
 * The spacing scale. Every gap and padding in the app is one of these values, so
 * unrelated screens end up on the same rhythm instead of each inventing its own.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
}

/** Component sizes that must agree across screens. */
object Dimens {
    /** Horizontal gutter for every screen's content. */
    val screenPadding = 20.dp

    /** Primary call-to-action height; secondary buttons sit one step below. */
    val buttonHeight = 54.dp
    val buttonHeightCompact = 46.dp

    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val iconXl = 32.dp

    val avatarSm = 40.dp
    val avatarMd = 56.dp
    val avatarLg = 88.dp

    val thumbnail = 78.dp
    val heroImage = 200.dp
    val mapPreview = 176.dp
    val mapDetail = 170.dp

    val cardElevation = 0.dp
    val raisedElevation = 6.dp
}
