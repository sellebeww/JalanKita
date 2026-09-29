package org.umn.jalankita.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Report photos live as compressed JPEG files in app-private storage; Room only
 * ever stores the path. A pending report's file is never deleted before its
 * upload succeeds.
 */
class PhotoStorage(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }

    fun fileFor(reportId: String): File = File(directory, "$reportId.jpg")

    fun exists(path: String?): Boolean =
        path != null && File(path).let { it.exists() && it.length() > 0 }

    /**
     * Rotates to upright, scales the long edge down to [MAX_DIMENSION] and writes
     * JPEG at [JPEG_QUALITY] — enough detail to judge road damage, small enough to
     * upload on a weak connection.
     */
    suspend fun saveCompressed(
        jpegBytes: ByteArray,
        rotationDegrees: Int,
        reportId: String,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, decodeOptions)
                ?: error("Foto tidak dapat diproses.")

            val upright = decoded.rotated(rotationDegrees)
            val scaled = upright.scaledToFit(MAX_DIMENSION)

            val target = fileFor(reportId)
            FileOutputStream(target).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            if (scaled !== upright) upright.recycle()
            if (upright !== decoded) decoded.recycle()
            scaled.recycle()

            target
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / 2 >= MAX_DIMENSION * 2) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        if (degrees % 360 == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.scaledToFit(maxDimension: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxDimension) return this
        val ratio = maxDimension.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val DIRECTORY_NAME = "report_photos"
        const val MAX_DIMENSION = 1280
        const val JPEG_QUALITY = 80
    }
}
