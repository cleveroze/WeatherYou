package io.github.cleveroze.weatheryou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cleveroze.weatheryou.ui.WeatherApp
import io.github.cleveroze.weatheryou.ui.WeatherViewModel
import io.github.cleveroze.weatheryou.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {
    private val vm: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            WeatherTheme(settings) {
                WeatherApp(vm)
            }
        }
    }
}
