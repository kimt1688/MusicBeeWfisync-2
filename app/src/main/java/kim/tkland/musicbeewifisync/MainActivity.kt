package kim.tkland.musicbeewifisync

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.RadioButtonDefaults

class MainActivity() : WifiSyncBaseActivity("") {
    private var serverStatusThread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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
    fun CustomView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        var isSyncFromMusicBeeChecked by remember { mutableStateOf(WifiSyncServiceSettings.syncFromMusicBee) }
        var isSyncToPlaycountsChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayCounts) }
        var isSyncToRatingChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncRatings) }
        var isSyncToPlaylistsChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlaylists) }
        var isSyncToPlaycountsEnabled by remember { mutableStateOf(false) }
        var isSyncToRatingEnabled by remember { mutableStateOf(false) }
        var isSyncToPlaylistsEnabled by remember { mutableStateOf(false) }
        var initialReverseSyncPlayer: Int = 2
        when (WifiSyncServiceSettings.reverseSyncPlayer) {
            WifiSyncServiceSettings.PLAYER_POWERAMP -> initialReverseSyncPlayer = 0
            WifiSyncServiceSettings.PLAYER_GONEMAD -> initialReverseSyncPlayer = 1
            0 -> initialReverseSyncPlayer = 2
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),

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
                                text = getString(R.string.app_name),
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
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.fillMaxWidth(),
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(getString(R.string.menuWifiFullSync))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Checkbox(
                                                checked = true,
                                                colors = CheckboxDefaults.colors(
                                                    checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                                    checkedColor = Color(getColor(R.color.colorButtonBackground)),
                                                ),
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
                                text = { Text(getString(R.string.menuWifiPlaylistSync)) },
                                onClick = {
                                    val intent = Intent(
                                        applicationContext,
                                        PlaylistSyncActivity::class.java
                                    )
                                    intent.putExtra("playlistSync", true)
                                    expanded = false
                                    startActivity(intent)
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
                                    onFullScanMenuItemClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(getString(R.string.menuAllPlaylistsDelete)) },
                                onClick = {
                                    expanded = false
                                    onDeleteAllPlaylistsClick()
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
                Row( // Or a Compose Row
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
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
                            WifiSyncServiceSettings.syncCustomFiles = false
                            // 画面情報を保存する
                            WifiSyncServiceSettings.syncFromMusicBee =
                                isSyncFromMusicBeeChecked
                            WifiSyncServiceSettings.reverseSyncPlayCounts =
                                isSyncToPlaycountsChecked
                            WifiSyncServiceSettings.reverseSyncRatings =
                                isSyncToRatingChecked
                            WifiSyncServiceSettings.reverseSyncPlaylists =
                                isSyncToPlaylistsChecked
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
                        }) {
                        Modifier.weight(1f)

                        Text(getString(R.string.syncPreview), fontSize = 24.sp)
                    }
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
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
                            WifiSyncServiceSettings.syncCustomFiles = false
                            WifiSyncServiceSettings.syncFromMusicBee =
                                isSyncFromMusicBeeChecked
                            WifiSyncServiceSettings.reverseSyncPlayCounts =
                                isSyncToPlaycountsChecked
                            WifiSyncServiceSettings.reverseSyncRatings =
                                isSyncToRatingChecked
                            WifiSyncServiceSettings.reverseSyncPlaylists =
                                isSyncToPlaylistsChecked
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
                        }) {
                        Modifier.weight(1f)

                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync",
                        )
                        Text(getString(R.string.syncNow), fontSize = 24.sp)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Row(modifier = Modifier.padding(top = 15.dp)) {
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_forward),
                        contentDescription = "Header"
                    )
                    Text(
                        text = applicationContext.getString(R.string.syncFromPrompt),
                        fontSize = 16.sp
                    )
                }
                Row() {
                    Row(
                        modifier = Modifier
                            .toggleable(
                                value = isSyncFromMusicBeeChecked,
                                enabled = true,
                                role = Role.Checkbox,
                                onValueChange = {
                                    isSyncFromMusicBeeChecked = !isSyncFromMusicBeeChecked
                                }
                            )
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Checkbox(
                            checked = isSyncFromMusicBeeChecked,
                            enabled = true,
                            colors = CheckboxDefaults.colors(
                                checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                checkedColor = Color(getColor(R.color.colorButtonBackground)),
                            ),
                            onCheckedChange = {
                                isSyncFromMusicBeeChecked = it
                            }
                        )
                        Text(getString(R.string.syncFromDefault), fontSize = 20.sp)
                    }
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
                            value = isSyncToPlaycountsChecked,
                            enabled = isSyncToPlaycountsEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToPlaycountsChecked = !isSyncToPlaycountsChecked
                            }
                        )
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToPlaycountsChecked,
                        enabled = isSyncToPlaycountsEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            checkedColor = Color(getColor(R.color.colorButtonBackground)),
                        ),
                        onCheckedChange = {
                            isSyncToPlaycountsChecked = it
                        }
                    )
                    Text(getString(R.string.syncToPlaycounts), fontSize = 20.sp)
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToRatingChecked,
                            enabled = isSyncToRatingEnabled,
                            role = Role.Checkbox,
                            onValueChange = { isSyncToRatingChecked = !isSyncToRatingChecked }
                        )
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToRatingChecked,
                        enabled = isSyncToRatingEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            checkedColor = Color(getColor(R.color.colorButtonBackground)),
                        ),
                        onCheckedChange = {
                            isSyncToRatingChecked = it
                        }
                    )
                    Text(getString(R.string.syncToRatings), fontSize = 20.sp)
                }
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncToPlaylistsChecked,
                            enabled = isSyncToPlaylistsEnabled,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncToPlaylistsChecked = !isSyncToPlaylistsChecked
                            }
                        )
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncToPlaylistsChecked,
                        enabled = isSyncToPlaylistsEnabled,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            checkedColor = Color(getColor(R.color.colorButtonBackground)),
                        ),
                        onCheckedChange = {
                            isSyncToPlaylistsChecked = it
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
                            initialReverseSyncPlayer
                        )
                    }

                    Column {
                        options.forEach { option ->
                            Row(
                                modifier = Modifier
                                    //.padding(8.dp)
                                    .selectable(
                                        selected = (option.index == selectedOption),
                                        onClick = {
                                            onOptionSelected(option.index)
                                            when (option.index) {
                                                0 -> {
                                                    WifiSyncServiceSettings.reverseSyncPlayer =
                                                        WifiSyncServiceSettings.PLAYER_POWERAMP
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
                                    )
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,

                                ) {
                                RadioButton(
                                    selected = (option.index == selectedOption),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(getColor(R.color.colorButtonTextEnabled)), // 選択時の色
                                        unselectedColor = Color(getColor(R.color.colorButtonBackground)) // 非選択時の色
                                    ),
                                    onClick = {
                                        onOptionSelected(option.index)
                                        when (option.index) {
                                            0 -> {
                                                WifiSyncServiceSettings.reverseSyncPlayer =
                                                    WifiSyncServiceSettings.PLAYER_POWERAMP
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
                                                isSyncToPlaycountsEnabled = true
                                                isSyncToRatingEnabled = true
                                                isSyncToPlaylistsEnabled = true
                                                WifiSyncServiceSettings.reverseSyncRatings = true
                                                WifiSyncServiceSettings.reverseSyncPlayCounts = true
                                                WifiSyncServiceSettings.reverseSyncPlaylists = true
                                            }

                                            2 -> {
                                                WifiSyncServiceSettings.reverseSyncPlayer = 0
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
                    var status = checkServerStatus()
                    Text(text = status, color = Color.Red, fontSize = 20.sp)
                }
            }
        }
    }


    data class RadioOption(val text: String, val value: String, val index: Int)

    override fun onDestroy() {
        if (serverStatusThread != null) {
            serverStatusThread!!.interrupt()
            serverStatusThread = null
        }
        mainWindow = null
        super.onDestroy()
    }

    private fun isConfigOK(): Boolean {
        var message: String? = null
        if (serverStatusThread != null)
        {
            message = getString(R.string.errorServerNotFound)
        }

        val anyReverseSync =
            (WifiSyncServiceSettings.reverseSyncPlayer == 1 || WifiSyncServiceSettings.reverseSyncPlayer == 2) &&
                    (WifiSyncServiceSettings.reverseSyncPlaylists || WifiSyncServiceSettings.reverseSyncRatings || WifiSyncServiceSettings.reverseSyncPlayCounts)
        if (!anyReverseSync)
        {
            if (!WifiSyncServiceSettings.syncFromMusicBee) {
                message = getString(R.string.errorSyncParamsNoneSelected)
            }
        }
        return if (message == null)
        {
            true
        } else
        {
            val builder = AlertDialog.Builder(mainWindow!!)
            builder.setTitle(getString(R.string.syncErrorHeader))
            builder.setMessage(message)
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setCancelable(false)
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
            false
        }
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
}
