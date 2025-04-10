package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuCompat
import com.google.android.material.snackbar.Snackbar

class SyncResultsStatusActivity : SyncResultsBaseActivity() {
    private var syncProgressBar: ProgressBar? = null
    private var syncWaitIndicator: ProgressBar? = null
    private var syncCompletionStatusMessage: TextView? = null
    private var syncFailedResults: ListView? = null
    private var syncProgressMessage: TextView? = null
    private var stopSyncButton: Button? = null
    private val timerHandler: Handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_status)
        WifiSyncService.resultsActivityReady.set()
        syncCompletionStatusMessage = findViewById(R.id.syncCompletionStatusMessage)
        syncFailedResults = findViewById(R.id.syncFailedResults)
        syncProgressBar = findViewById(R.id.syncProgressBar)
        syncWaitIndicator = findViewById(R.id.syncWaitIndicator)
        syncProgressMessage = findViewById(R.id.syncProgressMessage)
        stopSyncButton = findViewById(R.id.stopSyncButton)
        timerRunnable = object : Runnable {
            override fun run() {
                try {
                    if (WifiSyncService.syncPercentCompleted.get() == -1) {
                        showEndOfSyncInformation()
                    } else {
                        syncProgressMessage?.text = WifiSyncService.syncProgressMessage.get()
                        syncProgressBar?.progress = WifiSyncService.syncPercentCompleted.get()
                        timerHandler.postDelayed(this, 300)
                    }
                } catch (ex: Exception) {
                    ErrorHandler.logError("startProgress", ex)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable!!, 300)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(findViewById(R.id.my_toolbar))
    }

    override fun onDestroy() {
        WifiSyncService.resultsActivityReady.reset()
        timerHandler.removeCallbacks(timerRunnable!!)
        mainWindow = null
        super.onDestroy()
    }

    @SuppressLint("MissingSuperCall")
    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        // disable back button
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sync_status, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
            R.id.wifiSyncLogMenuItem -> {
                intent = Intent(this, ViewErrorLogActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return true
            }
            R.id.wifiSyncSettingsMenuItem -> {
                intent = Intent(this, SettingsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onStopSyncButton_Click(view: View) {
        if (stopSyncButton!!.text == getString(R.string.syncMore)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            val intent = Intent()
            intent.setClass(this, WifiSyncService::class.java)
            intent.action = getString(R.string.actionSyncAbort)
            startService(intent)
            stopProgressTimer()
            WifiSyncServiceSettings.saveSettings(this)
            WifiSyncService.syncErrorMessageId.set(R.string.syncCancelled)
            showEndOfSyncInformation()
        }
    }

    private fun stopProgressTimer() {
        syncProgressBar!!.visibility = View.INVISIBLE
        syncWaitIndicator!!.visibility = View.INVISIBLE
        syncProgressMessage!!.visibility = View.GONE
        timerHandler.removeCallbacks(timerRunnable!!)
        stopSyncButton!!.text = getString(R.string.syncMore)
    }

    private fun showEndOfSyncInformation() {
        val errorMessageId = WifiSyncService.syncErrorMessageId.getAndSet(0)
        WifiSyncServiceSettings.saveSettings(this)
        stopProgressTimer()
        if (errorMessageId == 0 || errorMessageId == R.string.syncCompletedFail) {
            val messageId = if (errorMessageId != 0) errorMessageId else R.string.syncCompleted
            syncCompletionStatusMessage!!.setText(messageId)
            syncCompletionStatusMessage!!.visibility = View.VISIBLE
            if (errorMessageId == R.string.syncCompletedFail) {
                if ((WifiSyncService.syncToResults == null || WifiSyncService.syncToResults!!.size == 0) && WifiSyncService.syncFailedFiles.size == 0) {
                    syncCompletionStatusMessage!!.setText(R.string.syncCompletedFailErrorLog)
                    syncFailedResults!!.visibility = View.VISIBLE
                } else {
                    val params =
                        syncCompletionStatusMessage!!.layoutParams as ConstraintLayout.LayoutParams
                    params.verticalBias = 0.0f
                    syncCompletionStatusMessage!!.layoutParams = params
                    syncCompletionStatusMessage!!.setText(R.string.syncCompletedFailMessage)
                    syncFailedResults!!.visibility = View.VISIBLE
                    val failedFrom = ArrayList<SyncResultsInfo>()
                    for (info in WifiSyncService.syncFailedFiles) {
                        failedFrom.add(
                            SyncResultsInfo(
                                info.filename.substring(
                                    info.filename.lastIndexOf(
                                        "/"
                                    ) + 1
                                ), info.errorMessage
                            )
                        )
                    }
                    showResults(syncFailedResults!!, WifiSyncService.syncToResults, failedFrom)
                }
            }
            val snackbar =
                Snackbar.make(stopSyncButton!!, getString(messageId), Snackbar.LENGTH_LONG)
            try {
                val snackbarView = snackbar.view
                val params = snackbarView.layoutParams as FrameLayout.LayoutParams
                params.setMargins(0, 0, 0, stopSyncButton!!.height)
                snackbarView.layoutParams = params
            } catch (ex: Exception) {
                Log.d("showEndOfSyncInformation", ex.message!!)
            }
            snackbar.show()
        } else if (errorMessageId == R.string.syncCancelled) {
            syncCompletionStatusMessage!!.setText(errorMessageId)
            syncCompletionStatusMessage!!.visibility = View.VISIBLE
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.syncErrorHeader))
            builder.setMessage(getString(errorMessageId))
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setCancelable(false)
            if (errorMessageId != R.string.errorServerNotFound) {
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    syncCompletionStatusMessage!!.setText(errorMessageId)
                    syncCompletionStatusMessage!!.visibility = View.VISIBLE
                }
            } else {
                builder.setNegativeButton(R.string.syncCancel) { _, _ ->
                    syncCompletionStatusMessage!!.setText(errorMessageId)
                    syncCompletionStatusMessage!!.visibility = View.VISIBLE
                }
                builder.setPositiveButton(R.string.syncRetry) { _, _ ->
                    WifiSyncService.startSynchronisation(
                        applicationContext,
                        WifiSyncService.syncIteration,
                        false,
                        false
                    )
                }
            }
            builder.show()
        }
    }
}
