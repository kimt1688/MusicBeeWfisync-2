package kim.tkland.musicbeewifisync

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

abstract class WifiSyncBaseActivity : AppCompatActivity() {
    protected var mainWindow: WifiSyncBaseActivity? = this
    private var accessPermissionsGranted = false
    protected var grantAccessToSdCard: File? = null
    protected var buttonTextEnabledColor = 0
    protected var buttonTextDisabledColor = 0
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
        if (hasAllFilesPermission()) {
            Toast.makeText(this, R.string.already_permission, Toast.LENGTH_LONG)
                .show()
        }


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
    private fun hasAllFilesPermission() = Environment.isExternalStorageManager()
}
