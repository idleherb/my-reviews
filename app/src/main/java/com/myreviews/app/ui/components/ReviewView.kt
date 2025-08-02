package com.myreviews.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.myreviews.app.domain.model.Review
import com.myreviews.app.di.AppModule
import com.myreviews.app.ui.settings.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ReviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
    private lateinit var avatarImageView: ImageView
    private lateinit var restaurantNameTextView: TextView
    private lateinit var userNameTextView: TextView
    private lateinit var ratingTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var commentTextView: TextView
    private lateinit var addressTextView: TextView
    
    init {
        orientation = VERTICAL
        setPadding(16, 16, 16, 16)
        setupViews()
    }
    
    private fun setupViews() {
        // Restaurant Name (ganz oben, wenn angezeigt)
        restaurantNameTextView = TextView(context).apply {
            textSize = 18f
            setTextColor(0xFF000000.toInt())
            setPadding(0, 0, 0, 8)
        }
        addView(restaurantNameTextView)
        
        // User info with small avatar
        val userInfoLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 0, 0, 8)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        // Small avatar (20x20dp - same height as text)
        avatarImageView = ImageView(context).apply {
            val size = (20 * resources.displayMetrics.density).toInt()
            layoutParams = LayoutParams(size, size).apply {
                setMargins(0, 0, 12, 0)  // Increased from 6 to 12
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(android.R.drawable.ic_menu_gallery)
            
            // Round corners with clipping
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        userInfoLayout.addView(avatarImageView)
        
        // Username
        userNameTextView = TextView(context).apply {
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setTypeface(null, Typeface.ITALIC)
        }
        userInfoLayout.addView(userNameTextView)
        
        addView(userInfoLayout)
        
        // Rating and Date
        val ratingLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        
        ratingTextView = TextView(context).apply {
            textSize = 16f
            setTextColor(0xFFFFB300.toInt()) // Gold
        }
        ratingLayout.addView(ratingTextView)
        
        dateTextView = TextView(context).apply {
            textSize = 14f
            setTextColor(0xFF666666.toInt())
        }
        ratingLayout.addView(dateTextView)
        
        addView(ratingLayout)
        
        // Comment
        commentTextView = TextView(context).apply {
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 8, 0, 8)
            visibility = GONE
        }
        addView(commentTextView)
        
        // Address
        addressTextView = TextView(context).apply {
            textSize = 12f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 8, 0, 0)
        }
        addView(addressTextView)
    }
    
    fun setReview(
        review: Review, 
        coroutineScope: CoroutineScope,
        showRestaurantName: Boolean = true,
        showAddress: Boolean = true
    ) {
        // Restaurant name nur wenn gewünscht
        if (showRestaurantName) {
            restaurantNameTextView.text = review.restaurantName
            restaurantNameTextView.visibility = VISIBLE
        } else {
            restaurantNameTextView.visibility = GONE
        }
        
        userNameTextView.text = review.userName
        ratingTextView.text = "★".repeat(review.rating.toInt()) + "☆".repeat(5 - review.rating.toInt())
        dateTextView.text = " • ${dateFormatter.format(review.visitDate)}"
        
        if (review.comment.isNotEmpty()) {
            commentTextView.text = review.comment
            commentTextView.visibility = VISIBLE
        } else {
            commentTextView.visibility = GONE
        }
        
        // Address nur wenn gewünscht
        if (showAddress) {
            addressTextView.text = review.restaurantAddress
            addressTextView.visibility = VISIBLE
        } else {
            addressTextView.visibility = GONE
        }
        
        // Load avatar
        loadUserAvatar(review.userId, coroutineScope)
    }
    
    private fun loadUserAvatar(userId: String, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            // Cloud-only avatars
            val cloudAvatarService = AppModule.getCloudAvatarService()
            if (cloudAvatarService != null) {
                val avatarUrl = cloudAvatarService.getUserAvatarUrl(userId)
                if (avatarUrl != null && avatarUrl.startsWith("/")) {
                    // User has an avatar, load it
                    val sharedPrefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    val serverUrl = sharedPrefs.getString(SettingsActivity.KEY_SERVER_URL, "") ?: ""
                    val serverPort = sharedPrefs.getString(SettingsActivity.KEY_SERVER_PORT, "3000") ?: "3000"
                    val fullUrl = "http://$serverUrl:$serverPort$avatarUrl"
                    loadAvatarFromUrl(fullUrl)
                } else {
                    // No avatar, use default
                    withContext(Dispatchers.Main) {
                        avatarImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
            } else {
                // Cloud sync not enabled, use default
                withContext(Dispatchers.Main) {
                    avatarImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }
    }
    
    private suspend fun loadAvatarFromUrl(url: String) {
        try {
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                withContext(Dispatchers.Main) {
                    avatarImageView.setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            // Keep default image on error
        }
    }
}