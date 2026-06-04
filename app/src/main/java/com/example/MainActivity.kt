package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.ActiveScreen
import com.example.ui.DashboardScreens
import com.example.ui.EditorScreens
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CardViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private val viewModel: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        enableEdgeToEdge()
        setContent {
            val activeTheme by viewModel.activeTheme.collectAsState()
            val useDarkTheme = when (activeTheme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = useDarkTheme) {
                var currentScreen by remember { mutableStateOf(ActiveScreen.SPLASH) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        if (currentScreen == ActiveScreen.EDITOR) {
                            EditorScreens(
                                viewModel = viewModel,
                                onNavigate = { currentScreen = it }
                            )
                        } else {
                            DashboardScreens(
                                viewModel = viewModel,
                                currentScreen = currentScreen,
                                onNavigate = { currentScreen = it },
                                onOpenEditor = { currentScreen = ActiveScreen.EDITOR }
                            )
                        }
                    }
                }
            }
        }
    }
}
