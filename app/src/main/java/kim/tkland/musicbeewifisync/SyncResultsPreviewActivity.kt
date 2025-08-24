package kim.tkland.musicbeewifisync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.MenuCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

class SyncResultsPreviewActivity : SyncResultsBaseActivity() {
    private var syncExcludeErrors: CheckBox? = null
    private var proceedSyncButton: LinearLayout? = null
    private var proceedSyncButtonImage: ImageView? = null
    private var proceedSyncButtonText: TextView? = null
    private var waitResultsThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_sync_preview)
        //val actionBar = supportActionBar
        //actionBar?.setDisplayHomeAsUpEnabled(true)
        setContent {
            MainAppComposable()
        }
        waitResultsThread = Thread {
            runOnUiThread {
                try {
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
                    } else if (previewToData.isEmpty() && previewFromData.isEmpty()) {
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
                            syncExcludeErrors?.let { it.visibility = View.VISIBLE }
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
                            syncExcludeErrors?.let { it.visibility = View.INVISIBLE }
                        } else {
                            previewErrorMessage.visibility = View.GONE
                            syncExcludeErrors?.let { it.visibility = View.GONE }
                        }
                        if (previewToDataCount > 0 && previewFromDataCount == 0 && okCount == 0 && warningCount == 0) {
                            disableProceedSyncButton()
                        }
                        previewListView.visibility = View.VISIBLE
                        showResults(previewListView, previewToData, previewFromData)
                    }
                } catch (_: InterruptedException) {
                    // ignore
                } catch (ex: Exception) {
                    ErrorHandler.logError("preview", ex)
                }
            }//
        }
        waitResultsThread!!.start()
        //setSupportActionBar(findViewById(R.id.my_toolbar))
    }

    @Composable
    fun MainAppComposable() {
        // Define MutableState variables to hold your data and UI state
        val previewToDataState = remember { mutableStateOf<ArrayList<SyncResultsInfo>?>(null) }
        val previewFromDataState = remember { mutableStateOf<ArrayList<SyncResultsInfo>?>(null) }
        //val isLoading = remember { mutableStateOf(true) } // Example loading state
        //val errorMessageIdState =
        //    remember { mutableStateOf<Int?>(null) } // Example error message state
        //val showProceedButton = remember { mutableStateOf(false) } // Example for button visibility
        // ... other states as needed

        // This is where you would launch your data fetching logic if it wasn't
        // tied to the existing waitResultsThread. For your current structure,
        // we'll update these states from the thread.

        while (waitResultsThread!!.isAlive) {
            // Show some loading indicator
            // CircularProgressIndicator() // Example
            Text("Loading preview...") // Placeholder
            Thread.sleep(300)
        }

        /*
        if (errorMessageIdState.value != null) {
            // Show error message, potentially an AlertDialog
            // For AlertDialog, you'd manage its visibility with another state
            val context = LocalContext.current
            androidx.compose.material3.AlertDialog( // Basic example, customize as needed
                onDismissRequest = { errorMessageIdState.value = null },
                title = { Text(stringResource(R.string.syncErrorHeader)) },
                text = { Text(stringResource(errorMessageIdState.value!!)) },
                confirmButton = {
                    Button(onClick = {
                        errorMessageIdState.value = null
                        if (errorMessageIdState.value == R.string.errorServerNotFound) {
                            // Handle retry logic if needed by updating state or calling a ViewModel function
                            WifiSyncService.startSynchronisation(
                                context.applicationContext,
                                0,
                                true,
                                false
                            )
                            // Potentially finish activity or reset state
                            (context as? Activity)?.finish()
                        }
                    }) {
                        Text(
                            if (errorMessageIdState.value == R.string.errorServerNotFound) stringResource(
                                R.string.syncRetry
                            ) else stringResource(android.R.string.ok)
                        )
                    }
                },
                dismissButton = if (errorMessageIdState.value == R.string.errorServerNotFound) {
                    {
                        Button(onClick = { errorMessageIdState.value = null }) {
                            Text(stringResource(R.string.syncCancel))
                        }
                    }
                } else null
            )
        } else if (previewToDataState.value?.isEmpty() == true && previewFromDataState.value?.isEmpty() == true) {
            // Show "no results" message
            Text(stringResource(R.string.syncPreviewNoResults))
        } else {
            // Call your CustomView (or ShowResultsComposable directly)

         */
            CustomView(
                title = getString(R.string.title_activity_sync_preview),
                resultsToData = previewToDataState.value,
                resultsFromData = previewFromDataState.value
                // Pass other necessary states like proceedButton visibility
            )
        //}
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView(
        title: String,
        resultsToData: ArrayList<SyncResultsInfo>?,
        resultsFromData: ArrayList<SyncResultsInfo>?
    ) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                Row( // Or a Compose Row
                    modifier = Modifier.fillMaxWidth()
                    //horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    //horizontalArrangement = Arrangement.Center,
                    // Your proceed button logic here, adapted to Compose
                    // Example:

                    //verticalAlignment = Alignment.Bottom

                ) {
                    Button(modifier = Modifier.weight(1f), onClick = { /* Handle proceed click */ }) {
                        Modifier.weight(1f)

                        //Icon()
                        //Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Sync", fontSize = 20.sp)
                    }
                }
            }
        ) { innerPadding ->
            ShowResultsComposable(innerPadding, resultsToData, resultsFromData)
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun ShowResultsComposable(
        innerPadding: PaddingValues,
        resultsToData: ArrayList<SyncResultsInfo>?,
        resultsFromData: ArrayList<SyncResultsInfo>?
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = innerPadding,
        ) {
            var resultsToData = resultsToData
            var resultsFromData = resultsFromData
            val maxResults = 256
            if (resultsToData == null) {
                resultsToData = ArrayList()
            }
            if (resultsFromData == null) {
                resultsFromData = ArrayList()
            }
            val resultsToDataCount = resultsToData.size
            val resultsFromDataCount = resultsFromData.size
            val filteredPreviewData: ArrayList<SyncResultsInfo>
            if (resultsToDataCount + resultsFromDataCount < maxResults + 16) {
                filteredPreviewData = ArrayList(resultsToDataCount + resultsFromDataCount)
                filteredPreviewData.addAll(resultsToData)
                filteredPreviewData.addAll(resultsFromData)
                // The items(...) call needs to be *inside* the LazyColumn's content lambda
                items(
                    // This was likely outside or misplaced
                    count = filteredPreviewData.size,
                    //key = { index -> filteredPreviewData[index].hashCode() } // Provide a stable key if possible
                ) { index ->
                    val item = filteredPreviewData[index]
                    // Your Composable item content here
                    // For example:
                    Text(
                        text = "${item.targetName}",
                        fontSize = 18.sp
                    )
                    val message =
                        if (item.estimatedSize!!.isEmpty()) item.action else "${item.action} - ${item.estimatedSize}"
                    Text(text = "$message", fontSize = 18.sp) // Replace with yo
                    // ... (rest of your item UI)
                }
            }
        }
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

    @OptIn(ExperimentalMaterial3Api::class)
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

//override fun onSupportNavigateUp(): Boolean {
//    finish()
//    return true
//}

    private fun disableProceedSyncButton() {
        proceedSyncButton?.isEnabled = false
        //DrawableCompat.setTint(proceedSyncButtonImage.getDrawable(), infoColor);
        proceedSyncButtonImage?.setColorFilter(infoColor)
        proceedSyncButtonText?.setTextColor(infoColor)
    }

    fun onProceedSyncButton_Click(view: View) {
        WifiSyncService.startSynchronisation(this, 1, false, !syncExcludeErrors!!.isChecked)
        finish()
    }
}
