package com.nilpo.contenttracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nilpo.contenttracker.ui.ContentTrackerApp
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.settings.applyPendingAppIcon
import com.nilpo.contenttracker.ui.theme.ContentTrackerTheme
import com.nilpo.contenttracker.ui.theme.rememberThemePreference
import com.nilpo.contenttracker.ui.theme.rememberThemePreferences
import com.nilpo.contenttracker.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            mediaRepository = (application as ContentTrackerApplication).mediaRepository,
            metadataRepository = (application as ContentTrackerApplication).metadataRepository,
            recommendationRepository = (application as ContentTrackerApplication).recommendationRepository,
            coverRepository = (application as ContentTrackerApplication).coverRepository,
            malSyncManager = (application as ContentTrackerApplication).malSyncManager,
            importEnrichmentManager = (application as ContentTrackerApplication).importEnrichmentManager,
            metadataRefreshManager = (application as ContentTrackerApplication).metadataRefreshManager,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleMalOAuthIntent(intent)
        setContent {
            val themePreferences = rememberThemePreferences()
            val themePreference by rememberThemePreference(themePreferences)
            val darkTheme = themePreference.resolveDarkTheme()

            val view = LocalView.current
            LaunchedEffect(darkTheme) {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    // Dark surfaces need light (white) system-bar icons, and vice versa.
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            ContentTrackerTheme(darkTheme = darkTheme) {
                ContentTrackerApp(
                    viewModel = homeViewModel,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) applyPendingAppIcon(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMalOAuthIntent(intent)
    }

    private fun handleMalOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "omnilog" && uri.host == "mal-oauth") {
            homeViewModel.handleMalAuthorizationRedirect(uri)
        }
    }
}
