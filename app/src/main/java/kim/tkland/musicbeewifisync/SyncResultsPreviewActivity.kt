package kim.tkland.musicbeewifisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.sp
import androidx.core.view.MenuCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider

class SyncResultsPreviewActivity : SyncResultsBaseActivity() {
    private var syncExcludeErrors: CheckBox? = null
    private var proceedSyncButton: LinearLayout? = null
    private var proceedSyncButtonImage: ImageView? = null
    private var proceedSyncButtonText: TextView? = null
    private var waitResultsThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }        //setContentView(R.layout.activity_sync_preview)
        //val actionBar = supportActionBar
        //actionBar?.setDisplayHomeAsUpEnabled(true)
        waitResultsThread = Thread {
            try {
                runOnUiThread {
                    WifiSyncService.waitSyncResults.waitOne()
                    val previewToData = WifiSyncService.syncToResults
                    val previewFromData = WifiSyncService.syncFromResults
                    if (mainWindow == null) {
                        // ignore
                        /*
                    } else if (previewToData == null || previewFromData == null) {
                        //disableProceedSyncButton()
                        var errorMessageId = WifiSyncService.syncErrorMessageId.get()
                        if (errorMessageId == 0) {
                            errorMessageId = R.string.errorSyncNonSpecific
                        }
                        val builder = AlertDialog.Builder(
                            mainWindow!!
                        )
                        builder.setTitle(getString(R.string.syncErrorHeader))
                        builder.setMessage(getString(errorMessageId))
                        builder.setIcon(android.R.drawable.ic_dialog_alert)
                        builder.setCancelable(false)
                        if (errorMessageId != R.string.errorServerNotFound) {
                            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                            }
                        } else {
                            builder.setNegativeButton(R.string.syncCancel) { _, _ ->
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

 */
                        //} else if (previewToData.isEmpty() && previewFromData.isEmpty()) {
                        //disableProceedSyncButton()
                    } else {
                        val previewToDataCount = previewToData?.size
                        val previewFromDataCount = previewFromData?.size
                        var okCount = 0
                        var warningCount = 0
                        var failedCount = 0
                        previewToData?.let {
                            for (index in it.indices) {
                                when (previewToData[index].alert.toInt()) {
                                    0 -> okCount += 1
                                    1 -> warningCount += 1
                                    2, 3 -> failedCount += 1
                                }
                            }
                        }
                        if (warningCount > 0) {
                            if (warningCount == 1) getString(R.string.reverseSyncFilesWarning1) else String.format(
                                getString(R.string.reverseSyncFilesWarningN),
                                warningCount
                            )
                        } else if (failedCount > 0) {
                            if (failedCount == 1) getString(R.string.reverseSyncFilesWarning1) else String.format(
                                getString(R.string.reverseSyncFilesWarningN),
                                failedCount
                            )
                        }
                        //if (previewToDataCount > 0 && previewFromDataCount == 0 && okCount == 0 && warningCount == 0) {
                        //disableProceedSyncButton()
                        //}
                        setContent {
                            //MainAppComposable()
                            CustomView(
                                title = getString(R.string.title_activity_sync_preview),
                                resultsToData = previewToData,
                                resultsFromData = previewFromData
                            )
                        }
                    }
                }
            } catch (_: InterruptedException) {
                // ignore
            } catch (ex: Exception) {
                ErrorHandler.logError("preview", ex)
            }
        }
        waitResultsThread!!.start()
        //setSupportActionBar(findViewById(R.id.my_toolbar))

    }
    private fun requireContext(): Context {
        return applicationContext
    }

    @Composable
    fun MainAppComposable() {
        // Define MutableState variables to hold your data and UI state
        val previewToDataState = remember { mutableStateOf<ArrayList<SyncResultsInfo>?>(null) }
        val previewFromDataState = remember { mutableStateOf<ArrayList<SyncResultsInfo>?>(null) }
        //val isLoading = remember { mutableStateOf(true) } // Example loading state
        val errorMessageIdState =
            remember { mutableStateOf<Int?>(null) } // Example error message state
        val showProceedButton = remember { mutableStateOf(false) } // Example for button visibility
        // ... other states as needed

        // This is where you would launch your data fetching logic if it wasn't
        // tied to the existing waitResultsThread. For your current structure,
        // we'll update these states from the thread.

        //while (waitResultsThread!!.isAlive) {
            // Show some loading indicator
            // CircularProgressIndicator() // Example
        //    Text("Loading preview...") // Placeholder
        //    Thread.sleep(300)
        //}

        if (errorMessageIdState.value != null) {
            // Show error message, potentially an AlertDialog
            // For AlertDialog, you'd manage its visibility with another state
            /*
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

             */
        } else if (previewToDataState.value?.isEmpty() == true && previewFromDataState.value?.isEmpty() == true) {
            // Show "no results" message
            Text(stringResource(R.string.syncPreviewNoResults))
        } else {
            // Call your CustomView (or ShowResultsComposable directly)

            CustomView(
                title = getString(R.string.title_activity_sync_preview),
                resultsToData = previewToDataState.value,
                resultsFromData = previewFromDataState.value
                // Pass other necessary states like proceedButton visibility
            )
        }
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
                    Button(modifier = Modifier.weight(1f)
                                                .height(80.dp),
                        onClick = {
                            try {
                                WifiSyncServiceSettings.syncCustomFiles = false
                                //syncPreview = false
                                WifiSyncService.startSynchronisation(applicationContext, 0, false, false)
                            }catch (ex:Exception){
                                Log.d("onSyncStartButtonClick", ex.message!!)
                            } finally {
                                //syncStartButton!!.isEnabled = true
                            }
                        }) {
                        Modifier.weight(1f)

                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync",
                        )
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
            var resultsToData = resultsToData
            var resultsFromData = resultsFromData
            val maxResults = 256
            if (resultsToData == null) {
                resultsToData = ArrayList()
            }
            if (resultsFromData == null) {
                resultsFromData = ArrayList()
            }
            val resultsToDataCount = if (resultsToData.isNullOrEmpty()) 0 else resultsToData.size
            val resultsFromDataCount = if (resultsFromData.isNullOrEmpty()) 0 else resultsFromData.size
            val filteredPreviewData: ArrayList<SyncResultsInfo>
            if (resultsToDataCount + resultsFromDataCount < maxResults + 16) {
                filteredPreviewData = ArrayList(resultsToDataCount + resultsFromDataCount)
                filteredPreviewData.addAll(resultsToData)
                filteredPreviewData.addAll(resultsFromData)
            } else {
                filteredPreviewData = ArrayList(maxResults + 2)
                var filteredPreviewFromCount = resultsFromDataCount
                var filteredPreviewToCount = resultsToDataCount
                if (resultsToDataCount < maxResults / 4) {
                    filteredPreviewFromCount = maxResults - resultsToDataCount
                } else if (resultsFromDataCount < maxResults / 4) {
                    filteredPreviewToCount = maxResults - resultsFromDataCount
                } else {
                    val scaling =
                        maxResults.toDouble() / (resultsToDataCount + resultsFromDataCount).toDouble()
                    filteredPreviewToCount *= scaling.toInt()
                    filteredPreviewFromCount *= scaling.toInt()
                }
                for (index in 0 until filteredPreviewToCount) {
                    filteredPreviewData.add(resultsToData[index])
                }
                if (filteredPreviewToCount < resultsToDataCount) {
                    filteredPreviewData.add(
                        SyncResultsInfo(
                            String.format(
                                getString(R.string.syncPreviewMoreResults),
                                resultsToDataCount - filteredPreviewToCount
                            )
                        )
                    )
                }
                for (index in 0 until filteredPreviewFromCount) {
                    filteredPreviewData.add(resultsFromData[index])
                }
                if (filteredPreviewFromCount < resultsFromDataCount) {
                    filteredPreviewData.add(
                        SyncResultsInfo(
                            String.format(
                                getString(R.string.syncPreviewMoreResults),
                                resultsFromDataCount - filteredPreviewFromCount
                            )
                        )
                    )
                }
            }
        LazyColumn (
            modifier =
                Modifier.padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
        ) {
            for (info: SyncResultsInfo in filteredPreviewData) {
                item {
                    Text(text = if (info.targetName.isNullOrEmpty()) "" else info.targetName, fontSize = 24.sp)
                }
                item {
                    Text(
                        text = if (info.estimatedSize.isNullOrEmpty()) info.message!! else "${info.action!!} - ${info.estimatedSize}",
                        fontSize = 24.sp
                    )
                }
                item {
                    HorizontalDivider(thickness = 2.dp)
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
