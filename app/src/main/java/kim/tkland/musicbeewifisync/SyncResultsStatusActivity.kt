package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compose.AppTheme
import kotlinx.coroutines.delay

class SyncResultsStatusActivity : SyncResultsBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WifiSyncService.resultsActivityReady.set()

        // OnBackPressedDispatcher を取得
        onBackPressedDispatcher.addCallback(
            this, // LifecycleOwner を指定
            object : OnBackPressedCallback(true) { // trueでコールバックを有効にする
                override fun handleOnBackPressed() {
                    // 「戻る」操作をインターセプトする処理をここに書く
                    // 何もせず、あるいは特定の処理を行う場合は、super.handleOnBackPressed() を呼ばない
                    // 例: 何もしない
                }
            }
        )
        setContent{
            var buttonText by remember { mutableStateOf("STOP") }
            var currentSyncMessage by remember { mutableStateOf("") }

            CustomView(
                currentSyncMessage = currentSyncMessage,
                onCurrentSyncMessageChange = { newText ->
                    currentSyncMessage = newText
                },
                buttonText = buttonText,
                onButtonTextChange = { newText ->
                        buttonText = newText
                }
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView(currentSyncMessage: String, onCurrentSyncMessageChange: (String) -> Unit, buttonText: String, onButtonTextChange: (String) -> Unit) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current // Get the context here

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                        modifier = Modifier.height(75.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            titleContentColor = Color(getColor(R.color.colorButtonTextEnabled))
                    ),
                    title = {
                        Box( // Wrap the Text in a Box
                            modifier = Modifier.fillMaxHeight(), // Fill the available height in the title slot
                            contentAlignment = Alignment.BottomCenter // Align content to the bottom start
                        ) {
                            Text(
                                text = getString(R.string.title_activity_sync_status),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuSyncSettings)) },
                                    onClick = {
                                        val intent = Intent(
                                            applicationContext,
                                            SettingsActivity::class.java
                                        )
                                        expanded = false
                                        startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuWifiSyncLog)) },
                                    onClick = {
                                        val intent = Intent(
                                            applicationContext,
                                            ViewErrorLogActivity::class.java
                                        )
                                        expanded = false
                                        startActivity(intent)
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                Column (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(R.color.colorButtonTextEnabled)), // 枠線の色
                            ),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            if (buttonText == "STOP") {
                                try {
                                    WifiSyncServiceSettings.syncCustomFiles = false
                                    runOnUiThread {
                                        WifiSyncService.waitSyncResults.waitOne()
                                        WifiSyncService.startSynchronisation(
                                            applicationContext,
                                            0,
                                            false,
                                            false
                                        )
                                        onButtonTextChange("Sync More")
                                    }
                                } catch (ex: Exception) {
                                    Log.d("onSyncStartButtonClick", ex.message!!)
                                }
                            } else if (buttonText == "Sync More"){
                                val intent =
                                    Intent(context, MainActivity::class.java) // Use the context
                                context.startActivity(intent)
                                finish()
                            }
                        }
                    ){
                        Text(buttonText, fontSize = 20.sp)
                    }
                }
            }
        ){ innerPadding ->
            ShowSyncStatusComposable(innerPadding,
                currentSyncMessage, onCurrentSyncMessageChange,
                buttonText, onButtonTextChange,
            scrollBehavior)
        }
    }
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ShowSyncStatusComposable(innerPadding: PaddingValues,
                             currentSyncMessage: String, onCurrentSyncMessageChange: (String) -> Unit,
                             buttonText: String, onButtonTextChange: (String) -> Unit,
                             scrollBehavior: TopAppBarScrollBehavior) {
    var rawTargetSyncProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var showEndOfSyncInfo by remember { mutableStateOf(false) }
    var isErrorEnd by remember { mutableStateOf(false) }
    var completeMessage by remember { mutableStateOf("") }

    val animatedSyncProgress by animateFloatAsState(
        targetValue = rawTargetSyncProgress,
        label = "syncProgressAnimation" // Optional but good for debugging
    )

    LaunchedEffect(Unit) {
        while (true) {
            if (WifiSyncService.syncPercentCompleted.get() != -1) {
                rawTargetSyncProgress = WifiSyncService.syncPercentCompleted.get() / 100f
                onCurrentSyncMessageChange(WifiSyncService.syncProgressMessage.get())
                showEndOfSyncInfo = false
                onButtonTextChange("STOP")
            } else {
                rawTargetSyncProgress = 1f // Or whatever the final state should be
                onCurrentSyncMessageChange("Sync finished") // Or a message from WifiSyncService
                showEndOfSyncInfo = true
                onButtonTextChange("Sync More")
                loading = false
                break
            }
            delay(300)
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding)
    ) {
        if (loading) {
            LinearProgressIndicator(
                { animatedSyncProgress },
                modifier = Modifier
                    .padding(top = 120.dp, start = 15.dp, end = 15.dp)
                    .fillMaxWidth()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 15.dp, end = 15.dp)
                    .height(90.dp),
                //.padding(start = 15.dp, top = 200.dp),
                text = completeMessage,
                maxLines = 3,
                fontSize = 18.sp
            )
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
            )
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    text = currentSyncMessage,
                    maxLines = 7,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
    if (showEndOfSyncInfo) {
        onCurrentSyncMessageChange("")
        completeMessage = getString(R.string.syncCompleted)
        if (WifiSyncService.syncErrorMessageId.get() == 0 && !isErrorEnd) {
            // 正常系のメッセージ表示、データのクリアを行う
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                LinearProgressIndicator(
                    { animatedSyncProgress },
                    modifier = Modifier
                        .padding(top = 120.dp, start = 15.dp, end = 15.dp)
                        .fillMaxWidth()
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 15.dp, end = 15.dp)
                        .height(90.dp),
                    //.padding(start = 15.dp, top = 200.dp),
                    text = completeMessage,
                    maxLines = 3,
                    fontSize = 18.sp
                )
            }
        } else {
            isErrorEnd = true
            ShowEndOfSyncInformation(innerPadding, completeMessage, { completeMessage = it })
        }
    }
}

    override fun onDestroy() {
        WifiSyncService.resultsActivityReady.reset()
        mainWindow = null
        super.onDestroy()
    }

    @Composable
    fun onStopSyncButton_Click(view: View) {
        /*
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
            ShowEndOfSyncInformation()
        }
         */
    }

    /*
    private fun stopProgressTimer() {
        syncProgressBar!!.visibility = View.INVISIBLE
        syncWaitIndicator!!.visibility = View.INVISIBLE
        syncProgressMessage!!.visibility = View.GONE
        timerHandler.removeCallbacks(timerRunnable!!)
        stopSyncButton!!.text = getString(R.string.syncMore)
    }
         */

    @Composable
    private fun ShowEndOfSyncInformation(innerPadding: PaddingValues, completeMessage: String, onCompleteTextChange: (String) -> Unit) {
        val errorMessageId = WifiSyncService.syncErrorMessageId.getAndSet(0)
        WifiSyncServiceSettings.saveSettings(this)
        var errorMessage = ""
        if (errorMessageId == 0 || errorMessageId == R.string.syncCompletedFail) {
            val messageId = if (errorMessageId != 0) errorMessageId else R.string.syncCompleted
            if (errorMessageId == R.string.syncCompletedFail) {
                if ((WifiSyncService.syncToResults == null || WifiSyncService.syncToResults!!.isEmpty()) && WifiSyncService.syncFailedFiles.isEmpty()) {
                    errorMessage = getString(R.string.syncCompletedFailErrorLog)
                    onCompleteTextChange(errorMessage)
                } else {

                    errorMessage = getString(R.string.syncCompletedFailMessage)
                    onCompleteTextChange(errorMessage)
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
                    ShowResultsComposable(innerPadding, errorMessage, WifiSyncService.syncToResults,failedFrom)
                }
            }
        } else if (errorMessageId == R.string.syncCancelled) {
            errorMessage = getString(errorMessageId)
            onCompleteTextChange(errorMessage)
            ShowResultsComposable(innerPadding, errorMessage, WifiSyncService.syncToResults,null)
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.syncErrorHeader))
            builder.setMessage(getString(errorMessageId))
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setCancelable(false)
            if (errorMessageId != R.string.errorServerNotFound) {
                builder.setPositiveButton(android.R.string.ok) { _, _ ->
                    errorMessage = getString(errorMessageId)
                    onCompleteTextChange(errorMessage)
                }
            } else {
                builder.setNegativeButton(R.string.syncCancel) { _, _ ->
                    errorMessage = getString(errorMessageId)
                    onCompleteTextChange(errorMessage)
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
            ShowResultsComposable(innerPadding, errorMessage, WifiSyncService.syncToResults,null)
            builder.show()
        }
    }
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun ShowResultsComposable(
        innerPadding: PaddingValues,
        errorMessage: String,
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
                Modifier.padding(innerPadding)
                    .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            item {
                Text(text = errorMessage, maxLines = 5, fontSize = 20.sp)
            }
            item {
                HorizontalDivider(thickness = 2.dp)
            }
            for (info: SyncResultsInfo in filteredPreviewData) {
                val iconid =
                    if (info.alert != SyncResultsInfo.ALERT_INFO) android.R.drawable.stat_notify_error else 0

                item {
                    Row() {
                        if (iconid != 0) {
                            Icon(
                                painter = painterResource(id = iconid),
                                contentDescription = "Sync Direction Icon", // Provide a content description
                            )
                        }
                        Text(
                            text = info.targetName!!,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 24.sp
                        )
                    }
                }
                item {
                    Text(
                        text = info.message!!,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 24.sp
                    )
                }
                item {
                    HorizontalDivider(thickness = 2.dp)
                }
            }
        }
    }
}
