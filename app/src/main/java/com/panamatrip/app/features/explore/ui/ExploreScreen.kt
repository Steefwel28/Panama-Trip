package com.panamatrip.app.features.explore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.ui.noticeTextRes
import com.panamatrip.app.core.ui.CollectionChoiceUi
import com.panamatrip.app.core.ui.CollectionPickerSheet
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.Ecosystem
import com.panamatrip.app.domain.model.TerrainStatus
import com.panamatrip.app.domain.model.VehicleType

@Composable
fun ExploreRoute(
    container: AppContainer,
    onOpenDestination: (String) -> Unit,
    onPlanRoute: (String) -> Unit,
    onOpenSaved: () -> Unit,
) {
    val viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { notice ->
            snackbarHostState.showSnackbar(context.getString(notice.noticeTextRes()))
        }
    }

    ExploreScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSelectTab = viewModel::selectTab,
        onToggleVehicle = viewModel::toggleVehicleFilter,
        onToggleEcosystem = viewModel::toggleEcosystemFilter,
        onToggleDistance = viewModel::toggleDistanceFilter,
        onClearFilters = viewModel::clearFilters,
        onRetry = viewModel::retry,
        onOpenDestination = onOpenDestination,
        onOpenSaved = onOpenSaved,
        onLike = viewModel::toggleLike,
        onOpenComments = viewModel::openComments,
        onCloseComments = viewModel::closeComments,
        onSubmitComment = viewModel::submitComment,
        onOpenSaveSheet = viewModel::openSaveSheet,
        onCloseSheets = viewModel::closeSheets,
        onToggleSave = viewModel::toggleSave,
        onCreateCollection = viewModel::createCollectionAndSave,
        onPlanRoute = { destination ->
            viewModel.rememberRoute(destination)
            onPlanRoute(destination.id)
        },
    )
}

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (ExploreTab) -> Unit,
    onToggleVehicle: (VehicleType) -> Unit,
    onToggleEcosystem: (Ecosystem) -> Unit,
    onToggleDistance: (Int) -> Unit,
    onClearFilters: () -> Unit,
    onRetry: () -> Unit,
    onOpenDestination: (String) -> Unit,
    onOpenSaved: () -> Unit,
    onLike: (CommunityPost) -> Unit,
    onOpenComments: (String) -> Unit,
    onCloseComments: () -> Unit,
    onSubmitComment: (String, String, TerrainStatus?, String) -> Unit,
    onOpenSaveSheet: (String) -> Unit,
    onCloseSheets: () -> Unit,
    onToggleSave: (String, CommunityPost) -> Unit,
    onCreateCollection: (String, CommunityPost) -> Unit,
    onPlanRoute: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ExploreHeader(
            guestMode = (state as? ExploreUiState.Content)?.guestMode ?: true,
            onOpenSaved = onOpenSaved,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        )

        when (state) {
            ExploreUiState.Loading -> ScreenLoading()
            is ExploreUiState.Error -> ErrorState(
                title = stringResource(R.string.error_title),
                body = stringResource(R.string.error_body),
                retryLabel = stringResource(R.string.error_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            is ExploreUiState.Empty -> EmptyExplore(
                tab = state.tab,
                onSelectTab = onSelectTab,
            )
            is ExploreUiState.Content -> {
                ExploreSegmentedControl(
                    selectedTab = state.tab,
                    onSelectTab = onSelectTab,
                    modifier = Modifier.padding(
                        start = Dimens.cardPadding,
                        end = Dimens.cardPadding,
                        top = Dimens.itemGap,
                        bottom = Dimens.itemGap,
                    ),
                )
                when (state.tab) {
                    ExploreTab.COMMUNITY -> CommunityFeed(
                        posts = state.posts,
                        onLike = onLike,
                        onOpenComments = onOpenComments,
                        onOpenSaveSheet = onOpenSaveSheet,
                        onPlanRoute = onPlanRoute,
                        onOpenDestination = onOpenDestination,
                        modifier = Modifier.weight(1f),
                    )
                    ExploreTab.DIRECTORY -> DirectoryContent(
                        entries = state.directory,
                        filters = state.filters,
                        onToggleVehicle = onToggleVehicle,
                        onToggleEcosystem = onToggleEcosystem,
                        onToggleDistance = onToggleDistance,
                        onClearFilters = onClearFilters,
                        onOpenDestination = onOpenDestination,
                        modifier = Modifier.weight(1f),
                    )
                }

                state.commentSheet?.let { sheet ->
                    CommentsSheet(
                        sheet = sheet,
                        onDismiss = onCloseComments,
                        onSubmit = onSubmitComment,
                    )
                }
                state.saveSheet?.let { sheet ->
                    CollectionPickerSheet(
                        destinationName = sheet.post.destination.name,
                        choices = sheet.collections.map { collection ->
                            CollectionChoiceUi(
                                id = collection.id,
                                name = collection.name,
                                symbol = collection.symbol,
                                postCount = collection.postCount,
                                selected = collection.containsPost,
                            )
                        },
                        onDismiss = onCloseSheets,
                        onToggle = { collectionId -> onToggleSave(collectionId, sheet.post) },
                        onCreate = { name -> onCreateCollection(name, sheet.post) },
                    )
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
private fun ExploreHeader(
    guestMode: Boolean,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.explore_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (guestMode) {
            TonalTag(
                text = stringResource(R.string.explore_guest_mode),
                tone = Tone.Neutral,
                modifier = Modifier.padding(end = Dimens.tightGap),
            )
        }
        IconButton(onClick = onOpenSaved) {
            Icon(
                imageVector = PanamaIcons.Bookmark,
                contentDescription = stringResource(R.string.nav_saved),
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyExplore(
    tab: ExploreTab,
    onSelectTab: (ExploreTab) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        ExploreSegmentedControl(
            selectedTab = tab,
            onSelectTab = onSelectTab,
            modifier = Modifier.padding(horizontal = Dimens.cardPadding),
        )
        EmptyState(
            title = stringResource(R.string.explore_empty_title),
            body = stringResource(R.string.explore_empty_body),
            actionLabel = stringResource(R.string.explore_empty_action),
            onAction = {
                onSelectTab(
                    if (tab == ExploreTab.COMMUNITY) ExploreTab.DIRECTORY else ExploreTab.COMMUNITY,
                )
            },
            modifier = Modifier.padding(top = Dimens.sectionGap),
        )
    }
}

@Composable
private fun ExploreSegmentedControl(
    selectedTab: ExploreTab,
    onSelectTab: (ExploreTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = ExploreTab.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, tab ->
            SegmentedButton(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = when (tab) {
                            ExploreTab.COMMUNITY -> stringResource(R.string.explore_tab_community)
                            ExploreTab.DIRECTORY -> stringResource(R.string.explore_tab_directory)
                        },
                    )
                },
            )
        }
    }
}
