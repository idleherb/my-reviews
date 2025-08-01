package com.myreviews.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker
import com.myreviews.app.data.repository.RestaurantRepository
import com.myreviews.app.di.AppModule
import com.myreviews.app.ui.review.AddReviewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.myreviews.app.domain.model.Restaurant
import com.myreviews.app.MainActivity
import kotlinx.coroutines.flow.first
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.myreviews.app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.FrameLayout

class MapFragment : Fragment() {
    
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var myLocationButton: FloatingActionButton
    private val restaurantRepository = AppModule.restaurantRepository
    
    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
        // Grüne Meile 25, 69115 Heidelberg
        private val HEIDELBERG_CENTER = GeoPoint(49.409445, 8.693886)
        private const val DEFAULT_ZOOM = 16.0
        
        // Singleton für den aktuellen Kartenbereich
        var currentMapBounds: org.osmdroid.util.BoundingBox? = null
        
        // Restaurant aus der Suche
        var pendingRestaurant: com.myreviews.app.domain.model.Restaurant? = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // OSMDroid Konfiguration
        Configuration.getInstance().load(
            requireContext(),
            requireActivity().getPreferences(Context.MODE_PRIVATE)
        )
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout
        val rootView = inflater.inflate(R.layout.fragment_map, container, false)
        val mapContainer = rootView.findViewById<FrameLayout>(R.id.mapContainer)
        myLocationButton = rootView.findViewById(R.id.myLocationButton)
        
        // Create and setup map
        mapView = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(HEIDELBERG_CENTER)
        }
        
        // Add map to container
        mapContainer.addView(mapView)
        
        // User-Agent setzen für OSMDroid (wichtig!)
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        // Kartenbewegungen überwachen
        mapView.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                updateMapBounds()
                loadRestaurantsInBounds()
                return false
            }
            
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                updateMapBounds()
                loadRestaurantsInBounds()
                return false
            }
        })
        
        // Setup My Location button
        myLocationButton.setOnClickListener {
            centerOnMyLocation()
        }
        
        setupLocationOverlay()
        requestPermissionsIfNecessary()
        updateMapBounds()
        
        return rootView
    }
    
    private fun updateMapBounds() {
        currentMapBounds = mapView.boundingBox
        Log.d("MapFragment", "Map bounds updated: ${currentMapBounds}")
    }
    
    private fun centerOnMyLocation() {
        val location = myLocationOverlay.myLocation
        if (location != null) {
            mapView.controller.animateTo(location)
            mapView.controller.setZoom(17.0)
            
            // Nach kurzer Verzögerung Restaurants laden
            mapView.postDelayed({
                loadRestaurantsInBounds()
            }, 500)
        } else {
            // Falls keine Location verfügbar, zeige Toast
            android.widget.Toast.makeText(
                requireContext(), 
                "Standort wird ermittelt...", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // Versuche Location zu aktivieren
            myLocationOverlay.enableFollowLocation()
        }
    }
    
    private fun setupLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            mapView
        ).apply {
            enableMyLocation()
            // Follow location nur bei Button-Klick
            
            // Icon für den aktuellen Standort anpassen
            setPersonHotspot(24.0f, 24.0f)
        }
        mapView.overlays.add(myLocationOverlay)
        
        // Nach 2 Sekunden zur aktuellen Position zoomen und Restaurants laden
        mapView.postDelayed({
            val location = myLocationOverlay.myLocation ?: HEIDELBERG_CENTER
            mapView.controller.animateTo(location)
            mapView.controller.setZoom(17.0)
            
            // Restaurants im sichtbaren Bereich laden
            loadRestaurantsInBounds()
        }, 2000)
    }
    
    private fun requestPermissionsIfNecessary() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != 
                PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::mapView.isInitialized) {
            mapView.onResume()
            
            // Prüfe ob ein Restaurant aus der Suche angezeigt werden soll
            pendingRestaurant?.let { restaurant ->
                // Zur Restaurant-Position zoomen
                val restaurantLocation = GeoPoint(restaurant.latitude, restaurant.longitude)
                mapView.controller.animateTo(restaurantLocation)
                mapView.controller.setZoom(18.0)
                
                // Nach kurzer Verzögerung Dialog zeigen
                mapView.postDelayed({
                    showRestaurantDialog(restaurant)
                }, 500)
                
                // Restaurant verarbeitet, zurücksetzen
                pendingRestaurant = null
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) {
            mapView.onPause()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // MapView sauber aufräumen
        if (::mapView.isInitialized) {
            mapView.onDetach()
        }
    }
    
    private fun loadNearbyRestaurants(userLocation: GeoPoint) {
        Log.d("MapFragment", "Loading restaurants near: ${userLocation.latitude}, ${userLocation.longitude}")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val restaurants = withContext(Dispatchers.IO) {
                    restaurantRepository.getRestaurantsNearby(
                        userLocation.latitude,
                        userLocation.longitude,
                        1000 // 1km Radius
                    )
                }
                
                Log.d("MapFragment", "Found ${restaurants.size} restaurants")
                
                // Marker für jedes Restaurant hinzufügen
                restaurants.forEach { restaurant ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(restaurant.latitude, restaurant.longitude)
                        title = restaurant.name
                        snippet = restaurant.address
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        // Icon basierend auf amenity type mit Material Icon Font
                        val (iconChar, color) = when (restaurant.amenityType) {
                            "cafe" -> MaterialIcons.LOCAL_CAFE to ContextCompat.getColor(requireContext(), R.color.marker_cafe)
                            "fast_food" -> MaterialIcons.FASTFOOD to ContextCompat.getColor(requireContext(), R.color.marker_fast_food)
                            else -> MaterialIcons.RESTAURANT to ContextCompat.getColor(requireContext(), R.color.marker_restaurant)
                        }
                        icon = MarkerIconHelper.createMarkerIcon(requireContext(), iconChar, color)
                        
                        // Click-Listener für den Marker
                        setOnMarkerClickListener { marker, mapView ->
                            marker.showInfoWindow()
                            mapView.controller.animateTo(marker.position)
                            
                            // Zeige Dialog zur Auswahl
                            showRestaurantDialog(restaurant)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                
                // Karte neu zeichnen
                mapView.invalidate()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private var lastLoadJob: kotlinx.coroutines.Job? = null
    
    private fun loadRestaurantsInBounds() {
        // Vorherigen Job abbrechen falls noch läuft
        lastLoadJob?.cancel()
        
        val bounds = mapView.boundingBox
        if (bounds == null) {
            Log.w("MapFragment", "BoundingBox is null")
            return
        }
        
        Log.d("MapFragment", "Loading restaurants in bounds: ${bounds.latSouth},${bounds.lonWest} to ${bounds.latNorth},${bounds.lonEast}")
        
        lastLoadJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Kleine Verzögerung um zu viele API-Calls zu vermeiden
                kotlinx.coroutines.delay(500)
                
                val restaurants = withContext(Dispatchers.IO) {
                    restaurantRepository.getRestaurantsInBounds(
                        bounds.latSouth,
                        bounds.lonWest,
                        bounds.latNorth,
                        bounds.lonEast
                    )
                }
                
                Log.d("MapFragment", "Found ${restaurants.size} restaurants in bounds")
                
                // Alte Marker entfernen
                mapView.overlays.removeAll { it is Marker && it != myLocationOverlay }
                
                // Neue Marker hinzufügen
                restaurants.forEach { restaurant ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(restaurant.latitude, restaurant.longitude)
                        title = restaurant.name
                        snippet = restaurant.address
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        // Icon basierend auf amenity type mit Material Icon Font
                        val (iconChar, color) = when (restaurant.amenityType) {
                            "cafe" -> MaterialIcons.LOCAL_CAFE to ContextCompat.getColor(requireContext(), R.color.marker_cafe)
                            "fast_food" -> MaterialIcons.FASTFOOD to ContextCompat.getColor(requireContext(), R.color.marker_fast_food)
                            else -> MaterialIcons.RESTAURANT to ContextCompat.getColor(requireContext(), R.color.marker_restaurant)
                        }
                        icon = MarkerIconHelper.createMarkerIcon(requireContext(), iconChar, color)
                        
                        // Click-Listener für den Marker
                        setOnMarkerClickListener { marker, mapView ->
                            marker.showInfoWindow()
                            mapView.controller.animateTo(marker.position)
                            
                            // Zeige Dialog zur Auswahl
                            showRestaurantDialog(restaurant)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
                
                // MyLocation Overlay wieder hinzufügen (falls entfernt)
                if (!mapView.overlays.contains(myLocationOverlay)) {
                    mapView.overlays.add(myLocationOverlay)
                }
                
                // Karte neu zeichnen
                mapView.invalidate()
                
            } catch (e: Exception) {
                Log.e("MapFragment", "Error loading restaurants in bounds", e)
            }
        }
    }
    
    private fun showRestaurantDialog(restaurant: Restaurant) {
        lifecycleScope.launch {
            // Lade alle Bewertungen für dieses Restaurant
            val reviews = withContext(Dispatchers.IO) {
                AppModule.reviewRepository.getAllReviews().first()
                    .filter { it.restaurantId == restaurant.id }
                    .sortedByDescending { it.visitDate }
            }
            
            // Custom View für den Dialog erstellen
            val dialogView = createRestaurantDialogView(restaurant, reviews)
            
            // Dialog anzeigen
            val buttonText = if (reviews.isNotEmpty()) "Erneut bewerten" else "Bewertung abgeben"
            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton(buttonText) { _, _ ->
                    val intent = Intent(requireContext(), AddReviewActivity::class.java).apply {
                        putExtra("restaurant_id", restaurant.id)
                        putExtra("restaurant_name", restaurant.name)
                        putExtra("restaurant_lat", restaurant.latitude)
                        putExtra("restaurant_lon", restaurant.longitude)
                        putExtra("restaurant_address", restaurant.address)
                        putExtra("restaurant_amenity_type", restaurant.amenityType)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Schließen", null)
                .create()
            
            dialog.show()
        }
    }
    
    private fun createRestaurantDialogView(restaurant: Restaurant, reviews: List<com.myreviews.app.domain.model.Review>): View {
        val context = requireContext()
        val density = context.resources.displayMetrics.density
        
        return ScrollView(context).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    (24 * density).toInt(),
                    (16 * density).toInt(),
                    (24 * density).toInt(),
                    (16 * density).toInt()
                )
                
                // Restaurant Name (Titel)
                addView(TextView(context).apply {
                    text = restaurant.name
                    textSize = 22f
                    setTextColor(0xFF000000.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                })
                
                // Adresse
                addView(TextView(context).apply {
                    text = restaurant.address
                    textSize = 14f
                    setTextColor(0xFF666666.toInt())
                    setPadding(0, (4 * density).toInt(), 0, 0)
                })
                
                // Separator
                addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * density).toInt()
                    ).apply {
                        setMargins(0, (16 * density).toInt(), 0, (16 * density).toInt())
                    }
                    setBackgroundColor(0xFFE0E0E0.toInt())
                })
                
                // Bewertungen Header
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    
                    addView(TextView(context).apply {
                        text = "Bewertungen"
                        textSize = 18f
                        setTextColor(0xFF000000.toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    })
                    
                    // Durchschnitt und Anzahl
                    if (reviews.isNotEmpty()) {
                        val average = reviews.map { it.rating }.average()
                        addView(TextView(context).apply {
                            text = "⭐ %.1f".format(average)
                            textSize = 16f
                            setTextColor(0xFFFFB300.toInt())
                            setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
                        })
                    }
                    
                    addView(TextView(context).apply {
                        text = "(${reviews.size})"
                        textSize = 16f
                        setTextColor(0xFF999999.toInt())
                    })
                })
                
                // Bewertungen oder Platzhalter
                if (reviews.isEmpty()) {
                    addView(TextView(context).apply {
                        text = "Noch keine Bewertungen vorhanden"
                        textSize = 14f
                        setTextColor(0xFF999999.toInt())
                        setPadding(0, (16 * density).toInt(), 0, 0)
                        setTypeface(null, android.graphics.Typeface.ITALIC)
                    })
                } else {
                    // Bewertungsliste (max 3 sichtbar)
                    val reviewContainer = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(0, (12 * density).toInt(), 0, 0)
                    }
                    
                    reviews.take(5).forEachIndexed { index, review ->
                        if (index > 0) {
                            // Separator zwischen Bewertungen
                            reviewContainer.addView(View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    (1 * density).toInt()
                                ).apply {
                                    setMargins(0, (8 * density).toInt(), 0, (8 * density).toInt())
                                }
                                setBackgroundColor(0xFFF0F0F0.toInt())
                            })
                        }
                        
                        // Einzelne Bewertung
                        reviewContainer.addView(createReviewItemView(context, review))
                    }
                    
                    // ScrollView für Bewertungen wenn mehr als 3
                    if (reviews.size > 3) {
                        val scrollableReviews = ScrollView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                (200 * density).toInt() // Max Höhe für 3 Einträge
                            )
                            addView(reviewContainer)
                        }
                        addView(scrollableReviews)
                    } else {
                        addView(reviewContainer)
                    }
                    
                    // Hinweis wenn es mehr Bewertungen gibt
                    if (reviews.size > 5) {
                        addView(TextView(context).apply {
                            text = "... und ${reviews.size - 5} weitere Bewertungen"
                            textSize = 12f
                            setTextColor(0xFF999999.toInt())
                            setPadding(0, (8 * density).toInt(), 0, 0)
                            setTypeface(null, android.graphics.Typeface.ITALIC)
                        })
                    }
                }
            })
        }
    }
    
    private fun createReviewItemView(context: Context, review: com.myreviews.app.domain.model.Review): View {
        val density = context.resources.displayMetrics.density
        
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            
            // Rating und Datum
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                
                // Sterne
                addView(TextView(context).apply {
                    text = "★".repeat(review.rating.toInt()) + "☆".repeat(5 - review.rating.toInt())
                    textSize = 14f
                    setTextColor(0xFFFFB300.toInt())
                })
                
                // Datum
                addView(TextView(context).apply {
                    text = " • ${java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMAN).format(review.visitDate)}"
                    textSize = 12f
                    setTextColor(0xFF999999.toInt())
                })
            })
            
            // Kommentar
            if (review.comment.isNotEmpty()) {
                addView(TextView(context).apply {
                    text = review.comment
                    textSize = 14f
                    setTextColor(0xFF333333.toInt())
                    setPadding(0, (4 * density).toInt(), 0, 0)
                    maxLines = 3
                    ellipsize = android.text.TextUtils.TruncateAt.END
                })
            }
        }
    }
}