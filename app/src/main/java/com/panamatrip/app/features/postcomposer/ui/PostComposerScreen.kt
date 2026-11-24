package com.panamatrip.app.features.postcomposer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.InfoNotice
import com.panamatrip.app.core.designsystem.component.PrimaryActionButton
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.SecondaryActionButton
import com.panamatrip.app.core.designsystem.component.TonalImage
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.dateLabel
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.ui.LocalSnackbarHostState
import com.panamatrip.app.core.ui.noticeTextRes
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifVerification

@Composable
fun PostComposerRoute(
    container: AppContainer,
    onBack: () -> Unit,
    onPublished: () -> Unit,
) {
    val viewModel: PostComposerViewModel = viewModel(
        factory = PostComposerViewModel.factory(container),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackbarHostState.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { notice ->
            snackbarHostState.showSnackbar(resources.getString(notice.noticeTextRes()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event == PostComposerEvent.PUBLISHED) onPublished()
        }
    }

    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(viewModel::selectPhoto)
    }
    // ACCESS_MEDIA_LOCATION es permiso runtime: sin él MediaStore entrega la copia
    // redactada y el EXIF GPS desaparece. Si se niega, el picker sigue funcionando.
    val mediaLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
    val openPicker = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            mediaLocationPermission.launch(Manifest.permission.ACCESS_MEDIA_LOCATION)
        } else {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    PostComposerScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onSelectDestination = viewModel::selectDestination,
        onPickPhoto = openPicker,
        onCaptionChange = viewModel::updateCaption,
        onPublish = viewModel::publish,
    )
}

@Composable
fun PostComposerScreen(
    state: PostComposerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectDestination: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onPublish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ComposerHeader(
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )

            when (state) {
                PostComposerUiState.Loading -> ScreenLoading()
                is PostComposerUiState.Error -> ErrorState(
                    title = stringResource(R.string.error_title),
                    body = stringResource(R.string.error_body),
                    retryLabel = stringResource(R.string.error_retry),
                    onRetry = onRetry,
                    modifier = Modifier.padding(top = Dimens.sectionGap),
                )
                PostComposerUiState.Empty -> EmptyState(
                    title = stringResource(R.string.composer_empty_title),
                    body = stringResource(R.string.composer_empty_body),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                    modifier = Modifier.padding(top = Dimens.sectionGap),
                )
                is PostComposerUiState.Content -> ComposerContent(
                    content = state,
                    onSelectDestination = onSelectDestination,
                    onPickPhoto = onPickPhoto,
                    onCaptionChange = onCaptionChange,
                    onPublish = onPublish,
                )
            }
        }

    }
}

@Composable
private fun ComposerHeader(
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
                text = stringResource(R.string.composer_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.composer_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ComposerContent(
    content: PostComposerUiState.Content,
    onSelectDestination: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onPublish: () -> Unit,
) {
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
        if (content.isBusy) {
            item(key = "busy") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item(key = "intro") {
            InfoNotice(
                text = stringResource(R.string.composer_intro),
                icon = PanamaIcons.Camera,
            )
        }

        item(key = "destination") {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                Text(
                    text = stringResource(R.string.composer_destination_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                DestinationChips(
                    destinations = content.destinations,
                    selectedDestination = content.selectedDestination,
                    onSelectDestination = onSelectDestination,
                )
            }
        }

        item(key = "photo") {
            PhotoPickerCard(
                photo = content.photo,
                enabled = !content.isBusy,
                onPickPhoto = onPickPhoto,
            )
        }

        item(key = "caption") {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                Text(
                    text = stringResource(R.string.composer_caption_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextField(
                    value = content.caption,
                    onValueChange = onCaptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.composer_caption_placeholder)) },
                    shape = RoundedCornerShape(Dimens.innerCorner),
                    minLines = 3,
                    maxLines = 6,
                )
            }
        }

        item(key = "publish") {
            PrimaryActionButton(
                text = stringResource(R.string.composer_publish),
                onClick = onPublish,
                enabled = content.canPublish,
                icon = PanamaIcons.Check,
            )
        }

        item(key = "hint") {
            InfoNotice(
                text = stringResource(R.string.composer_publish_hint),
                icon = PanamaIcons.Verified,
            )
        }
    }
}

@Composable
private fun DestinationChips(
    destinations: List<Destination>,
    selectedDestination: Destination?,
    onSelectDestination: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        destinations.forEach { destination ->
            FilterChip(
                selected = destination.id == selectedDestination?.id,
                onClick = { onSelectDestination(destination.id) },
                label = { Text(destination.name) },
            )
        }
    }
}

@Composable
private fun PhotoPickerCard(
    photo: SelectedPhotoUi?,
    enabled: Boolean,
    onPickPhoto: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
            if (photo != null) {
                TonalImage(
                    imageUrl = photo.fileUri,
                    contentDescription = stringResource(R.string.composer_photo_selected),
                    seed = photo.fileUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PHOTO_HEIGHT),
                )
            }
            Column(
                modifier = Modifier.padding(
                    start = Dimens.cardPadding,
                    end = Dimens.cardPadding,
                    bottom = Dimens.cardPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                ) {
                    Icon(
                        imageVector = PanamaIcons.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.composer_photo_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (photo == null) {
                    Text(
                        text = stringResource(R.string.composer_photo_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    VerificationDetails(photo = photo)
                }
                SecondaryActionButton(
                    text = stringResource(
                        if (photo == null) {
                            R.string.composer_photo_pick
                        } else {
                            R.string.composer_photo_change
                        },
                    ),
                    onClick = onPickPhoto,
                    enabled = enabled,
                    icon = PanamaIcons.Camera,
                )
            }
        }
    }
}

@Composable
private fun VerificationDetails(photo: SelectedPhotoUi) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
    ) {
        VerificationTag(check = photo.check)
        photo.check.distanceMeters?.let { distance ->
            TonalTag(
                text = stringResource(
                    R.string.composer_distance_to_destination,
                    distanceLabel(distance),
                ),
                tone = Tone.Neutral,
            )
        }
    }
    photo.metadata.capturedAtEpochMillis?.let { capturedAt ->
        Text(
            text = stringResource(R.string.composer_exif_date, dateLabel(capturedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VerificationTag(check: ExifCheck) {
    val textRes = when (check.verification) {
        ExifVerification.VERIFIED_ON_SITE -> R.string.post_verified_on_site
        ExifVerification.OUTSIDE_RADIUS -> R.string.post_outside_radius
        ExifVerification.NO_GPS -> R.string.post_no_gps_verification
    }
    val tone = when (check.verification) {
        ExifVerification.VERIFIED_ON_SITE -> Tone.Primary
        ExifVerification.OUTSIDE_RADIUS -> Tone.Tertiary
        ExifVerification.NO_GPS -> Tone.Neutral
    }
    TonalTag(text = stringResource(textRes), tone = tone)
}

private val PHOTO_HEIGHT = 220.dp
