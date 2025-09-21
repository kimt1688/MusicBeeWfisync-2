package kim.tkland.musicbeewifisync

import androidx.compose.runtime.getValue // <-- Add this
import androidx.compose.runtime.setValue
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

abstract class WifiSyncStartSyncBaseActivity() : WifiSyncBaseActivity("") {
    protected var WifiSyncStartSyncBaseActivity.showProgress: Boolean
        get() = viewModel.showDialog.value
        set(value) {}
    protected val WifiSyncStartSyncBaseActivity.viewModel: WifiSyncViewModel
        get() = this.viewModels<WifiSyncViewModel>().value
    protected var configErrorMessage: MutableState<String> = mutableStateOf("")

    protected var isFullSync = mutableStateOf(false)
    protected var isPlaylistSync = mutableStateOf(false)

    protected var appBarTitle = mutableStateOf("")

    var showDialog = mutableStateOf(false)

    var statusBarPadding : PaddingValues = PaddingValues(0.dp)
    var navigationBarPadding : PaddingValues = PaddingValues(0.dp)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    open fun CustomView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }
        var showFullScanDialogShow by remember { mutableStateOf(false) }
        val showDialogFromViewModel by viewModel.showDialog.collectAsStateWithLifecycle()
        var showProgressDialogShow by remember { mutableStateOf(showDialogFromViewModel) }

        // You'll need to observe changes from the ViewModel and update the local state
        LaunchedEffect(showDialogFromViewModel) {
            showProgressDialogShow = showDialogFromViewModel
        }

        statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

        if (showProgress) {
            CreateProgressDialog(viewModel)
        }
        if (showDeleteAllPlaylistsDialog.value) {
            AlertDialog(
                onDismissRequest = { showDeleteAllPlaylistsDialog.value = false },
                icon = { // Use the dedicated 'icon' parameter
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_info),
                        contentDescription = "Info Icon"
                    )  /* Provide a content description)*/
                },
                title = { Text(getString(R.string.progressDialogTitle)) },
                text = { Text(getString(R.string.menuAllPlaylistsDeleteConfirm)) },
                confirmButton = {
                    Button(
                        onClick = {
                            runBlocking {
                                val deffered = async {
                                    showDeleteAllPlaylistsDialog.value = false
                                }
                                deffered.await()
                            }
                            deleteAllPlaylists()
                            val thread = Thread(deleteAllPlaylistsThread)
                            viewModel.setValues(
                                thread,
                                getString(R.string.playlistDeletingMessage)
                            )
                            lifecycleScope.launch {
                                viewModel.doAsyncWork()
                            }
                            showDeleteAllPlaylistsDialog.value = false // ダイアログを閉じる
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                        )
                    ) { /* Handle confirm action */
                        Text(getString(android.R.string.ok)) // Or use a string resource
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showDeleteAllPlaylistsDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                        )
                    ) { /* Handle confirm action */
                        Text(getString(android.R.string.cancel)) // Or use a string resource
                    }
                }
            )
        }
        if (showFullScanDialogShow) {
            AlertDialog(
                onDismissRequest = {
                    showFullScanDialogShow = false
                }, // ダイアログの外側をクリックしたときの処理
                icon = { // Use the dedicated 'icon' parameter
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_info),
                        contentDescription = "Info Icon"
                    )  /* Provide a content description)*/
                },
                title = { Text(getString(R.string.syncConfirmHeader)) },
                text = { Text(getString(R.string.alertDialogMessage)) },
                confirmButton = {
                    Button(
                        onClick = {
                            runBlocking {
                                val deffered = async {
                                    showFullScanDialogShow = false
                                }
                                deffered.await()
                            }
                            getMusicFiles()
                            val thread = Thread(getMusicFilesThread)
                            viewModel.setValues(
                                thread,
                                getString(R.string.progressDialogMessage)
                            )
                            lifecycleScope.launch {
                                viewModel.doAsyncWork()
                            }
                            showProgressDialogShow = false // ダイアログを閉じる
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                        )
                    ) { /* Handle confirm action */
                        Text(getString(android.R.string.ok)) // Or use a string resource
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showFullScanDialogShow = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                        )
                    ) { /* Handle confirm action */
                        Text(getString(android.R.string.cancel)) // Or use a string resource
                    }
                }
            )
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = {
                }, // ダイアログの外側をクリックしたときの処理
                icon = { // Use the dedicated 'icon' parameter
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                        contentDescription = "Error Icon"
                    )  /* Provide a content description)*/
                },
                title = { Text(getString(R.string.syncErrorHeader)) },
                text = { Text(configErrorMessage.value) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                        )
                    ) { /* Handle confirm action */
                        Text(getString(android.R.string.ok)) // Or use a string resource
                    }
                },
                dismissButton = null,
            )
        }
    }

    abstract fun onPreviewButtonClick()
    abstract fun onSyncNowButtonClick()

    override fun onDestroy() {
        mainWindow = null
        super.onDestroy()
    }

    data class RadioOption(val text: String, val value: String, val index: Int)
}

