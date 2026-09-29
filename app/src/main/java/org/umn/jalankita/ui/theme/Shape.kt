package org.umn.jalankita.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One rounding ladder for the whole app.
 *
 * `small` is the button/input radius, `medium` the card radius, `large` the
 * radius for hero surfaces and sheets. Nothing should use a literal
 * RoundedCornerShape outside this file.
 */
val JalanKitaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Pill shape for chips and badges, where the ladder above would look boxy. */
val PillShape = RoundedCornerShape(percent = 50)
