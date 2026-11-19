package com.panamatrip.app.features.explore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.MetricCapsule
import com.panamatrip.app.core.designsystem.component.MetricItem
import com.panamatrip.app.core.designsystem.component.OverlayTag
import com.panamatrip.app.core.designsystem.component.PhotoHero
import com.panamatrip.app.core.designsystem.component.SocialAction
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.commentsLabel
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.likesLabel
import com.panamatrip.app.core.designsystem.labels.monthYearLabel
import com.panamatrip.app.core.designsystem.labels.walkLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination

@Composable
fun CommunityFeed(
    posts: List<FeedPostUi>,
    onLike: (CommunityPost) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenSaveSheet: (String) -> Unit,
    onPlanRoute: (Destination) -> Unit,
    onOpenDestination: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (posts.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.feed_empty_title),
            body = stringResource(R.string.feed_empty_body),
            modifier = modifier,
            icon = PanamaIcons.Chat,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap),
    ) {
        items(items = posts, key = { it.post.id }) { item ->
            CommunityPostCard(
                item = item,
                onLike = onLike,
                onOpenComments = onOpenComments,
                onOpenSaveSheet = onOpenSaveSheet,
                onPlanRoute = onPlanRoute,
                onOpenDestination = onOpenDestination,
            )
        }
    }
}

@Composable
private fun CommunityPostCard(
    item: FeedPostUi,
    onLike: (CommunityPost) -> Unit,
    onOpenComments: (String) -> Unit,
    onOpenSaveSheet: (String) -> Unit,
    onPlanRoute: (Destination) -> Unit,
    onOpenDestination: (String) -> Unit,
) {
    val post = item.post
    val destination = post.destination
    val openDetailLabel = stringResource(R.string.action_open_detail)

    Column(modifier = Modifier.fillMaxWidth()) {
        PhotoHero(
            imageUrl = post.imageUrl,
            contentDescription = stringResource(R.string.photo_of, destination.name),
            height = Dimens.feedHeroHeight,
            seed = post.id,
            modifier = Modifier.clickable(
                onClickLabel = openDetailLabel,
                onClick = { onOpenDestination(destination.id) },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.overlayPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
            ) {
                AuthorBadge(initials = post.authorInitials)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        text = post.authorRole,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }
                OverlayTag(text = monthYearLabel(post.visitedMonth, post.visitedYear))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(Dimens.overlayPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
            ) {
                post.activityTag?.let { tag -> OverlayTag(text = tag) }
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(
                        R.string.destination_meta,
                        destination.province,
                        destination.ecosystem.displayName(),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                MetricCapsule(
                    items = listOf(
                        MetricItem(
                            label = stringResource(R.string.metric_vehicle),
                            value = post.vehicleUsed.displayName(),
                        ),
                        MetricItem(
                            label = stringResource(R.string.metric_walk),
                            value = walkLabel(post.walkDistanceMeters, post.walkMinutes),
                        ),
                        MetricItem(
                            label = stringResource(R.string.metric_elevation),
                            value = elevationLabel(destination.elevationGainMeters),
                        ),
                    ),
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = Dimens.overlayPadding, vertical = Dimens.itemGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = CAPTION_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SocialAction(
                    icon = if (post.isLiked) PanamaIcons.Heart else PanamaIcons.HeartOutline,
                    contentDescription = stringResource(R.string.action_like),
                    onClick = { onLike(post) },
                    label = likesLabel(post.likeCount),
                    selected = post.isLiked,
                )
                SocialAction(
                    icon = PanamaIcons.Chat,
                    contentDescription = stringResource(R.string.action_comment),
                    onClick = { onOpenComments(post.id) },
                    label = commentsLabel(post.commentCount),
                )
                SocialAction(
                    icon = if (post.savedCollectionIds.isEmpty()) {
                        PanamaIcons.BookmarkOutline
                    } else {
                        PanamaIcons.Bookmark
                    },
                    contentDescription = stringResource(R.string.action_save),
                    onClick = { onOpenSaveSheet(post.id) },
                    selected = post.savedCollectionIds.isNotEmpty(),
                    selectedTint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                PlanRouteButton(onClick = { onPlanRoute(destination) })
            }
        }
    }
}

@Composable
private fun AuthorBadge(initials: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun PlanRouteButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.innerCorner),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = PanamaIcons.Route,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.action_plan_route),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private const val CAPTION_MAX_LINES = 4
