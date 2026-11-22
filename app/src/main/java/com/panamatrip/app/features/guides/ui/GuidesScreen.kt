package com.panamatrip.app.features.guides.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.panamatrip.app.core.designsystem.component.TonalTag
import com.panamatrip.app.core.designsystem.component.Tone
import com.panamatrip.app.core.designsystem.icons.PanamaIcons
import com.panamatrip.app.core.designsystem.labels.displayName
import com.panamatrip.app.core.designsystem.labels.usdLabel
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.core.designsystem.theme.MetricType
import com.panamatrip.app.core.intents.WhatsAppIntents
import com.panamatrip.app.core.intents.launchFirstAvailable
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.model.LocalGuide
import kotlinx.coroutines.launch

@Composable
fun GuidesRoute(container: AppContainer) {
    val viewModel: GuidesViewModel = viewModel(factory = GuidesViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    GuidesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSelectDestination = viewModel::selectDestination,
        onRetry = viewModel::retry,
        onWhatsApp = { guide ->
            val message = resources.getString(
                R.string.guides_whatsapp_message,
                guide.name,
                guide.destinationName,
            )
            val launched = context.launchFirstAvailable(
                WhatsAppIntents.chat(guide.whatsappNumber, message),
            )
            if (!launched) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.guides_whatsapp_unavailable),
                    )
                }
            }
        },
    )
}

@Composable
fun GuidesScreen(
    state: GuidesUiState,
    snackbarHostState: SnackbarHostState,
    onSelectDestination: (String?) -> Unit,
    onRetry: () -> Unit,
    onWhatsApp: (LocalGuide) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GuidesHeader(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))

            when (state) {
                GuidesUiState.Loading -> ScreenLoading()
                is GuidesUiState.Error -> ErrorState(
                    title = stringResource(R.string.error_title),
                    body = stringResource(R.string.error_body),
                    retryLabel = stringResource(R.string.error_retry),
                    onRetry = onRetry,
                    modifier = Modifier.padding(top = Dimens.sectionGap),
                )
                GuidesUiState.Empty -> EmptyState(
                    title = stringResource(R.string.guides_empty_title),
                    body = stringResource(R.string.guides_empty_body),
                    modifier = Modifier.padding(top = Dimens.sectionGap),
                    icon = PanamaIcons.Verified,
                )
                is GuidesUiState.Content -> GuidesContent(
                    state = state,
                    onSelectDestination = onSelectDestination,
                    onWhatsApp = onWhatsApp,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun GuidesHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.itemGap),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.guides_eyebrow),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = stringResource(R.string.guides_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.guides_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GuidesContent(
    state: GuidesUiState.Content,
    onSelectDestination: (String?) -> Unit,
    onWhatsApp: (LocalGuide) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap),
        ) {
            item(key = "all") {
                FilterChip(
                    selected = state.selectedDestinationId == null,
                    onClick = { onSelectDestination(null) },
                    label = { Text(text = stringResource(R.string.guides_filter_all)) },
                )
            }
            items(items = state.filters, key = { it.id }) { filter ->
                FilterChip(
                    selected = state.selectedDestinationId == filter.id,
                    onClick = { onSelectDestination(filter.id) },
                    label = {
                        Text(
                            text = filter.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.itemGap))

        if (state.guides.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.guides_no_match_title),
                body = stringResource(R.string.guides_no_match_body),
                actionLabel = stringResource(R.string.guides_filter_all),
                onAction = { onSelectDestination(null) },
                icon = PanamaIcons.Verified,
            )
            return@Column
        }

        Text(
            text = if (state.selectedDestinationId == null) {
                stringResource(R.string.guides_result_count, state.guides.size)
            } else {
                stringResource(
                    R.string.guides_result_filtered,
                    state.guides.size,
                    state.totalCount,
                )
            },
            modifier = Modifier.padding(horizontal = Dimens.cardPadding),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimens.itemGap))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.cardPadding,
                end = Dimens.cardPadding,
                bottom = Dimens.sectionGap,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            items(items = state.guides, key = { it.id }) { guide ->
                GuideCard(
                    guide = guide,
                    onWhatsApp = { onWhatsApp(guide) },
                )
            }
        }
    }
}

@Composable
private fun GuideCard(
    guide: LocalGuide,
    onWhatsApp: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(Dimens.cardCorner),
        tonalElevation = Dimens.tonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                GuideMonogram(name = guide.name)
                Spacer(modifier = Modifier.width(Dimens.itemGap))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = guide.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (guide.verified) {
                        VerifiedBadge()
                    }
                }
            }

            Text(
                text = guide.headline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PanamaIcons.Pin,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.guides_destination_meta,
                        guide.destinationName,
                        guide.province,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.tightGap)) {
                guide.languages.forEach { language ->
                    TonalTag(text = language.displayName(), tone = Tone.Neutral)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = usdLabel(guide.rateUsdCents),
                        style = MetricType.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.guides_rate_suffix),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onWhatsApp,
                    modifier = Modifier.height(Dimens.minimumTouchTarget),
                    shape = RoundedCornerShape(Dimens.innerCorner),
                ) {
                    Icon(
                        imageVector = PanamaIcons.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.guides_whatsapp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideMonogram(name: String) {
    val initials = remember(name) {
        name.split(' ')
            .asSequence()
            .mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString(separator = "")
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Column(
            modifier = Modifier
                .size(52.dp)
                .padding(Dimens.tightGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun VerifiedBadge() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = PanamaIcons.Verified,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.guides_verified),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
