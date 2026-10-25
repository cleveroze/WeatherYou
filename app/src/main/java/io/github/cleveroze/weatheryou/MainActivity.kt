package io.github.cleveroze.weatheryou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.cleveroze.weatheryou.ui.WeatherRoute
import io.github.cleveroze.weatheryou.ui.WeatherViewModel
import io.github.cleveroze.weatheryou.ui.theme.WeatherYouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherYouTheme {
                WeatherApp()
            }
        }
    }
}

@Composable
private fun WeatherApp() {
    val viewModel: WeatherViewModel = viewModel(
        factory = WeatherViewModel.Factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
    WeatherRoute(viewModel)
}
