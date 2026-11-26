package com.panamatrip.app.core.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.panamatrip.app.core.designsystem.theme.PanamaTripTheme
import com.panamatrip.app.features.destinationdetail.ui.DestinationDetailScreen
import com.panamatrip.app.features.explore.ui.ExploreScreen
import com.panamatrip.app.features.explore.ui.ExploreTab
import com.panamatrip.app.features.guides.ui.GuidesScreen
import com.panamatrip.app.features.postcomposer.ui.PostComposerScreen
import com.panamatrip.app.features.routeplan.ui.RoutePlanScreen
import com.panamatrip.app.features.saved.ui.SavedScreen
import com.panamatrip.app.features.trackrecorder.ui.TrackRecorderScreen

@Composable
private fun PreviewFrame(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    PanamaTripTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            content()
        }
    }
}

@Composable
private fun ExplorePreview(tab: ExploreTab) {
    ExploreScreen(
        state = PreviewFixtures.exploreState(tab),
        onSelectTab = {},
        onToggleVehicle = {},
        onToggleEcosystem = {},
        onToggleDistance = {},
        onClearFilters = {},
        onRetry = {},
        onOpenDestination = {},
        onOpenRecorder = {},
        onNewPost = {},
        onLike = {},
        onOpenComments = {},
        onCloseComments = {},
        onSubmitComment = { _, _, _, _ -> },
        onOpenSaveSheet = {},
        onCloseSheets = {},
        onToggleSave = { _, _ -> },
        onCreateCollection = { _, _ -> },
        onPlanRoute = {},
    )
}

@Preview(name = "Comunidad · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExploreCommunityLightPreview() {
    PreviewFrame(darkTheme = false) { ExplorePreview(ExploreTab.COMMUNITY) }
}

@Preview(
    name = "Comunidad · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ExploreCommunityDarkPreview() {
    PreviewFrame(darkTheme = true) { ExplorePreview(ExploreTab.COMMUNITY) }
}

@Preview(name = "Directorio · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExploreDirectoryLightPreview() {
    PreviewFrame(darkTheme = false) { ExplorePreview(ExploreTab.DIRECTORY) }
}

@Preview(
    name = "Directorio · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ExploreDirectoryDarkPreview() {
    PreviewFrame(darkTheme = true) { ExplorePreview(ExploreTab.DIRECTORY) }
}

@Composable
private fun DetailPreview() {
    DestinationDetailScreen(
        state = PreviewFixtures.detailState(),
        onBack = {},
        onRetry = {},
        onWaze = {},
        onGoogleMaps = {},
        onPlanRoute = {},
    )
}

@Preview(name = "Detalle · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun DetailLightPreview() {
    PreviewFrame(darkTheme = false) { DetailPreview() }
}

@Preview(
    name = "Detalle · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DetailDarkPreview() {
    PreviewFrame(darkTheme = true) { DetailPreview() }
}

@Composable
private fun SavedPreview() {
    SavedScreen(
        state = PreviewFixtures.savedState(),
        onSelectCollection = {},
        onCreateCollection = {},
        onUnsave = { _, _ -> },
        onRetry = {},
        onOpenDestination = {},
        onExplore = {},
    )
}

@Preview(name = "Guardados · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SavedLightPreview() {
    PreviewFrame(darkTheme = false) { SavedPreview() }
}

@Preview(
    name = "Guardados · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SavedDarkPreview() {
    PreviewFrame(darkTheme = true) { SavedPreview() }
}

@Composable
private fun RoutePreview() {
    RoutePlanScreen(
        state = PreviewFixtures.routeState(),
        onRetry = {},
        onExplore = {},
        onWaze = {},
        onGoogleMaps = {},
        onSelectFuel = {},
        onSetSurcharge = {},
        onSelectDeparture = {},
        onSelectOrigin = {},
        onSavePlan = {},
        onOpenRecorder = {},
    )
}

@Preview(name = "Mi Ruta · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RouteLightPreview() {
    PreviewFrame(darkTheme = false) { RoutePreview() }
}

@Preview(
    name = "Mi Ruta · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RouteDarkPreview() {
    PreviewFrame(darkTheme = true) { RoutePreview() }
}

@Composable
private fun GuidesPreview() {
    GuidesScreen(
        state = PreviewFixtures.guidesState(),
        onSelectDestination = {},
        onRetry = {},
        onWhatsApp = {},
    )
}

@Preview(name = "Guías · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GuidesLightPreview() {
    PreviewFrame(darkTheme = false) { GuidesPreview() }
}

@Preview(
    name = "Guías · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GuidesDarkPreview() {
    PreviewFrame(darkTheme = true) { GuidesPreview() }
}

@Composable
private fun RecorderPreview() {
    TrackRecorderScreen(
        state = PreviewFixtures.recorderState(),
        permissionDenied = false,
        onBack = {},
        onStart = {},
        onPause = {},
        onResume = {},
        onFinish = {},
        onDiscard = {},
        onRetry = {},
    )
}

@Preview(name = "Grabador · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RecorderLightPreview() {
    PreviewFrame(darkTheme = false) { RecorderPreview() }
}

@Preview(
    name = "Grabador · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RecorderDarkPreview() {
    PreviewFrame(darkTheme = true) { RecorderPreview() }
}

@Composable
private fun ComposerPreview() {
    PostComposerScreen(
        state = PreviewFixtures.composerState(),
        onBack = {},
        onRetry = {},
        onSelectDestination = {},
        onAddMedia = {},
        onRemoveMedia = {},
        onCaptionChange = {},
        onSearchChange = {},
        onSelectTrack = {},
        onPublish = {},
    )
}

@Preview(name = "Compositor · claro", showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ComposerLightPreview() {
    PreviewFrame(darkTheme = false) { ComposerPreview() }
}

@Preview(
    name = "Compositor · oscuro",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ComposerDarkPreview() {
    PreviewFrame(darkTheme = true) { ComposerPreview() }
}
