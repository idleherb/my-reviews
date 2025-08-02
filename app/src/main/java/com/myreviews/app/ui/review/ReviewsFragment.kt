package com.myreviews.app.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.myreviews.app.di.AppModule
import com.myreviews.app.domain.model.Review
import com.myreviews.app.domain.model.Restaurant
import com.myreviews.app.data.api.ReactionService
import com.myreviews.app.MainActivity
import com.myreviews.app.ui.map.MapFragment
import com.myreviews.app.R
import android.content.Intent
import com.myreviews.app.ui.settings.SettingsActivity
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import android.content.Context

class ReviewsFragment : Fragment() {
    
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var sortSpinner: Spinner
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    private val reviewRepository = AppModule.reviewRepository
    private var allReviews: List<Review> = emptyList()
    private var filteredReviews: List<Review> = emptyList()
    private var currentUserId: String = ""
    
    private fun isCloudSyncEnabled(): Boolean {
        val prefs = requireContext().getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(SettingsActivity.KEY_CLOUD_SYNC_ENABLED, false)
    }
    
    private fun isOwnReview(review: Review): Boolean {
        // Prüfe ob die Review vom aktuellen Benutzer ist
        return review.userId == currentUserId
    }
    
    override fun onResume() {
        super.onResume()
        // Force refresh der Liste um Avatar-Änderungen zu reflektieren
        if (::listView.isInitialized && allReviews.isNotEmpty()) {
            // Erstelle einen neuen Adapter um View-Caching zu umgehen
            listView.adapter = ReviewAdapter(filteredReviews)
        }
    }
    
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
        
        // Sync Button (nur wenn Cloud Sync aktiviert ist)
        if (isCloudSyncEnabled()) {
            val syncButton = com.google.android.material.button.MaterialButton(
                requireContext(), 
                null, 
                com.google.android.material.R.attr.borderlessButtonStyle
            ).apply {
                text = "Synchronisieren"
                icon = androidx.core.content.res.ResourcesCompat.getDrawable(
                    resources,
                    android.R.drawable.ic_popup_sync,
                    null
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                
                setOnClickListener {
                    performSync(this)
                }
            }
            controlsLayout.addView(syncButton)
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
        
        // Load current user first, then reviews
        lifecycleScope.launch {
            loadCurrentUser()
            loadReviews()
        }
        
        return layout
    }
    
    private suspend fun loadCurrentUser() {
        // Stelle sicher, dass ein User existiert
        val currentUser = AppModule.userRepository.ensureDefaultUser()
        currentUserId = currentUser.userId
        Log.d("ReviewsFragment", "Current user loaded: $currentUserId (${currentUser.userName})")
    }
    
    private fun loadReviews() {
        lifecycleScope.launch {
            AppModule.reviewRepository.getAllReviews().collect { reviews ->
                if (!isAdded) return@collect
                
                // Load reactions if cloud sync is enabled
                if (isCloudSyncEnabled()) {
                    allReviews = loadReactionsForReviews(reviews)
                } else {
                    allReviews = reviews
                }
                filterAndSortReviews()
            }
        }
    }
    
    private suspend fun loadReactionsForReviews(reviews: List<Review>): List<Review> {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = requireContext().getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val serverUrl = prefs.getString(SettingsActivity.KEY_SERVER_URL, "") ?: ""
                val serverPort = prefs.getString(SettingsActivity.KEY_SERVER_PORT, "3000") ?: "3000"
                
                if (serverUrl.isEmpty()) return@withContext reviews
                
                val reactionService = ReactionService("http://$serverUrl:$serverPort")
                
                reviews.map { review ->
                    try {
                        val reactionResponse = reactionService.getReactions(review.id)
                        review.copy(reactionCounts = reactionResponse.counts)
                    } catch (e: Exception) {
                        review // Return original if loading fails
                    }
                }
            } catch (e: Exception) {
                reviews // Return original list if any error occurs
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
            
            // Adresse mit Karten-Icon
            val addressLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            // Karten-Icon (Material Icons place/location)
            addressLayout.addView(TextView(requireContext()).apply {
                val typeface = androidx.core.content.res.ResourcesCompat.getFont(
                    requireContext(), 
                    R.font.material_icons_regular
                )
                setTypeface(typeface)
                text = "\uE55F" // place icon
                textSize = 16f
                setTextColor(0xFF999999.toInt()) // Grau
                setPadding(0, 0, 8, 0)
            })
            
            // Adresse
            addressLayout.addView(TextView(requireContext()).apply {
                text = review.restaurantAddress
                textSize = 12f
                setTextColor(0xFF999999.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
            
            contentLayout.addView(addressLayout)
            
            // ReviewView Komponente verwenden (ohne Restaurantname und Adresse)
            val reviewView = com.myreviews.app.ui.components.ReviewView(requireContext()).apply {
                setReview(review, lifecycleScope, showRestaurantName = false, showAddress = false)
            }
            contentLayout.addView(reviewView)
            
            // Debug logging
            Log.d("ReviewsFragment", "Review: ${review.restaurantName}, userId: ${review.userId}, currentUserId: $currentUserId, isOwn: ${isOwnReview(review)}")
            
            // Reaktions-Leiste (nur wenn Cloud-Sync aktiviert ist und es nicht die eigene Bewertung ist)
            if (isCloudSyncEnabled() && !isOwnReview(review)) {
                contentLayout.addView(createReactionBar(review))
            }
            
            itemView.addView(contentLayout)
            
            // Action Buttons Container (nur für eigene Reviews)
            if (isOwnReview(review)) {
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
            }
            
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
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
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
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
                // Nur lokal als gelöscht markieren
                // Der Server wird beim nächsten Sync informiert
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
    
    private fun createReactionBar(review: Review): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 8)
            
            // Nur Emojis mit Reaktionen anzeigen
            val reactionsWithCounts = review.reactionCounts.filter { it.value > 0 }
            
            // Reaktionen anzeigen
            reactionsWithCounts.forEach { (emoji, count) ->
                addView(createReactionChip(emoji, count, true) {
                    handleReactionClick(review, emoji)
                })
            }
            
            // "Add Reaction" Button (graues Smiley)
            addView(createReactionChip("😊", 0, false) {
                showReactionPicker(review)
            })
        }
    }
    
    private fun createReactionChip(emoji: String, count: Int, showCount: Boolean, onClick: () -> Unit): View {
        return TextView(requireContext()).apply {
            text = if (showCount && count > 0) "$emoji $count" else emoji
            textSize = 16f
            setPadding(12, 6, 12, 6)
            
            // Modern chip style with rounded corners
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16f * resources.displayMetrics.density
                setColor(0xFFF0F0F0.toInt()) // Light gray background
                setStroke(1, 0xFFE0E0E0.toInt()) // Subtle border
            }
            
            if (emoji == "😊") {
                setTextColor(0xFF999999.toInt()) // Gray for add button
                alpha = 0.6f // Make it more subtle
            } else {
                setTextColor(0xFF333333.toInt())
            }
            
            isClickable = true
            isFocusable = true
            
            setOnClickListener { onClick() }
            
            // Add margin between chips
            (layoutParams as? LinearLayout.LayoutParams)?.apply {
                setMargins(0, 0, 8, 0)
            }
        }
    }
    
    private fun showReactionPicker(review: Review) {
        val emojis = listOf("❤️", "👍", "😂", "🤔", "😮")
        
        // Create a horizontal emoji picker dialog
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reaktion wählen")
            .create()
        
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER
        }
        
        emojis.forEach { emoji ->
            layout.addView(TextView(requireContext()).apply {
                text = emoji
                textSize = 24f
                setPadding(16, 8, 16, 8)
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
                
                setOnClickListener {
                    handleReactionClick(review, emoji)
                    dialog.dismiss()
                }
            })
        }
        
        dialog.setView(layout)
        dialog.show()
    }
    
    private fun handleReactionClick(review: Review, emoji: String) {
        Log.d("ReviewsFragment", "Reaction clicked: $emoji on review ${review.id}, currentUserId: $currentUserId")
        
        // Reaktion ohne UI-Feedback speichern
        lifecycleScope.launch {
            try {
                val prefs = requireContext().getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val serverUrl = prefs.getString(SettingsActivity.KEY_SERVER_URL, "") ?: ""
                val serverPort = prefs.getString(SettingsActivity.KEY_SERVER_PORT, "3000") ?: "3000"
                
                if (serverUrl.isNotEmpty() && currentUserId.isNotEmpty()) {
                    val reactionService = ReactionService("http://$serverUrl:$serverPort")
                    
                    // Check if user already reacted
                    val existingReactions = reactionService.getReactions(review.id)
                    val userReacted = existingReactions.reactions.any { 
                        it.userId == currentUserId && it.emoji == emoji 
                    }
                    
                    if (userReacted) {
                        // Remove reaction
                        val success = reactionService.removeReaction(review.id, currentUserId)
                        if (success) {
                            // Refresh the list to update reaction counts
                            withContext(Dispatchers.Main) {
                                // Reload reviews to get updated reaction counts
                                loadReviews()
                            }
                        }
                    } else {
                        // Add reaction
                        val success = reactionService.addReaction(review.id, currentUserId, emoji)
                        if (success) {
                            // Refresh the list to update reaction counts
                            withContext(Dispatchers.Main) {
                                // Reload reviews to get updated reaction counts
                                loadReviews()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - no toast as per user preference
                e.printStackTrace()
            }
        }
    }
    
    private fun loadAvatarIntoImageView(url: String, imageView: ImageView) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                // Keep default image on error
            }
        }
    }
    
    private fun performSync(button: com.google.android.material.button.MaterialButton) {
        button.isEnabled = false
        button.text = "Synchronisiere..."
        
        lifecycleScope.launch {
            val result = AppModule.syncRepository.performSync()
            
            withContext(Dispatchers.Main) {
                when (result) {
                    is com.myreviews.app.data.api.SyncResult.Success -> {
                        val message = result.message ?: "${result.syncedCount} Bewertungen synchronisiert"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                    is com.myreviews.app.data.api.SyncResult.Error -> {
                        Toast.makeText(requireContext(), "Sync fehlgeschlagen: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
                
                button.isEnabled = true
                button.text = "Synchronisieren"
            }
        }
    }
}