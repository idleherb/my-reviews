package com.myreviews.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.myreviews.app.ui.settings.SettingsActivity

class AppPreferences(context: Context) {
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
    
    var isCloudSyncEnabled: Boolean
        get() = sharedPrefs.getBoolean(SettingsActivity.KEY_CLOUD_SYNC_ENABLED, false)
        set(value) = sharedPrefs.edit().putBoolean(SettingsActivity.KEY_CLOUD_SYNC_ENABLED, value).apply()
    
    var serverUrl: String
        get() = sharedPrefs.getString(SettingsActivity.KEY_SERVER_URL, "") ?: ""
        set(value) = sharedPrefs.edit().putString(SettingsActivity.KEY_SERVER_URL, value).apply()
    
    var serverPort: String
        get() = sharedPrefs.getString(SettingsActivity.KEY_SERVER_PORT, "3000") ?: "3000"
        set(value) = sharedPrefs.edit().putString(SettingsActivity.KEY_SERVER_PORT, value).apply()
    
    val serverBaseUrl: String
        get() = if (serverUrl.isNotEmpty() && serverPort.isNotEmpty()) {
            "http://$serverUrl:$serverPort"
        } else {
            ""
        }
    
    fun hasValidServerConfig(): Boolean {
        return isCloudSyncEnabled && serverUrl.isNotEmpty() && serverPort.isNotEmpty()
    }
}