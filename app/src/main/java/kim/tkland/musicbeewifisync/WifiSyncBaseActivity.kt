package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.DialogInterface
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

abstract class WifiSyncBaseActivity(private val myStringParam: String) : AppCompatActivity() {
    protected var mainWindow: WifiSyncBaseActivity? = this
    protected var buttonTextEnabledColor = 0
    protected var buttonTextDisabledColor = 0
    protected var progressDialog: WifiSyncAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonTextEnabledColor = resources.getColor(R.color.colorButtonTextEnabled, null)
        buttonTextDisabledColor = resources.getColor(R.color.colorButtonTextDisabled, null)

    }

    override fun onDestroy() {
        mainWindow = null
        super.onDestroy()
    }

    fun onFullScanMenuItemClick() {
        AlertDialog.Builder((application as WifiSyncApp).currentActivity!!)
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
            .create()
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

    fun onDeleteAllPlaylistsClick() {
//    fun onDeleteAllPlaylistsClick(item: MenuItem) {
        /// 確認ダイアログを出してOKの時に処理
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.progressDialogTitle)
            .setMessage(R.string.menuAllPlaylistsDeleteConfirm)
            .setCancelable(true)
            .setPositiveButton("OK") { dialog: DialogInterface, _ ->
                // OKボタン押下時に実行したい処理を記述
                dialog.dismiss()
                val thread = Thread(DeleteAllPlaylists())
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
