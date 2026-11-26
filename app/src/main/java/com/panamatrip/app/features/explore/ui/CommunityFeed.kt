package com.panamatrip.app.features.explore.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.EmptyState
import com.panamatrip.app.core.designsystem.component.MetricCapsule
import com.panamatrip.app.core.designsystem.component.MetricItem
import com.panamatrip.app.core.designsystem.component.OverlayTag
import com.panamatrip.app.core.designsystem.component.SocialAction
import com.panamatrip.app.core.designsystem.component.TonalImage
import com.panamatrip.app.core.designsystem.haptic.rememberPanamaHaptics
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.commentsLabel
import com.panamatrip.app.core.designsystem.labels.distanceFromYouLabel
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.elevationLabel
import com.panamatrip.app.core.designsystem.labels.likesLabel
import com.panamatrip.app.core.designsystem.labels.monthYearLabel
import com.panamatrip.app.core.designsystem.labels.walkLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.PanamaMotion
import com.panamatrip.app.domain.model.CommunityPost
import com.panamatrip.app.domain.model.Destination
import com.panamatrip.app.domain.model.PostMediaType

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
    val haptics = rememberPanamaHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = PanamaMotion.spatial()),
    ) {
        PostMediaHero(
            post = post,
            contentDescription = stringResource(R.string.photo_of, destination.name),
            onClickLabel = openDetailLabel,
            onOpenDetail = { onOpenDestination(destination.id) },
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
                OverlayTag(text = distanceFromYouLabel(item.distanceMeters))
                post.activityTag?.let { tag -> OverlayTag(text = tag) }
                if (post.photoVerified) {
                    OverlayTag(text = stringResource(R.string.post_verified_on_site))
                }
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
                            numeric = true,
                        ),
                        MetricItem(
                            label = stringResource(R.string.metric_elevation),
                            value = elevationLabel(destination.elevationGainMeters),
                            numeric = true,
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
                    onClick = {
                        haptics.toggle(!post.isLiked)
                        onLike(post)
                    },
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
                    onClick = {
                        haptics.segment()
                        onOpenSaveSheet(post.id)
                    },
                    selected = post.savedCollectionIds.isNotEmpty(),
                    selectedTint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                PlanRouteButton(onClick = { onPlanRoute(destination) })
            }
        }
    }
}

/**
 * Portada del post: foto en 1:1 o 3:4 según sus dimensiones reales; video en
 * 3:4 con botón play que abre el visor a pantalla completa.
 */
@Composable
private fun PostMediaHero(
    post: CommunityPost,
    contentDescription: String?,
    onClickLabel: String,
    onOpenDetail: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var videoUri by remember(post.id) { mutableStateOf<String?>(null) }
    val cover = post.media.firstOrNull()
    val isVideo = cover?.type == PostMediaType.VIDEO
    val aspect = when {
        isVideo -> 3f / 4f
        cover == null || cover.width <= 0 || cover.height <= 0 -> 1f
        cover.width >= cover.height -> 1f
        else -> 3f / 4f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(0.dp))
            .clickable(onClickLabel = onClickLabel) {
                if (isVideo) videoUri = cover?.uri else onOpenDetail()
            },
    ) {
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PanamaIcons.Play,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            TonalImage(
                imageUrl = cover?.uri ?: post.imageUrl,
                contentDescription = contentDescription,
                seed = post.id,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.45f),
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
                ),
        )

        content()

        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = PanamaIcons.Play,
                    contentDescription = stringResource(R.string.video_play),
                    modifier = Modifier.size(32.dp),
                    tint = Color.White,
                )
            }
        }
    }

    videoUri?.let { uri ->
        PostVideoViewer(uri = uri, onDismiss = { videoUri = null })
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
