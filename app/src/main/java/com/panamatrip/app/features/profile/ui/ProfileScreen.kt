package com.panamatrip.app.features.profile.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.haptic.rememberPanamaHaptics
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.dateLabel
import com.panamatrip.app.core.designsystem.labels.distanceLabel
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.speedLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.MetricType
import com.panamatrip.app.core.designsystem.theme.PanamaMotion
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.VehicleType
import java.util.Locale

@Composable
fun ProfileRoute(
    container: com.panamatrip.app.di.AppContainer,
    onOpenDestination: (String) -> Unit,
    onExplore: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onSelectTab = viewModel::selectTab,
        onToggleLike = viewModel::toggleLike,
        onOpenDestination = onOpenDestination,
        onSetPreferredVehicle = viewModel::setPreferredVehicle,
        onSetCashAlerts = viewModel::setCashAlerts,
        onRetry = viewModel::retry,
        savedContent = {
            com.panamatrip.app.features.saved.ui.SavedRoute(
                container = container,
                onOpenDestination = onOpenDestination,
                onExplore = onExplore,
                showHeader = false,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onSelectTab: (ProfileTab) -> Unit,
    onToggleLike: (CommunityPost) -> Unit,
    onOpenDestination: (String) -> Unit,
    onSetPreferredVehicle: (VehicleType) -> Unit,
    onSetCashAlerts: (Boolean) -> Unit,
    onRetry: () -> Unit,
    savedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberPanamaHaptics()

    Column(modifier = modifier.fillMaxSize()) {
        ProfileHeader(
            guestMode = (state as? ProfileUiState.Content)?.guestMode ?: true,
            onOpenSettings = { showSettings = true },
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        )

        when (state) {
            ProfileUiState.Loading -> ScreenLoading()
            is ProfileUiState.Error -> ErrorState(
                title = stringResource(R.string.error_title),
                body = stringResource(R.string.error_body),
                retryLabel = stringResource(R.string.error_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            is ProfileUiState.Content -> {
                ExpeditionStatsCard(stats = state.stats)
                ProfileTabs(
                    selected = state.tab,
                    onSelect = { tab ->
                        haptics.segment()
                        onSelectTab(tab)
                    },
                )
                AnimatedContent(
                    targetState = state.tab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(FADE_MILLIS)) +
                                slideInHorizontally(
                                    animationSpec = PanamaMotion.spatial<IntOffset>(),
                                    initialOffsetX = { it / 12 },
                                )
                            ).togetherWith(fadeOut(animationSpec = tween(FADE_EXIT_MILLIS)))
                    },
                    label = "profile-tabs",
                ) { tab ->
                    when (tab) {
                        ProfileTab.POSTS -> PostList(
                            posts = state.posts,
                            emptyTitle = stringResource(R.string.profile_posts_empty_title),
                            emptyBody = stringResource(R.string.profile_posts_empty_body),
                            onToggleLike = onToggleLike,
                            onOpenDestination = onOpenDestination,
                        )
                        ProfileTab.LIKES -> PostList(
                            posts = state.likes,
                            emptyTitle = stringResource(R.string.profile_likes_empty_title),
                            emptyBody = stringResource(R.string.profile_likes_empty_body),
                            onToggleLike = onToggleLike,
                            onOpenDestination = onOpenDestination,
                        )
                        ProfileTab.ROUTES -> RouteList(routes = state.routes)
                        ProfileTab.SAVED -> savedContent()
                    }
                }
            }
        }
    }

    if (showSettings && state is ProfileUiState.Content) {
        SettingsSheet(
            preferredVehicle = state.preferredVehicle,
            cashAlertsEnabled = state.cashAlertsEnabled,
            onSetPreferredVehicle = onSetPreferredVehicle,
            onSetCashAlerts = onSetCashAlerts,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun ProfileHeader(
    guestMode: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = stringResource(R.string.profile_menu),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (guestMode) {
            TonalTag(
                text = stringResource(R.string.profile_guest_mode),
                tone = Tone.Neutral,
            )
        }
    }
}

@Composable
private fun ExpeditionStatsCard(stats: ExpeditionStats) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardPadding),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            StatCell(
                value = String.format(Locale.US, "%.1f", stats.totalKilometers),
                label = stringResource(R.string.profile_stat_kilometers),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = stats.elevationGainMeters.toString(),
                label = stringResource(R.string.profile_stat_elevation),
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = stats.completedRoutes.toString(),
                label = stringResource(R.string.profile_stat_routes),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MetricType.Large,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileTabs(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = Dimens.cardPadding,
        divider = {},
    ) {
        ProfileTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = tab.label(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun ProfileTab.label(): String = stringResource(
    when (this) {
        ProfileTab.POSTS -> R.string.profile_tab_posts
        ProfileTab.ROUTES -> R.string.profile_tab_routes
        ProfileTab.SAVED -> R.string.profile_tab_saved
        ProfileTab.LIKES -> R.string.profile_tab_likes
    },
)

@Composable
private fun PostList(
    posts: List<CommunityPost>,
    emptyTitle: String,
    emptyBody: String,
    onToggleLike: (CommunityPost) -> Unit,
    onOpenDestination: (String) -> Unit,
) {
    if (posts.isEmpty()) {
        EmptyState(title = emptyTitle, body = emptyBody)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = Dimens.cardPadding,
            end = Dimens.cardPadding,
            top = Dimens.itemGap,
            bottom = Dimens.sectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
    ) {
        items(posts, key = { it.id }) { post ->
            ProfilePostRow(
                post = post,
                onOpen = { onOpenDestination(post.destination.id) },
                onToggleLike = { onToggleLike(post) },
            )
        }
    }
}

@Composable
private fun ProfilePostRow(
    post: CommunityPost,
    onOpen: () -> Unit,
    onToggleLike: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.innerCorner),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.profile_likes_count, post.likeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (post.isLiked) PanamaIcons.Heart else PanamaIcons.HeartOutline,
                    contentDescription = stringResource(R.string.action_like),
                    tint = if (post.isLiked) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Icon(
                imageVector = PanamaIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteList(routes: List<ProfileRouteUi>) {
    if (routes.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.profile_routes_empty_title),
            body = stringResource(R.string.profile_routes_empty_body),
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            start = Dimens.cardPadding,
            end = Dimens.cardPadding,
            top = Dimens.itemGap,
            bottom = Dimens.sectionGap,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
    ) {
        items(routes, key = { it.id }) { route ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(Dimens.innerCorner),
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Text(
                        text = route.name.ifBlank {
                            stringResource(R.string.profile_route_free)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.profile_route_meta,
                            distanceLabel(route.distanceMeters),
                            dateLabel(route.finishedAtEpochMillis),
                        ),
                        style = MetricType.Small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
                        Text(
                            text = elevationLabel(route.elevationGainMeters),
                            style = MetricType.Small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = speedLabel(route.averageSpeedKmh),
                            style = MetricType.Small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    preferredVehicle: VehicleType?,
    cashAlertsEnabled: Boolean,
    onSetPreferredVehicle: (VehicleType) -> Unit,
    onSetCashAlerts: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val haptics = rememberPanamaHaptics()
        Column(
            modifier = Modifier.padding(
                start = Dimens.cardPadding,
                end = Dimens.cardPadding,
                bottom = Dimens.sectionGap,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_vehicle_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_vehicle_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val options = VehicleType.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, vehicle ->
                    SegmentedButton(
                        selected = preferredVehicle == vehicle,
                        onClick = {
                            haptics.segment()
                            onSetPreferredVehicle(vehicle)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(vehicle.displayName()) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.tightGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_cash_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_cash_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.itemGap))
                Switch(
                    checked = cashAlertsEnabled,
                    onCheckedChange = { enabled ->
                        haptics.toggle(enabled)
                        onSetCashAlerts(enabled)
                    },
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(Dimens.innerCorner),
            ) {
                Column(modifier = Modifier.padding(Dimens.itemGap)) {
                    Text(
                        text = stringResource(R.string.settings_offline_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_offline_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val FADE_MILLIS = 180
private const val FADE_EXIT_MILLIS = 90
