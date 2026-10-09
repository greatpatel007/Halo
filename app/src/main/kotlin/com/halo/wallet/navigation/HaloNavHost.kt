package com.halo.wallet.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.halo.core.designsystem.theme.HaloEasing
import com.halo.core.designsystem.theme.HaloFade
import com.halo.core.designsystem.theme.HaloSpring
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.feature.showroom.navigation.ShowroomHomeRoute
import com.halo.feature.showroom.navigation.navigateToShowroomPage
import com.halo.feature.showroom.navigation.showroomGraph

/**
 * App navigation. Transitions are spring-driven and seekable, so the system predictive-back gesture
 * scrubs the pop animation (scale 1 → 0.92 + slide) and settles on commit/cancel.
 */
@Composable
fun HaloNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    HandleNewIntents(navController)
    val motion = HaloTheme.motion
    val slide = motion.spring<IntOffset>(HaloSpring.Gentle, IntOffset(1, 1))
    val scale = motion.spring<Float>(HaloSpring.Gentle)
    NavHost(
        navController = navController,
        startDestination = ShowroomHomeRoute,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(slide) { it / 5 } +
                fadeIn(motion.fade(HaloFade.Base, HaloEasing.EmphasizedDecelerate))
        },
        exitTransition = {
            slideOutHorizontally(slide) { -it / 10 } +
                fadeOut(motion.fade(HaloFade.Fast, HaloEasing.EmphasizedAccelerate))
        },
        popEnterTransition = {
            slideInHorizontally(slide) { -it / 10 } +
                fadeIn(motion.fade(HaloFade.Base, HaloEasing.EmphasizedDecelerate))
        },
        popExitTransition = {
            scaleOut(scale, targetScale = 0.92f) +
                slideOutHorizontally(slide) { it / 5 } +
                fadeOut(motion.fade(HaloFade.Base, HaloEasing.EmphasizedAccelerate))
        },
        sizeTransform = null,
    ) {
        showroomGraph(
            onOpenPage = navController::navigateToShowroomPage,
            onBack = { navController.popBackStack() },
        )
    }
}

/**
 * Routes deep links that arrive while the (singleTask) activity is already running: navigate in place, single
 * top — so linking to the page that is already showing never stacks a duplicate (and never restarts the
 * activity, which would drop in-memory UI state).
 */
@Composable
private fun HandleNewIntents(navController: NavHostController) {
    val activity = LocalContext.current as? ComponentActivity ?: return
    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent ->
            val uri = intent.data ?: return@Consumer
            runCatching { navController.navigate(uri, navOptions { launchSingleTop = true }) }
        }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}
