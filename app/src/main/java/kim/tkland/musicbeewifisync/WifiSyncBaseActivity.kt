package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import java.io.File

abstract class WifiSyncBaseActivity : AppCompatActivity() {
    protected var mainWindow: WifiSyncBaseActivity? = this
    protected var buttonTextEnabledColor = 0
    protected var buttonTextDisabledColor = 0
    protected var progressDialog: WifiSyncAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val resources = resources
        @Suppress("DEPRECATION")
        buttonTextEnabledColor = resources.getColor(R.color.colorButtonTextEnabled)
        @Suppress("DEPRECATION")
        buttonTextDisabledColor = resources.getColor(R.color.colorButtonTextDisabled)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.

            v.updateLayoutParams() {
                v.left = (insets.left).toInt()
                v.bottom = (insets.bottom).toInt()
                v.right = (insets.right).toInt()
            }
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )

            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
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

    fun onDeleteAllPlaylistsClick(item: MenuItem) {
        /// 確認ダイアログを出してOKの時に処理
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.progressDialogTitle)
            .setMessage(R.string.menuAllPlaylitsDeleteConfirm)
            .setCancelable(true)
            .setPositiveButton("OK") { dialog: DialogInterface, _ ->
                // OKボタン押下時に実行したい処理を記述
                dialog.dismiss()
                var thread = Thread(DeleteAllPlaylists())
                showWifiSyncAlertDialog(getString(R.string.playlistDeletingMessage), thread, null)
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _ ->
                // クリックしたときの処理
                dialog.dismiss()
            }
            .show()
    }

    protected inner class DeleteAllPlaylists() : Thread() {
        override fun run() {
            val playListCollection = MediaStore.Audio.Playlists.getContentUri(
                MediaStore.getExternalVolumeNames(applicationContext)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
            )
            var contentUri: Uri? = null

            try {
                var cursor: Cursor? = null
                try {
                    cursor = applicationContext.contentResolver.query(
                        playListCollection,
                        arrayOf(
                            MediaStore.Audio.Playlists._ID,
                            MediaStore.Audio.Playlists.RELATIVE_PATH,
                            MediaStore.Audio.Playlists.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null,
                        null
                    )
                } catch (e: Exception) {
                    Log.d("SQLite Error", e.stackTraceToString())
                    progressDialog!!.dismiss()
                    interrupt()
                    return
                }
                if (cursor != null) {
                    try {
                        cursor.moveToFirst()
                        do {
                            contentUri =
                                ContentUris.withAppendedId(playListCollection, cursor.getLong(0))
                            (application as WifiSyncApp).delete(contentUri)
                        } while (cursor.moveToNext())
                        cursor.close()
                        progressDialog!!.dismiss()
                        interrupt()
                        return
                    } catch (e: InterruptedException) {
                        Log.d("onDeleteAllPlaylistsClick", e.toString())
                        Log.d("onDeleteAllPlaylistsClick", e.stackTraceToString())
                        progressDialog!!.dismiss()
                        interrupt()
                        return
                    }
                }
            } catch (ex: Exception) {
                Log.d("onDeleteAllPlaylistsClick", ex.toString())
                Log.d("onDeleteAllPlaylistsClick", ex.stackTraceToString())
                progressDialog!!.dismiss()
                interrupt()
                return
            }
        }
    }
}
