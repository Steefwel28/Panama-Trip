package com.panamatrip.app.features.explore.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.icons.PanamaIcons

/**
 * Visor de video a pantalla completa con letterboxing nativo de VideoView.
 * El reproductor se libera en `onDispose`, así que cerrar con botón o Atrás
 * nunca deja audio corriendo en segundo plano.
 */
@Composable
fun PostVideoViewer(
    uri: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        var player by remember { mutableStateOf<VideoView?>(null) }
        var isPlaying by remember { mutableStateOf(true) }

        DisposableEffect(uri) {
            onDispose {
                player?.stopPlayback()
                player = null
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(uri))
                            setOnPreparedListener { media ->
                                media.isLooping = false
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener { isPlaying = false }
                            setOnErrorListener { _, _, _ ->
                                isPlaying = false
                                true
                            }
                            setBackgroundColor(android.graphics.Color.BLACK)
                            player = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.medium,
                        ),
                ) {
                    Icon(
                        imageVector = PanamaIcons.Close,
                        contentDescription = stringResource(R.string.video_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                FilledIconButton(
                    onClick = {
                        val view = player ?: return@FilledIconButton
                        if (view.isPlaying) {
                            view.pause()
                            isPlaying = false
                        } else {
                            view.start()
                            isPlaying = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) PanamaIcons.Pause else PanamaIcons.Play,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.video_pause else R.string.video_play,
                        ),
                    )
                }
            }
        }
    }
}
