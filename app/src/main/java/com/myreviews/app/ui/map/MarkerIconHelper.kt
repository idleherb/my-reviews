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
        val circleRadius = width * 0.42f
        
        // Draw filled pin shape
        val pinPath = Path().apply {
            // Start at the bottom point
            moveTo(centerX, height.toFloat())
            
            // Calculate tangent points where pin meets circle
            val angle = Math.asin((circleRadius * 0.7) / (height - centerY).toDouble()).toFloat()
            val tangentX = circleRadius * Math.sin(angle.toDouble()).toFloat()
            val tangentY = circleRadius * Math.cos(angle.toDouble()).toFloat()
            
            // Left side of pin
            lineTo(centerX - tangentX, centerY + tangentY)
            
            // Arc around the circle (counter-clockwise from left tangent to right tangent)
            val startAngle = Math.toDegrees(angle.toDouble()).toFloat() + 90
            val sweepAngle = 360 - (2 * Math.toDegrees(angle.toDouble()).toFloat())
            arcTo(
                centerX - circleRadius,
                centerY - circleRadius,
                centerX + circleRadius,
                centerY + circleRadius,
                startAngle,
                sweepAngle,
                false
            )
            
            // Right side of pin back to bottom
            lineTo(centerX, height.toFloat())
            close()
        }
        
        paint.color = backgroundColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(pinPath, paint)
        
        // Draw thinner border ring
        paint.color = backgroundColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpToPx(context, 1.5f).toFloat()
        canvas.drawCircle(centerX, centerY, circleRadius - paint.strokeWidth / 2, paint)
        
        // Draw white circle for icon background
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, width * 0.32f, paint)
        
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