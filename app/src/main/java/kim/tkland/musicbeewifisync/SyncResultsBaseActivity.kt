package kim.tkland.musicbeewifisync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
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

    protected var isPreview = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        infoColor = ContextCompat.getColor(this, R.color.colorButtonTextDisabled)
        errorColor = ContextCompat.getColor(this, R.color.colorWarning)
        warningColor = ContextCompat.getColor(this, R.color.colorWarning)

    }

    override fun onDestroy() {
        mainWindow = null
        WifiSyncService.syncFromResults = null
        WifiSyncService.syncToResults = null
        super.onDestroy()
    }
}
