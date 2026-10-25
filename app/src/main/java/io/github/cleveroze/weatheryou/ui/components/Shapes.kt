@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.cleveroze.weatheryou.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import io.github.cleveroze.weatheryou.data.Sky

/** Builds a path from cubics that live in the unit square, scaled to [size]. */
fun List<Cubic>.toPath(size: Size, path: Path = Path()): Path {
    path.reset()
    if (isEmpty()) return path
    val w = size.width
    val h = size.height
    path.moveTo(first().anchor0X * w, first().anchor0Y * h)
    for (c in this) {
        path.cubicTo(c.control0X * w, c.control0Y * h, c.control1X * w, c.control1Y * h, c.anchor1X * w, c.anchor1Y * h)
    }
    path.close()
    return path
}

fun RoundedPolygon.unit(): RoundedPolygon = normalized()

class PolygonShape(polygon: RoundedPolygon) : Shape {
    private val cubics = polygon.unit().cubics
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(cubics.toPath(size))
}

class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(morph.asCubics(progress).toPath(size))
}

/** Draws a unit-square polygon into the given box. */
fun DrawScope.drawPolygon(
    polygon: RoundedPolygon,
    color: Color,
    left: Float = 0f,
    top: Float = 0f,
    size: Size = this.size,
    rotation: Float = 0f,
    stroke: Stroke? = null,
    brush: Brush? = null,
) {
    val path = polygon.cubics.toPath(size)
    translate(left, top) {
        rotate(rotation, pivot = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)) {
            when {
                brush != null -> drawPath(path, brush, style = stroke ?: androidx.compose.ui.graphics.drawscope.Fill)
                stroke != null -> drawPath(path, color, style = stroke)
                else -> drawPath(path, color)
            }
        }
    }
}

/** The M3 Expressive shape that represents each kind of sky. */
object SkyShapes {
    fun hero(sky: Sky, isDay: Boolean): RoundedPolygon = when (sky) {
        Sky.Clear -> if (isDay) MaterialShapes.VerySunny else MaterialShapes.Cookie12Sided
        Sky.PartlyCloudy -> MaterialShapes.Cookie9Sided
        Sky.Cloudy -> MaterialShapes.Cookie7Sided
        Sky.Fog -> MaterialShapes.Ghostish
        Sky.Drizzle -> MaterialShapes.Clover4Leaf
        Sky.Rain -> MaterialShapes.Clover8Leaf
        Sky.Snow -> MaterialShapes.Flower
        Sky.Thunder -> MaterialShapes.SoftBurst
    }

    /** Decorative shapes floating in the background. */
    val floaters: List<RoundedPolygon> by lazy {
        listOf(
            MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.Sunny,
            MaterialShapes.Cookie4Sided, MaterialShapes.Clover8Leaf, MaterialShapes.SoftBurst,
            MaterialShapes.Cookie6Sided, MaterialShapes.Flower,
        ).map { it.unit() }
    }
}

/**
 * A shape that morphs to every new [target] with an expressive spring and
 * (optionally) keeps slowly spinning. Content is drawn upright on top.
 */
@Composable
fun MorphingShapeBox(
    target: RoundedPolygon,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    brush: Brush? = null,
    spin: Boolean = true,
    spinMillis: Int = 30_000,
    content: @Composable BoxScope.() -> Unit = {},
) {
    var from by remember { mutableStateOf(target.unit()) }
    var to by remember { mutableStateOf(target.unit()) }
    var shown by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    val spatial = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target) {
        if (target !== shown) {
            shown = target
            from = to
            to = target.unit()
            progress.snapTo(0f)
            progress.animateTo(1f, spatial)
        }
    }
    val morph = remember(from, to) { Morph(from, to) }
    val angle = if (spin) {
        val t = rememberInfiniteTransition(label = "spin")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(spinMillis, easing = LinearEasing), RepeatMode.Restart), label = "a").value
    } else 0f

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val path = morph.asCubics(progress.value.coerceIn(0f, 1f)).toPath(size)
            rotate(angle) {
                if (brush != null) drawPath(path, brush) else drawPath(path, color)
            }
        }
        content()
    }
}
