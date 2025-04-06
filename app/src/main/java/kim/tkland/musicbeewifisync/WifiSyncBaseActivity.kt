package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.reflect.KClass

abstract class WifiSyncBaseActivity : AppCompatActivity() {
    protected var mainWindow: WifiSyncBaseActivity? = this
    protected var buttonTextEnabledColor = 0
    protected var buttonTextDisabledColor = 0
    protected var progressDialog: WifiSyncAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resources = resources
        @Suppress("DEPRECATION")
        buttonTextEnabledColor = resources.getColor(R.color.colorButtonTextEnabled)
        @Suppress("DEPRECATION")
        buttonTextDisabledColor = resources.getColor(R.color.colorButtonTextDisabled)
    }

    override fun onDestroy() {
        mainWindow = null
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.fullSyncMenuItem -> {
                if (!item.isChecked) {
                    intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                return true
            }

            R.id.playlistSyncMenuItem -> {
                if (!item.isChecked) {
                    intent = Intent(this, PlaylistSyncActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                return true
            }

            R.id.wifiSyncSettingsMenuItem -> {
                intent = Intent(this, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return true
            }

            R.id.wifiSyncLogMenuItem -> {
                intent = Intent(this, ViewErrorLogActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onFullScanMenuItemClick(item: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.progressDialogTitle)
            .setMessage(R.string.alertDialogMessage)
            .setPositiveButton("OK") { dialog: DialogInterface, _ ->
                // OKボタン押下時に実行したい処理を記述
                listNewFiles()
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.syncCancel)) { dialog: DialogInterface, _ ->
                // クリックしたときの処理
                dialog.dismiss()
            }
//            .create()
            .show()
    }

    // 有線 Syncのファイルを見つけて登録する
    protected fun listNewFiles() {
        val thread = Thread(GetMusicFiles())
        showWifiSyncAlertDialog(resources.getString(R.string.progressDialogMessage), thread, null)
    }

    protected inner class GetMusicFiles() : Thread() {
        private var interrupted: Boolean = false
            get() = field
            set(value) {
                field = value
            }

        override fun run() {
            val sm = applicationContext.getSystemService(StorageManager::class.java)
            val svl = sm.storageVolumes
            for (sv in svl) {
                if (interrupted() || interrupted) {
                    interrupted = true
                    return
                }
                if (sv.directory != null) {
                    val path = "${sv.directory!!.absolutePath}/Music/"
                    searchFilesInDirectory(File(path))
                }
            }

            progressDialog!!.dismiss()
        }

        @Throws(InterruptedException::class)
        private fun searchFilesInDirectory(dir: File) {
            val files: Array<File>? = dir.listFiles()
            if (files!!.isNotEmpty()) {
                //ファイルが存在していた時のみ処理を行う
                for (f in files) {
                    if (interrupted() || interrupted) {
                        interrupted = true
                        return
                    }
                    if (f.isDirectory()) {
                        //ディレクトリの場合再帰的に検索する
                        searchFilesInDirectory(f)
                    } else {
                        MediaScannerConnection.scanFile(
                            applicationContext,
                            arrayOf(f.path),
                            null,
                            null
                        )
                    }
                }
            }
        }
    }


    private inner class MediaScannerClient() : MediaScannerConnection.MediaScannerConnectionClient {
        override fun onMediaScannerConnected() : Unit {

        }

        override fun onScanCompleted(path: String, uri: Uri): Unit {

        }
    }

    @SuppressLint("InflateParams")
    fun showWifiSyncAlertDialog(msg: String, thread: Thread, savedInstanceState: Bundle?) {
        // Create an instance of the dialog fragment and show it.
        progressDialog = WifiSyncAlertDialog()
        progressDialog!!.thread = WifiSyncAlertDialogThread(thread, progressDialog!!)
        progressDialog!!.msg = msg

        progressDialog!!.show(supportFragmentManager, "WIFISYNC_DIALOG")
        progressDialog!!.thread!!.thread!!.start()
    }
}
