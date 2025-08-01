package com.myreviews.app.ui.map

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.myreviews.app.R

object MarkerIconHelper {
    
    fun createMarkerIcon(context: Context, iconChar: String, backgroundColor: Int): BitmapDrawable {
        val width = dpToPx(context, 36f)
        val height = dpToPx(context, 48f)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.createFromAsset(context.assets, "fonts/MaterialIcons-Regular.ttf")
            textAlign = Paint.Align.CENTER
        }
        
        val centerX = width / 2f
        val centerY = height * 0.35f
        
        // Draw pin shape
        val pinPath = Path().apply {
            moveTo(centerX, height.toFloat()) // Bottom point
            cubicTo(
                width * 0.2f, height * 0.65f,  // Control point 1
                0f, centerY + height * 0.15f,   // Control point 2
                0f, centerY                      // Left side
            )
            addCircle(centerX, centerY, width * 0.45f, Path.Direction.CW)
            cubicTo(
                width.toFloat(), centerY + height * 0.15f, // Control point 1
                width * 0.8f, height * 0.65f,    // Control point 2
                centerX, height.toFloat()         // Bottom point
            )
        }
        
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(pinPath, paint)
        
        // Draw white circle for icon background
        paint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, width * 0.35f, paint)
        
        // Draw icon
        textPaint.textSize = width * 0.4f
        textPaint.color = backgroundColor
        val textBounds = Rect()
        textPaint.getTextBounds(iconChar, 0, iconChar.length, textBounds)
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(iconChar, centerX, textY, textPaint)
        
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}

object MaterialIcons {
    const val RESTAURANT = "\uE56C"      // restaurant icon
    const val LOCAL_CAFE = "\uE541"      // local_cafe icon  
    const val FASTFOOD = "\uE57A"        // fastfood icon
}