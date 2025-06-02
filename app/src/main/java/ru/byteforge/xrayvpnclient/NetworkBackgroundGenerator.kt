package ru.byteforge.xrayvpnclient

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class NetworkBackgroundGenerator {
    companion object {
        val GoydaYellow = Color.parseColor("#FFF200")
        val GoydaBlue = Color.parseColor("#00B2FF")
        val DarkBlue = Color.parseColor("#0A1428")
        val MidnightBlue = Color.parseColor("#1A2235")
    }
    
    fun exportBackground(
        context: Context,
        lifecycleScope: LifecycleCoroutineScope,
        width: Int = 1920,
        height: Int = 1080
    ) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                
                drawNetworkBackground(canvas, width, height)
                
                withContext(Dispatchers.IO) {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, "goyda_background.png")
                    
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    Log.d("ExportBackground", "Файл сохранен в ${file.absolutePath}")
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, 
                            "Фон сохранен в Downloads/goyda_background.png", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, 
                        "Ошибка: ${e.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }
    
    private fun drawNetworkBackground(canvas: Canvas, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        val gradientColors = intArrayOf(DarkBlue, MidnightBlue)
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            gradientColors, null, Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        
        val nodes = List(40) {
            PointF(
                Random.nextFloat() * width,
                Random.nextFloat() * height
            )
        }
        
        val stars = List(60) {
            Triple(
                Random.nextFloat() * width,
                Random.nextFloat() * height,
                0.5f + Random.nextFloat() * 1f
            )
        }
        
        val animationProgress = 0.5f
        val secondaryAnimation = 0.3f
        val pulseAnimation = 0.5f
        
        nodes.forEachIndexed { i, node ->
            val nearbyNodes = nodes.filterIndexed { j, _ -> i != j }
                .sortedBy { other ->
                    val dx = node.x - other.x
                    val dy = node.y - other.y
                    sqrt(dx * dx + dy * dy)
                }
                .take(3)

            nearbyNodes.forEach { nearby ->
                val dx = node.x - nearby.x
                val dy = node.y - nearby.y
                val distance = sqrt(dx * dx + dy * dy)
                val maxDistance = width * 0.3f
                val alpha = ((1f - (distance / maxDistance)) * 255 * 0.3f).toInt()
                    .coerceIn(13, 77)
                
                paint.color = GoydaBlue
                paint.alpha = alpha
                paint.strokeWidth = 1.2f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(node.x, node.y, nearby.x, nearby.y, paint)
                
                val midX = node.x + (nearby.x - node.x) * 0.5f
                val midY = node.y + (nearby.y - node.y) * 0.5f
                
                val pulseSize = 3f + pulseAnimation * 2f
                
                paint.color = GoydaBlue
                paint.alpha = 51  // 0.2 * 255
                paint.style = Paint.Style.FILL
                canvas.drawCircle(midX, midY, pulseSize, paint)
            }
            
            val basePhase = i * 0.1f
            val nodePulsePhase = (
                sin(animationProgress * PI/3 + basePhase) * 0.3f +
                cos(secondaryAnimation * PI/5 + basePhase) * 0.2f +
                0.5f
            ).toFloat()
            
            val pulseSize = 2f + nodePulsePhase * 2f
            val nodeAlpha = (0.4f + nodePulsePhase * 0.4f) * 255
            
            paint.color = GoydaYellow
            paint.alpha = nodeAlpha.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawCircle(node.x, node.y, pulseSize, paint)
            
            paint.alpha = (0.1f * nodePulsePhase * 255).toInt()
            canvas.drawCircle(node.x, node.y, pulseSize * 3f, paint)
        }
        
        nodes.forEachIndexed { i, node ->
            if (i < nodes.size - 1) {
                val nearby = nodes[(i + 1) % nodes.size]
                
                val phase = (i * 0.05f) % 1f
                val combinedAnim = (animationProgress * 0.15f + secondaryAnimation * 0.1f + phase) % 1f
                
                val posX = node.x + (nearby.x - node.x) * combinedAnim
                val posY = node.y + (nearby.y - node.y) * combinedAnim
                
                paint.color = GoydaYellow
                paint.alpha = 204  // 0.8 * 255
                paint.style = Paint.Style.FILL
                canvas.drawCircle(posX, posY, 2.5f, paint)
                
                paint.alpha = 51  // 0.2 * 255
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawCircle(posX, posY, 5f, paint)
            }
        }
        
        stars.forEachIndexed { index, (x, y, starSize) ->
            val baseOffset = index * 0.7f
            val starPhase = (
                sin(animationProgress * PI * 0.1f + baseOffset) * 0.3f +
                sin(secondaryAnimation * PI * 0.08f + baseOffset * 1.3f) * 0.2f +
                0.5f
            ).toFloat()
            
            val alpha = (0.1f + starPhase * 0.2f) * 255
            
            paint.color = Color.WHITE
            paint.alpha = alpha.toInt()
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x, y, starSize, paint)
        }
        
        val radialGradient = RadialGradient(
            width / 2f, height * 0.95f, width * 0.7f,
            intArrayOf(Color.argb(20, 0, 178, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        paint.shader = radialGradient
        canvas.drawCircle(width / 2f, height * 0.95f, width * 0.7f, paint)
    }
}