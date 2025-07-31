package com.myreviews.app.ui.search

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.myreviews.app.data.repository.RestaurantRepository
import com.myreviews.app.di.AppModule
import com.myreviews.app.domain.model.Restaurant
import com.myreviews.app.MainActivity
import com.myreviews.app.ui.map.MapFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.LocationManager
import android.content.Context
import android.util.Log

class SearchFragment : Fragment() {
    
    private lateinit var searchView: SearchView
    private lateinit var resultsListView: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    
    private val restaurantRepository = AppModule.restaurantRepository
    private var searchJob: Job? = null
    private var currentLocation: Location? = null
    
    companion object {
        fun newInstance() = SearchFragment()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getCurrentLocation()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            
            // SearchView
            searchView = SearchView(context).apply {
                queryHint = "Restaurant suchen..."
                isIconified = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            addView(searchView)
            
            // ProgressBar
            progressBar = ProgressBar(context).apply {
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                    topMargin = 32
                }
            }
            addView(progressBar)
            
            // Empty view
            emptyView = TextView(context).apply {
                text = "Gib einen Restaurant-Namen ein"
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 32
                }
            }
            addView(emptyView)
            
            // Results ListView
            resultsListView = ListView(context).apply {
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            addView(resultsListView)
        }
        
        setupSearchView()
        
        return view
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchRestaurants(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                if (newText.isNullOrEmpty()) {
                    showEmptyState()
                } else if (newText.length >= 3) {
                    // Debounce: Warte 500ms bevor Suche startet
                    searchJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(500)
                        searchRestaurants(newText)
                    }
                }
                return true
            }
        })
    }
    
    private fun searchRestaurants(query: String) {
        Log.d("SearchFragment", "Starting search for: $query")
        showLoading()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("SearchFragment", "Calling repository...")
                val results = withContext(Dispatchers.IO) {
                    restaurantRepository.searchRestaurantsByName(
                        query,
                        currentLocation?.latitude,
                        currentLocation?.longitude
                    )
                }
                Log.d("SearchFragment", "Got ${results.size} results")
                
                if (results.isEmpty()) {
                    showNoResults()
                } else {
                    showResults(results)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                showError()
            }
        }
    }
    
    private fun showResults(restaurants: List<Restaurant>) {
        progressBar.visibility = View.GONE
        emptyView.visibility = View.GONE
        resultsListView.visibility = View.VISIBLE
        
        val adapter = RestaurantAdapter(requireContext(), restaurants, currentLocation)
        resultsListView.adapter = adapter
        
        resultsListView.setOnItemClickListener { _, _, position, _ ->
            val restaurant = restaurants[position]
            
            // Zur Karte wechseln und Restaurant zeigen
            (activity as? MainActivity)?.let { mainActivity ->
                // Restaurant-Position für MapFragment speichern
                MapFragment.pendingRestaurant = restaurant
                
                // Zum Karten-Tab wechseln
                mainActivity.switchToMapTab()
            }
        }
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        resultsListView.visibility = View.GONE
    }
    
    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = "Gib einen Restaurant-Namen ein"
        resultsListView.visibility = View.GONE
    }
    
    private fun showNoResults() {
        progressBar.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = "Keine Restaurants gefunden"
        resultsListView.visibility = View.GONE
    }
    
    private fun showError() {
        progressBar.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = "Fehler bei der Suche"
        resultsListView.visibility = View.GONE
    }
    
    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
    }
    
    // Adapter für die Restaurant-Liste
    private class RestaurantAdapter(
        context: Context,
        private val restaurants: List<Restaurant>,
        private val userLocation: Location?
    ) : ArrayAdapter<Restaurant>(context, android.R.layout.simple_list_item_2, restaurants) {
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                android.R.layout.simple_list_item_2, 
                parent, 
                false
            )
            
            val restaurant = restaurants[position]
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)
            
            text1.text = restaurant.name
            
            // Zeige Adresse und Entfernung
            val distanceText = userLocation?.let {
                val distance = calculateDistance(
                    it.latitude, it.longitude,
                    restaurant.latitude, restaurant.longitude
                )
                String.format("%.1f km - ", distance)
            } ?: ""
            
            text2.text = "$distanceText${restaurant.address}"
            
            return view
        }
        
        private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            return results[0] / 1000.0 // Konvertiere zu Kilometern
        }
    }
}