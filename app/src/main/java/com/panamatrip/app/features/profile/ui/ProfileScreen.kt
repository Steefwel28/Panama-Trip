package com.panamatrip.app.features.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.TonalImage
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
import com.panamatrip.app.domain.passport.PassportBadge
import com.panamatrip.app.domain.passport.PassportCatalog
import com.panamatrip.app.domain.profile.OdometerTier
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfileRoute(
    container: com.panamatrip.app.di.AppContainer,
    onOpenDestination: (String) -> Unit,
    onExplore: () -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> uri?.let(viewModel::setAvatar) }
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> uri?.let(viewModel::setBanner) }

    ProfileScreen(
        state = state,
        onSelectTab = viewModel::selectTab,
        onToggleLike = viewModel::toggleLike,
        onOpenDestination = onOpenDestination,
        onSetPreferredVehicle = viewModel::setPreferredVehicle,
        onSetCashAlerts = viewModel::setCashAlerts,
        onRetry = viewModel::retry,
        onPickAvatar = {
            avatarPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onPickBanner = {
            bannerPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onExplore = onExplore,
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
    onPickAvatar: () -> Unit,
    onPickBanner: () -> Unit,
    onExplore: () -> Unit,
    savedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberPanamaHaptics()
    val content = state as? ProfileUiState.Content

    // Desenfoque nativo solo en el fondo, detrás del modal sólido. En API
    // 26–30 el modificador es no-op y queda el scrim oscuro del bottom sheet.
    val backgroundModifier = if (showSettings) {
        modifier.fillMaxSize().blur(BACKGROUND_BLUR)
    } else {
        modifier.fillMaxSize()
    }

    Column(modifier = backgroundModifier) {
        ProfileHeader(
            content = content,
            onOpenSettings = { showSettings = true },
            onPickAvatar = onPickAvatar,
            onPickBanner = onPickBanner,
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
                OdometerCard(
                    content = state,
                    modifier = Modifier.padding(
                        start = Dimens.cardPadding,
                        end = Dimens.cardPadding,
                        top = Dimens.itemGap,
                    ),
                )
                PassportCard(
                    passport = state.passport,
                    modifier = Modifier.padding(
                        start = Dimens.cardPadding,
                        end = Dimens.cardPadding,
                        top = Dimens.itemGap,
                    ),
                )
                EcosystemBar(
                    shares = state.ecosystems,
                    onClick = {
                        haptics.segment()
                        onExplore()
                    },
                    modifier = Modifier.padding(
                        start = Dimens.cardPadding,
                        end = Dimens.cardPadding,
                        top = Dimens.itemGap,
                    ),
                )
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
    content: ProfileUiState.Content?,
    onOpenSettings: () -> Unit,
    onPickAvatar: () -> Unit,
    onPickBanner: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(228.dp),
    ) {
        TonalImage(
            imageUrl = content?.bannerPath,
            contentDescription = stringResource(R.string.profile_banner_description),
            seed = "profile-banner",
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onPickBanner),
        )
        // Degradado sólido hacia la superficie: sin tarjetas translúcidas.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.45f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = Dimens.tightGap),
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = stringResource(R.string.profile_menu),
                tint = Color.White,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = Dimens.cardPadding, vertical = Dimens.itemGap),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = content?.userName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (content != null && content.userHandle.isNotBlank()) {
                    Text(
                        text = "@${content.userHandle}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (content != null && content.userBio.isNotBlank()) {
                    Text(
                        text = content.userBio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (content?.guestMode == true) {
                    TonalTag(
                        text = stringResource(R.string.profile_guest_mode),
                        tone = Tone.Neutral,
                    )
                }
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                TonalImage(
                    imageUrl = content?.avatarPath,
                    contentDescription = stringResource(R.string.profile_avatar_description),
                    seed = "profile-avatar",
                    corner = 48.dp,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape),
                )
                FilledIconButton(
                    onClick = onPickAvatar,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = PanamaIcons.Plus,
                        contentDescription = stringResource(R.string.profile_avatar_change),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Odómetro animado: solo distancia válida a pie de tracks completados. */
@Composable
private fun OdometerCard(
    content: ProfileUiState.Content,
    modifier: Modifier = Modifier,
) {
    val animatedKm by animateFloatAsState(
        targetValue = content.odometerMeters / METERS_PER_KILOMETER,
        animationSpec = tween(durationMillis = 900),
        label = "odometer",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Text(
                text = stringResource(R.string.profile_odometer_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", animatedKm),
                    style = MetricType.Large,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.profile_stat_kilometers),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                )
            }
            Text(
                text = stringResource(content.odometerTier.labelRes()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (content.metersToNextTier > 0) {
                    stringResource(
                        R.string.profile_odometer_next_tier,
                        distanceLabel(content.metersToNextTier),
                    )
                } else {
                    stringResource(R.string.profile_odometer_top_tier)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Pasaporte: 10 provincias + 6 comarcas e insignias de temporada. */
@Composable
private fun PassportCard(
    passport: PassportUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_passport_eyebrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = stringResource(R.string.profile_passport_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TonalTag(
                    text = stringResource(
                        R.string.profile_passport_progress,
                        passport.unlockedRegions,
                        passport.totalRegions,
                    ),
                    tone = Tone.Primary,
                )
            }
            PassportRegionGroup(
                title = stringResource(R.string.profile_passport_provinces),
                regions = PassportCatalog.provinces,
                unlocked = passport.unlockedRegionCodes,
            )
            PassportRegionGroup(
                title = stringResource(R.string.profile_passport_comarcas),
                regions = PassportCatalog.comarcas,
                unlocked = passport.unlockedRegionCodes,
            )
            Text(
                text = stringResource(R.string.profile_passport_badges),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PassportBadge.entries.forEach { badge ->
                    val unlocked = badge in passport.badges
                    TonalTag(
                        text = stringResource(badge.labelRes()),
                        tone = if (unlocked) Tone.Primary else Tone.Neutral,
                    )
                }
            }
        }
    }
}

@Composable
private fun PassportRegionGroup(
    title: String,
    regions: List<com.panamatrip.app.domain.passport.PassportRegion>,
    unlocked: Set<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            regions.forEach { region ->
                val isUnlocked = region.code in unlocked
                TonalTag(
                    text = if (isUnlocked) region.name else "· ${region.name}",
                    tone = if (isUnlocked) Tone.Primary else Tone.Neutral,
                )
            }
        }
    }
}

/** Barra de afinidad por ecosistema; tocarla lleva al explorador. */
@Composable
private fun EcosystemBar(
    shares: List<EcosystemShareUi>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (shares.isEmpty()) return
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primaryContainer,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Text(
                text = stringResource(R.string.profile_ecosystem_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                shares.forEachIndexed { index, share ->
                    Box(
                        modifier = Modifier
                            .weight(share.share.coerceAtLeast(MIN_ECOSYSTEM_SHARE))
                            .fillMaxHeight()
                            .background(colors[index % colors.size]),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                shares.forEach { share ->
                    Text(
                        text = stringResource(
                            R.string.profile_ecosystem_share,
                            share.ecosystem.displayName(),
                            (share.share * 100f).roundToInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val MIN_ECOSYSTEM_SHARE = 0.04f
private val BACKGROUND_BLUR = 16.dp

private fun OdometerTier.labelRes(): Int = when (this) {
    OdometerTier.PRIMER_PASO -> R.string.profile_tier_first_step
    OdometerTier.TROCHA_DE_BARRIO -> R.string.profile_tier_neighborhood
    OdometerTier.SENDERO_DE_SEMANA -> R.string.profile_tier_weekend
    OdometerTier.GUIA_DE_TROCHA -> R.string.profile_tier_guide
    OdometerTier.VETERANO_DEL_ISTMO -> R.string.profile_tier_veteran
    OdometerTier.A_PURO_PULMON -> R.string.profile_tier_gold
}

private fun PassportBadge.labelRes(): Int = when (this) {
    PassportBadge.FIRST_STEP -> R.string.profile_badge_first_step
    PassportBadge.A_PURO_PULMON -> R.string.profile_badge_gold
    PassportBadge.CAZADOR_DE_AMANECERES -> R.string.profile_badge_dawn
    PassportBadge.DOS_OCEANOS -> R.string.profile_badge_oceans
    PassportBadge.A_PRUEBA_DE_BAJAREQUE -> R.string.profile_badge_rain
}

private const val METERS_PER_KILOMETER = 1_000.0f

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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
