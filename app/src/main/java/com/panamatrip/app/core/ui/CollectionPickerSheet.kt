package com.panamatrip.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.theme.Dimens

/** Colección disponible en el selector de guardado. */
data class CollectionChoiceUi(
    val id: String,
    val name: String,
    val symbol: String,
    val postCount: Int,
    val selected: Boolean,
)

/** Selector compartido de colecciones (feed y detalle de destino). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPickerSheet(
    destinationName: String,
    choices: List<CollectionChoiceUi>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newCollectionName by rememberSaveable { mutableStateOf("") }

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
                text = stringResource(R.string.save_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = destinationName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            choices.forEach { choice ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(choice.id) },
                    color = if (choice.selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = RoundedCornerShape(Dimens.innerCorner),
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.itemGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = choice.symbol, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(Dimens.tightGap))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = choice.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.save_sheet_post_count, choice.postCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (choice.selected) {
                            Icon(
                                imageVector = PanamaIcons.Check,
                                contentDescription = stringResource(R.string.save_sheet_saved),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            TextField(
                value = newCollectionName,
                onValueChange = { newCollectionName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.save_sheet_new_collection_placeholder)) },
                shape = RoundedCornerShape(Dimens.innerCorner),
                singleLine = true,
            )
            TextButton(
                onClick = {
                    onCreate(newCollectionName)
                    newCollectionName = ""
                },
                enabled = newCollectionName.isNotBlank(),
            ) {
                Text(text = stringResource(R.string.save_sheet_create))
            }
            Spacer(modifier = Modifier.size(Dimens.tightGap))
        }
    }
}
