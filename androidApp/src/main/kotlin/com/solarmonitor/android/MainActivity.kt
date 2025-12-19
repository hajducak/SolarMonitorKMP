package com.solarmonitor.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solarmonitor.presentation.dashboard.DashboardScreen
import com.solarmonitor.presentation.dashboard.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SolarMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: DashboardViewModel = viewModel()
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SolarMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            secondary = androidx.compose.ui.graphics.Color(0xFF8BC34A),
            tertiary = androidx.compose.ui.graphics.Color(0xFFCDDC39)
        )
    } else {
        lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            secondary = androidx.compose.ui.graphics.Color(0xFF8BC34A),
            tertiary = androidx.compose.ui.graphics.Color(0xFFCDDC39)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
