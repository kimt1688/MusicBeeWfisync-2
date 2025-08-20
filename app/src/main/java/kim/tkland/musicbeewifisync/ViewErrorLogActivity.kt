package kim.tkland.musicbeewifisync

import android.R.color.transparent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

class ViewErrorLogActivity : AppCompatActivity() {
    private var errorText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_error_log)
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bars =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

            //v.updateLayoutParams() {
            //    v.top = (insets.top).toInt()
                //v.left = (insets.left).toInt()
                //v.bottom = (insets.bottom).toInt()
                //v.right = (insets.right).toInt()
            //}
            v.updatePadding(
            //    left = bars.left,
                top = bars.top,
            //    right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        //WindowCompat.getInsetsController(window, window.decorView)
        //    .isAppearanceLightStatusBars = true
        setSupportActionBar(findViewById(R.id.my_toolbar))
        errorText = findViewById(R.id.errorText)
        errorText?.let{ it.movementMethod = ScrollingMovementMethod() }
        val thread = Thread {
            val errorLog = ErrorHandler.log
            runOnUiThread {
                if (errorLog.isNullOrEmpty()) {
                    errorText?.let{ it.text = R.string.errorNone.toString() }
                    findViewById<View>(R.id.copyToClipboardButton).isEnabled = false
                } else {
                    errorText?.let{ it.text = errorLog }
                }
            }
        }
        thread.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    fun copyToClipboardButton_Click(view: View) {
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newPlainText(
                    getString(R.string.title_activity_view_error_log),
                    errorText!!.text.toString()
                )
            )
        } catch (ex: Exception) {
            Log.d("copyToClipboardButton_Click", ex.message!!)
        }
    }
}