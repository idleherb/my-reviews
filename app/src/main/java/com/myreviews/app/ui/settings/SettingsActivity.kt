package com.myreviews.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.myreviews.app.di.ServiceLocator
import com.myreviews.app.di.SearchServiceType
import com.myreviews.app.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection
import java.net.URL
import com.myreviews.app.data.api.SyncResult

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var cloudSyncSwitch: SwitchCompat
    private lateinit var serverUrlEditText: EditText
    private lateinit var serverPortEditText: EditText
    private lateinit var testConnectionButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var statusTextView: TextView
    private lateinit var overpassRadioButton: RadioButton
    private lateinit var nominatimRadioButton: RadioButton
    private lateinit var userNameEditText: EditText
    private lateinit var userIdTextView: TextView
    private lateinit var syncButton: MaterialButton
    
    private lateinit var sharedPrefs: SharedPreferences
    private var connectionTestPassed = false
    private var currentUserId: String = ""
    
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
        // Haupt-Layout mit LinearLayout statt nur ScrollView
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // ScrollView für den scrollbaren Inhalt
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Nimmt den verfügbaren Platz ein
            )
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_xl)
            setPadding(padding, padding, padding, padding)
        }
        
        // Header mit Titel und Close-Button
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val bottomPadding = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_xl)
            setPadding(0, 0, 0, bottomPadding)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        // Titel
        headerLayout.addView(TextView(this).apply {
            text = "Einstellungen"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })
        
        // Close Button (rechts)
        val closeButton = ImageButton(this).apply {
            val closeIcon = ContextCompat.getDrawable(this@SettingsActivity, com.myreviews.app.R.drawable.ic_close)
            closeIcon?.setTint(0xFF666666.toInt()) // Subtiles Grau
            setImageDrawable(closeIcon)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.myreviews.app.R.dimen.touch_target_min),
                resources.getDimensionPixelSize(com.myreviews.app.R.dimen.touch_target_min)
            )
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = "Schließen"
            setOnClickListener {
                finish()
            }
        }
        headerLayout.addView(closeButton)
        
        layout.addView(headerLayout)
        
        // Benutzer Section
        layout.addView(TextView(this).apply {
            text = "Benutzer"
            textSize = 18f
            setPadding(0, 0, 0, 16)
        })
        
        // Benutzername
        layout.addView(TextView(this).apply {
            text = "Dein Name"
            textSize = 14f
            setPadding(0, 0, 0, 8)
        })
        
        userNameEditText = EditText(this).apply {
            hint = "Anonym"
            setSingleLine()
            setBackgroundResource(android.R.drawable.edit_text)
            val editPadding = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_md)
            setPadding(editPadding, editPadding, editPadding, editPadding)
        }
        layout.addView(userNameEditText)
        
        // User ID (klein und grau)
        userIdTextView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 8, 0, 0)
        }
        layout.addView(userIdTextView)
        
        // Trennlinie
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 32
                bottomMargin = 32
            }
            setBackgroundColor(0xFFDDDDDD.toInt())
        })
        
        // API-Auswahl Section
        layout.addView(TextView(this).apply {
            text = "Such-API"
            textSize = 18f
            setPadding(0, 0, 0, 16)
        })
        
        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        
        overpassRadioButton = RadioButton(this).apply {
            text = "Overpass API (empfohlen)"
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        radioGroup.addView(overpassRadioButton)
        
        nominatimRadioButton = RadioButton(this).apply {
            text = "Nominatim API"
            textSize = 16f
            setPadding(0, 8, 0, 8)
        }
        radioGroup.addView(nominatimRadioButton)
        
        layout.addView(radioGroup)
        
        // Trennlinie
        layout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 32
                bottomMargin = 32
            }
            setBackgroundColor(0xFFDDDDDD.toInt())
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
            val editPadding = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_md)
            setPadding(editPadding, editPadding, editPadding, editPadding)
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
            val editPadding = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_md)
            setPadding(editPadding, editPadding, editPadding, editPadding)
        }
        layout.addView(serverPortEditText)
        
        // Test Connection Button (secondary style)
        testConnectionButton = MaterialButton(this).apply {
            text = "Verbindung testen"
            setTextColor(0xFF4CAF50.toInt()) // Grüne Schrift
            background = null // Kein Hintergrund
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
        
        // ScrollView mit dem Inhalt füllen
        scrollView.addView(layout)
        mainLayout.addView(scrollView)
        
        // Buttons außerhalb der ScrollView am unteren Rand
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val marginLR = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_xl)
                val marginTop = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_md)
                val marginBottom = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_xl)
                setMargins(marginLR, marginTop, marginLR, marginBottom)
            }
        }
        
        // Sync Button (nur wenn Cloud-Sync aktiviert)
        val syncButton = MaterialButton(this).apply {
            text = "Jetzt synchronisieren"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                rightMargin = resources.getDimensionPixelSize(com.myreviews.app.R.dimen.spacing_md)
            }
            visibility = if (cloudSyncSwitch.isChecked) View.VISIBLE else View.GONE
        }
        
        syncButton.setOnClickListener {
            performSync()
        }
        
        buttonLayout.addView(syncButton)
        
        // Speichern Button
        saveButton = MaterialButton(this).apply {
            text = "Einstellungen speichern"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        buttonLayout.addView(saveButton)
        
        mainLayout.addView(buttonLayout)
        
        // Store sync button reference for visibility updates
        this.syncButton = syncButton
        
        setContentView(mainLayout)
        
        // Action Bar verstecken, da wir einen eigenen Close-Button haben
        supportActionBar?.hide()
    }
    
    private fun loadSettings() {
        // User-Daten laden
        lifecycleScope.launch {
            val currentUser = AppModule.userRepository.ensureDefaultUser()
            currentUserId = currentUser.userId
            userNameEditText.setText(currentUser.userName)
            userIdTextView.text = "ID: ${currentUser.userId}"
        }
        
        // API-Auswahl laden
        when (ServiceLocator.currentSearchService) {
            SearchServiceType.OVERPASS -> overpassRadioButton.isChecked = true
            SearchServiceType.NOMINATIM -> nominatimRadioButton.isChecked = true
        }
        
        // Cloud-Sync Einstellungen laden
        cloudSyncSwitch.isChecked = sharedPrefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
        val defaultHost = resources.getString(com.myreviews.app.R.string.default_server_host)
        val defaultPort = resources.getString(com.myreviews.app.R.string.default_server_port)
        serverUrlEditText.setText(sharedPrefs.getString(KEY_SERVER_URL, defaultHost))
        serverPortEditText.setText(sharedPrefs.getString(KEY_SERVER_PORT, defaultPort))
        
        // Wenn Cloud-Sync aktiviert war, prüfe ob die Verbindung noch funktioniert
        if (cloudSyncSwitch.isChecked && serverUrlEditText.text.isNotEmpty()) {
            connectionTestPassed = false // Verbindung muss neu getestet werden
        }
        
        updateUIState()
    }
    
    private fun setupListeners() {
        cloudSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                connectionTestPassed = false
                statusTextView.text = ""
            } else {
                // Prüfe ob Benutzername noch Anonym ist
                val currentName = userNameEditText.text.toString().trim()
                if (currentName.isEmpty() || currentName == "Anonym") {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Benutzername festlegen")
                        .setMessage("Möchtest du einen Namen für deine Bewertungen festlegen? Dies hilft, deine Bewertungen beim Cloud-Sync zu identifizieren.")
                        .setPositiveButton("Name eingeben") { _, _ ->
                            userNameEditText.requestFocus()
                            userNameEditText.selectAll()
                        }
                        .setNegativeButton("Anonym bleiben", null)
                        .show()
                }
            }
            updateUIState()
            syncButton.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        serverUrlEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                connectionTestPassed = false
                updateUIState()
            }
        })
        
        serverPortEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                connectionTestPassed = false
                updateUIState()
            }
        })
        
        testConnectionButton.setOnClickListener {
            testConnection()
        }
        
        saveButton.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun updateUIState() {
        val isCloudSyncEnabled = cloudSyncSwitch.isChecked
        serverUrlEditText.isEnabled = isCloudSyncEnabled
        serverPortEditText.isEnabled = isCloudSyncEnabled
        testConnectionButton.isEnabled = isCloudSyncEnabled
        
        // Verbindung testen Button visuell anpassen
        if (isCloudSyncEnabled) {
            testConnectionButton.setTextColor(0xFF4CAF50.toInt()) // Grün
        } else {
            testConnectionButton.setTextColor(0xFFCCCCCC.toInt()) // Grau
        }
        
        // Save Button nur aktivieren wenn:
        // 1. Cloud-Sync aus ist ODER
        // 2. Cloud-Sync an ist UND Verbindungstest erfolgreich war
        val canSave = !isCloudSyncEnabled || connectionTestPassed
        saveButton.isEnabled = canSave
        
        // Save Button visuell anpassen
        if (canSave) {
            saveButton.setBackgroundColor(0xFF4CAF50.toInt()) // Grün
            saveButton.setTextColor(0xFFFFFFFF.toInt()) // Weiß
        } else {
            saveButton.setBackgroundColor(0xFFCCCCCC.toInt()) // Grau
            saveButton.setTextColor(0xFF666666.toInt()) // Dunkelgrau
        }
        
        if (!isCloudSyncEnabled) {
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
                connectionTestPassed = true
                statusTextView.text = "✅ Verbindung erfolgreich!"
                statusTextView.setTextColor(0xFF4CAF50.toInt())
            } else {
                connectionTestPassed = false
                statusTextView.text = "❌ Keine Verbindung zum Server möglich"
                statusTextView.setTextColor(0xFFFF0000.toInt())
            }
            updateUIState()
        }
    }
    
    private fun saveSettings() {
        lifecycleScope.launch {
            // Benutzername speichern
            val newUserName = userNameEditText.text.toString().trim().ifEmpty { "Anonym" }
            val currentUser = AppModule.userRepository.getCurrentUser()
            
            if (currentUser != null && currentUser.userName != newUserName) {
                // Username hat sich geändert
                AppModule.userRepository.updateUserName(currentUser.userId, newUserName)
                
                // Alle Reviews dieses Users aktualisieren
                AppModule.reviewRepository.updateUserNameInReviews(currentUser.userId, newUserName)
                
                // Bei Cloud-Sync: Dialog zeigen
                if (cloudSyncSwitch.isChecked && currentUser.userName == "Anonym" && newUserName != "Anonym") {
                    android.app.AlertDialog.Builder(this@SettingsActivity)
                        .setTitle("Benutzername für Cloud-Sync")
                        .setMessage("Deine Bewertungen werden jetzt mit dem Namen '$newUserName' synchronisiert.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            
            // API-Auswahl speichern
            when {
                overpassRadioButton.isChecked -> ServiceLocator.switchToOverpass()
                nominatimRadioButton.isChecked -> ServiceLocator.switchToNominatim()
            }
            
            // Cloud-Sync Einstellungen speichern
            val editor = sharedPrefs.edit()
            editor.putBoolean(KEY_CLOUD_SYNC_ENABLED, cloudSyncSwitch.isChecked)
            editor.putString(KEY_SERVER_URL, serverUrlEditText.text.toString().trim())
            editor.putString(KEY_SERVER_PORT, serverPortEditText.text.toString().trim())
            editor.apply()
            
            Toast.makeText(this@SettingsActivity, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun performSync() {
        syncButton.isEnabled = false
        statusTextView.text = "⏳ Synchronisiere..."
        statusTextView.setTextColor(0xFF666666.toInt())
        
        lifecycleScope.launch {
            val result = AppModule.syncRepository.performSync()
            
            when (result) {
                is SyncResult.Success -> {
                    statusTextView.text = "✅ ${result.syncedCount} Bewertungen synchronisiert"
                    statusTextView.setTextColor(0xFF4CAF50.toInt())
                }
                is SyncResult.Error -> {
                    statusTextView.text = "❌ Sync fehlgeschlagen: ${result.message}"
                    statusTextView.setTextColor(0xFFFF0000.toInt())
                }
            }
            
            syncButton.isEnabled = true
        }
    }
}