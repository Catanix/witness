package com.witness.mobile.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.witness.mobile.ui.screens.DetailScreen
import com.witness.mobile.ui.screens.HomeScreen
import com.witness.mobile.ui.screens.RecordingScreen
import com.witness.mobile.ui.screens.SettingsScreen

@Composable
fun WitnessNavHost(recordingTick: Int = 0) {
    val nav = rememberNavController()
    // тап по уведомлению записи -> экран записи
    LaunchedEffect(recordingTick) {
        if (recordingTick > 0) nav.navigate("recording") { launchSingleTop = true }
    }
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onRecord = { nav.navigate("recording") },
                onOpen = { id -> nav.navigate("detail/$id") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("recording") {
            RecordingScreen(onDone = { nav.popBackStack() })
        }
        composable("detail/{id}") { entry ->
            DetailScreen(id = entry.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
