package com.panamatrip.app.core.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.panamatrip.app.core.designsystem.theme.PanamaTripTheme
import com.panamatrip.app.features.destinationdetail.ui.DestinationDetailScreen
import com.panamatrip.app.features.explore.ui.ExploreScreen
import com.panamatrip.app.features.explore.ui.ExploreTab
import com.panamatrip.app.features.guides.ui.GuidesScreen
import com.panamatrip.app.features.routeplan.ui.RoutePlanScreen
import com.panamatrip.app.features.saved.ui.SavedScreen

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
        snackbarHostState = remember { SnackbarHostState() },
        onSelectTab = {},
        onToggleVehicle = {},
        onToggleEcosystem = {},
        onToggleDistance = {},
        onClearFilters = {},
        onRetry = {},
        onOpenDestination = {},
        onOpenSaved = {},
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
        snackbarHostState = remember { SnackbarHostState() },
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
        snackbarHostState = remember { SnackbarHostState() },
        onRetry = {},
        onExplore = {},
        onWaze = {},
        onGoogleMaps = {},
        onSelectFuel = {},
        onSetSurcharge = {},
        onSelectDeparture = {},
        onSavePlan = {},
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
        snackbarHostState = remember { SnackbarHostState() },
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
