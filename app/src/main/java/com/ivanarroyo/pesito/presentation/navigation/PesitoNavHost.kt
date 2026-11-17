package com.ivanarroyo.pesito.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ivanarroyo.pesito.presentation.converter.ConverterRoute
import com.ivanarroyo.pesito.presentation.history.HistoryRoute
import kotlinx.serialization.Serializable

/** Rutas con tipos: si falta un argumento, el error sale al compilar y no con la app abierta. */
@Serializable
data object ConverterDestination

@Serializable
data class HistoryDestination(val from: String, val to: String)

@Composable
fun PesitoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ConverterDestination) {
        composable<ConverterDestination> {
            ConverterRoute(
                onOpenHistory = { from, to -> navController.navigate(HistoryDestination(from, to)) },
            )
        }
        composable<HistoryDestination> {
            HistoryRoute(onBack = { navController.popBackStack() })
        }
    }
}
