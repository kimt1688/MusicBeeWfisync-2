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
import java.util.concurrent.atomic.AtomicReference

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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}

    fun deleteUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val activity = currentActivity ?: return
        Log.d("WifiSyncApp", "deleteUris(${uris.size} items)")

        activity.runOnUiThread {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(activity.contentResolver, uris)
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    555,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                Log.e("WifiSyncApp", "Error in deleteUris", e)
            }
        }
    }

    fun delete(uri: Uri) {
        val activity = currentActivity ?: return
        Log.d("WifiSyncApp", "delete(uri):$uri")
        val list = listOf(uri)

        activity.runOnUiThread {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(activity.contentResolver, list)
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    777,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                Log.e("WifiSyncApp", "Error in delete", e)
            }
        }
    }

    fun update(uri: Uri) {
        val activity = currentActivity ?: return
        if (WifiSyncServiceSettings.debugMode) {
            Log.d("WifiSyncApp", "update(uri):$uri")
        }
        val list = listOf(uri)

        activity.runOnUiThread {
            try {
                val pendingIntent = MediaStore.createWriteRequest(activity.contentResolver, list)
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender,
                    999,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                Log.e("WifiSyncApp", "Error in update", e)
            }
        }
    }
}