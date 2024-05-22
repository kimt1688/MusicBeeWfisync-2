package kim.tkland.musicbeewifisync

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ViewErrorLogActivity : AppCompatActivity() {
    private var errorText: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_error_log)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        errorText = findViewById(R.id.errorText)
        errorText?.let{it.setMovementMethod(ScrollingMovementMethod())}
        val thread = Thread {
            val errorLog = ErrorHandler.log
            runOnUiThread {
                if (errorLog == null || errorLog.length == 0) {
                    errorText?.let{it.setText(R.string.errorNone)}
                    findViewById<View>(R.id.copyToClipboardButton).isEnabled = false
                } else {
                    errorText?.let{it.setText(errorLog)}
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
        }
    }
}