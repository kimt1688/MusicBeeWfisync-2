package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.content.ContentUris
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kim.tkland.musicbeewifisync.PlaylistSyncActivity.FileSelectedInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Thread.interrupted
import java.net.SocketTimeoutException
import kotlin.getValue

abstract class WifiSyncBaseActivity(private val myStringParam: String) : AppCompatActivity() {
    protected var mainWindow: WifiSyncBaseActivity? = this
    protected var buttonTextEnabledColor = 0
    protected var buttonTextDisabledColor = 0
    var showDeleteAllPlaylistsDialog = mutableStateOf(false)

    var getMusicFilesThread = Thread()

    var deleteAllPlaylistsThread = Thread()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buttonTextEnabledColor = resources.getColor(R.color.colorButtonTextEnabled, null)
        buttonTextDisabledColor = resources.getColor(R.color.colorButtonTextDisabled, null)

    }

    override fun onDestroy() {
        mainWindow = null
        super.onDestroy()
    }


    protected fun getMusicFiles() {
        getMusicFilesThread = Thread(Runnable {
            val sm = applicationContext.getSystemService(StorageManager::class.java)
            val svl = sm.storageVolumes
            for (sv in svl) {
                if (getMusicFilesThread.isInterrupted) {
                    return@Runnable
                }
                if (sv.directory != null) {
                    val path = "${sv.directory!!.absolutePath}/Music/"
                    searchFilesInDirectory(File(path))
                }
            }
        })
    }

    @Throws(InterruptedException::class)
    private fun searchFilesInDirectory(dir: File) {
        val files: Array<File>? = dir.listFiles()
        if (files!!.isNotEmpty()) {
            //ファイルが存在していた時のみ処理を行う
            for (f in files) {
                if (getMusicFilesThread.isInterrupted) {
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

    @Composable
    fun CreateProgressDialog(vm: WifiSyncViewModel) {
        val msg by vm.msg.collectAsState()

        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .width(300.dp)
                    .background(Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .height(150.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(start = 10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            color = Color(getColor(R.color.colorAccent)),
                            trackColor = Color(getColor(R.color.colorButtonTextEnabled))
                        )
                    }
                    Column() {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 150.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End
                    ) {
                        Button(
                            onClick = {
                                // showDialog = false
                                vm.cancelProcess()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(getColor(R.color.colorButtonBackground)),
                                contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            )
                        ) { /* Handle confirm action */
                            Text(getString(android.R.string.cancel)) // Or use a string resource
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams", "ViewModelConstructorInComposable")
    fun showWifiSyncAlertDialog(msg: String) {
        // Create an instance of the dialog fragment and show it.
        showDeleteAllPlaylistsDialog.value = true
    }
    protected fun deleteAllPlaylists() {
        deleteAllPlaylistsThread = Thread(Runnable {
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
                    //progressDialog!!.dismiss()
                    //interrupt()
                    return@Runnable
                }
                if (cursor != null) {
                    try {
                        cursor.moveToFirst()
                        if (cursor.count == 0) {
                            return@Runnable
                        }
                        do {
                            contentUri =
                                ContentUris.withAppendedId(playListCollection, cursor.getLong(0))
                            (application as WifiSyncApp).delete(contentUri)
                        } while (cursor.moveToNext())
                        cursor.close()
                        //progressDialog!!.dismiss()
                        //interrupt()
                        return@Runnable
                    } catch (e: InterruptedException) {
                        Log.d("onDeleteAllPlaylistsClick", e.toString())
                        Log.d("onDeleteAllPlaylistsClick", e.stackTraceToString())
                        //progressDialog!!.dismiss()
                        //interrupt()
                        return@Runnable
                    }
                }
            } catch (ex: Exception) {
                Log.d("onDeleteAllPlaylistsClick", ex.toString())
                Log.d("onDeleteAllPlaylistsClick", ex.stackTraceToString())
                //progressDialog!!.dismiss()
                //interrupt()
                return@Runnable
            }
        })
    }
}
