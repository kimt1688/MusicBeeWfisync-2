package kim.tkland.musicbeewifisync

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WifiSyncApp : Application(), ActivityLifecycleCallbacks {
    @JvmField
    var currentActivity: Activity? = null

    private val pendingDeleteUris = mutableSetOf<Uri>()
    private val pendingUpdateUris = mutableSetOf<Uri>()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val deleteRunnable = Runnable {
        val list = synchronized(pendingDeleteUris) {
            val l = pendingDeleteUris.toList()
            pendingDeleteUris.clear()
            l
        }
        if (list.isNotEmpty()) {
            deleteUris(list)
        }
    }

    private val updateRunnable = Runnable {
        val list = synchronized(pendingUpdateUris) {
            val l = pendingUpdateUris.toList()
            pendingUpdateUris.clear()
            l
        }
        if (list.isNotEmpty()) {
            updateUris(list)
        }
    }

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
        synchronized(pendingDeleteUris) {
            pendingDeleteUris.add(uri)
        }
        mainHandler.removeCallbacks(deleteRunnable)
        mainHandler.postDelayed(deleteRunnable, 300)
    }

    fun deleteUrisImmediate(uris: List<Uri>) {
        deleteUris(uris)
    }

    private fun deleteUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val activity = currentActivity
        if (activity == null) {
            Log.w("WifiSyncApp", "deleteUris: No active activity to handle request")
            return
        }
        Log.d("WifiSyncApp", "deleteUris(${uris.size} items)")

        activity.runOnUiThread {
            uris.chunked(2000).forEach { chunk ->
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(activity.contentResolver, chunk)
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender,
                        555,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (e: android.content.IntentSender.SendIntentException) {
                    Log.e("WifiSyncApp", "SendIntentException in deleteUris", e)
                } catch (e: Exception) {
                    Log.e("WifiSyncApp", "Error in deleteUris", e)
                }
            }
        }
    }

    fun update(uri: Uri) {
        synchronized(pendingUpdateUris) {
            pendingUpdateUris.add(uri)
        }
        mainHandler.removeCallbacks(updateRunnable)
        // 連続呼び出しが止まってから少し待って実行
        mainHandler.postDelayed(updateRunnable, 300)
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
        if (WifiSyncServiceSettings.debugMode) {
            Log.d("WifiSyncApp", "updateUris(${uris.size} items)")
        }

        activity.runOnUiThread {
            uris.chunked(2000).forEach { chunk ->
                try {
                    val pendingIntent = MediaStore.createWriteRequest(activity.contentResolver, chunk)
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
                } catch (e: Exception) {
                    Log.e("WifiSyncApp", "Error in updateUris", e)
                }
            }
        }
    }
}
