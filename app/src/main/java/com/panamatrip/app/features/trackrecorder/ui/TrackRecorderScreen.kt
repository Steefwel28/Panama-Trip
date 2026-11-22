package com.panamatrip.app.features.trackrecorder.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.InfoNotice
import com.panamatrip.app.core.designsystem.component.PrimaryActionButton
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.SecondaryActionButton
import com.panamatrip.app.core.designsystem.component.SectionHeader
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.labels.durationLabel
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.speedLabel
import com.panamatrip.app.core.designsystem.labels.trackTimestampLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.MetricType
import com.panamatrip.app.core.ui.noticeTextRes
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.RecorderStatus

@Composable
fun TrackRecorderRoute(
    container: AppContainer,
    onBack: () -> Unit,
) {
    val viewModel: TrackRecorderViewModel = viewModel(
        factory = TrackRecorderViewModel.factory(container),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { notice ->
            snackbarHostState.showSnackbar(resources.getString(notice.noticeTextRes()))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionDenied = !granted
        if (granted) viewModel.start()
    }

    TrackRecorderScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        permissionDenied = permissionDenied,
        onBack = onBack,
        onStart = {
            if (context.hasLocationPermission()) {
                permissionDenied = false
                viewModel.start()
            } else {
                permissionLauncher.launch(
                    buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray(),
                )
            }
        },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onFinish = viewModel::finish,
        onDiscard = viewModel::discard,
        onRetry = viewModel::retry,
    )
}

@Composable
fun TrackRecorderScreen(
    state: TrackRecorderUiState,
    snackbarHostState: SnackbarHostState,
    permissionDenied: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TrackRecorderHeader(
                active = (state as? TrackRecorderUiState.Content)?.active,
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )

            when (state) {
                TrackRecorderUiState.Loading -> ScreenLoading()
                is TrackRecorderUiState.Error -> ErrorState(
                    title = stringResource(R.string.error_title),
                    body = stringResource(R.string.error_body),
                    retryLabel = stringResource(R.string.error_retry),
                    onRetry = onRetry,
                    modifier = Modifier.padding(top = Dimens.sectionGap),
                )
                is TrackRecorderUiState.Content -> TrackRecorderContent(
                    content = state,
                    permissionDenied = permissionDenied,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onFinish = onFinish,
                    onDiscard = { showDiscardDialog = true },
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.recorder_discard_title)) },
            text = { Text(stringResource(R.string.recorder_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDiscard()
                    },
                ) {
                    Text(stringResource(R.string.recorder_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TrackRecorderHeader(
    active: ActiveTrackUi?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = PanamaIcons.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                modifier = Modifier.size(Dimens.iconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.recorder_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.recorder_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (active != null) {
            TonalTag(
                text = stringResource(
                    if (active.status == RecorderStatus.RECORDING) {
                        R.string.recorder_status_recording
                    } else {
                        R.string.recorder_status_paused
                    },
                ),
                tone = if (active.status == RecorderStatus.RECORDING) {
                    Tone.Primary
                } else {
                    Tone.Tertiary
                },
            )
        }
    }
}

@Composable
private fun TrackRecorderContent(
    content: TrackRecorderUiState.Content,
    permissionDenied: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val active = content.active
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.cardPadding,
            end = Dimens.cardPadding,
            top = Dimens.tightGap,
            bottom = Dimens.sectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
    ) {
        if (permissionDenied) {
            item(key = "permission") {
                InfoNotice(text = stringResource(R.string.recorder_permission_needed))
            }
        }

        item(key = "status") {
            if (active == null) {
                RecorderIdleCard(onStart = onStart)
            } else {
                ActiveTrackCard(active = active)
            }
        }

        if (active != null) {
            item(key = "controls") {
                RecorderControls(
                    active = active,
                    onPause = onPause,
                    onResume = onResume,
                    onFinish = onFinish,
                    onDiscard = onDiscard,
                )
            }
            item(key = "background") {
                InfoNotice(text = stringResource(R.string.recorder_background_notice))
            }
        }

        item(key = "history_header") {
            SectionHeader(
                title = stringResource(R.string.recorder_history_title),
                eyebrow = stringResource(R.string.recorder_history_eyebrow),
            )
        }

        if (content.history.isEmpty()) {
            item(key = "history_empty") {
                EmptyState(
                    title = stringResource(R.string.recorder_history_empty_title),
                    body = stringResource(R.string.recorder_history_empty_body),
                    icon = PanamaIcons.Route,
                )
            }
        } else {
            items(items = content.history, key = { it.id }) { track ->
                RecordedTrackRow(track = track)
            }
        }
    }
}

@Composable
private fun RecorderIdleCard(onStart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Text(
                text = stringResource(R.string.recorder_intro_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.recorder_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryActionButton(
                text = stringResource(R.string.recorder_start),
                onClick = onStart,
                icon = PanamaIcons.Play,
            )
        }
    }
}

@Composable
private fun ActiveTrackCard(active: ActiveTrackUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Text(
                text = active.destinationName ?: stringResource(R.string.recorder_no_destination),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.recorder_started_at,
                    trackTimestampLabel(active.startedAtEpochMillis),
                    active.pointCount,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
                TrackStatTile(
                    label = stringResource(R.string.recorder_stat_distance),
                    value = distanceLabel(active.distanceMeters),
                    modifier = Modifier.weight(1f),
                )
                TrackStatTile(
                    label = stringResource(R.string.recorder_stat_time),
                    value = durationLabel(active.activeDurationMillis),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
                TrackStatTile(
                    label = stringResource(R.string.recorder_stat_speed),
                    value = speedLabel(active.averageSpeedKmh),
                    modifier = Modifier.weight(1f),
                )
                TrackStatTile(
                    label = stringResource(R.string.recorder_stat_elevation),
                    value = elevationLabel(active.elevationGainMeters),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrackStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(Dimens.innerCorner),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.tightGap),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MetricType.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RecorderControls(
    active: ActiveTrackUi,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
            SecondaryActionButton(
                text = stringResource(
                    if (active.status == RecorderStatus.RECORDING) {
                        R.string.recorder_pause
                    } else {
                        R.string.recorder_resume
                    },
                ),
                onClick = if (active.status == RecorderStatus.RECORDING) onPause else onResume,
                icon = if (active.status == RecorderStatus.RECORDING) {
                    PanamaIcons.Pause
                } else {
                    PanamaIcons.Play
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryActionButton(
                text = stringResource(R.string.recorder_finish),
                onClick = onFinish,
                icon = PanamaIcons.Check,
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = onDiscard, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.recorder_discard))
        }
    }
}

@Composable
private fun RecordedTrackRow(track: RecordedTrackUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.itemGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Icon(
                imageVector = PanamaIcons.Route,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = track.destinationName ?: stringResource(R.string.recorder_no_destination),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = trackTimestampLabel(track.startedAtEpochMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.recorder_history_meta,
                        distanceLabel(track.distanceMeters),
                        durationLabel(track.activeDurationMillis),
                    ),
                    style = MetricType.Small,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            TonalTag(
                text = elevationLabel(track.elevationGainMeters),
                tone = Tone.Secondary,
                style = MetricType.Small,
            )
        }
    }
}

private fun Context.hasLocationPermission(): Boolean {
    val fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}
