package com.myreviews.app.ui.review

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.myreviews.app.di.AppModule
import com.myreviews.app.domain.model.Restaurant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AddReviewActivity : AppCompatActivity() {
    
    private lateinit var restaurantNameText: TextView
    private lateinit var restaurantAddressText: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var commentEditText: EditText
    private lateinit var visitDateButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    
    private lateinit var restaurant: Restaurant
    private var selectedDate: Date = Date()
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    private var isEditMode = false
    private var reviewId: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prüfe ob Edit-Modus
        isEditMode = intent.getBooleanExtra("edit_mode", false)
        if (isEditMode) {
            reviewId = intent.getLongExtra("review_id", 0)
        }
        
        // Restaurant-Daten aus Intent holen
        val restaurantId = intent.getLongExtra("restaurant_id", -1)
        val restaurantName = intent.getStringExtra("restaurant_name") ?: ""
        val restaurantLat = intent.getDoubleExtra("restaurant_lat", 0.0)
        val restaurantLon = intent.getDoubleExtra("restaurant_lon", 0.0)
        val restaurantAddress = intent.getStringExtra("restaurant_address") ?: ""
        
        restaurant = Restaurant(
            id = restaurantId,
            name = restaurantName,
            latitude = restaurantLat,
            longitude = restaurantLon,
            address = restaurantAddress,
            averageRating = null,
            reviewCount = 0
        )
        
        setupUI()
        setupListeners()
        
        // Im Edit-Modus vorhandene Daten laden
        if (isEditMode) {
            loadExistingReviewData()
        }
    }
    
    private fun setupUI() {
        // Hauptlayout
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Restaurant Info
        val restaurantInfoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF5F5F5.toInt())
        }
        
        restaurantNameText = TextView(this).apply {
            text = restaurant.name
            textSize = 20f
            setTextColor(0xFF000000.toInt())
            setPadding(0, 0, 0, 8)
        }
        restaurantInfoCard.addView(restaurantNameText)
        
        restaurantAddressText = TextView(this).apply {
            text = restaurant.address
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }
        restaurantInfoCard.addView(restaurantAddressText)
        
        layout.addView(restaurantInfoCard)
        
        // Abstand
        layout.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                32
            )
        })
        
        // Bewertung Label
        layout.addView(TextView(this).apply {
            text = "Deine Bewertung"
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            setPadding(0, 0, 0, 16)
        })
        
        // Rating Bar
        ratingBar = RatingBar(this).apply {
            numStars = 5
            stepSize = 1f
            rating = 4f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(ratingBar)
        
        // Abstand
        layout.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                32
            )
        })
        
        // Kommentar Label
        layout.addView(TextView(this).apply {
            text = "Kommentar"
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            setPadding(0, 0, 0, 16)
        })
        
        // Kommentar EditText
        commentEditText = EditText(this).apply {
            hint = "Wie war dein Besuch?"
            minLines = 4
            gravity = android.view.Gravity.TOP
            setBackgroundResource(android.R.drawable.edit_text)
            setPadding(16, 16, 16, 16)
        }
        layout.addView(commentEditText)
        
        // Abstand
        layout.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                32
            )
        })
        
        // Besuchsdatum
        layout.addView(TextView(this).apply {
            text = "Besuchsdatum"
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            setPadding(0, 0, 0, 16)
        })
        
        visitDateButton = MaterialButton(this).apply {
            text = dateFormatter.format(selectedDate)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(visitDateButton)
        
        // Abstand
        layout.addView(Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48
            )
        })
        
        // Buttons
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        cancelButton = MaterialButton(this).apply {
            text = "Abbrechen"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, 8, 0)
            }
        }
        buttonLayout.addView(cancelButton)
        
        saveButton = MaterialButton(this).apply {
            text = "Speichern"
            setBackgroundColor(0xFF4CAF50.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(8, 0, 0, 0)
            }
        }
        buttonLayout.addView(saveButton)
        
        layout.addView(buttonLayout)
        
        scrollView.addView(layout)
        setContentView(scrollView)
        
        // Titel setzen
        title = if (isEditMode) "Bewertung bearbeiten" else "Bewertung hinzufügen"
    }
    
    private fun setupListeners() {
        visitDateButton.setOnClickListener {
            showDatePicker()
        }
        
        cancelButton.setOnClickListener {
            finish()
        }
        
        saveButton.setOnClickListener {
            saveReview()
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                visitDateButton.text = dateFormatter.format(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun saveReview() {
        val rating = ratingBar.rating
        val comment = commentEditText.text.toString().trim()
        
        if (rating == 0f) {
            Toast.makeText(this, "Bitte eine Bewertung abgeben", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (comment.isEmpty()) {
            Toast.makeText(this, "Bitte einen Kommentar eingeben", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Bewertung speichern oder aktualisieren
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (isEditMode) {
                    // Bestehende Bewertung aktualisieren
                    val existingReview = withContext(Dispatchers.IO) {
                        AppModule.reviewRepository.getReviewById(reviewId)
                    }
                    existingReview?.let { review ->
                        val updatedReview = review.copy(
                            rating = rating,
                            comment = comment,
                            visitDate = selectedDate,
                            updatedAt = Date()
                        )
                        withContext(Dispatchers.IO) {
                            AppModule.reviewRepository.updateReview(updatedReview)
                        }
                        Toast.makeText(
                            this@AddReviewActivity,
                            "Bewertung aktualisiert!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Neue Bewertung speichern
                    val reviewId = AppModule.reviewRepository.saveReview(
                        restaurantId = restaurant.id,
                        restaurantName = restaurant.name,
                        restaurantLat = restaurant.latitude,
                        restaurantLon = restaurant.longitude,
                        restaurantAddress = restaurant.address,
                        rating = rating,
                        comment = comment,
                        visitDate = selectedDate
                    )
                    
                    Toast.makeText(
                        this@AddReviewActivity,
                        "Bewertung gespeichert!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddReviewActivity,
                    "Fehler beim Speichern: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun loadExistingReviewData() {
        // Lade vorhandene Daten aus Intent
        val existingRating = intent.getFloatExtra("existing_rating", 0f)
        val existingComment = intent.getStringExtra("existing_comment") ?: ""
        val existingVisitDate = intent.getLongExtra("existing_visit_date", Date().time)
        
        // UI mit vorhandenen Daten füllen
        ratingBar.rating = existingRating
        commentEditText.setText(existingComment)
        selectedDate = Date(existingVisitDate)
        visitDateButton.text = dateFormatter.format(selectedDate)
    }
}