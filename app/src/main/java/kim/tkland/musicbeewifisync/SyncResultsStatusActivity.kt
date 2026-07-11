package kim.tkland.musicbeewifisync

import android.R.color.white
import android.app.Activity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class SyncResultsStatusActivity : SyncResultsBaseActivity() {
    var showEndOfSyncInfoDialog = mutableStateOf(false)
    var showEndOfSyncDialog = mutableStateOf(false)
    var showRetryDialog = mutableStateOf(false)

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
            var buttonText by remember { mutableStateOf(getString(R.string.syncStop))}
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999) { // MediaStore.createWriteRequest
            (application as WifiSyncApp).signalPermissionResult()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView(currentSyncMessage: String, onCurrentSyncMessageChange: (String) -> Unit, buttonText: String, onButtonTextChange: (String) -> Unit) {
        val topAppBarState = rememberTopAppBarState()

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current // Get the context here

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        var completeMessage by remember { mutableStateOf("") }
        Scaffold(
            //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ResultScreenTopBar(
                    getString(R.string.title_activity_sync_status),
                    expanded,
                    { newValue -> expanded = newValue },
                )
            },
            bottomBar = {
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(white)), // 枠線の色
                            ),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            if (buttonText == getString(R.string.syncStop)) {
                                val intent = Intent()
                                intent.setClass(context, WifiSyncService::class.java)
                                intent.action = getString(R.string.actionSyncAbort)
                                startService(intent)
                                WifiSyncService.syncIsRunning.set(false)
                                WifiSyncService.syncPercentCompleted.set(-1)
                                WifiSyncServiceSettings.saveSettings(context)
                                WifiSyncService.syncErrorMessageId.set(R.string.syncCancelled)
                                completeMessage = getString(R.string.syncCancelled)
                                showEndOfSyncInfoDialog.value = true
                                onButtonTextChange(getString(R.string.syncMore))
                            } else if (buttonText == getString(R.string.syncMore)){
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
                statusBarPadding,
                currentSyncMessage, onCurrentSyncMessageChange,
                buttonText, onButtonTextChange,
            scrollBehavior)

            if (showEndOfSyncInfoDialog.value) {
                ShowEndOfSyncInformation(
                    paddingValue = innerPadding,
                    statusBarPadding = statusBarPadding,
                    message = completeMessage,
                    onDismiss = { showEndOfSyncInfoDialog.value = false})
            }
        }
    }

    @Composable
    fun ShowEndOfSyncInformation(
        paddingValue: PaddingValues,
        statusBarPadding: PaddingValues,
        message: String,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .padding(paddingValue),
                //.padding(statusBarPadding),
            //title = { Text(text = getString(R.string.syncCancelled)) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(getColor(R.color.colorButtonBackground)),
                        contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                    )
            ){
                    Text(text = getString(android.R.string.ok))
                }
            },
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ShowSyncStatusComposable(innerPadding: PaddingValues,
                             statusBarPadding: PaddingValues,
                             currentSyncMessage: String,
                             onCurrentSyncMessageChange: (String) -> Unit,
                             buttonText: String,
                             onButtonTextChange: (String) -> Unit,
                             scrollBehavior: TopAppBarScrollBehavior) {
    var rawTargetSyncProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    var showEndOfSyncInfo by remember { mutableStateOf(false) }
    var isErrorEnd by remember { mutableStateOf(false) }
    var compMessage by remember { mutableStateOf("") }
    var errorId by remember { mutableStateOf<Int?>(null) }

    val animatedSyncProgress by animateFloatAsState(
        targetValue = rawTargetSyncProgress,
        label = "syncProgressAnimation" // Optional but good for debugging
    )

    LaunchedEffect(showEndOfSyncInfo) {
        if (showEndOfSyncInfo && errorId == null) {
            errorId = WifiSyncService.syncErrorMessageId.getAndSet(0)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val isRunning = WifiSyncService.syncIsRunning.get()
            val progress = WifiSyncService.syncPercentCompleted.get()
            if (isRunning && progress != -1) {
                rawTargetSyncProgress = progress / 100f
                onCurrentSyncMessageChange(WifiSyncService.syncProgressMessage.get())
                showEndOfSyncInfo = false
                if (buttonText != getString(R.string.syncStop)) {
                    onButtonTextChange(getString(R.string.syncStop))
                }
            } else {
                rawTargetSyncProgress = if (progress == -1) 1f else progress / 100f
                onCurrentSyncMessageChange(WifiSyncService.syncProgressMessage.get())
                showEndOfSyncInfo = true
                if (buttonText != getString(R.string.syncMore)) {
                    onButtonTextChange(getString(R.string.syncMore))
                }
                loading = false
                break
            }
            delay(300.milliseconds)
        }
    }
    Column(
        modifier = Modifier
            //.fillMaxWidth()
            .background(Color(color = getColor(white)))
            .padding(innerPadding),
                //.padding(statusBarPadding)
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (loading) {
            LinearProgressIndicator(
                { animatedSyncProgress },
                modifier = Modifier
                    .padding(top = 120.dp, start = 15.dp, end = 15.dp)
                    .fillMaxWidth(),
                    color = Color(getColor(R.color.colorAccent)),
                    trackColor = Color(getColor(R.color.colorButtonBackground))
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 15.dp, end = 15.dp)
                    .height(90.dp),
                //.padding(start = 15.dp, top = 200.dp),
                text = compMessage,
                maxLines = 3,
                fontSize = 18.sp
            )
            CircularProgressIndicator(
                modifier = Modifier.width(64.dp),
                color = Color(getColor(R.color.colorAccent)),
                trackColor = Color(getColor(R.color.colorButtonTextEnabled))
            )
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Bottom) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .padding(start = 15.dp, end = 15.dp),
                    text = currentSyncMessage,
                    maxLines = 7,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
    if (showEndOfSyncInfo) {
        compMessage = getString(R.string.syncCompleted)
        val currentErrorId = errorId ?: 0
        if (currentErrorId == 0 && !isErrorEnd) {
            // 正常系のメッセージ表示、データのクリアを行う
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(statusBarPadding)
            ) {
                LinearProgressIndicator(
                    { animatedSyncProgress },
                    modifier = Modifier
                        .padding(top = 120.dp, start = 15.dp, end = 15.dp)
                        .fillMaxWidth(),
                    color = Color(getColor(R.color.colorButtonTextEnabled)),
                    trackColor = Color(getColor(R.color.colorButtonBackground))
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 15.dp, end = 15.dp)
                        .height(90.dp),
                    //.padding(start = 15.dp, top = 200.dp),
                    text = compMessage,
                    maxLines = 3,
                    fontSize = 18.sp
                )
            }
        } else {
            isErrorEnd = true
            ShowEndOfSyncInformation(innerPadding, statusBarPadding, compMessage, currentErrorId, onCompleteTextChange = {compMessage = it})
        }
    }
}

    override fun onDestroy() {
        WifiSyncService.resultsActivityReady.reset()
        mainWindow = null
        super.onDestroy()
    }

    @Composable
    private fun ShowEndOfSyncInformation(innerPadding: PaddingValues, statusBarPadding: PaddingValues, compMessage: String, errorMessageId: Int, onCompleteTextChange: (String) -> Unit) {
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
                    ShowResultsComposable(innerPadding, statusBarPadding, errorMessage, WifiSyncService.syncToResults,failedFrom)
                }
            }
        } else if (errorMessageId == R.string.syncCancelled) {
            errorMessage = getString(errorMessageId)
            onCompleteTextChange(errorMessage)
            ShowResultsComposable(innerPadding, statusBarPadding, errorMessage, WifiSyncService.syncToResults,null)

        } else {
            if (errorMessageId == R.string.errorServerNotFound) {
                showRetryDialog.value = true
                setContent {
                    if (showRetryDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showRetryDialog.value = false },
                            icon = { // Use the dedicated 'icon' parameter
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                                    contentDescription = "Error Icon"
                                )  /* Provide a content description)*/
                            },
                            title = { Text(getString(R.string.syncErrorHeader)) },
                            text = { Text(getString(errorMessageId)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        errorMessage = getString(R.string.syncRetry)
                                        onCompleteTextChange(errorMessage)
                                        WifiSyncService.startSynchronisation(
                                            applicationContext,
                                            WifiSyncService.syncIteration,
                                            false,
                                            false
                                        )
                                        WifiSyncService.syncErrorMessageId.getAndSet(0)
                                        showRetryDialog.value = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(getColor(R.color.colorButtonBackground)),
                                        contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                    )
                                ) {
                                    Text(getString(R.string.syncRetry))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        errorMessage = getString(R.string.syncCancel)
                                        onCompleteTextChange(errorMessage)
                                        setContent {
                                            ShowErrorResultsComposable(
                                                errorMessage,
                                                WifiSyncService.syncToResults,
                                                null
                                            )
                                        }
                                        WifiSyncService.syncErrorMessageId.getAndSet(0)
                                        showRetryDialog.value = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(getColor(R.color.colorButtonBackground)),
                                        contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                    )

                                ) {
                                    Text(getString(R.string.syncCancel))
                                }
                            }
                        )
                    }
                }
            } else {
                showEndOfSyncDialog.value = true
                setContent {
                    if (showEndOfSyncDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showEndOfSyncDialog.value = false },
                            icon = { // Use the dedicated 'icon' parameter
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                                    contentDescription = "Error Icon"
                                )  /* Provide a content description)*/
                            },
                            title = { Text(getString(R.string.syncErrorHeader)) },
                            text = { Text(getString(errorMessageId)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        errorMessage = getString(errorMessageId)
                                        onCompleteTextChange(errorMessage)
                                        setContent {
                                            ShowErrorResultsComposable(
                                                errorMessage,
                                                WifiSyncService.syncToResults,
                                                null
                                            )
                                        }
                                        WifiSyncService.syncErrorMessageId.getAndSet(0)
                                        showEndOfSyncDialog.value = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(getColor(R.color.colorButtonBackground)),
                                        contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                    )
                                ) {
                                    Text(getString(android.R.string.ok))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    @Composable
    fun ShowResultsComposable(
        innerPadding: PaddingValues,
        statusBarPadding: PaddingValues,
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
                Modifier
                    .background(Color(color = getColor(white)))
                    .padding(innerPadding),
                    //.padding(statusBarPadding),
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowErrorResultsComposable(
        errorMessage: String,
        resultsToData: ArrayList<SyncResultsInfo>?,
        resultsFromData: ArrayList<SyncResultsInfo>?
    ) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current // Get the context here
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

        Scaffold(
            //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ResultScreenTopBar(
                    getString(R.string.title_activity_sync_status),
                    expanded,
                    { newValue -> expanded = newValue },
                )
            },
            bottomBar = {
                Column (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(white)), // 枠線の色
                            ),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            val intent =
                                Intent(context, MainActivity::class.java) // Use the context
                            context.startActivity(intent)
                            finish()
                        }
                    ){
                        Text(getString(R.string.syncMore), fontSize = 20.sp)
                    }
                }
            }
        ){ innerPadding ->
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
                        .background(Color(color = getColor(white)))
                        .padding(innerPadding),
                //.padding(statusBarPadding),
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
}
