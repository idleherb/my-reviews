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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReviewsFragment : Fragment() {
    
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // ListView für Bewertungen
        listView = ListView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Empty View
        emptyView = TextView(requireContext()).apply {
            text = "Noch keine Bewertungen vorhanden.\n\nTippe auf ein Restaurant in der Karte,\num eine Bewertung zu hinterlassen."
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(32, 64, 32, 64)
            visibility = View.GONE
        }
        
        layout.addView(listView)
        layout.addView(emptyView)
        
        loadReviews()
        
        // Click Listener für die Items
        listView.setOnItemClickListener { _, _, position, _ ->
            val adapter = listView.adapter as? ReviewAdapter
            adapter?.getItem(position)?.let { review ->
                navigateToRestaurantOnMap(review)
            }
        }
        
        return layout
    }
    
    private fun loadReviews() {
        lifecycleScope.launch {
            AppModule.reviewRepository.getAllReviews().collect { reviews ->
                if (!isAdded) return@collect
                
                if (reviews.isEmpty()) {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    listView.adapter = ReviewAdapter(reviews)
                }
            }
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
            
            val itemView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
                isClickable = true
                isFocusable = true
                setBackgroundResource(android.R.drawable.list_selector_background)
            }
            
            // Restaurant Name
            itemView.addView(TextView(requireContext()).apply {
                text = review.restaurantName
                textSize = 18f
                setTextColor(0xFF000000.toInt())
                setPadding(0, 0, 0, 8)
            })
            
            // Bewertung und Datum
            val ratingLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            
            // Sterne als Text anzeigen (einfacher als RatingBar)
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
                setPadding(8, 0, 0, 0)
            })
            
            itemView.addView(ratingLayout)
            
            // Kommentar
            itemView.addView(TextView(requireContext()).apply {
                text = review.comment
                textSize = 14f
                setTextColor(0xFF333333.toInt())
                setPadding(0, 12, 0, 0)
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            
            // Adresse
            itemView.addView(TextView(requireContext()).apply {
                text = review.restaurantAddress
                textSize = 12f
                setTextColor(0xFF999999.toInt())
                setPadding(0, 8, 0, 0)
            })
            
            // Hinweis zum Anklicken
            itemView.addView(TextView(requireContext()).apply {
                text = "→ Auf Karte anzeigen"
                textSize = 12f
                setTextColor(0xFF2196F3.toInt()) // Blau
                setPadding(0, 8, 0, 0)
                setTypeface(null, android.graphics.Typeface.ITALIC)
            })
            
            // Separator
            if (position < reviews.size - 1) {
                itemView.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        topMargin = 24
                    }
                    setBackgroundColor(0xFFDDDDDD.toInt())
                })
            }
            
            return itemView
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
            reviewCount = 0
        )
        
        // Zur Karte wechseln und Restaurant zeigen
        (activity as? MainActivity)?.let { mainActivity ->
            // Restaurant-Position für MapFragment speichern
            MapFragment.pendingRestaurant = restaurant
            
            // Zum Karten-Tab wechseln
            mainActivity.switchToMapTab()
        }
    }
}