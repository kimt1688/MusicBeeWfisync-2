package kim.tkland.musicbeewifisync

import android.app.ProgressDialog
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import java.io.File

abstract class WifiSyncBaseActivity : AppCompatActivity() {
    protected var mainWindow: WifiSyncBaseActivity? = this
    protected var buttonTextEnabledColor = 0
    protected var buttonTextDisabledColor = 0
    protected var progressDialog: ProgressDialog? = null

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

    // 有線 Syncのファイルを見つけて登録する
    protected fun listNewFiles() {
        var thread: Thread? = null
        progressDialog = ProgressDialog((application as WifiSyncApp).currentActivity)
        progressDialog!!.setTitle(R.string.progressDialogTitle)
        progressDialog!!.setMessage("Registering files modified by Wired Sync in the MediaStore…")
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
        thread = Thread(
            GetMusicFiles()
        )
        thread.start()
    }

    protected inner class GetMusicFiles() : Thread() {
        override fun run() {
            val sm = applicationContext.getSystemService(StorageManager::class.java)
            val svl = sm.storageVolumes
            for (sv in svl) {
                if (sv.directory != null) {
                    val path = "${sv.directory!!.absolutePath}/Music/"
                    searchFilesInDirectory(File(path))
                }
            }
            progressDialog!!.dismiss()
        }

        private fun searchFilesInDirectory(dir: File){
            val files: Array<File>? = dir.listFiles()

            if (!files.isNullOrEmpty()) {
                //ファイルが存在していた時のみ処理を行う
                for (f in files) {
                    if (f.isDirectory()) {
                        //ディレクトリの場合再帰的に検索する
                        searchFilesInDirectory(f)
                    } else {
                        // broadcastNewFiles(f)
                        //outList.add(f)
                        //Log.d("searchFiles:", f.name)
                        MediaScannerConnection.scanFile(applicationContext, arrayOf(f.path), null, null)
                    }
                }
            }
            return
        }
    }
}
