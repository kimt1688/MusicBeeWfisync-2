package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import kim.tkland.musicbeewifisync.ui.theme.AppShapes
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger


class SyncResultsStatusActivity : SyncResultsBaseActivity() {
    private var syncCompletionStatusMessage: TextView? = null
    private var syncFailedResults: ListView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_sync_status)
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
            AppTheme() {
                val navController = rememberNavController()
                var buttonText by remember { mutableStateOf("STOP") }
                NavHost(navController = navController, startDestination = "CustomView") {
                    composable(route = "CustomView") {
                        CustomView(navController, buttonText){ newText->
                            buttonText = newText
                        }
                    }
                    composable(route = "CustomErrorView") {
                        CustomErrorView(buttonText){ newText->
                            buttonText = newText
                        }
                    }
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView(navController: NavController, buttonText: String, onButtonTextChange: (String) -> Unit) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                        modifier = Modifier.height(75.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
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
        ){ innerPadding ->
            ShowSyncStatusComposable(innerPadding, navController, buttonText, onButtonTextChange)
        }
    }
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
    fun ShowSyncStatusComposable(innerPadding: PaddingValues, navController: NavController, buttonText: String, onButtonTextChange: (String) -> Unit) {
    var rawTargetSyncProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope() // Create a coroutine scope
    var currentSyncMessage by remember { mutableStateOf("") }
    var showEndOfSyncInfo by remember { mutableStateOf(false) }
    var completeMessage by remember { mutableStateOf("") }

    val animatedSyncProgress by animateFloatAsState(
        targetValue = rawTargetSyncProgress,
        label = "syncProgressAnimation" // Optional but good for debugging
    )

    LaunchedEffect(Unit) {
        while (true) {
            if (WifiSyncService.syncPercentCompleted.get() != -1) {
                rawTargetSyncProgress = WifiSyncService.syncPercentCompleted.get() / 100f
                currentSyncMessage = WifiSyncService.syncProgressMessage.get()
                showEndOfSyncInfo = false
                onButtonTextChange("STOP")
            } else {
                rawTargetSyncProgress = 1f // Or whatever the final state should be
                currentSyncMessage = "Sync finished" // Or a message from WifiSyncService
                showEndOfSyncInfo = true
                onButtonTextChange("Sync More")
                loading = false
                break
            }
            delay(300)
        }
    }
    Scaffold(
        //modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            Column (
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .height(170.dp),
                    text = currentSyncMessage,
                    maxLines = 7,
                    fontSize = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
                Button(
                    modifier = Modifier.fillMaxWidth()
                                        .height(80.dp),
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
                                }
                                onButtonTextChange("Sync More")
                            } catch (ex: Exception) {
                                Log.d("onSyncStartButtonClick", ex.message!!)
                            }
                        } else {
                            val intent =
                                Intent(this@SyncResultsStatusActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    ){
                    //Modifier.weight(1f)

                    Text(buttonText, fontSize = 20.sp)
                        }
                    }
                }
        ){ scaffoldPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            LinearProgressIndicator(
                { animatedSyncProgress },
                modifier = Modifier.padding(top = 120.dp, start = 15.dp, end = 15.dp)
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
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.width(64.dp),
                )
            }

            if (showEndOfSyncInfo) {
                currentSyncMessage = ""
                completeMessage = getString(R.string.syncCompleted)
                if (WifiSyncService.syncErrorMessageId == AtomicInteger(0)) {
                    ShowEndOfSyncInformation() // Call your composable here based on state
                } else {
                    navController.navigate("CustomErrorView")
                }
            }
        }
        }
    }

    override fun onDestroy() {
        WifiSyncService.resultsActivityReady.reset()
        //timerHandler.removeCallbacks(timerRunnable!!)
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

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun CustomErrorView(buttonText: String, onButtonTextChange: (String) -> Unit) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }


        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                        modifier = Modifier.height(75.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth()
                            .height(80.dp),
                        shape = androidx.compose.ui.graphics.RectangleShape,
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
                                    }
                                    onButtonTextChange("Sync More")
                                } catch (ex: Exception) {
                                    Log.d("onSyncStartButtonClick", ex.message!!)
                                }
                            } else {
                                val intent =
                                    Intent(this@SyncResultsStatusActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        },
                    ){Text(buttonText, fontSize = 20.sp)}
                }
            }
        ){ innerPadding ->
            ShowEndOfSyncInformation()
        }
    }
    @Composable
    private fun ShowEndOfSyncInformation() {
        val errorMessageId = WifiSyncService.syncErrorMessageId.getAndSet(0)
        WifiSyncServiceSettings.saveSettings(this)
        if (errorMessageId == 0 || errorMessageId == R.string.syncCompletedFail) {
            val messageId = if (errorMessageId != 0) errorMessageId else R.string.syncCompleted
            //syncCompletionStatusMessage!!.setText(messageId)
            //syncCompletionStatusMessage!!.visibility = View.VISIBLE
            if (errorMessageId == R.string.syncCompletedFail) {
                if ((WifiSyncService.syncToResults == null || WifiSyncService.syncToResults!!.isEmpty()) && WifiSyncService.syncFailedFiles.isEmpty()) {
                    syncCompletionStatusMessage!!.setText(R.string.syncCompletedFailErrorLog)
                    syncFailedResults!!.visibility = View.VISIBLE
                } else {
                    val params =
                        syncCompletionStatusMessage!!.layoutParams as ConstraintLayout.LayoutParams
                    params.topToTop = findViewById<Toolbar>(R.id.my_toolbar).top
                    params.setMargins(0, findViewById<Toolbar>(R.id.my_toolbar).height, 0, 0)
                    params.bottomToTop = findViewById<Button>(R.id.stopSyncButton).top
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
                    Text(text = getString(errorMessageId))
                    ShowResultsComposable(
                        PaddingValues(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 0.dp),
                        WifiSyncService.syncToResults,
                        failedFrom
                    )
                }
            }
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
