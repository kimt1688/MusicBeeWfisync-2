package kim.tkland.musicbeewifisync

import android.R
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.*


internal object Dialog {
    @JvmStatic
    fun showOkCancel(parentActivity: Activity, prompt: String?): Int {
        val dialogWait = Object()
        val result = AtomicInteger()
        parentActivity.runOnUiThread {
            val errorDialog = AlertDialog.Builder(parentActivity)
            errorDialog.setMessage(prompt)
            errorDialog.setNegativeButton(
                R.string.cancel,
                DialogInterface.OnClickListener { _, _ ->
                    result.set(R.string.cancel)
                    synchronized(dialogWait) { dialogWait.notifyAll() }
                })
            errorDialog.setPositiveButton(
                R.string.ok,
                object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        result.set(R.string.ok)
                        synchronized(dialogWait) { dialogWait.notifyAll() }
                    }
                })
            errorDialog.setCancelable(false)
            errorDialog.show()
        }
        synchronized(dialogWait) {
            try {
                dialogWait.wait()
            } catch (ex: Exception) {
                Log.d("showOkCancel", ex.message!!)
            }
        }
        return result.get()
    }
}

internal object ErrorHandler {
    private var folderPath: File? = null
    private var fileHandler: FileHandler? = null
    private val logger = Logger.getLogger("WifiSync")
    fun initialise(context: Context) {
        folderPath = context.filesDir
    }

    private fun initialise() {
        if (fileHandler == null) {
            try {
                //folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                val file = File(folderPath, "MusicBeeWifiSyncErrorLog.txt")
                try {
                    FileOutputStream(file).channel.truncate(0).close()
                } catch (e: IOException) { /* log and ignore */
                    Log.d("ErrorHandler", e.message!!)
                }
                fileHandler = FileHandler(file.path)
                fileHandler!!.formatter = LogFormatter()
                logger.addHandler(fileHandler as Handler)
                //logger.info(Build.MODEL + ";  " + Build.VERSION.RELEASE + ";  " + BuildConfig.VERSION_NAME)
            } catch (ex: Exception) {
                Log.e("WifiSync: ErrorHandler", ex.toString())
            }
        }
    }

    val log: String?
        get() {
            try {
                val errorLog = File(folderPath, "MusicBeeWifiSyncErrorLog.txt")
                FileInputStream(errorLog).use { stream ->
                    val buffer = ByteArray(errorLog.length().toInt())
                    stream.read(buffer, 0, buffer.size)
                    return String(buffer, StandardCharsets.UTF_8)
                }
            } catch (ex: Exception) {
                return null
            }
        }

    fun logError(tag: String, message: String?) {
        if (message != null) {
            initialise()
            logger.severe("$tag: $message")
        }
    }

    @JvmStatic
    @JvmOverloads
    fun logError(tag: String, ex: Exception, info: String? = null) {
        initialise()
        val message = ex.toString()
        logger.severe(tag + ": " + message + (if ((info == null)) "" else ": $info"))
        if (WifiSyncServiceSettings.debugMode) {
            for (element: StackTraceElement in ex.stackTrace) {
                logger.info(element.toString())
            }
        }
        /*else if (BuildConfig.DEBUG) {
            Log.d("WifiSync: $tag", message)
            for (element: StackTraceElement in ex.stackTrace) {
                Log.d("WifiSync", element.toString())
            }
        }*/
    }

    @JvmStatic
    fun logInfo(tag: String?, message: String?) {
        if (message != null) {
            initialise()
            logger.info(if ((tag == null)) message else "$tag: $message")
        }
    }

    private class LogFormatter : SimpleFormatter() {
        private var writeHeader = true
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        private val timeFormat = SimpleDateFormat("HH:mm:ss")
        override fun format(record: LogRecord): String {
            val builder = StringBuilder()
            if (writeHeader) {
                writeHeader = false
                builder.append(dateFormat.format(Date(record.millis)))
            } else {
                builder.append(timeFormat.format(Date(record.millis)))
            }
            builder.append(": ")
            builder.append(record.message)
            builder.append("\n")
            return builder.toString()
        }
    }
}

internal class CaseInsensitiveMap : HashMap<String, String?>() {
    override fun put(key: String, value: String?): String? {
        return super.put(key.lowercase(), value)
    }

    override fun get(key: String): String? {
        return super.get(key.lowercase())
    }

    override fun containsKey(key: String): Boolean {
        return super.containsKey(key.lowercase())
    }
}