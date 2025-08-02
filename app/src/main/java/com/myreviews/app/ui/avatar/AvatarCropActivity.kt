package com.myreviews.app.ui.avatar

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class AvatarCropActivity : AppCompatActivity() {
    
    private lateinit var imageView: ImageView
    private lateinit var overlayView: View
    private lateinit var originalBitmap: Bitmap
    private var imageUri: Uri? = null
    
    // Transformation values
    private var scaleFactor = 1.0f
    private var focusX = 0f
    private var focusY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    
    // Gesture detectors
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    
    // Circle dimensions
    private var circleCenterX = 0f
    private var circleCenterY = 0f
    private var circleRadius = 0f
    
    companion object {
        const val EXTRA_IMAGE_URI = "image_uri"
        const val RESULT_CROPPED_URI = "cropped_uri"
        const val MIN_SCALE = 1.0f
        const val MAX_SCALE = 5.0f
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get image URI from intent
        imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        if (imageUri == null) {
            finish()
            return
        }
        
        setupUI()
        loadImage()
        setupGestureDetector()
    }
    
    private fun setupUI() {
        // Main layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }
        
        // Container for image and overlay
        val containerLayout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        // Image view
        imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.MATRIX
        }
        containerLayout.addView(imageView)
        
        // Overlay with circle cutout
        overlayView = object : View(this@AvatarCropActivity) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                
                // Draw semi-transparent overlay
                val paint = Paint().apply {
                    color = Color.parseColor("#B0000000") // Semi-transparent black
                    style = Paint.Style.FILL
                }
                
                // Calculate circle dimensions
                val size = min(width, height)
                circleRadius = size * 0.4f // 80% of the smaller dimension
                circleCenterX = width / 2f
                circleCenterY = height / 2f
                
                // Create a path with a hole
                val path = Path().apply {
                    addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
                    addCircle(circleCenterX, circleCenterY, circleRadius, Path.Direction.CCW)
                    fillType = Path.FillType.EVEN_ODD
                }
                
                canvas.drawPath(path, paint)
                
                // Draw white circle border
                val borderPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                    isAntiAlias = true
                }
                canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, borderPaint)
            }
        }
        containerLayout.addView(overlayView)
        
        mainLayout.addView(containerLayout)
        
        // Button layout
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#212121"))
        }
        
        // Cancel button
        val cancelButton = MaterialButton(this).apply {
            text = "Abbrechen"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, 8, 0)
            }
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.WHITE)
            setOnClickListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
        buttonLayout.addView(cancelButton)
        
        // Save button
        val saveButton = MaterialButton(this).apply {
            text = "Fertig"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(8, 0, 0, 0)
            }
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.WHITE)
            setOnClickListener {
                cropAndSaveImage()
            }
        }
        buttonLayout.addView(saveButton)
        
        mainLayout.addView(buttonLayout)
        setContentView(mainLayout)
    }
    
    private fun loadImage() {
        try {
            val inputStream = contentResolver.openInputStream(imageUri!!)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            imageView.setImageBitmap(originalBitmap)
            
            // Center the image initially
            imageView.post {
                centerImage()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }
    
    private fun setupGestureDetector() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = max(MIN_SCALE, min(scaleFactor, MAX_SCALE))
                
                focusX = detector.focusX
                focusY = detector.focusY
                
                updateImageMatrix()
                return true
            }
        })
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val pointerIndex = event.actionIndex
                lastTouchX = event.getX(pointerIndex)
                lastTouchY = event.getY(pointerIndex)
                activePointerId = event.getPointerId(0)
            }
            
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex >= 0) {
                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)
                    
                    if (!scaleGestureDetector.isInProgress) {
                        val dx = x - lastTouchX
                        val dy = y - lastTouchY
                        
                        focusX += dx
                        focusY += dy
                        
                        updateImageMatrix()
                    }
                    
                    lastTouchX = x
                    lastTouchY = y
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        
        return true
    }
    
    private fun centerImage() {
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val imageWidth = originalBitmap.width.toFloat()
        val imageHeight = originalBitmap.height.toFloat()
        
        // Calculate scale to fit the circle
        val scaleX = (circleRadius * 2) / imageWidth
        val scaleY = (circleRadius * 2) / imageHeight
        scaleFactor = max(scaleX, scaleY) * 1.2f // Slightly larger than circle
        
        // Center the image
        focusX = viewWidth / 2
        focusY = viewHeight / 2
        
        updateImageMatrix()
    }
    
    private fun updateImageMatrix() {
        val matrix = Matrix()
        
        // Scale around the center of the image
        matrix.postScale(scaleFactor, scaleFactor)
        
        // Calculate translation to keep image centered at focus point
        val scaledWidth = originalBitmap.width * scaleFactor
        val scaledHeight = originalBitmap.height * scaleFactor
        val translateX = focusX - scaledWidth / 2
        val translateY = focusY - scaledHeight / 2
        
        matrix.postTranslate(translateX, translateY)
        
        imageView.imageMatrix = matrix
    }
    
    private fun cropAndSaveImage() {
        try {
            // Create a bitmap for the cropped image
            val croppedSize = (circleRadius * 2).toInt()
            val croppedBitmap = Bitmap.createBitmap(croppedSize, croppedSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(croppedBitmap)
            
            // Draw white background
            canvas.drawColor(Color.WHITE)
            
            // Create circular clip
            val path = Path().apply {
                addCircle(circleRadius, circleRadius, circleRadius, Path.Direction.CW)
            }
            canvas.clipPath(path)
            
            // Get current matrix values
            val matrix = imageView.imageMatrix
            val values = FloatArray(9)
            matrix.getValues(values)
            
            // Adjust for crop position
            val adjustedMatrix = Matrix(matrix)
            adjustedMatrix.postTranslate(
                -(circleCenterX - circleRadius),
                -(circleCenterY - circleRadius)
            )
            
            // Draw the image
            canvas.drawBitmap(originalBitmap, adjustedMatrix, null)
            
            // Save to cache directory
            val outputFile = File(cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            // Return result
            val resultIntent = Intent().apply {
                putExtra(RESULT_CROPPED_URI, Uri.fromFile(outputFile))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
            
        } catch (e: Exception) {
            e.printStackTrace()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}