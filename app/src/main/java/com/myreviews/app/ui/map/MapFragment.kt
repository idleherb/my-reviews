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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class MapFragment : Fragment() {
    
    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val restaurantRepository = RestaurantRepository()
    
    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
        // Grüne Meile 25, 69115 Heidelberg
        private val HEIDELBERG_CENTER = GeoPoint(49.409445, 8.693886)
        private const val DEFAULT_ZOOM = 16.0
        
        // Singleton für den aktuellen Kartenbereich
        var currentMapBounds: org.osmdroid.util.BoundingBox? = null
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
        mapView = MapView(requireContext()).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(HEIDELBERG_CENTER)
        }
        
        // User-Agent setzen für OSMDroid (wichtig!)
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        // Kartenbewegungen überwachen
        mapView.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                updateMapBounds()
                return false
            }
            
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                updateMapBounds()
                return false
            }
        })
        
        setupLocationOverlay()
        requestPermissionsIfNecessary()
        updateMapBounds()
        
        return mapView
    }
    
    private fun updateMapBounds() {
        currentMapBounds = mapView.boundingBox
        Log.d("MapFragment", "Map bounds updated: ${currentMapBounds}")
    }
    
    private fun setupLocationOverlay() {
        myLocationOverlay = MyLocationNewOverlay(
            GpsMyLocationProvider(requireContext()),
            mapView
        ).apply {
            enableMyLocation()
            enableFollowLocation()
            
            // Icon für den aktuellen Standort anpassen
            setPersonHotspot(24.0f, 24.0f)
        }
        mapView.overlays.add(myLocationOverlay)
        
        // Nach 2 Sekunden zur aktuellen Position zoomen und Restaurants laden
        mapView.postDelayed({
            myLocationOverlay.myLocation?.let { location ->
                mapView.controller.animateTo(location)
                mapView.controller.setZoom(17.0)
                
                // Restaurants in der Nähe laden
                loadNearbyRestaurants(location)
            }
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
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val restaurants = withContext(Dispatchers.IO) {
                    restaurantRepository.getRestaurantsNearby(
                        userLocation.latitude,
                        userLocation.longitude,
                        1000 // 1km Radius
                    )
                }
                
                // Marker für jedes Restaurant hinzufügen
                restaurants.forEach { restaurant ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(restaurant.latitude, restaurant.longitude)
                        title = restaurant.name
                        snippet = restaurant.address
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        // Click-Listener für den Marker
                        setOnMarkerClickListener { marker, mapView ->
                            marker.showInfoWindow()
                            mapView.controller.animateTo(marker.position)
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
}