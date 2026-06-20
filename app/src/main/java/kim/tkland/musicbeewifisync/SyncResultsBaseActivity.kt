package kim.tkland.musicbeewifisync

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

abstract class SyncResultsBaseActivity : AppCompatActivity() {
    @JvmField
    protected var mainWindow: SyncResultsBaseActivity? = this
    @JvmField
    protected var infoColor = 0
    @JvmField
    protected var errorColor = 0
    @JvmField
    protected var warningColor = 0
    protected var appBarTitle = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        infoColor = ContextCompat.getColor(this, R.color.colorButtonTextDisabled)
        errorColor = ContextCompat.getColor(this, R.color.colorWarning)
        warningColor = ContextCompat.getColor(this, R.color.colorWarning)
        setContent {
            //if (showDialog.value) {
            if (WifiSyncService.syncErrorMessageId.get() != 0) {
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
                    text = { Text(getString(WifiSyncService.syncErrorMessageId.get())) },
                    confirmButton = {
                        Button(
                            onClick = {
                                finishAndRemoveTask()
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

    }

    override fun onDestroy() {
        mainWindow = null
        WifiSyncService.syncFromResults = null
        WifiSyncService.syncToResults = null
        super.onDestroy()
    }
}
