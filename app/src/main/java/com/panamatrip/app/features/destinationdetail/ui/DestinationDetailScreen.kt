package com.panamatrip.app.features.destinationdetail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.InfoNotice
import com.panamatrip.app.core.designsystem.component.OverlayTag
import com.panamatrip.app.core.designsystem.component.PhotoHero
import com.panamatrip.app.core.designsystem.component.PrimaryActionButton
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.SectionHeader
import com.panamatrip.app.core.designsystem.component.StarRating
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.component.VehicleTrafficLightCard
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.ratingOverlayLabel
import com.panamatrip.app.core.designsystem.labels.walkLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.MetricType
import com.panamatrip.app.core.intents.MapIntents
import com.panamatrip.app.core.intents.launchFirstAvailable
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.DestinationReview

@Composable
fun DestinationDetailRoute(
    container: AppContainer,
    onBack: () -> Unit,
    onPlanRoute: (String) -> Unit,
) {
    val viewModel: DestinationDetailViewModel = viewModel(
        factory = DestinationDetailViewModel.factory(container),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DestinationDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onWaze = { destination ->
            context.launchFirstAvailable(MapIntents.waze(destination.parking))
        },
        onGoogleMaps = { destination ->
            context.launchFirstAvailable(MapIntents.googleMaps(destination.parking, destination.name))
        },
        onPlanRoute = { destination ->
            viewModel.rememberRoute(destination)
            onPlanRoute(destination.id)
        },
    )
}

@Composable
fun DestinationDetailScreen(
    state: DestinationDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onWaze: (Destination) -> Unit,
    onGoogleMaps: (Destination) -> Unit,
    onPlanRoute: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            DestinationDetailUiState.Loading -> ScreenLoading()
            is DestinationDetailUiState.Error -> ErrorState(
                title = stringResource(R.string.error_title),
                body = stringResource(R.string.error_body),
                retryLabel = stringResource(R.string.error_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            DestinationDetailUiState.Empty -> EmptyState(
                title = stringResource(R.string.detail_empty_title),
                body = stringResource(R.string.detail_empty_body),
                actionLabel = stringResource(R.string.detail_empty_action),
                onAction = onBack,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            is DestinationDetailUiState.Content -> DetailContent(
                destination = state.destination,
                reviews = state.reviews,
                onWaze = onWaze,
                onGoogleMaps = onGoogleMaps,
                onPlanRoute = onPlanRoute,
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(Dimens.tightGap),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = PanamaIcons.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    destination: Destination,
    reviews: List<DestinationReview>,
    onWaze: (Destination) -> Unit,
    onGoogleMaps: (Destination) -> Unit,
    onPlanRoute: (Destination) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Dimens.itemGap),
        ) {
            item(key = "hero") {
                DetailHero(destination = destination)
            }
            item(key = "intro") {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Dimens.cardPadding,
                        vertical = Dimens.itemGap,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
                ) {
                    Text(
                        text = destination.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                    ) {
                        TonalTag(text = destination.recommendedVehicle.displayName(), tone = Tone.Primary)
                        TonalTag(
                            text = walkLabel(destination.walkDistanceMeters, destination.walkMinutes),
                            tone = Tone.Neutral,
                        )
                        TonalTag(
                            text = elevationLabel(destination.elevationGainMeters),
                            tone = Tone.Neutral,
                            style = MetricType.Small,
                        )
                    }
                    InfoNotice(
                        text = destination.accessNotes,
                        icon = PanamaIcons.Pin,
                    )
                }
            }
            item(key = "access") {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Dimens.cardPadding,
                        vertical = Dimens.itemGap,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
                ) {
                    SectionHeader(
                        title = stringResource(R.string.detail_semaphore_title),
                        eyebrow = stringResource(R.string.detail_semaphore_eyebrow),
                        subtitle = stringResource(R.string.detail_semaphore_subtitle),
                    )
                    VehicleTrafficLightCard(summaries = destination.vehicleAccess)
                }
            }
            item(key = "coordinates") {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Dimens.cardPadding,
                        vertical = Dimens.itemGap,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
                ) {
                    SectionHeader(
                        title = stringResource(R.string.detail_arrive_title),
                        eyebrow = stringResource(R.string.detail_arrive_eyebrow),
                        subtitle = stringResource(R.string.detail_arrive_subtitle),
                    )
                    ParkingCard(
                        destination = destination,
                        onWaze = onWaze,
                        onGoogleMaps = onGoogleMaps,
                    )
                    AttractionCard(destination = destination)
                }
            }
            item(key = "reviews") {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Dimens.cardPadding,
                        vertical = Dimens.itemGap,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
                ) {
                    SectionHeader(
                        title = stringResource(R.string.detail_reviews_title),
                        eyebrow = stringResource(R.string.detail_reviews_eyebrow),
                    )
                    if (reviews.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_reviews_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        reviews.forEach { review -> ReviewCard(review) }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = Dimens.raisedElevation,
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        horizontal = Dimens.cardPadding,
                        vertical = Dimens.itemGap,
                    ),
            ) {
                PrimaryActionButton(
                    text = stringResource(R.string.action_plan_route),
                    onClick = { onPlanRoute(destination) },
                    icon = PanamaIcons.Route,
                )
            }
        }
    }
}

@Composable
private fun DetailHero(destination: Destination) {
    PhotoHero(
        imageUrl = destination.heroImageUrl,
        contentDescription = stringResource(R.string.photo_of, destination.name),
        height = Dimens.detailHeroHeight,
        seed = destination.id,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(Dimens.overlayPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            OverlayTag(text = destination.ecosystem.displayName())
            Text(
                text = destination.name,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Text(
                text = destination.province,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = PanamaIcons.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White,
                )
                Text(
                    text = ratingOverlayLabel(destination.averageRating, destination.reviewCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ParkingCard(
    destination: Destination,
    onWaze: (Destination) -> Unit,
    onGoogleMaps: (Destination) -> Unit,
) {
    CoordinateCard(
        title = stringResource(R.string.detail_parking_title),
        subtitle = stringResource(R.string.detail_parking_subtitle),
        coordinates = destination.parking.formatted,
        accent = true,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
            MapButton(
                text = stringResource(R.string.action_waze),
                onClick = { onWaze(destination) },
                modifier = Modifier.weight(1f),
            )
            MapButton(
                text = stringResource(R.string.action_google_maps),
                onClick = { onGoogleMaps(destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AttractionCard(destination: Destination) {
    CoordinateCard(
        title = stringResource(R.string.detail_attraction_title),
        subtitle = stringResource(R.string.detail_attraction_subtitle),
        coordinates = destination.attraction.formatted,
        accent = false,
    ) {
        Text(
            text = stringResource(
                R.string.detail_attraction_walk,
                distanceLabel(destination.walkDistanceMeters),
                destination.walkMinutes,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CoordinateCard(
    title: String,
    subtitle: String,
    coordinates: String,
    accent: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (accent) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = RoundedCornerShape(Dimens.innerCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PanamaIcons.Pin,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = coordinates,
                style = MetricType.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun MapButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.minimumTouchTarget),
        shape = RoundedCornerShape(Dimens.innerCorner),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ReviewCard(review: DestinationReview) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.innerCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = review.authorRole,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StarRating(rating = review.rating)
            }
            Text(
                text = review.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.detail_review_visit, review.visitLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
