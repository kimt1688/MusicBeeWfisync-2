package kim.tkland.musicbeewifisync

import android.R.color.white
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking


class SyncResultsPreviewActivity : SyncResultsBaseActivity() {
    private var waitResultsThread: Thread? = null

    // private var showDialogState = mutableStateOf(false)

    private var isSyncButtonEnabled = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (WifiSyncService.syncErrorMessageId.get() != 0) {
            return
        }
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        waitResultsThread = Thread {
            try {
                WifiSyncService.waitSyncResults.waitOne()
                val previewToData = WifiSyncService.syncToResults
                val previewFromData = WifiSyncService.syncFromResults
                if (mainWindow == null) {
                    // ignore
                } else if (previewToData == null || previewFromData == null) {
                    setContent {
                        disableProceedSyncButton()
                    }
                    var errorMessageId = WifiSyncService.syncErrorMessageId.get()
                    if (errorMessageId == 0) {
                        errorMessageId = R.string.errorSyncNonSpecific
                    }
                } else if (previewToData.isEmpty() && previewFromData.isEmpty()) {
                    setContent {
                        disableProceedSyncButton()
                        ShowErrorMessageView(
                            getString(R.string.title_activity_sync_preview),
                            getString(R.string.syncPreviewNoResults),
                            getColor(R.color.colorError)
                        )
                    }
                } else {
                    val previewToDataCount = previewToData.size
                    val previewFromDataCount = previewFromData.size
                    var okCount = 0
                    var warningCount = 0
                    var failedCount = 0
                    previewToData.let {
                        for (index in it.indices) {
                            when (previewToData[index].alert.toInt()) {
                                0 -> okCount += 1
                                1 -> warningCount += 1
                                2, 3 -> failedCount += 1
                            }
                        }
                    }
                    if (warningCount > 0) {
                        val warningColor = getColor(R.color.colorWarning)
                        val warningText = String.format(
                            getString(R.string.reverseSyncWarnings),
                        if (warningCount == 1) getString(R.string.reverseSyncFilesWarning1) else String.format(
                            getString(R.string.reverseSyncFilesWarningN),
                            warningCount
                        ))
                        setContent {
                            ShowErrorMessageView(
                                getString(R.string.title_activity_sync_preview),
                                warningText,
                                warningColor
                            )
                        }
                    } else if (failedCount > 0) {
                        val failedText = String.format(
                            getString(R.string.reverseSyncFailed),
                        if (failedCount == 1) getString(R.string.reverseSyncFilesWarning1) else String.format(
                            getString(R.string.reverseSyncFilesWarningN),
                            failedCount
                        ))
                        setContent {
                            ShowErrorMessageView(
                                getString(R.string.title_activity_sync_preview),
                                failedText,
                                getColor(R.color.colorButtonTextEnabled)
                            )
                        }
                    }
                    if (previewToDataCount > 0 && previewFromDataCount == 0 && okCount == 0 && warningCount == 0) {
                        setContent {
                            disableProceedSyncButton()
                        }
                    }
                    setContent {
                        CustomView(
                            title = getString(R.string.title_activity_sync_preview),
                            resultsToData = previewToData,
                            resultsFromData = previewFromData
                        )
                    }
                }
            } catch (_: InterruptedException) {
                // ignore
            } catch (ex: Exception) {
                ErrorHandler.logError("preview", ex)
            }
        }
        setContent {
            ShowWaitResultsView()
        }
        waitResultsThread!!.start()
    }
    private fun requireContext(): Context {
        return applicationContext
    }

    @Composable
    fun ShowWaitResultsView() {
        var expanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                ResultScreenTopBar(
                    getString(R.string.title_activity_sync_preview),
                    expanded,
                    { newValue -> expanded = newValue },
                )
            },
        ){ innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
                    .padding(top = 120.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
                propagateMinConstraints = false
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(64.dp),
                    color = Color(getColor(R.color.colorAccent)),
                    trackColor = Color(getColor(R.color.colorButtonTextEnabled)),
                )
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowErrorMessageView(title: String, message: String, color: Int) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        val statusBarPadding = WindowInsets.systemBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

            topBar = {
                ResultScreenTopBar(
                    title,
                    expanded,
                    { newValue -> expanded = newValue },
                )
            },
            bottomBar = {
                Row( // Or a Compose Row
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding)
                ) {
                    Button(modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .border(
                            width = 2.dp, // 枠線の幅
                            color = Color(getColor(white)), // 枠線の色
                        ),
                        enabled = isSyncButtonEnabled.value,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            try {
                                WifiSyncServiceSettings.syncCustomFiles = false
                                WifiSyncService.startSynchronisation(applicationContext, 0, false, false)
                            }catch (ex:Exception){
                                Log.d("onSyncStartButtonClick", ex.message!!)
                            } finally {
                                isSyncButtonEnabled.value = true
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
            Text(
                message,
                modifier = Modifier
                    .padding(top = 100.dp, start = 20.dp, end = 20.dp)
                    .padding(innerPadding)
                    .padding(statusBarPadding),
                color = Color(color),
                fontSize = 20.sp,
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
        var expanded by remember { mutableStateOf(false) }

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

        Scaffold(
            // modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ResultScreenTopBar(
                    title,
                    expanded,
                    { newValue -> expanded = newValue },
                )
            },
            bottomBar = {
                Row( // Or a Compose Row
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding)
                ) {
                    Button(modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .border(
                            width = 2.dp, // 枠線の幅
                            color = Color(getColor(white)), // 枠線の色
                        ),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        enabled = isSyncButtonEnabled.value,
                        onClick = {
                            try {
                                WifiSyncService.startSynchronisation(applicationContext, 1, false, false)
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
                        Text(getString(R.string.syncNow), fontSize = 20.sp)
                    }
                }
            }
        ) { innerPadding ->
            ShowResultsComposable(innerPadding, statusBarPadding, resultsToData, resultsFromData)
        }
    }

    @Composable
    fun ShowResultsComposable(
        innerPadding: PaddingValues,
        statusBarPadding: PaddingValues,
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
        val resultsToDataCount = if (resultsToData.isEmpty()) 0 else resultsToData.size
        val resultsFromDataCount = if (resultsFromData.isEmpty()) 0 else resultsFromData.size
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
                Modifier
                    .background(Color(getColor(white)))
                    .padding(innerPadding)
                    .padding(top = 3.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            for (info: SyncResultsInfo in filteredPreviewData) {
                val iconid = if (info.direction == SyncResultsInfo.DIRECTION_REVERSE_SYNC) R.drawable.ic_arrow_back else R.drawable.ic_arrow_forward
                item {
                    Row() {
                        Icon(
                            painter = painterResource(id = iconid),
                            contentDescription = "Sync Direction Icon", // Provide a content description
                        )
                        Text(
                            text = info.targetName ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 24.sp
                        )
                    }
                }
                item {
                    Text(
                        modifier = Modifier.padding(start = 20.dp),
                        text = if (info.estimatedSize == null) info.message!! else "${info.action!!} - ${info.estimatedSize}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 24.sp,
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

    @Composable
    private fun disableProceedSyncButton() {
        isSyncButtonEnabled.value = false
        //proceedSyncButton?.isEnabled = false
        //DrawableCompat.setTint(proceedSyncButtonImage.getDrawable(), infoColor);
        //proceedSyncButtonImage?.setColorFilter(infoColor)
        //proceedSyncButtonText?.setTextColor(infoColor)
    }
}
