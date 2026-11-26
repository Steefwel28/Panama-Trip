package com.panamatrip.app.data.photo

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Busca fotos y videos capturados entre el inicio y el fin de una grabación.
 * Así el compositor abre con el material real del recorrido y no con una
 * galería genérica. Sin permiso de lectura devuelve vacío: nunca rompe el flujo.
 */
class RecentMediaScanner(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun scan(
        startMillis: Long,
        endMillis: Long,
        limit: Int = MAX_RESULTS,
    ): List<Uri> = withContext(ioDispatcher) {
        if (endMillis <= startMillis) return@withContext emptyList()
        val startSeconds = (startMillis / MILLIS_PER_SECOND) - WINDOW_SLACK_SECONDS
        val endSeconds = (endMillis / MILLIS_PER_SECOND) + WINDOW_SLACK_SECONDS
        val args = arrayOf(startSeconds.toString(), endSeconds.toString())

        val images = query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, args, limit)
        val videos = query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, args, limit)
        (images + videos).take(limit)
    }

    private fun query(collection: Uri, args: Array<String>, limit: Int): List<Uri> {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = buildString {
            append(MediaStore.MediaColumns.DATE_ADDED)
            append(" BETWEEN ? AND ?")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append(" AND ")
                append(MediaStore.MediaColumns.IS_PENDING)
                append(" = 0")
            }
        }
        return runCatching {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                args,
                "${MediaStore.MediaColumns.DATE_ADDED} ASC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                buildList {
                    while (cursor.moveToNext() && size < limit) {
                        add(ContentUris.withAppendedId(collection, cursor.getLong(idColumn)))
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MAX_RESULTS = 10
        const val MILLIS_PER_SECOND = 1_000L
        const val WINDOW_SLACK_SECONDS = 5L
    }
}
