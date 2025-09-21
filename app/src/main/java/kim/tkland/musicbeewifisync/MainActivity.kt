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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity() : WifiSyncStartSyncBaseActivity() {
    var isSyncFromMusicBeeChecked = mutableStateOf(WifiSyncServiceSettings.syncFromMusicBee)
    var isSyncToPlaycountsChecked = mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayCounts)
    var isSyncToRatingChecked = mutableStateOf(WifiSyncServiceSettings.reverseSyncRatings)
    var isSyncToPlaylistsChecked = mutableStateOf(WifiSyncServiceSettings.reverseSyncPlaylists)
    var initialReverseSyncPlayer = mutableIntStateOf(2)
    var reverseSyncPlayer = mutableIntStateOf(WifiSyncServiceSettings.reverseSyncPlayer)
    private var serverStatusThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ComposeView(applicationContext).apply {
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

        WifiSyncServiceSettings.loadSettings(applicationContext)
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
        when (reverseSyncPlayer.intValue) {
            0 -> initialReverseSyncPlayer.intValue = 2
            WifiSyncServiceSettings.PLAYER_GONEMAD -> initialReverseSyncPlayer.intValue = 1
            WifiSyncServiceSettings.PLAYER_POWERAMP -> initialReverseSyncPlayer.intValue = 0
        }

        // You'll need to observe changes from the ViewModel and update the local state
        LaunchedEffect(showDialogFromViewModel) {
            showProgressDialogShow = showDialogFromViewModel
        }
        var isSyncToPlaycountsEnabled by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_POWERAMP || WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD) }
        var isSyncToRatingEnabled by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_POWERAMP || WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD) }
        var isSyncToPlaylistsEnabled by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD) }

        isFullSync.value = true
        isPlaylistSync.value = false
        appBarTitle.value = getString(R.string.app_name)
        val buttons = WifiSyncSyncButtons(::onPreviewButtonClick, ::onSyncNowButtonClick)

        super.CustomView()
        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
                    title = {
                        Box(
                        ) {
                            Text(
                                text = appBarTitle.value,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                        }
                        DropdownMenu(
                            modifier = Modifier.background(Color(getColor(white))),
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.fillMaxWidth(),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(getString(R.string.menuWifiFullSync))
                                        }
                                        if (isFullSync.value) {
                                            Checkbox(
                                                checked = true,
                                                colors = CheckboxDefaults.colors(
                                                    checkmarkColor = Color(getColor(android.R.color.white)),
                                                    uncheckedColor = Color(getColor(android.R.color.black)),
                                                    checkedColor = Color(getColor(R.color.colorAccent)),                                                    ),
                                                onCheckedChange = { isChecked ->
                                                }
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    val intent = Intent(
                                        applicationContext,
                                        PlaylistSyncActivity::class.java
                                    )
                                    intent.putExtra("playlistSync", true)
                                    expanded = false
                                    startActivity(intent)
                                },
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(getString(R.string.menuWifiPlaylistSync))
                                        }
                                        if (isPlaylistSync.value) {
                                            Checkbox(
                                                checked = true,
                                                colors = CheckboxDefaults.colors(
                                                    checkmarkColor = Color(getColor(android.R.color.white)),
                                                    uncheckedColor = Color(getColor(android.R.color.black)),
                                                    checkedColor = Color(getColor(R.color.colorAccent)),                                                ),
                                                onCheckedChange = { }
                                            )
                                        }
                                    }
                                }
                            )
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
                                text = { Text(getString(R.string.menuFullScanFiles)) },
                                onClick = {
                                    expanded = false
                                    showFullScanDialogShow = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(getString(R.string.menuAllPlaylistsDelete)) },
                                onClick = {
                                    expanded = false
                                    showDeleteAllPlaylistsDialog.value = true
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
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                buttons.BottomBarContent()
            }
        )
        { innnerPadding ->
            Column(
                modifier = Modifier
                    .background(Color(getColor(white)))
                    .padding(innnerPadding)
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
                        text = applicationContext.getString(R.string.syncFromPrompt),
                        fontSize = 16.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncFromMusicBeeChecked.value,
                            enabled = true,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncFromMusicBeeChecked.value = !isSyncFromMusicBeeChecked.value
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncFromMusicBeeChecked.value,
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncFromMusicBeeChecked.value = it
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
                        text = applicationContext.getString(R.string.syncToPrompt), fontSize = 16.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToPlaycountsChecked.value,
                            enabled = isSyncToPlaycountsEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToPlaycountsChecked.value = !isSyncToPlaycountsChecked.value
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToPlaycountsChecked.value,
                        enabled = isSyncToPlaycountsEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncToPlaycountsChecked.value = it
                        }
                    )
                    Text(getString(R.string.syncToPlaycounts), fontSize = 20.sp)
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToRatingChecked.value,
                            enabled = isSyncToRatingEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToRatingChecked.value = !isSyncToRatingChecked.value
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToRatingChecked.value,
                        enabled = isSyncToRatingEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncToRatingChecked.value = it
                        }
                    )
                    Text(getString(R.string.syncToRatings), fontSize = 20.sp)
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToPlaylistsChecked.value,
                            enabled = isSyncToPlaylistsEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToPlaylistsChecked.value = !isSyncToPlaylistsChecked.value
                            }
                        )
                        .padding(8.dp),
                    //.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToPlaylistsChecked.value,
                        enabled = isSyncToPlaylistsEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(white)),
                            uncheckedColor = Color(getColor(black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncToPlaylistsChecked.value = it
                        }
                    )
                    Text(getString(R.string.syncToPlaylists), fontSize = 20.sp)
                }
                Row() {
                    Text(
                        text = applicationContext.getString(R.string.syncToUsingPlayer),
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
                                                    reverseSyncPlayer.intValue =
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
                                                    reverseSyncPlayer.intValue =
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
                                                    reverseSyncPlayer.intValue = 0
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
                                                reverseSyncPlayer.intValue =
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
                                                reverseSyncPlayer.intValue =
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
                                                reverseSyncPlayer.intValue = 0
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
        WifiSyncServiceSettings.syncFromMusicBee =
            isSyncFromMusicBeeChecked.value
        WifiSyncServiceSettings.reverseSyncPlayCounts =
            isSyncToPlaycountsChecked.value
        WifiSyncServiceSettings.reverseSyncRatings =
            isSyncToRatingChecked.value
        WifiSyncServiceSettings.reverseSyncPlaylists =
            isSyncToPlaylistsChecked.value
        WifiSyncServiceSettings.reverseSyncPlayer =
            reverseSyncPlayer.intValue
        WifiSyncServiceSettings.saveSettings(applicationContext)
        if (isConfigOK()) {
            try {
                WifiSyncService.startSynchronisation(
                    applicationContext,
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
        WifiSyncServiceSettings.syncFromMusicBee =
            isSyncFromMusicBeeChecked.value
        WifiSyncServiceSettings.reverseSyncPlayCounts =
            isSyncToPlaycountsChecked.value
        WifiSyncServiceSettings.reverseSyncRatings =
            isSyncToRatingChecked.value
        WifiSyncServiceSettings.reverseSyncPlaylists =
            isSyncToPlaylistsChecked.value
        WifiSyncServiceSettings.reverseSyncPlayer =
            reverseSyncPlayer.intValue
        WifiSyncServiceSettings.saveSettings(applicationContext)
        if (isConfigOK()) {
            try {
                WifiSyncService.startSynchronisation(
                    applicationContext,
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
