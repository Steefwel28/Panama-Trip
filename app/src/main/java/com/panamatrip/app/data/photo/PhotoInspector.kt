package com.panamatrip.app.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.panamatrip.app.domain.photo.ExifMetadata
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

/** Foto ya comprimida en almacenamiento interno junto a sus metadatos EXIF. */
data class PreparedPhoto(
    val fileUri: String,
    val metadata: ExifMetadata,
)

/**
 * Lee la doble llave EXIF (GPS + fecha original) antes de comprimir y guarda
 * una copia local apta para subida diferida. La compresión elimina el EXIF
 * físico, por lo que la verificación se persiste por separado en Room.
 */
class PhotoInspector(private val context: Context) {

    fun preparePhoto(uri: Uri): PreparedPhoto {
        val rawExif = readRawExif(uri)
        val compressedUri = compressToPendingUploads(uri, rawExif.orientation)
        return PreparedPhoto(fileUri = compressedUri, metadata = rawExif.metadata)
    }

    fun readExif(uri: Uri): ExifMetadata = readRawExif(uri).metadata

    private fun readRawExif(uri: Uri): RawExif {
        val exif = context.contentResolver.openInputStream(uri)?.use { stream ->
            runCatching { ExifInterface(stream) }.getOrNull()
        } ?: return RawExif(ExifMetadata(null, null, null), ExifInterface.ORIENTATION_NORMAL)

        val latitude = exif.gpsCoordinate(
            valueTag = ExifInterface.TAG_GPS_LATITUDE,
            refTag = ExifInterface.TAG_GPS_LATITUDE_REF,
            positiveRef = "N",
            negativeRef = "S",
        )
        val longitude = exif.gpsCoordinate(
            valueTag = ExifInterface.TAG_GPS_LONGITUDE,
            refTag = ExifInterface.TAG_GPS_LONGITUDE_REF,
            positiveRef = "E",
            negativeRef = "W",
        )
        val capturedAt = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?.trim('\u0000', ' ')
            ?.takeIf { it.isNotEmpty() }
            ?.let(::parseExifDate)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        return RawExif(
            metadata = ExifMetadata(
                latitude = latitude,
                longitude = longitude,
                capturedAtEpochMillis = capturedAt,
            ),
            orientation = orientation,
        )
    }

    private fun compressToPendingUploads(uri: Uri, orientation: Int): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "La imagen seleccionada no se pudo decodificar"
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("La imagen seleccionada no se pudo decodificar")

        val scaled = scaleDown(decoded, MAX_DIMENSION_PX)
        val oriented = applyOrientation(scaled, orientation)

        val directory = File(context.filesDir, PENDING_UPLOADS_DIR).apply { mkdirs() }
        val target = File(directory, "post-${UUID.randomUUID()}.jpg")
        FileOutputStream(target).use { output ->
            check(oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                "No se pudo comprimir la foto"
            }
        }
        if (oriented !== scaled) oriented.recycle()
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return Uri.fromFile(target).toString()
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / sample > MAX_DIMENSION_PX * SAMPLE_HEADROOM) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun ExifInterface.gpsCoordinate(
        valueTag: String,
        refTag: String,
        positiveRef: String,
        negativeRef: String,
    ): Double? {
        val value = getAttributeDouble(valueTag, Double.NaN)
        if (value.isNaN()) return null
        return when (getAttribute(refTag)?.trim()?.uppercase()) {
            negativeRef -> -abs(value)
            positiveRef -> abs(value)
            else -> value
        }
    }

    private fun parseExifDate(raw: String): Long? = runCatching {
        LocalDateTime.parse(raw, EXIF_DATE_FORMAT)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    private data class RawExif(
        val metadata: ExifMetadata,
        val orientation: Int,
    )

    private companion object {
        const val PENDING_UPLOADS_DIR = "pending_uploads"
        const val MAX_DIMENSION_PX = 1600
        const val SAMPLE_HEADROOM = 2
        const val JPEG_QUALITY = 80

        val EXIF_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
    }
}
