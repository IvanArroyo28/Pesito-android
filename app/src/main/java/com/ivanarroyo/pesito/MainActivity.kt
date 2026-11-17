package com.ivanarroyo.pesito

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ivanarroyo.pesito.presentation.navigation.PesitoNavHost
import com.ivanarroyo.pesito.ui.theme.PesitoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PesitoTheme {
                PesitoNavHost()
            }
        }
    }
}
