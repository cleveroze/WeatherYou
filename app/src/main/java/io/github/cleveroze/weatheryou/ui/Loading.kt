@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.weatheryou.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.cleveroze.weatheryou.ui.components.MorphingShapeBox

/** Shapes the loading indicator morphs through — cookies, clovers and suns. */
val WeatherLoadingPolygons by lazy {
    listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.VerySunny,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.SoftBurst,
        MaterialShapes.Clover8Leaf,
        MaterialShapes.Cookie12Sided,
    )
}

@Composable
fun LoadingScreen(stage: String) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.8f))
        Box(
            Modifier.size(168.dp).clip(RoundedCornerShape(56.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator(
                modifier = Modifier.size(132.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                polygons = WeatherLoadingPolygons,
            )
        }
        Spacer(Modifier.height(28.dp))
        AnimatedContent(
            targetState = stage,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
            },
            label = "stage",
        ) { text ->
            Text(
                text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(36.dp))
        Skeleton(Modifier.fillMaxWidth().height(120.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Skeleton(Modifier.weight(1f).height(110.dp))
            Skeleton(Modifier.weight(1f).height(110.dp))
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Shimmering placeholder card. */
@Composable
fun Skeleton(modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "shimmer")
        .animateFloat(-1f, 2f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x").value
    Box(
        modifier.clip(RoundedCornerShape(28.dp)).background(
            Brush.linearGradient(
                listOf(cs.surfaceContainerHigh, cs.surfaceContainerHighest, cs.surfaceContainerHigh),
                start = Offset(t * 800f, 0f), end = Offset(t * 800f + 500f, 300f),
            )
        )
    )
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit, onPickCity: () -> Unit) {
    val shapes = remember { listOf(MaterialShapes.SoftBoom, MaterialShapes.Cookie7Sided) }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MorphingShapeBox(
            target = shapes[0],
            modifier = Modifier.size(180.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            spinMillis = 20_000,
        ) {
            Text("☁", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onErrorContainer)
        }
        Spacer(Modifier.height(28.dp))
        Text("Погода потерялась", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onRetry, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Rounded.Refresh, null)
            Text("Попробовать снова", modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onPickCity, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Rounded.Search, null)
            Text("Выбрать город", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
