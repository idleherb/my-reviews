package com.myreviews.app.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.myreviews.app.ui.map.MapFragment
import com.myreviews.app.ui.search.SearchFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : 
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