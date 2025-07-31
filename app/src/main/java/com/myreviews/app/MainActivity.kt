package com.myreviews.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.myreviews.app.ui.ViewPagerAdapter
import com.myreviews.app.di.ServiceLocator
import com.myreviews.app.di.SearchServiceType
import android.widget.LinearLayout
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import android.widget.Button
import android.app.AlertDialog
import android.util.Log

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Erstelle Layout programmatisch
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // Header mit TabLayout und Button
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // TabLayout
        tabLayout = TabLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // weight
            )
        }
        headerLayout.addView(tabLayout)
        
        // API-Wechsel Button (kompakt mit nur Icon)
        val apiButton = Button(this).apply {
            text = "⋮"  // Drei-Punkte-Menü (vertikale Ellipse)
            textSize = 24f
            minWidth = 96  // 24dp in Pixel (bei 4x Density)
            minHeight = 0
            minimumWidth = 96
            minimumHeight = 0
            setPadding(0, 0, 0, 0)  // Kein internes Padding
            background = null  // Kein Hintergrund
            layoutParams = LinearLayout.LayoutParams(
                96,  // 24dp * 4 = 96 Pixel bei xxhdpi
                LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(16, 0, 16, 0)  // Mehr Abstand außen für bessere Touch-Erfahrung
            }
            setOnClickListener {
                showApiSelectionDialog()
            }
        }
        headerLayout.addView(apiButton)
        
        Log.d("MainActivity", "API Button added to header layout")
        
        layout.addView(headerLayout)
        
        // ViewPager2
        viewPager = ViewPager2(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        layout.addView(viewPager)
        
        setContentView(layout)
        
        setupViewPager()
    }
    
    private fun setupViewPager() {
        viewPager.adapter = ViewPagerAdapter(this)
        
        // Behalte beide Tabs im Speicher, damit die Karte nicht neu geladen wird
        viewPager.offscreenPageLimit = 1
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Karte"
                1 -> "Suche"
                else -> ""
            }
        }.attach()
    }
    
    private fun showApiSelectionDialog() {
        val apis = arrayOf("Overpass API", "Nominatim API")
        val currentSelection = when (ServiceLocator.currentSearchService) {
            SearchServiceType.OVERPASS -> 0
            SearchServiceType.NOMINATIM -> 1
        }
        
        AlertDialog.Builder(this)
            .setTitle("Wähle Such-API")
            .setSingleChoiceItems(apis, currentSelection) { dialog, which ->
                when (which) {
                    0 -> {
                        ServiceLocator.switchToOverpass()
                        Toast.makeText(this, "Verwende jetzt Overpass API", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        ServiceLocator.switchToNominatim()
                        Toast.makeText(this, "Verwende jetzt Nominatim API", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .show()
    }
}