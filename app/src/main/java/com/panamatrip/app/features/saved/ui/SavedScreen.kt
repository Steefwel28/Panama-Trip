package com.panamatrip.app.features.saved.ui

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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.ErrorState
import com.panamatrip.app.core.designsystem.component.ScreenLoading
import com.panamatrip.app.core.designsystem.component.TonalImage
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.monthYearLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.ui.noticeTextRes
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.CommunityPost

@Composable
fun SavedRoute(
    container: AppContainer,
    onOpenDestination: (String) -> Unit,
    onExplore: () -> Unit,
) {
    val viewModel: SavedViewModel = viewModel(factory = SavedViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { notice ->
            snackbarHostState.showSnackbar(resources.getString(notice.noticeTextRes()))
        }
    }

    SavedScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSelectCollection = viewModel::selectCollection,
        onCreateCollection = viewModel::createCollection,
        onUnsave = viewModel::unsave,
        onRetry = viewModel::retry,
        onOpenDestination = onOpenDestination,
        onExplore = onExplore,
    )
}

@Composable
fun SavedScreen(
    state: SavedUiState,
    snackbarHostState: SnackbarHostState,
    onSelectCollection: (String?) -> Unit,
    onCreateCollection: (String) -> Unit,
    onUnsave: (CommunityPost, String) -> Unit,
    onRetry: () -> Unit,
    onOpenDestination: (String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        SavedHeader(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            onCreateCollection = { showCreateDialog = true },
        )

        when (state) {
            SavedUiState.Loading -> ScreenLoading()
            is SavedUiState.Error -> ErrorState(
                title = stringResource(R.string.error_title),
                body = stringResource(R.string.error_body),
                retryLabel = stringResource(R.string.error_retry),
                onRetry = onRetry,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            SavedUiState.Empty -> EmptyState(
                title = stringResource(R.string.saved_empty_title),
                body = stringResource(R.string.saved_empty_body),
                actionLabel = stringResource(R.string.saved_empty_action),
                onAction = onExplore,
                modifier = Modifier.padding(top = Dimens.sectionGap),
            )
            is SavedUiState.Content -> SavedContent(
                state = state,
                onSelectCollection = onSelectCollection,
                onUnsave = onUnsave,
                onOpenDestination = onOpenDestination,
                onExplore = onExplore,
            )
        }
    }

    SnackbarHost(hostState = snackbarHostState)

    if (showCreateDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateCollection(name)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun SavedHeader(
    onCreateCollection: () -> Unit,
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
                text = stringResource(R.string.saved_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.saved_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onCreateCollection) {
            Icon(
                imageVector = PanamaIcons.Plus,
                contentDescription = stringResource(R.string.saved_new_collection),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SavedContent(
    state: SavedUiState.Content,
    onSelectCollection: (String?) -> Unit,
    onUnsave: (CommunityPost, String) -> Unit,
    onOpenDestination: (String) -> Unit,
    onExplore: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.collections.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dimens.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
            ) {
                items(items = state.collections, key = { it.id }) { collection ->
                    CollectionTile(
                        collection = collection,
                        onClick = { onSelectCollection(collection.id) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.itemGap))
        }

        if (!state.hasSavedPosts) {
            EmptyState(
                title = stringResource(R.string.saved_no_posts_title),
                body = stringResource(R.string.saved_no_posts_body),
                actionLabel = stringResource(R.string.saved_empty_action),
                onAction = onExplore,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Dimens.cardPadding,
                    end = Dimens.cardPadding,
                    bottom = Dimens.sectionGap,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
            ) {
                items(items = state.posts, key = { it.id }) { post ->
                    SavedPostCard(
                        post = post,
                        onOpen = { onOpenDestination(post.destination.id) },
                        onUnsave = {
                            val collectionId = state.selectedCollectionId
                                ?: post.savedCollectionIds.firstOrNull()
                            if (collectionId != null) onUnsave(post, collectionId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionTile(
    collection: SavedCollectionUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(148.dp)
            .clickable(onClick = onClick),
        color = if (collection.selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column {
            TonalImage(
                imageUrl = collection.previewImageUrl,
                contentDescription = null,
                seed = collection.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            )
            Column(modifier = Modifier.padding(Dimens.tightGap)) {
            Text(
                text = stringResource(
                    R.string.saved_collection_title,
                    collection.symbol,
                    collection.name,
                ),
                style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.saved_collection_count, collection.postCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SavedPostCard(
    post: CommunityPost,
    onOpen: () -> Unit,
    onUnsave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(Dimens.itemGap),
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            TonalImage(
                imageUrl = post.imageUrl,
                contentDescription = stringResource(R.string.photo_of, post.destination.name),
                seed = post.id,
                corner = Dimens.innerCorner,
                modifier = Modifier.size(92.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = post.destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = post.destination.province,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.saved_post_meta,
                        post.authorName,
                        monthYearLabel(post.visitedMonth, post.visitedYear),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            IconButton(onClick = onUnsave) {
                Icon(
                    imageVector = PanamaIcons.Bookmark,
                    contentDescription = stringResource(R.string.action_remove_saved),
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.saved_new_collection)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                Text(
                    text = stringResource(R.string.saved_new_collection_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.saved_new_collection_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(text = stringResource(R.string.saved_new_collection_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}
