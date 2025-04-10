package kim.tkland.musicbeewifisync

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuCompat

class SyncResultsPreviewActivity : SyncResultsBaseActivity() {
    private var syncExcludeErrors: CheckBox? = null
    private var proceedSyncButton: LinearLayout? = null
    private var proceedSyncButtonImage: ImageView? = null
    private var proceedSyncButtonText: TextView? = null
    private var waitResultsThread: Thread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_preview)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        syncExcludeErrors = findViewById(R.id.syncExcludeErrors)
        proceedSyncButton = findViewById(R.id.proceedSyncButton)
        proceedSyncButtonImage = findViewById(R.id.proceedSyncButtonImage)
        proceedSyncButtonText = findViewById(R.id.proceedSyncButtonText)
        waitResultsThread = Thread {
            try {
                WifiSyncService.waitSyncResults.waitOne()
                runOnUiThread {
                    findViewById<View>(R.id.previewWaitIndicator).visibility = View.GONE
                    proceedSyncButton?.visibility = View.VISIBLE
                    val previewStatusMessage = findViewById<TextView>(R.id.previewStatusMessage)
                    val previewListView = findViewById<ListView>(R.id.previewResults)
                    val previewErrorMessage = findViewById<TextView>(R.id.previewErrorMessage)
                    val previewToData = WifiSyncService.syncToResults
                    val previewFromData = WifiSyncService.syncFromResults
                    if (mainWindow == null) {
                        // ignore
                    } else if (previewToData == null || previewFromData == null) {
                        disableProceedSyncButton()
                        var errorMessageId = WifiSyncService.syncErrorMessageId.get()
                        if (errorMessageId == 0) {
                            errorMessageId = R.string.errorSyncNonSpecific
                        }
                        previewStatusMessage.setText(errorMessageId)
                        val builder = AlertDialog.Builder(
                            mainWindow!!
                        )
                        builder.setTitle(getString(R.string.syncErrorHeader))
                        builder.setMessage(getString(errorMessageId))
                        builder.setIcon(android.R.drawable.ic_dialog_alert)
                        builder.setCancelable(false)
                        if (errorMessageId != R.string.errorServerNotFound) {
                            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                                previewStatusMessage.visibility = View.VISIBLE
                            }
                        } else {
                            builder.setNegativeButton(R.string.syncCancel) { _, _ ->
                                previewStatusMessage.visibility = View.VISIBLE
                            }
                            builder.setPositiveButton(R.string.syncRetry) { _, _ ->
                                WifiSyncService.startSynchronisation(
                                    applicationContext,
                                    0,
                                    true,
                                    false
                                )
                                finish()
                            }
                        }
                        builder.show()
                    } else if (previewToData.size == 0 && previewFromData.size == 0) {
                        disableProceedSyncButton()
                        previewStatusMessage.setText(R.string.syncPreviewNoResults)
                        previewStatusMessage.visibility = View.VISIBLE
                    } else {
                        val previewToDataCount = previewToData.size
                        val previewFromDataCount = previewFromData.size
                        var okCount = 0
                        var warningCount = 0
                        var failedCount = 0
                        for (index in previewToData.indices) {
                            when (previewToData[index].alert.toInt()) {
                                0 -> okCount += 1
                                1 -> warningCount += 1
                                2, 3 -> failedCount += 1
                            }
                        }
                        if (warningCount > 0) {
                            previewErrorMessage.setTextColor(warningColor)
                            previewErrorMessage.text = String.format(
                                getString(R.string.reverseSyncWarnings),
                                if (warningCount == 1) getString(R.string.reverseSyncFilesWarning1) else String.format(
                                    getString(R.string.reverseSyncFilesWarningN),
                                    warningCount
                                )
                            )
                            previewErrorMessage.visibility = View.VISIBLE
                            syncExcludeErrors?.let{ it.visibility = View.VISIBLE }
                        } else if (failedCount > 0) {
                            previewErrorMessage.setTextColor(errorColor)
                            previewErrorMessage.text = String.format(
                                getString(R.string.reverseSyncFailed),
                                if (failedCount == 1) getString(R.string.reverseSyncFilesWarning1) else String.format(
                                    getString(R.string.reverseSyncFilesWarningN),
                                    failedCount
                                )
                            )
                            previewErrorMessage.visibility = View.VISIBLE
                            syncExcludeErrors?.let{ it.visibility = View.INVISIBLE }
                        } else {
                            previewErrorMessage.visibility = View.GONE
                            syncExcludeErrors?.let{ it.visibility = View.GONE }
                        }
                        if (previewToDataCount > 0 && previewFromDataCount == 0 && okCount == 0 && warningCount == 0) {
                            disableProceedSyncButton()
                        }
                        previewListView.visibility = View.VISIBLE
                        showResults(previewListView, previewToData, previewFromData)
                    }
                }
            } catch (ex: InterruptedException) {
                // ignore
            } catch (ex: Exception) {
                ErrorHandler.logError("preview", ex)
            }
        }
        waitResultsThread!!.start()
        setSupportActionBar(findViewById(R.id.my_toolbar))
    }

    override fun onDestroy() {
        if (waitResultsThread != null) {
            waitResultsThread!!.interrupt()
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sync_status, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent: Intent
        when (item.itemId) {
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun disableProceedSyncButton() {
        proceedSyncButton!!.isEnabled = false
        //DrawableCompat.setTint(proceedSyncButtonImage.getDrawable(), infoColor);
        proceedSyncButtonImage!!.setColorFilter(infoColor)
        proceedSyncButtonText!!.setTextColor(infoColor)
    }

    fun onProceedSyncButton_Click(view: View) {
        WifiSyncService.startSynchronisation(this, 1, false, !syncExcludeErrors!!.isChecked)
        finish()
    }
}