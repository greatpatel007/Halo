package com.halo.feature.showroom.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.halo.feature.showroom.ShowroomHomeScreen
import com.halo.feature.showroom.ShowroomPageScreen
import kotlinx.serialization.Serializable

/** Showroom index. */
@Serializable
data object ShowroomHomeRoute

/** One catalog page. Deep link: `halo://showroom/{pageId}` (e.g. halo://showroom/color). */
@Serializable
data class ShowroomPageRoute(val pageId: String)

const val SHOWROOM_DEEP_LINK = "halo://showroom"

fun NavController.navigateToShowroomPage(pageId: String, navOptions: NavOptions? = null) =
    navigate(ShowroomPageRoute(pageId), navOptions)

fun NavGraphBuilder.showroomGraph(
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit,
) {
    composable<ShowroomHomeRoute> {
        ShowroomHomeScreen(onOpenPage = onOpenPage)
    }
    composable<ShowroomPageRoute>(
        deepLinks = listOf(navDeepLink<ShowroomPageRoute>(basePath = SHOWROOM_DEEP_LINK)),
    ) {
        ShowroomPageScreen(onBack = onBack)
    }
}
