package com.myreviews.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.myreviews.app.ui.map.MapFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, MapFragment())
                .commit()
        }
    }
}