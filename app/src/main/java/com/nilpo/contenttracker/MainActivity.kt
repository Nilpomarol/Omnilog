package com.nilpo.contenttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.nilpo.contenttracker.ui.ContentTrackerApp
import com.nilpo.contenttracker.ui.home.HomeViewModel
import com.nilpo.contenttracker.ui.theme.ContentTrackerTheme

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModel.Factory(
            (application as ContentTrackerApplication).mediaRepository,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContentTrackerTheme {
                ContentTrackerApp(
                    viewModel = homeViewModel,
                )
            }
        }
    }
}
