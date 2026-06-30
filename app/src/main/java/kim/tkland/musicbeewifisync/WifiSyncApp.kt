package kim.tkland.musicbeewifisync

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WifiSyncApp : Application(), ActivityLifecycleCallbacks {
    @JvmField
    var currentActivity: Activity? = null

    override fun onCreate() {
        StrictMode.setVmPolicy(VmPolicy.Builder()
            .detectUnsafeIntentLaunch()
            .build()
        )
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    fun delete(uri: Uri) {
        deleteUris(listOf(uri))
    }

    fun deleteUrisImmediate(uris: List<Uri>) {
        deleteUris(uris)
    }

    private fun deleteUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val activity = currentActivity
        if (activity == null) {
            Log.w("WifiSyncApp", "deleteUris: No active activity to handle request")
            signalPermissionResult()
            return
        }

        activity.runOnUiThread {
            try {
                Log.d("WifiSyncApp", "Requesting delete for ${uris.size} items, first: ${uris.firstOrNull()}")

                // Filter URIs to ensure they are from recognized media collections.
                // MediaStore.createDeleteRequest can fail if URIs are from generic MediaStore.Files collection
                // or if they are not considered "media" items by the system.
                val mediaUris = uris.filter { uri ->
                    val path = uri.path ?: return@filter false
                    // Specific media collections are typically required for createDeleteRequest
                    path.contains("/audio/") || path.contains("/video/") ||
                    path.contains("/images/") || path.contains("/playlists/") ||
                    // Also allow if it's NOT a generic 'file' URI, as it's likely already a specific collection URI
                    !path.contains("/file/")
                }

                if (mediaUris.isEmpty()) {
                    Log.w("WifiSyncApp", "deleteUris: No valid MediaStore media URIs found in request")
                    signalPermissionResult()
                    return@runOnUiThread
                }

                if (mediaUris.size != uris.size) {
                    Log.i("WifiSyncApp", "Filtered out ${uris.size - mediaUris.size} non-media URIs from createDeleteRequest")
                }

                val pendingIntent = MediaStore.createDeleteRequest(activity.contentResolver, mediaUris)

                // Important: Use startIntentSenderForResult directly on the current activity
                // to trigger the system dialog properly.
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    999,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: android.content.IntentSender.SendIntentException) {
                Log.e("WifiSyncApp", "SendIntentException in deleteUris", e)
                signalPermissionResult()
            } catch (e: Exception) {
                Log.e("WifiSyncApp", "Error in deleteUris", e)
                signalPermissionResult()
            }
        }
    }

    fun update(uri: Uri) {
        updateUris(listOf(uri))
    }

    fun signalPermissionResult() {
        Log.d("WifiSyncApp", "signalPermissionResult() called")
        WifiSyncService.waitPermissionEvent.set()
    }

    /**
     * リストを直接渡して即時更新リクエストを行う
     */
    fun updateUrisImmediate(uris: List<Uri>) {
        updateUris(uris)
    }

    private fun updateUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val activity = currentActivity
        if (activity == null) {
            Log.w("WifiSyncApp", "updateUris: No active activity to handle request")
            return
        }

        activity.runOnUiThread {
            try {
                Log.d("WifiSyncApp", "Requesting write for ${uris.size} items, first: ${uris.first()}")
                val pendingIntent = MediaStore.createWriteRequest(activity.contentResolver, uris)
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    999,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: android.content.IntentSender.SendIntentException) {
                Log.e("WifiSyncApp", "SendIntentException in updateUris", e)
                signalPermissionResult()
            } catch (e: Exception) {
                Log.e("WifiSyncApp", "Error in updateUris", e)
                signalPermissionResult()
            }
        }
    }
}
