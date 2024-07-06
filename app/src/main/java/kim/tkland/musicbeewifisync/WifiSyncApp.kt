package kim.tkland.musicbeewifisync

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.sync.Mutex
import java.lang.Thread.sleep
import java.util.Collections

val lockd = Any()
val locku = Any()

class WifiSyncApp : Application(), ActivityLifecycleCallbacks {
    @JvmField
    var currentActivity: Activity? = null
    override fun onCreate() {
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

    fun delete(uriList: Array<Uri>, requestCode: Int) {
        // WARNING: if the URI isn't a MediaStore Uri and specifically
        // only for media files (images, videos, audio) then the request
        // will throw an IllegalArgumentException, with the message:
        // 'All requested items must be referenced by specific ID'

        // No need to handle 'onActivityResult' callback, when the system returns
        // from the user permission prompt the files will be already deleted.
        // Multiple 'owned' and 'not-owned' files can be combined in the
        // same batch request. The system will automatically delete them
        // using the same prompt dialog, making the experience homogeneous.

        val list: MutableList<Uri?> = ArrayList()
        Collections.addAll(list, *uriList)

        synchronized(lockd) {
            val pendingIntent =
                MediaStore.createDeleteRequest(currentActivity!!.contentResolver, list)
            currentActivity!!.startIntentSenderForResult(
                pendingIntent.intentSender,
                requestCode,
                null,
                0,
                0,
                0,
                null
            )
            sleep(400)
        }
    }

    fun update(uriList: Array<Uri>, requestCode: Int) {
        val list: MutableList<Uri?> = ArrayList()
        Collections.addAll(list, *uriList)

        synchronized(locku) {
            val pendingIntent =
                MediaStore.createWriteRequest(currentActivity!!.contentResolver, list)
            currentActivity!!.startIntentSenderForResult(
                pendingIntent.intentSender,
                requestCode,
                null,
                0,
                0,
                0,
                null
            )
            sleep(1000)
        }
    }
}