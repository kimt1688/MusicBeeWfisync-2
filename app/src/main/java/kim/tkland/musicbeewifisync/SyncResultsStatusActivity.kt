package kim.tkland.musicbeewifisync

import android.R.attr.onClick
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowSize
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
import androidx.core.view.MenuCompat
import com.example.compose.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            AppTheme {
                CustomView()
            }
        }
        /*
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
         */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // setSupportActionBar(findViewById(R.id.my_toolbar))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

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
                            getString(R.string.title_activity_sync_status),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
            ShowSyncStatusComposable(innerPadding)
            /*
            AndroidView(
                modifier = Modifier
                    .fillMaxSize() // Fill the available space within the Scaffold
                    .padding(innerPadding), // Apply padding from the Scaffold (e.g., for the TopAppBar)
                factory = { context ->
                    val rootView =
                        LayoutInflater.from(context).inflate(R.layout.activity_sync_status, null)
                    rootView.apply {
                        // Creates view
                        syncCompletionStatusMessage = findViewById(R.id.syncCompletionStatusMessage)
                        //syncCompletionStatusMessage!!.visibility = View.VISIBLE
                        syncFailedResults = findViewById(R.id.syncFailedResults)
                        syncProgressBar = findViewById(R.id.syncProgressBar)
                        //syncProgressBar!!.visibility = View.VISIBLE
                        syncWaitIndicator = findViewById(R.id.syncWaitIndicator)
                        syncProgressMessage = findViewById(R.id.syncProgressMessage)
                        stopSyncButton = findViewById(R.id.stopSyncButton)
                    }

                    // Return the inflated and configured view
                    rootView
                },
                update = { view ->
                    // Called when the composable recomposes.
                    // You can update the view here if its state needs to change
                    // based on changes in Compose state.
                    // For example, if WifiSyncServiceSettings could change from elsewhere
                    // and you needed to update the checkboxes.
                }
            )

             */
        }
    }
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
    fun ShowSyncStatusComposable(innerPadding: PaddingValues) {
    var rawTargetSyncProgress by remember { mutableFloatStateOf(0f) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope() // Create a coroutine scope
    var currentSyncMessage by remember { mutableStateOf("") }
    var showEndOfSyncInfo by remember { mutableStateOf(false) }
    var buttonText by remember { mutableStateOf("STOP") }
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
                buttonText = "STOP"
            } else {
                rawTargetSyncProgress = 1f // Or whatever the final state should be
                currentSyncMessage = "Sync finished" // Or a message from WifiSyncService
                showEndOfSyncInfo = true
                buttonText = "Sync More"
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
                                buttonText = "Sync More"
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
                    .height(30.dp),
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
                ShowEndOfSyncInformation() // Call your composable here based on state
            }
        }
        }
    }

    suspend fun loadProgress(updateProgress: (Float) -> Unit, updateText: (String) -> Unit, onComplete: () -> Unit) {
        while (WifiSyncService.syncPercentCompleted.get() != -1) {
            val currentProgress = WifiSyncService.syncPercentCompleted.get() / 100f
            val currentMessage = WifiSyncService.syncProgressMessage.get()

            updateProgress(currentProgress)
            updateText(currentMessage)
            // timerHandler.postDelayed(this, 300)
            delay(300)
        }
        onComplete()
    }

    override fun onDestroy() {
        WifiSyncService.resultsActivityReady.reset()
        //timerHandler.removeCallbacks(timerRunnable!!)
        mainWindow = null
        super.onDestroy()
    }

    //@SuppressLint("MissingSuperCall")
    //@/Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    //override fun onBackPressed() {
    //    // disable back button
    //}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sync_status, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
    private fun ShowEndOfSyncInformation() {
        val errorMessageId = WifiSyncService.syncErrorMessageId.getAndSet(0)
        WifiSyncServiceSettings.saveSettings(this)
        // stopProgressTimer()
        if (errorMessageId == 0 || errorMessageId == R.string.syncCompletedFail) {
            val messageId = if (errorMessageId != 0) errorMessageId else R.string.syncCompleted
            /*
            Text(modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .padding(start = 15.dp, top = 200.dp),
                text = getString(R.string.syncCompleted),
                maxLines = 3,
                fontSize = 18.sp
            )
             */
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
                    showResults(syncFailedResults!!, WifiSyncService.syncToResults, failedFrom)
                }
            }
                /*
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
                 */
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
