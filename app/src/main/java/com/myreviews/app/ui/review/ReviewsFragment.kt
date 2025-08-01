package com.myreviews.app.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.myreviews.app.di.AppModule
import com.myreviews.app.domain.model.Review
import com.myreviews.app.domain.model.Restaurant
import com.myreviews.app.MainActivity
import com.myreviews.app.ui.map.MapFragment
import com.myreviews.app.R
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

class ReviewsFragment : Fragment() {
    
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var sortSpinner: Spinner
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    private val reviewRepository = AppModule.reviewRepository
    private var allReviews: List<Review> = emptyList()
    private var filteredReviews: List<Review> = emptyList()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ReviewsFragment", "onCreateView called")
        
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Such- und Sortierbereich
        val controlsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFF5F5F5.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Suchfeld
        searchEditText = EditText(requireContext()).apply {
            hint = "Suche nach Restaurant oder Bewertung..."
            setSingleLine()
            setBackgroundResource(android.R.drawable.edit_text)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    filterAndSortReviews()
                }
            })
        }
        controlsLayout.addView(searchEditText)
        
        // Sortierung
        val sortLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        sortLayout.addView(TextView(requireContext()).apply {
            text = "Sortieren: "
            textSize = 14f
            setPadding(0, 12, 8, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })
        
        sortSpinner = Spinner(requireContext()).apply {
            val sortOptions = arrayOf("Datum (neueste zuerst)", "Datum (älteste zuerst)", 
                                    "Bewertung (beste zuerst)", "Bewertung (schlechteste zuerst)",
                                    "Restaurant (A-Z)", "Restaurant (Z-A)")
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    filterAndSortReviews()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
        sortLayout.addView(sortSpinner)
        
        controlsLayout.addView(sortLayout)
        layout.addView(controlsLayout)
        
        Log.d("ReviewsFragment", "Added controls layout with search and sort")
        
        // ListView für Bewertungen
        listView = ListView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        // Empty View
        emptyView = TextView(requireContext()).apply {
            text = "Noch keine Bewertungen vorhanden.\n\nTippe auf ein Restaurant in der Karte,\num eine Bewertung zu hinterlassen."
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(32, 64, 32, 64)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        
        layout.addView(listView)
        layout.addView(emptyView)
        
        loadReviews()
        
        return layout
    }
    
    private fun loadReviews() {
        lifecycleScope.launch {
            AppModule.reviewRepository.getAllReviews().collect { reviews ->
                if (!isAdded) return@collect
                
                allReviews = reviews
                filterAndSortReviews()
            }
        }
    }
    
    private fun filterAndSortReviews() {
        if (!::searchEditText.isInitialized || !::sortSpinner.isInitialized) {
            Log.w("ReviewsFragment", "Views not initialized yet")
            return
        }
        
        val searchText = searchEditText.text.toString().lowercase()
        
        // Filtern
        filteredReviews = if (searchText.isEmpty()) {
            allReviews
        } else {
            allReviews.filter { review ->
                review.restaurantName.lowercase().contains(searchText) ||
                review.comment.lowercase().contains(searchText)
            }
        }
        
        // Sortieren
        filteredReviews = when (sortSpinner.selectedItemPosition) {
            0 -> filteredReviews.sortedByDescending { it.visitDate } // Datum (neueste zuerst)
            1 -> filteredReviews.sortedBy { it.visitDate } // Datum (älteste zuerst)
            2 -> filteredReviews.sortedByDescending { it.rating } // Bewertung (beste zuerst)
            3 -> filteredReviews.sortedBy { it.rating } // Bewertung (schlechteste zuerst)
            4 -> filteredReviews.sortedBy { it.restaurantName } // Restaurant (A-Z)
            5 -> filteredReviews.sortedByDescending { it.restaurantName } // Restaurant (Z-A)
            else -> filteredReviews
        }
        
        // Anzeigen
        if (filteredReviews.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.text = if (searchText.isNotEmpty()) {
                "Keine Bewertungen gefunden für \"$searchText\""
            } else {
                "Noch keine Bewertungen vorhanden.\n\nTippe auf ein Restaurant in der Karte,\num eine Bewertung zu hinterlassen."
            }
        } else {
            listView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            listView.adapter = ReviewAdapter(filteredReviews)
        }
    }
    
    private inner class ReviewAdapter(
        private val reviews: List<Review>
    ) : BaseAdapter() {
        
        override fun getCount() = reviews.size
        override fun getItem(position: Int) = reviews[position]
        override fun getItemId(position: Int) = reviews[position].id
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val review = reviews[position]
            
            // Wrapper für Item + Separator
            val wrapper = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            // Hauptcontainer
            val itemView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(24, 24, 16, 24)  // Rechts weniger Padding für Icons
                setBackgroundResource(android.R.drawable.list_selector_background)
                // Minimum Height für Touch-Target
                minimumHeight = (56 * resources.displayMetrics.density).toInt()
            }
            
            // Hauptinhalt (klickbar für Navigation)
            val contentLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                isClickable = true
                isFocusable = true
                
                setOnClickListener {
                    this@ReviewsFragment.navigateToRestaurantOnMap(review)
                }
            }
            
            // Restaurant Name
            contentLayout.addView(TextView(requireContext()).apply {
                text = review.restaurantName
                textSize = 18f
                setTextColor(0xFF000000.toInt())
                setPadding(0, 0, 0, 8)
            })
            
            // Bewertung und Datum
            val ratingLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            
            // Sterne als Text anzeigen
            val ratingText = TextView(requireContext()).apply {
                text = "★".repeat(review.rating.toInt()) + "☆".repeat(5 - review.rating.toInt())
                textSize = 16f
                setTextColor(0xFFFFB300.toInt()) // Gold
            }
            ratingLayout.addView(ratingText)
            
            // Datum
            ratingLayout.addView(TextView(requireContext()).apply {
                text = " • ${dateFormatter.format(review.visitDate)}"
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            })
            
            contentLayout.addView(ratingLayout)
            
            // Kommentar
            if (review.comment.isNotEmpty()) {
                contentLayout.addView(TextView(requireContext()).apply {
                    text = review.comment
                    textSize = 14f
                    setTextColor(0xFF333333.toInt())
                    setPadding(0, 8, 0, 0)
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
            }
            
            // Adresse
            contentLayout.addView(TextView(requireContext()).apply {
                text = review.restaurantAddress
                textSize = 12f
                setTextColor(0xFF999999.toInt())
                setPadding(0, 8, 0, 0)
            })
            
            // Hinweis zum Anklicken
            contentLayout.addView(TextView(requireContext()).apply {
                text = "→ Auf Karte anzeigen"
                textSize = 12f
                setTextColor(0xFF2196F3.toInt()) // Blau
                setPadding(0, 8, 0, 0)
                setTypeface(null, android.graphics.Typeface.ITALIC)
            })
            
            itemView.addView(contentLayout)
            
            // Action Buttons Container
            val actionsLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setMargins(8, 0, 0, 0)
                }
            }
            
            // Bearbeiten Icon (Material Design Pencil)
            actionsLayout.addView(createIconButton("edit", 0xFF999999.toInt()) {
                editReview(review)
            })
            
            // Löschen Icon (Material Design Delete)
            actionsLayout.addView(createIconButton("delete", 0xFF999999.toInt()) {
                confirmDeleteReview(review)
            })
            
            itemView.addView(actionsLayout)
            
            // Item zum Wrapper hinzufügen
            wrapper.addView(itemView)
            
            // Separator
            if (position < reviews.size - 1) {
                wrapper.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    )
                    setBackgroundColor(0xFFDDDDDD.toInt())
                })
            }
            
            return wrapper
        }
    }
    
    private fun navigateToRestaurantOnMap(review: Review) {
        // Restaurant-Objekt aus Review erstellen
        val restaurant = Restaurant(
            id = review.restaurantId,
            name = review.restaurantName,
            latitude = review.restaurantLat,
            longitude = review.restaurantLon,
            address = review.restaurantAddress,
            averageRating = null,
            reviewCount = 0,
            amenityType = "restaurant" // Default, da Review keine amenity info hat
        )
        
        // Zur Karte wechseln und Restaurant zeigen
        (activity as? MainActivity)?.let { mainActivity ->
            // Restaurant-Position für MapFragment speichern
            MapFragment.pendingRestaurant = restaurant
            
            // Zum Karten-Tab wechseln
            mainActivity.switchToMapTab()
        }
    }
    
    private fun showReviewOptionsDialog(review: Review) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(review.restaurantName)
            .setItems(arrayOf("Bearbeiten", "Löschen")) { _, which ->
                when (which) {
                    0 -> editReview(review)
                    1 -> confirmDeleteReview(review)
                }
            }
            .show()
    }
    
    private fun editReview(review: Review) {
        // Öffne AddReviewActivity im Edit-Modus
        val intent = Intent(requireContext(), AddReviewActivity::class.java).apply {
            putExtra("edit_mode", true)
            putExtra("review_id", review.id)
            putExtra("restaurant_id", review.restaurantId)
            putExtra("restaurant_name", review.restaurantName)
            putExtra("restaurant_lat", review.restaurantLat)
            putExtra("restaurant_lon", review.restaurantLon)
            putExtra("restaurant_address", review.restaurantAddress)
            putExtra("existing_rating", review.rating)
            putExtra("existing_comment", review.comment)
            putExtra("existing_visit_date", review.visitDate.time)
        }
        startActivity(intent)
    }
    
    private fun confirmDeleteReview(review: Review) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Bewertung löschen?")
            .setMessage("Möchten Sie diese Bewertung für ${review.restaurantName} wirklich löschen?")
            .setPositiveButton("Löschen") { _, _ ->
                deleteReview(review)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    private fun deleteReview(review: Review) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    reviewRepository.deleteReview(review)
                }
                Toast.makeText(requireContext(), "Bewertung gelöscht", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Fehler beim Löschen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun createIconButton(iconType: String, color: Int, onClick: () -> Unit): View {
        return TextView(requireContext()).apply {
            // Material Icons Font verwenden
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(
                requireContext(), 
                R.font.material_icons_regular
            )
            setTypeface(typeface)
            
            when (iconType) {
                "edit" -> {
                    // Material Design Edit Icon
                    text = "\uE3C9"  // edit icon codepoint
                }
                "delete" -> {
                    // Material Design Delete Icon  
                    text = "\uE872"  // delete icon codepoint
                }
            }
            
            // Material Design konforme Größen
            textSize = 24f  // Material Icons standard size
            setTextColor(color)
            
            // Touch-Target 48x48dp
            val density = resources.displayMetrics.density
            val size = (48 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                // 12dp Abstand nach unten (nur beim ersten Button relevant)
                if (iconType == "edit") {
                    setMargins(0, 0, 0, (12 * density).toInt())
                }
            }
            
            // Zentrieren
            gravity = android.view.Gravity.CENTER
            
            // Klickbar
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.list_selector_background)
            
            setOnClickListener { onClick() }
        }
    }
}