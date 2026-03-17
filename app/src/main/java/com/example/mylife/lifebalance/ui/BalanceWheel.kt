package com.example.mylife.lifebalance.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mylife.lifebalance.data.LifeSphere
import kotlin.math.cos
import kotlin.math.sin

val colorPalette = listOf(
    Color(0xFFE53935), // красный
    Color(0xFFFB8C00), // оранжевый
    Color(0xFFFDD835), // желтый
    Color(0xFF7CB342), // зеленый
    Color(0xFF26A69A), // бирюзовый
    Color(0xFF42A5F5), // голубой
    Color(0xFF5C6BC0), // синий
    Color(0xFF8E24AA), // фиолетовый
    Color(0xFFEC407A), // розовый
    Color(0xFF78909C), // серо-голубой
    Color(0xFF8D6E63), // коричневый
    Color(0xFF66BB6A)  // светло-зеленый
)

@Composable
fun BalanceWheel(
    spheres: List<LifeSphere>,
    onSectorClick: (LifeSphere) -> Unit,
    modifier: Modifier = Modifier
) {
    //Выносим цвета на уровень COMPOSABLE
    val centerCircleColor = colorScheme.surface
    val textColor = colorScheme.onSurface

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 20f
            val sectors = spheres.size
            if (sectors > 0) {
                val angleStep = 360f / sectors

                // Концентрические круги (шкала 0-10)
                for (i in 1..10) {
                    val circleRadius = radius * (i / 10f)
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = circleRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Сектора
                spheres.forEachIndexed { index, sphere ->
                    val startAngle = index * angleStep - 90f
                    val score = sphere.score
                    val scoreRadius = radius * (score / 10f)
                    val color = colorPalette[sphere.colorIndex % colorPalette.size]

                    if (score > 0) {
                        // Рисуем сектор до уровня оценки
                        drawArc(
                            color = color.copy(alpha = 0.7f),
                            startAngle = startAngle,
                            sweepAngle = angleStep,
                            useCenter = true,
                            topLeft = Offset(center.x - scoreRadius, center.y - scoreRadius),
                            size = Size(scoreRadius * 2, scoreRadius * 2)
                        )
                    }

                    // Линии границ сектора
                    val startRad = Math.toRadians(startAngle.toDouble())
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = center,
                        end = Offset(
                            (center.x + cos(startRad) * radius).toFloat(),
                            (center.y + sin(startRad) * radius).toFloat()
                        ),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Центральный круг
                drawCircle(
                    color = centerCircleColor,
                    radius = 20f,
                    center = center
                )
            }
        }

        // Названия секторов поверх Canvas
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            spheres.forEachIndexed { index, sphere ->
                val angle = (index * 360f / spheres.size - 90f + 180f / spheres.size)
                val angleRad = Math.toRadians(angle.toDouble())
                val labelRadius = 120f
                val offsetX = cos(angleRad).toFloat() * labelRadius
                val offsetY = sin(angleRad).toFloat() * labelRadius

                Box(
                    modifier = Modifier
                        .offset(x = offsetX.dp, y = offsetY.dp)
                        .clickable { onSectorClick(sphere) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sphere.name.uppercase(),// uppercase делает текст заглавными
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
