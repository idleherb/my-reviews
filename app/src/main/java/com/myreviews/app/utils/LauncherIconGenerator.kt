package com.myreviews.app.utils

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import com.myreviews.app.R
import java.io.File
import java.io.FileOutputStream

/**
 * Generates launcher icon PNGs using Material Icons font
 * Call this temporarily from MainActivity to generate icons
 */
object LauncherIconGenerator {
    
    fun generateAndSaveIcons(context: Context) {
        val sizes = mapOf(
            "mdpi" to 48,
            "hdpi" to 72, 
            "xhdpi" to 96,
            "xxhdpi" to 144,
            "xxxhdpi" to 192
        )
        
        // Generate launcher icons
        sizes.forEach { (density, size) ->
            val iconBitmap = createLauncherIcon(context, size)
            saveToExternalStorage(context, iconBitmap, "ic_launcher_${density}.png")
            saveToExternalStorage(context, iconBitmap, "ic_launcher_round_${density}.png")
        }
        
        // Generate foreground for adaptive icon (larger canvas)
        val foregroundBitmap = createForegroundIcon(context, 432) // 432dp for foreground
        saveToExternalStorage(context, foregroundBitmap, "ic_launcher_foreground.png")
    }
    
    private fun createLauncherIcon(context: Context, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Blue circle background
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#2196F3")
            style = Paint.Style.FILL
        }
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f
        
        // Draw blue circle
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Draw restaurant icon (increased by ~30%)
        drawRestaurantIcon(context, canvas, centerX, centerY, radius * 0.78f)
        
        return bitmap
    }
    
    private fun createForegroundIcon(context: Context, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Transparent background
        canvas.drawColor(Color.TRANSPARENT)
        
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Draw restaurant icon (larger for adaptive icon, increased by ~30%)
        drawRestaurantIcon(context, canvas, centerX, centerY, size * 0.325f)
        
        return bitmap
    }
    
    private fun drawRestaurantIcon(context: Context, canvas: Canvas, centerX: Float, centerY: Float, textSize: Float) {
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
            
            // Load Material Icons font
            val typeface = ResourcesCompat.getFont(context, R.font.material_icons_regular)
            setTypeface(typeface)
        }
        
        // Restaurant icon from Material Icons
        val restaurantIcon = "\uE56C"
        
        // Calculate vertical centering
        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.bottom - fontMetrics.top
        val textOffset = textHeight / 2f - fontMetrics.bottom
        
        canvas.drawText(restaurantIcon, centerX, centerY + textOffset, paint)
    }
    
    private fun saveToExternalStorage(context: Context, bitmap: Bitmap, filename: String) {
        try {
            // Save to external storage for easy access
            val downloadsDir = File("/storage/emulated/0/Download")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            println("Icon saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            
            // Fallback: save to app's external files directory
            val externalDir = context.getExternalFilesDir(null)
            val file = File(externalDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            println("Icon saved to fallback location: ${file.absolutePath}")
        }
    }
}