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
}