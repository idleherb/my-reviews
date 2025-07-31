package com.myreviews.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.myreviews.app.ui.map.MapFragment
import com.myreviews.app.ui.search.SearchFragment
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Erstelle Layout programmatisch
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // TabLayout
        tabLayout = TabLayout(this)
        layout.addView(tabLayout)
        
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
    
    private class ViewPagerAdapter(fragmentActivity: FragmentActivity) : 
        FragmentStateAdapter(fragmentActivity) {
        
        override fun getItemCount(): Int = 2
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MapFragment()
                1 -> SearchFragment.newInstance()
                else -> MapFragment()
            }
        }
    }
}