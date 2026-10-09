package com.halo.wallet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.halo.feature.showroom.debug.ShowroomDebugCommands
import com.halo.feature.showroom.debug.ShowroomDebugHost
import com.halo.wallet.navigation.HaloNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // A cold-start deep link carries FLAG_ACTIVITY_NEW_TASK, which makes NavController restart the
        // activity to rebuild the task. Strip it so the synthetic back stack is built in place.
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.let { launch ->
            intent = Intent(launch).apply { flags = flags and Intent.FLAG_ACTIVITY_NEW_TASK.inv() }
        }
        forwardParams(intent)
        setContent {
            // The theme switches in place (configChanges="uiMode"): re-style the system bars with it.
            val dark = isSystemInDarkTheme()
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            ShowroomDebugHost {
                HaloNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        forwardParams(intent)
    }

    /**
     * Tooling parameters on any deep link: `?theme=dark|light|system` (appearance) and
     * `?slowmo=5&grid=8&rm=1&log=0` (Showroom debug dock, see [ShowroomDebugCommands]).
     */
    private fun forwardParams(intent: Intent?) {
        val uri = intent?.data ?: return
        uri.getQueryParameter("theme")?.let(viewModel::applyThemeParam)
        ShowroomDebugCommands.submit(uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() })
    }
}
