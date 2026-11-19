package com.panamatrip.app.features.explore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.domain.model.PostComment
import com.panamatrip.app.domain.model.TerrainStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    sheet: CommentSheetUi,
    onDismiss: () -> Unit,
    onSubmit: (String, String, TerrainStatus?, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by rememberSaveable { mutableStateOf("") }
    var terrainName by rememberSaveable { mutableStateOf<String?>(null) }
    val terrain = terrainName?.let { runCatching { TerrainStatus.valueOf(it) }.getOrNull() }
    val guestAuthor = stringResource(R.string.guest_author)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Text(
                text = stringResource(R.string.comments_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = sheet.destinationName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.comments_sheet_quick_chips),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                TerrainStatus.entries.forEach { status ->
                    FilterChip(
                        selected = terrain == status,
                        onClick = {
                            terrainName = if (terrain == status) null else status.name
                        },
                        label = { Text(status.displayName()) },
                    )
                }
            }

            CommentList(comments = sheet.comments)

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
            ) {
                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.comments_sheet_placeholder)) },
                    shape = RoundedCornerShape(Dimens.innerCorner),
                    maxLines = 3,
                )
                Button(
                    onClick = {
                        onSubmit(sheet.postId, draft, terrain, guestAuthor)
                        draft = ""
                        terrainName = null
                    },
                    enabled = draft.isNotBlank() || terrain != null,
                    modifier = Modifier.heightIn(min = Dimens.minimumTouchTarget),
                ) {
                    Icon(
                        imageVector = PanamaIcons.Check,
                        contentDescription = stringResource(R.string.comments_sheet_send),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.size(Dimens.tightGap))
        }
    }
}

@Composable
private fun CommentList(comments: List<PostComment>) {
    if (comments.isEmpty()) {
        Text(
            text = stringResource(R.string.comments_sheet_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = Dimens.itemGap),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.heightIn(max = COMMENTS_MAX_HEIGHT),
        verticalArrangement = Arrangement.spacedBy(Dimens.tightGap),
    ) {
        items(items = comments, key = { it.id }) { comment ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    comment.terrainStatus?.let { status ->
                        Spacer(modifier = Modifier.width(Dimens.tightGap))
                        TonalTag(text = status.displayName(), tone = Tone.Tertiary)
                    }
                }
                if (comment.body.isNotBlank()) {
                    Text(
                        text = comment.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val COMMENTS_MAX_HEIGHT = 280.dp
