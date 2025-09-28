package kim.tkland.musicbeewifisync

import androidx.compose.runtime.getValue // <-- Add this
import androidx.compose.runtime.setValue
import android.R.color.black
import android.R.color.white
import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity() : WifiSyncStartSyncBaseActivity() {
    var initialReverseSyncPlayer = mutableIntStateOf(2)
    private var serverStatusThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        ErrorHandler.initialise(this)
        // needed so android "Recent Views" actually shows the icon - only seems to be an issue with P
        @Suppress("DEPRECATION") setTaskDescription(
            TaskDescription(
                null,
                R.drawable.ic_launcher_round
            )
        )

        WifiSyncServiceSettings.loadSettings(this)
        if (WifiSyncServiceSettings.defaultIpAddressValue.isEmpty()) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else if (WifiSyncService.syncIsRunning.get()) {
            val intent = Intent(this, SyncResultsStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            setContent {
                CustomView()
            }
        }

        if (!MediaStore.canManageMedia(this)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                    .setData("package:${packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun CustomView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }
        var showFullScanDialogShow by remember { mutableStateOf(false) }
        val showDialogFromViewModel by viewModel.showDialog.collectAsStateWithLifecycle()
        var showProgressDialogShow by remember { mutableStateOf(showDialogFromViewModel) }

        var isSyncFromMusicBee by remember { mutableStateOf(WifiSyncServiceSettings.syncFromMusicBee) }
        var isSyncToPlaycounts by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayCounts) }
        var isSyncToRatings by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncRatings) }
        var isSyncToPlaylists by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlaylists) }

        when (WifiSyncServiceSettings.reverseSyncPlayer) {
            0 -> initialReverseSyncPlayer.intValue = 2
            WifiSyncServiceSettings.PLAYER_GONEMAD -> initialReverseSyncPlayer.intValue = 1
            WifiSyncServiceSettings.PLAYER_POWERAMP -> initialReverseSyncPlayer.intValue = 0
        }
        val context = LocalContext.current

        // You'll need to observe changes from the ViewModel and update the local state
        LaunchedEffect(showDialogFromViewModel) {
            showProgressDialogShow = showDialogFromViewModel
        }
        var isSyncToPlaycountsEnabled by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_POWERAMP || WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD) }
        var isSyncToRatingEnabled by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_POWERAMP || WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD) }
        var isSyncToPlaylistsEnabled by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD) }

        isFullSync.value = true
        appBarTitle.value = getString(R.string.app_name)
        val buttons = WifiSyncSyncButtons(::onPreviewButtonClick, ::onSyncNowButtonClick)

        super.CustomView()
        Scaffold(
            topBar = {
                SyncScreenTopBar(appBarTitle.value,
                    expanded,
                    { newValue -> expanded = newValue},
                    showDeleteAllPlaylistsDialog.value,
                    showFullScanDialogShow,
                    {newValue -> showFullScanDialogShow = newValue},
                    {newValue -> showDeleteAllPlaylistsDialog.value = newValue},
                    isFullSync.value)
            },
            bottomBar = {
                buttons.BottomBarContent()
            },
        )
        { innerPadding ->
            Column(
                modifier = Modifier
                    .background(Color(getColor(white)))
                    .padding(innerPadding)
                    //.padding(statusBarPadding),
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Row(/*modifier = Modifier.padding(top = 15.dp)*/) {
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = "Header"
                    )
                    Text(
                        text = context.getString(R.string.syncFromPrompt),
                        fontSize = 16.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncFromMusicBee,
                            enabled = true,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncFromMusicBee = it
                                WifiSyncServiceSettings.syncFromMusicBee = it
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncFromMusicBee,
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncFromMusicBee = it
                            WifiSyncServiceSettings.syncFromMusicBee = it
                        }
                    )
                    Text(getString(R.string.syncFromDefault), fontSize = 20.sp)
                }
                Row(modifier = Modifier.padding(top = 15.dp)) {
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "ReverseSync"
                    )
                    Text(
                        text = context.getString(R.string.syncToPrompt), fontSize = 16.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToPlaycounts,
                            enabled = isSyncToPlaycountsEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToPlaycounts = it
                                WifiSyncServiceSettings.reverseSyncPlayCounts = it
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToPlaycounts,
                        enabled = isSyncToPlaycountsEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncToPlaycounts = it
                            WifiSyncServiceSettings.reverseSyncPlayCounts = it
                        }
                    )
                    Text(getString(R.string.syncToPlaycounts), fontSize = 20.sp)
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToRatings,
                            enabled = isSyncToRatingEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToRatings = it
                                WifiSyncServiceSettings.reverseSyncRatings = it
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToRatings,
                        enabled = isSyncToRatingEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncToRatings = it
                            WifiSyncServiceSettings.reverseSyncRatings = it
                        }
                    )
                    Text(getString(R.string.syncToRatings), fontSize = 20.sp)
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToPlaylists,
                            enabled = isSyncToPlaylistsEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToPlaylists = it
                                WifiSyncServiceSettings.reverseSyncPlaylists = it
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToPlaylists,
                        enabled = isSyncToPlaylistsEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncToPlaylists = it
                            WifiSyncServiceSettings.reverseSyncPlaylists = it
                        }
                    )
                    Text(getString(R.string.syncToPlaylists), fontSize = 20.sp)
                }
                Row() {
                    Text(
                        text = context.getString(R.string.syncToUsingPlayer),
                        fontSize = 16.sp
                    )
                }
                Row(modifier = Modifier.padding(top = 15.dp)) {
                    val options = listOf(
                        RadioOption(
                            "Poweramp",
                            "reverceFromPoweramp",
                            index = 0
                        ),
                        RadioOption(
                            "GoneMAD",
                            "reverceFromGoneMAD",
                            index = 1
                        ),
                        RadioOption("None", "reverceFromNone", index = 2)
                    )

                    val (selectedOption, onOptionSelected) = remember {
                        mutableIntStateOf(
                            initialReverseSyncPlayer.intValue
                        )
                    }

                    Column {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    //.padding(8.dp)
                                    .selectable(
                                        selected = (option.index == initialReverseSyncPlayer.intValue),
                                        onClick = {
                                            onOptionSelected(option.index)
                                            when (option.index) {
                                                0 -> {
                                                    WifiSyncServiceSettings.reverseSyncPlayer =
                                                        WifiSyncServiceSettings.PLAYER_POWERAMP
                                                    initialReverseSyncPlayer.intValue = 0
                                                    isSyncToPlaycountsEnabled = true
                                                    isSyncToRatingEnabled = true
                                                    isSyncToPlaylistsEnabled = false
                                                    WifiSyncServiceSettings.reverseSyncPlayCounts =
                                                        true
                                                    WifiSyncServiceSettings.reverseSyncRatings =
                                                        true
                                                    WifiSyncServiceSettings.reverseSyncPlaylists =
                                                        false
                                                }

                                                1 -> {
                                                    WifiSyncServiceSettings.reverseSyncPlayer =
                                                        WifiSyncServiceSettings.PLAYER_GONEMAD
                                                    initialReverseSyncPlayer.intValue = 1
                                                    isSyncToPlaycountsEnabled = true
                                                    isSyncToRatingEnabled = true
                                                    isSyncToPlaylistsEnabled = true
                                                    WifiSyncServiceSettings.reverseSyncRatings =
                                                        true
                                                    WifiSyncServiceSettings.reverseSyncPlayCounts =
                                                        true
                                                    WifiSyncServiceSettings.reverseSyncPlaylists =
                                                        true
                                                }

                                                2 -> {
                                                    WifiSyncServiceSettings.reverseSyncPlayer = 0
                                                    initialReverseSyncPlayer.intValue = 2
                                                    isSyncToPlaycountsEnabled = false
                                                    isSyncToRatingEnabled = false
                                                    isSyncToPlaylistsEnabled = false
                                                    WifiSyncServiceSettings.reverseSyncPlayCounts =
                                                        false
                                                    WifiSyncServiceSettings.reverseSyncRatings =
                                                        false
                                                    WifiSyncServiceSettings.reverseSyncPlaylists =
                                                        false
                                                }
                                            }
                                        }
                                    ),
                                //.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,

                                ) {
                                RadioButton(
                                    selected = (option.index == initialReverseSyncPlayer.intValue),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(getColor(R.color.colorAccent)), // 選択時の色
                                        unselectedColor = Color(getColor(R.color.colorButtonBackground)) // 非選択時の色
                                    ),
                                    onClick = {
                                        onOptionSelected(option.index)
                                        when (option.index) {
                                            0 -> {
                                                WifiSyncServiceSettings.reverseSyncPlayer =
                                                    WifiSyncServiceSettings.PLAYER_POWERAMP
                                                initialReverseSyncPlayer.intValue = 0
                                                isSyncToPlaycountsEnabled = true
                                                isSyncToRatingEnabled = true
                                                isSyncToPlaylistsEnabled = false
                                                WifiSyncServiceSettings.reverseSyncPlayCounts = true
                                                WifiSyncServiceSettings.reverseSyncRatings = true
                                                WifiSyncServiceSettings.reverseSyncPlaylists = false
                                            }

                                            1 -> {
                                                WifiSyncServiceSettings.reverseSyncPlayer =
                                                    WifiSyncServiceSettings.PLAYER_GONEMAD
                                                initialReverseSyncPlayer.intValue = 1
                                                isSyncToPlaycountsEnabled = true
                                                isSyncToRatingEnabled = true
                                                isSyncToPlaylistsEnabled = true
                                                WifiSyncServiceSettings.reverseSyncRatings = true
                                                WifiSyncServiceSettings.reverseSyncPlayCounts = true
                                                WifiSyncServiceSettings.reverseSyncPlaylists = true
                                            }

                                            2 -> {
                                                WifiSyncServiceSettings.reverseSyncPlayer = 0
                                                initialReverseSyncPlayer.intValue = 2
                                                isSyncToPlaycountsEnabled = false
                                                isSyncToRatingEnabled = false
                                                isSyncToPlaylistsEnabled = false
                                                WifiSyncServiceSettings.reverseSyncPlayCounts =
                                                    false
                                                WifiSyncServiceSettings.reverseSyncRatings = false
                                                WifiSyncServiceSettings.reverseSyncPlaylists = false
                                            }
                                        }
                                    }
                                )
                                Text(text = option.text, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.padding(top = 15.dp)) {
                    val status = checkServerStatus()
                    Text(
                        text = status,
                        color = Color(getColor(R.color.colorError)),
                        fontSize = 20.sp
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (serverStatusThread != null) {
            serverStatusThread!!.interrupt()
            serverStatusThread = null
        }

        mainWindow = null
        super.onDestroy()
    }

    private fun isConfigOK(): Boolean {
        if (serverStatusThread != null) {
            configErrorMessage.value = getString(R.string.errorServerNotFound)
            showDialog.value = true
            return false
        }

        val anyReverseSync =
            (WifiSyncServiceSettings.reverseSyncPlayer == 1 || WifiSyncServiceSettings.reverseSyncPlayer == 2) &&
                    (WifiSyncServiceSettings.reverseSyncPlaylists || WifiSyncServiceSettings.reverseSyncRatings || WifiSyncServiceSettings.reverseSyncPlayCounts)
        if (!anyReverseSync) {
            if (!WifiSyncServiceSettings.syncFromMusicBee) {
                configErrorMessage.value = getString(R.string.errorSyncParamsNoneSelected)
                showDialog.value = true
                return false
            }
        }

        return true
    }

    private fun checkServerStatus(): String {
        var returnValue = ""
        serverStatusThread = Thread {
            try {
                var statusDisplayed = false
                while (true) {
                    if (WifiSyncService.ServerPinger.ping()) {
                        if (statusDisplayed) {
                            runOnUiThread {
                                if (serverStatusThread != null) {
                                    serverStatusThread = null
                                    //findViewById<View>(R.id.syncServerStatus).visibility = View.GONE
                                }
                            }
                        }
                        break
                    }
                    if (!statusDisplayed) {
                        runOnUiThread {
                            returnValue = getString(R.string.errorServerNotFound)
                        }
                        statusDisplayed = true
                    }
                    Thread.sleep(2500)
                }
            } catch (_: Exception) {
            }
            serverStatusThread = null
        }
        serverStatusThread!!.start()
        return returnValue
    }

    override fun onPreviewButtonClick() {
        WifiSyncServiceSettings.syncCustomFiles = false
        // 画面情報を保存する
        WifiSyncServiceSettings.saveSettings(this)
        if (isConfigOK()) {
            try {
                WifiSyncService.startSynchronisation(
                    this,
                    0,
                    true,
                    false
                )
            } catch (ex: Exception) {
                Log.d("onPreviewButtonClick", ex.message!!)
            } finally {
                // syncStartButton!!.isEnabled = true
            }
        }
    }

    override fun onSyncNowButtonClick() {
        WifiSyncServiceSettings.syncCustomFiles = false
        WifiSyncServiceSettings.saveSettings(this)
        if (isConfigOK()) {
            try {
                WifiSyncService.startSynchronisation(
                    this,
                    0,
                    false,
                    false
                )
            } catch (ex: Exception) {
                Log.d("onSyncStartButtonClick", ex.message!!)
            } finally {
            }
        }
    }
}
