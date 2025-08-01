package com.myreviews.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var cloudSyncSwitch: SwitchCompat
    private lateinit var serverUrlEditText: EditText
    private lateinit var serverPortEditText: EditText
    private lateinit var testConnectionButton: Button
    private lateinit var saveButton: Button
    private lateinit var statusTextView: TextView
    
    private lateinit var sharedPrefs: SharedPreferences
    
    companion object {
        const val PREFS_NAME = "MyReviewsPrefs"
        const val KEY_CLOUD_SYNC_ENABLED = "cloud_sync_enabled"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_SERVER_PORT = "server_port"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        setupUI()
        loadSettings()
        setupListeners()
    }
    
    private fun setupUI() {
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // Titel
        layout.addView(TextView(this).apply {
            text = "Einstellungen"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        })
        
        // Cloud Sync Switch
        layout.addView(TextView(this).apply {
            text = "Cloud-Synchronisation"
            textSize = 18f
            setPadding(0, 0, 0, 16)
        })
        
        cloudSyncSwitch = SwitchCompat(this).apply {
            text = "Mit Server synchronisieren"
            textSize = 16f
            setPadding(0, 0, 0, 24)
        }
        layout.addView(cloudSyncSwitch)
        
        // Server Einstellungen
        layout.addView(TextView(this).apply {
            text = "Server-Einstellungen"
            textSize = 18f
            setPadding(0, 16, 0, 16)
        })
        
        // Server URL
        layout.addView(TextView(this).apply {
            text = "Server-Adresse (IP oder Domain)"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        })
        
        serverUrlEditText = EditText(this).apply {
            hint = "z.B. 192.168.1.100 oder meinserver.local"
            setSingleLine()
            setBackgroundResource(android.R.drawable.edit_text)
            setPadding(16, 16, 16, 16)
        }
        layout.addView(serverUrlEditText)
        
        // Server Port
        layout.addView(TextView(this).apply {
            text = "Port"
            textSize = 14f
            setPadding(0, 16, 0, 8)
        })
        
        serverPortEditText = EditText(this).apply {
            hint = "z.B. 3000"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine()
            setBackgroundResource(android.R.drawable.edit_text)
            setPadding(16, 16, 16, 16)
        }
        layout.addView(serverPortEditText)
        
        // Test Connection Button
        testConnectionButton = Button(this).apply {
            text = "Verbindung testen"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 32
            }
        }
        layout.addView(testConnectionButton)
        
        // Status TextView
        statusTextView = TextView(this).apply {
            textSize = 14f
            setPadding(0, 16, 0, 16)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        layout.addView(statusTextView)
        
        // Speichern Button
        saveButton = Button(this).apply {
            text = "Einstellungen speichern"
            setBackgroundColor(0xFF4CAF50.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 48
            }
        }
        layout.addView(saveButton)
        
        scrollView.addView(layout)
        setContentView(scrollView)
        
        // Action Bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "Einstellungen"
    }
    
    private fun loadSettings() {
        cloudSyncSwitch.isChecked = sharedPrefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
        serverUrlEditText.setText(sharedPrefs.getString(KEY_SERVER_URL, ""))
        serverPortEditText.setText(sharedPrefs.getString(KEY_SERVER_PORT, "3000"))
        
        updateUIState()
    }
    
    private fun setupListeners() {
        cloudSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateUIState()
        }
        
        testConnectionButton.setOnClickListener {
            testConnection()
        }
        
        saveButton.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun updateUIState() {
        val isEnabled = cloudSyncSwitch.isChecked
        serverUrlEditText.isEnabled = isEnabled
        serverPortEditText.isEnabled = isEnabled
        testConnectionButton.isEnabled = isEnabled
        
        if (!isEnabled) {
            statusTextView.text = ""
        }
    }
    
    private fun testConnection() {
        val url = serverUrlEditText.text.toString().trim()
        val port = serverPortEditText.text.toString().trim()
        
        if (url.isEmpty() || port.isEmpty()) {
            statusTextView.text = "❌ Bitte Server-Adresse und Port eingeben"
            statusTextView.setTextColor(0xFFFF0000.toInt())
            return
        }
        
        statusTextView.text = "⏳ Teste Verbindung..."
        statusTextView.setTextColor(0xFF666666.toInt())
        
        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val testUrl = URL("http://$url:$port/api/health")
                    val connection = testUrl.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val responseCode = connection.responseCode
                    connection.disconnect()
                    
                    responseCode == 200
                } catch (e: Exception) {
                    false
                }
            }
            
            if (result) {
                statusTextView.text = "✅ Verbindung erfolgreich!"
                statusTextView.setTextColor(0xFF4CAF50.toInt())
            } else {
                statusTextView.text = "❌ Keine Verbindung zum Server möglich"
                statusTextView.setTextColor(0xFFFF0000.toInt())
            }
        }
    }
    
    private fun saveSettings() {
        val editor = sharedPrefs.edit()
        editor.putBoolean(KEY_CLOUD_SYNC_ENABLED, cloudSyncSwitch.isChecked)
        editor.putString(KEY_SERVER_URL, serverUrlEditText.text.toString().trim())
        editor.putString(KEY_SERVER_PORT, serverPortEditText.text.toString().trim())
        editor.apply()
        
        Toast.makeText(this, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}