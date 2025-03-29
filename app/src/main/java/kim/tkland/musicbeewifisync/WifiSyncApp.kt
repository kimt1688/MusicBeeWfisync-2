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
import java.util.concurrent.atomic.AtomicReference

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

    fun delete(uri: Uri) {
        // WARNING: if the URI isn't a MediaStore Uri and specifically
        // only for media files (images, videos, audio) then the request
        // will throw an IllegalArgumentException, with the message:
        // 'All requested items must be referenced by specific ID'

        // No need to handle 'onActivityResult' callback, when the system returns
        // from the user permission prompt the files will be already deleted.
        // Multiple 'owned' and 'not-owned' files can be combined in the
        // same batch request. The system will automatically delete them
        // using the same prompt dialog, making the experience homogeneous.

        val activity : AtomicReference<Activity> = AtomicReference(this.currentActivity)
        Log.d("WifiSyncApp", "delete(uri):${uri}")
        val list: MutableList<Uri> = ArrayList()
        list.add(uri)

        val pendingIntent =
            MediaStore.createDeleteRequest(activity.get().contentResolver, list)
        activity.get().startIntentSenderForResult(
            pendingIntent.intentSender,
            777,
            null,
            0,
            0,
            0,
            null
        )
        Thread.sleep(300)
    }

    fun update(uri: Uri) {
        val activity : AtomicReference<Activity> = AtomicReference(this.currentActivity)
        Log.d("WifiSyncApp", "delete(uri):${uri}")
        val list: MutableList<Uri?> = ArrayList()
        list.add(uri)

        val pendingIntent =
            MediaStore.createWriteRequest(activity.get().contentResolver, list)
        activity.get().startIntentSenderForResult(
            pendingIntent.intentSender,
            999,
            null,
            0,
            0,
            0,
            null
        )
        Thread.sleep(300)
    }
}