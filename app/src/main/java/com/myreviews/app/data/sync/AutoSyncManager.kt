package com.myreviews.app.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.myreviews.app.di.AppModule
import com.myreviews.app.ui.settings.SettingsActivity
import kotlinx.coroutines.*

class AutoSyncManager private constructor(
    private val context: Context
) {
    companion object {
        @Volatile
        private var INSTANCE: AutoSyncManager? = null
        
        fun getInstance(context: Context): AutoSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutoSyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        SettingsActivity.PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    private var syncJob: Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Prüft ob AutoSync aktiviert ist
     */
    fun isAutoSyncEnabled(): Boolean {
        val cloudSyncEnabled = prefs.getBoolean(SettingsActivity.KEY_CLOUD_SYNC_ENABLED, false)
        val autoSyncEnabled = prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true) // Default: true
        return cloudSyncEnabled && autoSyncEnabled
    }
    
    /**
     * Aktiviert/deaktiviert AutoSync
     */
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
        Log.d("AutoSyncManager", "AutoSync ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Führt Sync aus, wenn AutoSync aktiviert ist
     */
    fun triggerSyncIfEnabled(reason: String = "unknown") {
        if (!isAutoSyncEnabled()) {
            Log.d("AutoSyncManager", "Sync triggered ($reason) but AutoSync is disabled")
            return
        }
        
        Log.d("AutoSyncManager", "Triggering sync: $reason")
        
        // Cancel previous sync if still running
        syncJob?.cancel()
        
        syncJob = syncScope.launch {
            try {
                val result = AppModule.syncRepository.performSync()
                Log.d("AutoSyncManager", "AutoSync completed: $result")
            } catch (e: Exception) {
                Log.e("AutoSyncManager", "AutoSync failed: ${e.message}", e)
                // Silent failure - no UI notification for auto sync
            }
        }
    }
    
    /**
     * Führt manuellen Sync aus (für Settings)
     */
    suspend fun performManualSync(): com.myreviews.app.data.api.SyncResult {
        Log.d("AutoSyncManager", "Performing manual sync")
        return try {
            AppModule.syncRepository.performSync()
        } catch (e: Exception) {
            Log.e("AutoSyncManager", "Manual sync failed: ${e.message}", e)
            com.myreviews.app.data.api.SyncResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Cleanup
     */
    fun cleanup() {
        syncJob?.cancel()
        syncScope.cancel()
    }
}