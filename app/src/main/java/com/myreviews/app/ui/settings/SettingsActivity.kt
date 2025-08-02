package com.myreviews.app.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.myreviews.app.di.ServiceLocator
import com.myreviews.app.di.SearchServiceType
import com.myreviews.app.di.AppModule
import com.myreviews.app.data.api.AvatarService
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
    private lateinit var autoSyncSwitch: SwitchCompat
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
    private lateinit var avatarImageView: ImageView
    private lateinit var cloudSyncContainer: LinearLayout
    private lateinit var statusContainer: LinearLayout
    
    private lateinit var sharedPrefs: SharedPreferences
    private var connectionTestPassed = false
    private var connectionTestPerformed = false
    private var currentUserId: String = ""
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadAvatar(it) }
    }
    
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
        
        // AutoSync Switch (nur sichtbar wenn Cloud-Sync an)
        autoSyncSwitch = SwitchCompat(this).apply {
            text = "Automatisch synchronisieren"
            textSize = 16f
            setPadding(0, 0, 0, 16)
            visibility = View.GONE // Startet versteckt
        }
        layout.addView(autoSyncSwitch)
        
        // Container für Cloud-Sync bezogene Einstellungen
        val cloudSyncContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE // Startet versteckt
        }
        
        // Benutzer Section (nur bei Cloud-Sync)
        cloudSyncContainer.addView(TextView(this).apply {
            text = "Benutzer-Profil"
            textSize = 18f
            setPadding(0, 16, 0, 16)
        })
        
        // Benutzername
        cloudSyncContainer.addView(TextView(this).apply {
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
        cloudSyncContainer.addView(userNameEditText)
        
        // User ID (klein und grau)
        userIdTextView = TextView(this).apply {
            textSize = 12f
            setTextColor(0xFF999999.toInt())
            setPadding(0, 8, 0, 0)
        }
        cloudSyncContainer.addView(userIdTextView)
        
        // Avatar Section
        cloudSyncContainer.addView(TextView(this).apply {
            text = "Profilbild"
            textSize = 14f
            setPadding(0, 24, 0, 8)
            setTextColor(0xFF666666.toInt())
        })
        
        val avatarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        
        // Avatar ImageView
        avatarImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                setMargins(0, 0, 16, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(android.R.drawable.ic_menu_gallery)
        }
        avatarLayout.addView(avatarImageView)
        
        // Avatar Buttons
        val avatarButtonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val changeAvatarButton = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "Foto ändern"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
            setOnClickListener {
                showAvatarPicker()
            }
        }
        avatarButtonsLayout.addView(changeAvatarButton)
        
        val deleteAvatarButton = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "Foto löschen"
            setTextColor(0xFFFF5252.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                deleteAvatar()
            }
        }
        avatarButtonsLayout.addView(deleteAvatarButton)
        
        avatarLayout.addView(avatarButtonsLayout)
        cloudSyncContainer.addView(avatarLayout)
        
        // Trennlinie vor Server-Einstellungen
        cloudSyncContainer.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 32
                bottomMargin = 32
            }
            setBackgroundColor(0xFFDDDDDD.toInt())
        })
        
        // Server Einstellungen
        cloudSyncContainer.addView(TextView(this).apply {
            text = "Server-Einstellungen"
            textSize = 18f
            setPadding(0, 0, 0, 16)
        })
        
        // Server URL
        cloudSyncContainer.addView(TextView(this).apply {
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
        cloudSyncContainer.addView(serverUrlEditText)
        
        // Server Port
        cloudSyncContainer.addView(TextView(this).apply {
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
        cloudSyncContainer.addView(serverPortEditText)
        
        // Test Connection Button (Secondary Style - Text Button)
        testConnectionButton = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "Verbindung testen"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
            }
        }
        cloudSyncContainer.addView(testConnectionButton)
        
        // Status Container (Material Design inline message)
        val statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
        }
        
        statusTextView = TextView(this).apply {
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        statusContainer.addView(statusTextView)
        cloudSyncContainer.addView(statusContainer)
        
        // Store reference for later use
        this.statusContainer = statusContainer
        
        // CloudSyncContainer zu Layout hinzufügen
        layout.addView(cloudSyncContainer)
        
        // Speichere Referenz für spätere Sichtbarkeitsänderungen
        this.cloudSyncContainer = cloudSyncContainer
        
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
        
        // Sync Button (Secondary Style - nur wenn Cloud-Sync aktiviert)
        val syncButton = MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
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
        
        // Track if user made changes
        userNameEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // Show sync button if cloud sync is enabled and name changed
                if (cloudSyncSwitch.isChecked) {
                    syncButton.visibility = View.VISIBLE
                }
            }
        })
        
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
            
            // Cloud-only: Load avatar from server if cloud sync is enabled
            if (sharedPrefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)) {
                val cloudAvatarService = AppModule.getCloudAvatarService()
                cloudAvatarService?.let { service ->
                    val avatarUrl = service.getUserAvatarUrl(currentUserId)
                    avatarUrl?.let {
                        if (it.startsWith("/")) {
                            val serverUrl = sharedPrefs.getString(KEY_SERVER_URL, "") ?: ""
                            val serverPort = sharedPrefs.getString(KEY_SERVER_PORT, "3000") ?: "3000"
                            val fullUrl = "http://$serverUrl:$serverPort$it"
                            loadAvatarFromUrl(fullUrl)
                        }
                    }
                }
            }
        }
        
        // API-Auswahl laden
        when (ServiceLocator.currentSearchService) {
            SearchServiceType.OVERPASS -> overpassRadioButton.isChecked = true
            SearchServiceType.NOMINATIM -> nominatimRadioButton.isChecked = true
        }
        
        // Cloud-Sync Einstellungen laden
        cloudSyncSwitch.isChecked = sharedPrefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)
        autoSyncSwitch.isChecked = AppModule.autoSyncManager.isAutoSyncEnabled()
        val defaultHost = resources.getString(com.myreviews.app.R.string.default_server_host)
        val defaultPort = resources.getString(com.myreviews.app.R.string.default_server_port)
        serverUrlEditText.setText(sharedPrefs.getString(KEY_SERVER_URL, defaultHost))
        serverPortEditText.setText(sharedPrefs.getString(KEY_SERVER_PORT, defaultPort))
        
        // AutoSync und Container sichtbar machen wenn Cloud-Sync aktiviert ist
        autoSyncSwitch.visibility = if (cloudSyncSwitch.isChecked) View.VISIBLE else View.GONE
        cloudSyncContainer.visibility = if (cloudSyncSwitch.isChecked) View.VISIBLE else View.GONE
        
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
                statusContainer.visibility = View.GONE
                cloudSyncContainer.visibility = View.GONE
                autoSyncSwitch.visibility = View.GONE
            } else {
                cloudSyncContainer.visibility = View.VISIBLE
                autoSyncSwitch.visibility = View.VISIBLE
                // Prüfe ob Benutzername noch Anonym ist
                val currentName = userNameEditText.text.toString().trim()
                if (currentName.isEmpty() || currentName == "Anonym") {
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
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
            // Sync button nur sichtbar wenn Cloud-Sync an und AutoSync aus
            updateSyncButtonVisibility()
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
        
        autoSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateSyncButtonVisibility()
        }
        
        testConnectionButton.setOnClickListener {
            testConnection()
        }
        
        saveButton.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun updateSyncButtonVisibility() {
        // Sync button nur sichtbar wenn Cloud-Sync an UND AutoSync aus
        val showSyncButton = cloudSyncSwitch.isChecked && !autoSyncSwitch.isChecked
        syncButton.visibility = if (showSyncButton) View.VISIBLE else View.GONE
    }
    
    private fun updateUIState() {
        val isCloudSyncEnabled = cloudSyncSwitch.isChecked
        
        // Save Button nur deaktivieren wenn:
        // Cloud-Sync an ist UND Verbindungstest durchgeführt wurde UND fehlgeschlagen ist
        val canSave = !isCloudSyncEnabled || !connectionTestPerformed || connectionTestPassed
        saveButton.isEnabled = canSave
        
        // Save Button visuell anpassen (Material Design)
        if (canSave) {
            saveButton.alpha = 1f
        } else {
            saveButton.alpha = 0.5f
        }
        
        if (!isCloudSyncEnabled) {
            statusContainer.visibility = View.GONE
            connectionTestPerformed = false // Reset wenn Cloud-Sync deaktiviert wird
        }
    }
    
    private fun showStatus(message: String, type: StatusType) {
        statusContainer.removeAllViews()
        
        // Icon
        val iconView = TextView(this).apply {
            val typeface = androidx.core.content.res.ResourcesCompat.getFont(
                this@SettingsActivity, 
                com.myreviews.app.R.font.material_icons_regular
            )
            setTypeface(typeface)
            textSize = 20f
            setPadding(0, 0, 12, 0)
            
            when (type) {
                StatusType.SUCCESS -> {
                    text = "\uE876" // check_circle
                    setTextColor(0xFF4CAF50.toInt())
                }
                StatusType.ERROR -> {
                    text = "\uE000" // error
                    setTextColor(0xFFFF5252.toInt())
                }
                StatusType.INFO -> {
                    text = "\uE88E" // info
                    setTextColor(0xFF2196F3.toInt())
                }
                StatusType.LOADING -> {
                    text = "\uE86A" // sync
                    setTextColor(0xFF757575.toInt())
                }
            }
        }
        statusContainer.addView(iconView)
        
        // Message
        val messageView = TextView(this).apply {
            text = message
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            
            when (type) {
                StatusType.SUCCESS -> setTextColor(0xFF4CAF50.toInt())
                StatusType.ERROR -> setTextColor(0xFFFF5252.toInt())
                StatusType.INFO -> setTextColor(0xFF2196F3.toInt())
                StatusType.LOADING -> setTextColor(0xFF757575.toInt())
            }
        }
        statusContainer.addView(messageView)
        
        // Background
        statusContainer.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 8f * resources.displayMetrics.density
            when (type) {
                StatusType.SUCCESS -> setColor(0x1A4CAF50.toInt())
                StatusType.ERROR -> setColor(0x1AFF5252.toInt())
                StatusType.INFO -> setColor(0x1A2196F3.toInt())
                StatusType.LOADING -> setColor(0x1A757575.toInt())
            }
        }
        
        statusContainer.visibility = View.VISIBLE
    }
    
    private enum class StatusType {
        SUCCESS, ERROR, INFO, LOADING
    }
    
    private fun testConnection() {
        val url = serverUrlEditText.text.toString().trim()
        val port = serverPortEditText.text.toString().trim()
        
        if (url.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Bitte Server-Adresse und Port eingeben", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Zeige Loading-Status während des Tests
        statusContainer.visibility = View.VISIBLE
        showStatus("Teste Verbindung...", StatusType.LOADING)
        
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
                    e.printStackTrace()
                    false
                }
            }
            
            connectionTestPerformed = true
            
            if (result) {
                connectionTestPassed = true
                Toast.makeText(this@SettingsActivity, "Verbindung erfolgreich!", Toast.LENGTH_SHORT).show()
                statusContainer.visibility = View.GONE
            } else {
                connectionTestPassed = false
                Toast.makeText(this@SettingsActivity, "Keine Verbindung zum Server möglich", Toast.LENGTH_LONG).show()
                statusContainer.visibility = View.GONE
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
                
                // Bei Cloud-Sync: Info zeigen
                if (cloudSyncSwitch.isChecked && currentUser.userName == "Anonym" && newUserName != "Anonym") {
                    Toast.makeText(this@SettingsActivity,
                        "Deine Bewertungen werden jetzt mit dem Namen '$newUserName' synchronisiert.",
                        Toast.LENGTH_LONG).show()
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
            
            // AutoSync-Einstellungen speichern
            AppModule.autoSyncManager.setAutoSyncEnabled(autoSyncSwitch.isChecked)
            
            // AutoSync triggern wenn aktiviert (für Username/Avatar-Sync)
            if (cloudSyncSwitch.isChecked && autoSyncSwitch.isChecked) {
                AppModule.autoSyncManager.triggerSyncIfEnabled("settings_saved")
            }
            
            Toast.makeText(this@SettingsActivity, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun performSync() {
        // Speichere die aktuellen Einstellungen bevor Sync ausgeführt wird
        val editor = sharedPrefs.edit()
        editor.putBoolean(KEY_CLOUD_SYNC_ENABLED, cloudSyncSwitch.isChecked)
        editor.putString(KEY_SERVER_URL, serverUrlEditText.text.toString().trim())
        editor.putString(KEY_SERVER_PORT, serverPortEditText.text.toString().trim())
        editor.apply()
        
        syncButton.isEnabled = false
        showStatus("Synchronisiere...", StatusType.LOADING)
        
        lifecycleScope.launch {
            val result = AppModule.syncRepository.performSync()
            
            when (result) {
                is SyncResult.Success -> {
                    val message = result.message ?: "${result.syncedCount} Bewertungen synchronisiert"
                    Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
                }
                is SyncResult.Error -> {
                    Toast.makeText(this@SettingsActivity, "Sync fehlgeschlagen: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            syncButton.isEnabled = true
        }
    }
    
    private fun showAvatarPicker() {
        pickImageLauncher.launch("image/*")
    }
    
    private fun uploadAvatar(uri: Uri) {
        lifecycleScope.launch {
            try {
                showStatus("Lade Profilbild hoch...", StatusType.LOADING)
                
                val serverUrl = sharedPrefs.getString(KEY_SERVER_URL, "") ?: ""
                val serverPort = sharedPrefs.getString(KEY_SERVER_PORT, "3000") ?: "3000"
                
                if (serverUrl.isEmpty() || !sharedPrefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)) {
                    showStatus("Cloud-Sync muss aktiviert sein", StatusType.ERROR)
                    return@launch
                }
                
                val avatarService = AvatarService("http://$serverUrl:$serverPort")
                val avatarUrl = avatarService.uploadAvatar(this@SettingsActivity, currentUserId, uri)
                
                if (avatarUrl != null) {
                    // Cloud-only: Update cache and UI
                    AppModule.getCloudAvatarService()?.updateCache(currentUserId, avatarUrl)
                    
                    withContext(Dispatchers.Main) {
                        // Show the uploaded image
                        avatarImageView.setImageURI(uri)
                        Toast.makeText(this@SettingsActivity, "Profilbild hochgeladen", Toast.LENGTH_SHORT).show()
                        
                        // Notify other components about avatar change
                        setResult(RESULT_OK)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "Fehler beim Hochladen", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun deleteAvatar() {
        lifecycleScope.launch {
            try {
                showStatus("Lösche Profilbild...", StatusType.LOADING)
                
                val serverUrl = sharedPrefs.getString(KEY_SERVER_URL, "") ?: ""
                val serverPort = sharedPrefs.getString(KEY_SERVER_PORT, "3000") ?: "3000"
                
                if (serverUrl.isEmpty() || !sharedPrefs.getBoolean(KEY_CLOUD_SYNC_ENABLED, false)) {
                    // Cloud sync not enabled, cannot delete
                    withContext(Dispatchers.Main) {
                        showStatus("Cloud-Sync muss aktiviert sein", StatusType.ERROR)
                    }
                    return@launch
                }
                
                val avatarService = AvatarService("http://$serverUrl:$serverPort")
                val success = avatarService.deleteAvatar(currentUserId)
                
                if (success) {
                    // Cloud-only: Update cache and UI
                    AppModule.getCloudAvatarService()?.updateCache(currentUserId, null)
                    
                    withContext(Dispatchers.Main) {
                        avatarImageView.setImageResource(android.R.drawable.ic_menu_gallery)
                        Toast.makeText(this@SettingsActivity, "Profilbild gelöscht", Toast.LENGTH_SHORT).show()
                        
                        // Notify other components about avatar change
                        setResult(RESULT_OK)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "Fehler beim Löschen", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun loadAvatarFromUrl(url: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val input = connection.inputStream
                    val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        avatarImageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                // Fallback to default image
                avatarImageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}