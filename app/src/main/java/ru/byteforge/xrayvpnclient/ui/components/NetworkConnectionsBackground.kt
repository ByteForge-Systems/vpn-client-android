package ru.byteforge.xrayvpnclient.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import ru.technosopher.testvpnrun.ui.theme.GoydaBlue
import ru.technosopher.testvpnrun.ui.theme.GoydaYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun NetworkConnectionsBackground() {
    val infiniteTransition = rememberInfiniteTransition()

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val secondaryAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val nodes = remember {
        List(40) {
            Offset(
                x = Random.nextFloat() * 1000,
                y = Random.nextFloat() * 1000
            )
        }
    }

    val stars = remember {
        List(60) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                0.5f + Random.nextFloat() * 1f
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A1428),
                            Color(0xFF1A2235)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val scaledNodes = nodes.map { node ->
                Offset(
                    x = node.x * canvasWidth / 1000,
                    y = node.y * canvasHeight / 1000
                )
            }

            scaledNodes.forEachIndexed { i, node ->
                val nearbyNodes = scaledNodes.filterIndexed { j, _ -> i != j }
                    .sortedBy { other -> (node - other).getDistance() }
                    .take(3)

                nearbyNodes.forEach { nearby ->
                    val distance = (node - nearby).getDistance()
                    val maxDistance = canvasWidth * 0.3f
                    val alpha = (1f - (distance / maxDistance)).coerceIn(0.05f, 0.3f)

                    drawLine(
                        color = GoydaBlue.copy(alpha = alpha),
                        start = node,
                        end = nearby,
                        strokeWidth = 1.2f
                    )

                    val midPoint = Offset(
                        x = node.x + (nearby.x - node.x) * 0.5f,
                        y = node.y + (nearby.y - node.y) * 0.5f
                    )

                    val pulseSize = 3f + pulseAnimation * 2f

                    drawCircle(
                        color = GoydaBlue.copy(alpha = 0.2f),
                        radius = pulseSize,
                        center = midPoint
                    )
                }

                val basePhase = i * 0.1f
                val nodePulsePhase = (
                        sin(animationProgress * PI/3 + basePhase) * 0.3f +
                                cos(secondaryAnimation * PI/5 + basePhase) * 0.2f +
                                0.5f
                        ).toFloat()

                val pulseSize = 2f + nodePulsePhase * 2f
                val nodeAlpha = 0.4f + nodePulsePhase * 0.4f

                drawCircle(
                    color = GoydaYellow.copy(alpha = nodeAlpha),
                    radius = pulseSize,
                    center = node
                )

                drawCircle(
                    color = GoydaYellow.copy(alpha = 0.1f * nodePulsePhase),
                    radius = pulseSize * 3f,
                    center = node
                )
            }

            scaledNodes.forEachIndexed { i, node ->
                if (i < scaledNodes.size - 1) {
                    val nearby = scaledNodes[(i + 1) % scaledNodes.size]

                    val phase = (i * 0.05f) % 1f
                    val combinedAnim = (animationProgress * 0.15f + secondaryAnimation * 0.1f + phase) % 1f

                    val position = Offset(
                        x = node.x + (nearby.x - node.x) * combinedAnim,
                        y = node.y + (nearby.y - node.y) * combinedAnim
                    )

                    drawCircle(
                        color = GoydaYellow.copy(alpha = 0.8f),
                        radius = 2.5f,
                        center = position
                    )

                    drawCircle(
                        color = GoydaYellow.copy(alpha = 0.2f),
                        radius = 5f,
                        center = position,
                        style = Stroke(width = 1f)
                    )
                }
            }

            stars.forEachIndexed { index, (xRatio, yRatio, starSize) ->
                val x = xRatio * size.width
                val y = yRatio * size.height

                val baseOffset = index * 0.7f
                val starPhase = (
                        sin(animationProgress * PI * 0.1f + baseOffset) * 0.3f +
                                sin(secondaryAnimation * PI * 0.08f + baseOffset * 1.3f) * 0.2f +
                                0.5f
                        ).toFloat()

                val alpha = 0.1f + starPhase * 0.2f

                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = starSize,
                    center = Offset(x, y)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoydaBlue.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        radius = 500f
                    )
                )
        )
    }
}