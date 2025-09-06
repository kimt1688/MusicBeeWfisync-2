package kim.tkland.musicbeewifisync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

abstract class SyncResultsBaseActivity : AppCompatActivity() {
    @JvmField
    protected var mainWindow: SyncResultsBaseActivity? = this
    @JvmField
    protected var infoColor = 0
    @JvmField
    protected var errorColor = 0
    @JvmField
    protected var warningColor = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        infoColor = ContextCompat.getColor(this, R.color.colorButtonTextDisabled)
        errorColor = ContextCompat.getColor(this, R.color.colorWarning)
        warningColor = ContextCompat.getColor(this, R.color.colorWarning)

        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())


            v.updatePadding(
            //    left = bars.left,
            //    top = bars.top,
            //    right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onDestroy() {
        mainWindow = null
        WifiSyncService.syncFromResults = null
        WifiSyncService.syncToResults = null
        super.onDestroy()
    }
}
