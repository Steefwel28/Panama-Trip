package com.panamatrip.app.features.postcomposer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.ui.LocalSnackbarHostState
import com.panamatrip.app.core.ui.noticeTextRes
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.PostMediaType
import com.panamatrip.app.domain.photo.ExifCheck
import com.panamatrip.app.domain.photo.ExifVerification

@Composable
fun PostComposerRoute(
    container: AppContainer,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    initialTrackId: String? = null,
) {
    val viewModel: PostComposerViewModel = viewModel(
        factory = PostComposerViewModel.factory(container, initialTrackId),
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
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_MEDIA_ITEMS),
    ) { uris ->
        viewModel.addMedia(uris)
    }
    // ACCESS_MEDIA_LOCATION es permiso runtime: sin él MediaStore entrega la copia
    // redactada y el EXIF GPS desaparece. Si se niega, el picker sigue funcionando.
    val mediaLocationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        mediaPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
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
            mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
        }
    }

    PostComposerScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onSelectDestination = viewModel::selectDestination,
        onAddMedia = openPicker,
        onRemoveMedia = viewModel::removeMedia,
        onCaptionChange = viewModel::updateCaption,
        onSearchChange = viewModel::updateSearch,
        onSelectTrack = viewModel::selectTrack,
        onPublish = viewModel::publish,
    )
}

@Composable
fun PostComposerScreen(
    state: PostComposerUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectDestination: (String) -> Unit,
    onAddMedia: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onCaptionChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTrack: (String) -> Unit,
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
                    onAddMedia = onAddMedia,
                    onRemoveMedia = onRemoveMedia,
                    onCaptionChange = onCaptionChange,
                    onSearchChange = onSearchChange,
                    onSelectTrack = onSelectTrack,
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
    onAddMedia: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onCaptionChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectTrack: (String) -> Unit,
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
            DestinationSection(
                content = content,
                onSelectDestination = onSelectDestination,
                onSearchChange = onSearchChange,
            )
        }

        item(key = "media") {
            MediaCarouselCard(
                media = content.media,
                enabled = !content.isBusy,
                onAddMedia = onAddMedia,
                onRemoveMedia = onRemoveMedia,
            )
        }

        if (content.tracks.isNotEmpty()) {
            item(key = "track") {
                TrackSection(
                    content = content,
                    onSelectTrack = onSelectTrack,
                )
            }
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
private fun DestinationSection(
    content: PostComposerUiState.Content,
    onSelectDestination: (String) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
        Text(
            text = stringResource(R.string.composer_destination_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextField(
            value = content.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.composer_search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = PanamaIcons.Compass,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSize),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(Dimens.innerCorner),
        )
        // El chip cercano se destaca, pero nunca selecciona el destino solo.
        content.nearby?.let { nearby ->
            FilterChip(
                selected = content.selectedDestination?.id == nearby.destination.id,
                onClick = { onSelectDestination(nearby.destination.id) },
                label = {
                    Text(
                        text = stringResource(
                            R.string.composer_nearby_chip,
                            distanceLabel(nearby.distanceMeters),
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = PanamaIcons.Pin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
        if (content.destinations.isEmpty()) {
            Text(
                text = stringResource(R.string.composer_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content.destinations.forEach { destination ->
                    FilterChip(
                        selected = destination.id == content.selectedDestination?.id,
                        onClick = { onSelectDestination(destination.id) },
                        label = { Text(destination.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaCarouselCard(
    media: List<ComposerMediaUi>,
    enabled: Boolean,
    onAddMedia: () -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(vertical = Dimens.itemGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.cardPadding),
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
                    text = stringResource(R.string.composer_media_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (media.isNotEmpty()) {
                    TonalTag(
                        text = stringResource(R.string.composer_media_count, media.size),
                        tone = Tone.Neutral,
                    )
                }
            }

            if (media.isEmpty()) {
                Text(
                    text = stringResource(R.string.composer_media_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Dimens.cardPadding),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimens.cardPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                ) {
                    items(items = media, key = { it.id }) { item ->
                        MediaThumbnail(
                            item = item,
                            enabled = enabled,
                            onRemove = { onRemoveMedia(item.id) },
                        )
                    }
                }
                VerificationSummary(media = media)
            }

            SecondaryActionButton(
                text = stringResource(
                    if (media.isEmpty()) {
                        R.string.composer_media_add
                    } else {
                        R.string.composer_media_add_more
                    },
                ),
                onClick = onAddMedia,
                enabled = enabled && media.size < MAX_MEDIA_ITEMS,
                icon = PanamaIcons.Plus,
                modifier = Modifier.padding(horizontal = Dimens.cardPadding),
            )
        }
    }
}

@Composable
private fun MediaThumbnail(
    item: ComposerMediaUi,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(width = 132.dp, height = 176.dp)) {
        if (item.type == PostMediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = RoundedCornerShape(Dimens.innerCorner),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PanamaIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TonalImage(
                imageUrl = item.fileUri,
                contentDescription = stringResource(R.string.composer_media_selected),
                seed = item.id,
                corner = Dimens.innerCorner,
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
        ) {
            Icon(
                imageVector = PanamaIcons.Close,
                contentDescription = stringResource(R.string.composer_media_remove),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (item.type == PostMediaType.VIDEO) {
            TonalTag(
                text = stringResource(R.string.composer_media_video_tag),
                tone = Tone.Primary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun VerificationSummary(media: List<ComposerMediaUi>) {
    val photo = media.firstOrNull { it.type == PostMediaType.PHOTO && it.check != null }
        ?: return
    val check = photo.check ?: return
    Row(
        modifier = Modifier.padding(horizontal = Dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
    ) {
        VerificationTag(check = check)
        check.distanceMeters?.let { distance ->
            TonalTag(
                text = stringResource(
                    R.string.composer_distance_to_destination,
                    distanceLabel(distance),
                ),
                tone = Tone.Neutral,
            )
        }
    }
}

@Composable
private fun TrackSection(
    content: PostComposerUiState.Content,
    onSelectTrack: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
        Text(
            text = stringResource(R.string.composer_track_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.composer_track_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content.tracks.forEach { track ->
                FilterChip(
                    selected = content.linkedTrackId == track.id,
                    onClick = { onSelectTrack(track.id) },
                    label = {
                        Text(
                            text = stringResource(
                                R.string.composer_track_option,
                                track.name.ifBlank { stringResource(R.string.composer_track_unnamed) },
                                distanceLabel(track.distanceMeters),
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = PanamaIcons.Route,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
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

private const val MAX_MEDIA_ITEMS = 10
