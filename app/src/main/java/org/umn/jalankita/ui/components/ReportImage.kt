package org.umn.jalankita.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.umn.jalankita.model.Report
import java.io.File

/**
 * The local file wins when it is still on disk: it works offline and avoids a
 * round trip. The uploaded copy is the fallback.
 */
fun Report.imageModel(): Any? {
    val local = photoPath?.let(::File)?.takeIf { it.exists() && it.length() > 0 }
    return local?.let(Uri::fromFile) ?: photoRemoteUrl?.takeIf { it.isNotBlank() }
}

/**
 * A report's picture.
 *
 * When there is no photo — a sample report, or a photo that has been cleaned up —
 * the drawn example for that category is shown instead of a broken-image icon, so
 * a list of reports still reads at a glance.
 */
@Composable
fun ReportImage(
    report: Report,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
) {
    val model = report.imageModel()
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "Foto kerusakan ${report.category.label}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            DamageIllustration(
                category = report.category,
                shape = shape,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
