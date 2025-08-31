package kim.tkland.musicbeewifisync

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.MenuCompat
import androidx.core.net.toUri
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity() : WifiSyncBaseActivity() {
    private var syncToPlaylists: CheckBox? = null
    private var syncToPlaylistsPath: EditText? = null
    private var serverStatusThread: Thread? = null
    private var syncToPlayCounts: CheckBox? = null
    private var syncToRatings: CheckBox? = null

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun MainActivityScreen(onNavigateToConversation: () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = onNavigateToConversation) {
                    Icon(Icons.Filled.Info, contentDescription = "View Error Log")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ComposeView(applicationContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        //setContentView(R.layout.activity_settings)
        ErrorHandler.initialise(this)
        // needed so android "Recent Views" actually shows the icon - only seems to be an issue with P
        @Suppress("DEPRECATION") setTaskDescription(
            TaskDescription(
                null,
                R.drawable.ic_launcher_round
            )
        )

        WifiSyncServiceSettings.loadSettings(applicationContext)
        //WifiSyncServiceSettings.loadSettings(this)
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
                /*
                val navController = rememberNavController()
                //CustomView(onNavigateToViewErrorLog = {navController.navigate("ViewErrorLogActivityScreen")})
                //AppCompat {
                NavHost(navController = navController, startDestination = "CustomView") {
                    composable(route = "CustomView") {
                        CustomView(navController)
                    }
                    composable(route = "ViewErrorLogActivityScreen") {
                        ViewErrorLogActivityScreen(
                            navController
                        )
                    }
                }
                 */
                //}
            }

            checkServerStatus()
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
        var isFullSyncChecked by remember { mutableStateOf(WifiSyncServiceSettings.syncCustomFiles) }
        var initialReverseSyncPlayer: Int = 2
        when (WifiSyncServiceSettings.reverseSyncPlayer) {
            WifiSyncServiceSettings.PLAYER_POWERAMP -> initialReverseSyncPlayer = 0
            WifiSyncServiceSettings.PLAYER_GONEMAD -> initialReverseSyncPlayer = 1
            0 -> initialReverseSyncPlayer = 2
        }
        // var reverseSyncPrayerSelected by remember { mutableIntStateOf(value = initialReverseSyncPlayer) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),

            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            getString(R.string.app_name),
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
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(getString(R.string.menuWifiFullSync))
                                            Checkbox(
                                                checked = isFullSyncChecked,
                                                onCheckedChange = { isChecked ->
                                                    isFullSyncChecked = isChecked
                                                }
                                            )
                                        }
                                    },
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
                                    text = { Text(getString(R.string.menuWifiPlaylistSync)) },
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
                            .height(80.dp),
                        onClick = {
                            try {
                                //syncStartButton!!.isEnabled = false
                                WifiSyncServiceSettings.syncCustomFiles = false
                                // 画面情報を保存する
                                WifiSyncServiceSettings.syncFromMusicBee = isSyncFromMusicBeeChecked
                                WifiSyncServiceSettings.reverseSyncPlayCounts =
                                    isSyncToPlaycountsChecked
                                WifiSyncServiceSettings.reverseSyncRatings = isSyncToRatingChecked
                                WifiSyncServiceSettings.reverseSyncPlaylists =
                                    isSyncToPlaylistsChecked
                                //WifiSyncServiceSettings.reverseSyncPlayer = reverseSyncPrayerSelected

                                //syncPreview = false
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
                        }) {
                        Modifier.weight(1f)

                        //Icon(
                        //imageVector = Icons.Filled.Sync,
                        //contentDescription = "Sync",
                        //)
                        //Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Preview", fontSize = 24.sp)
                    }
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(80.dp),
                        onClick = {
                            try {
                                WifiSyncServiceSettings.syncCustomFiles = false
                                //syncPreview = false
                                WifiSyncServiceSettings.syncFromMusicBee = isSyncFromMusicBeeChecked
                                WifiSyncServiceSettings.reverseSyncPlayCounts =
                                    isSyncToPlaycountsChecked
                                WifiSyncServiceSettings.reverseSyncRatings = isSyncToRatingChecked
                                WifiSyncServiceSettings.reverseSyncPlaylists =
                                    isSyncToPlaylistsChecked
                                WifiSyncService.startSynchronisation(
                                    applicationContext,
                                    0,
                                    false,
                                    false
                                )
                            } catch (ex: Exception) {
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
                        Text("Sync", fontSize = 24.sp)
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
                    CheckableRow(
                        text = applicationContext.getString(R.string.syncFromDefault),
                        checked = WifiSyncServiceSettings.syncFromMusicBee,
                        enabled = true,
                        onCheckedChange = {
                            isSyncFromMusicBeeChecked = it
                        }
                    )
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
                Row() {
                    CheckableRow(
                        text = applicationContext.getString(R.string.syncToPlaycounts),
                        checked = isSyncToPlaycountsChecked,
                        enabled = isSyncToPlaycountsEnabled,
                        onCheckedChange = {
                            isSyncToPlaycountsChecked = it
                        }
                    )
                }
//                Row() {
                /*
                    Row() {
                        MaterialTheme {
                            Row(
                                modifier = Modifier
                                    .toggleable(
                                        value = isSyncToPlaycountsChecked,
                                        enabled = isSyncToPlaycountsEnabled,
                                        role = Role.Checkbox,
                                        onValueChange = { isSyncToPlaycountsChecked = !isSyncToPlaycountsChecked }
                                    )
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start,
                            ) {
                                Checkbox(
                                    checked = isSyncToPlaycountsChecked,
                                    enabled = isSyncToPlaycountsEnabled,
                                    onCheckedChange = {
                                        isSyncToPlaycountsChecked = it
                                    }
                                )
                                Text(getString(R.string.syncToPlaycounts), fontSize = 20.sp)
                            }
                        }
                    }
              }
                     */
                Row() {
                    CheckableRow(
                        text = applicationContext.getString(R.string.syncToRatings),
                        checked = isSyncToRatingChecked,
                        enabled = isSyncToRatingEnabled,
                        onCheckedChange = {
                            isSyncToRatingChecked = it
                        }
                    )
                }
                /*
                    Row() {
                        MaterialTheme {
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
                                    onCheckedChange = {
                                        isSyncToRatingChecked = it
                                    }
                                )
                                Text(getString(R.string.syncToRatings), fontSize = 20.sp)
                            }
                        }
                    }
*/
                Row() {
                    CheckableRow(
                        text = applicationContext.getString(R.string.syncToPlaylists),
                        checked = isSyncToPlaylistsChecked,
                        enabled = isSyncToPlaylistsEnabled,
                        onCheckedChange = {
                            isSyncToPlaylistsChecked = it
                        }
                    )
                    /*
                    MaterialTheme {
                        Row(
                            modifier = Modifier
                                .toggleable(
                                    value = isSyncToPlaylistsChecked,
                                    enabled = isSyncToPlaylistsEnabled,
                                    role = Role.Checkbox,
                                    onValueChange = { isSyncToPlaylistsChecked = !isSyncToPlaylistsChecked }
                                )
                                .padding(8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Checkbox(
                                checked = isSyncToPlaylistsChecked,
                                enabled = isSyncToPlaylistsEnabled,
                                onCheckedChange = {
                                    isSyncToPlaylistsChecked = it
                                }
                            )
                            Text(getString(R.string.syncToPlaylists), fontSize = 20.sp)
                        }
                    }

                     */
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
                                                }

                                                1 -> {
                                                    WifiSyncServiceSettings.reverseSyncPlayer =
                                                        WifiSyncServiceSettings.PLAYER_GONEMAD
                                                    isSyncToPlaycountsEnabled = true
                                                    isSyncToRatingEnabled = true
                                                    isSyncToPlaylistsEnabled = true
                                                }

                                                2 -> {
                                                    WifiSyncServiceSettings.reverseSyncPlayer = 0
                                                    isSyncToPlaycountsEnabled = false
                                                    isSyncToRatingEnabled = false
                                                    isSyncToPlaylistsEnabled = false
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
                                    onClick = {
                                        onOptionSelected(option.index)
                                        when (option.index) {
                                            0 -> WifiSyncServiceSettings.reverseSyncPlayer =
                                                WifiSyncServiceSettings.PLAYER_POWERAMP

                                            1 -> WifiSyncServiceSettings.reverseSyncPlayer =
                                                WifiSyncServiceSettings.PLAYER_GONEMAD

                                            2 -> WifiSyncServiceSettings.reverseSyncPlayer = 0
                                        }
                                    }
                                )
                                Text(text = option.text, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun CheckableRow(text: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean)->Unit) {
        MaterialTheme {
            var checked by remember { mutableStateOf(checked) }
            Row(
                modifier = Modifier
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { checked = !checked }
                    )
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Checkbox(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange
                )
                Text(text, fontSize = 20.sp)
            }
        }
    }

    data class RadioOption(val text: String, val value: String, val index: Int)


    /*
                    rootView.apply


                        // Creates view
                        syncToPlaylists = findViewById(R.id.syncToPlaylists)
                        syncToPlaylistsPath = findViewById(R.id.syncToPlaylistPath)
                        syncPlayerGoneMad = findViewById(R.id.reverceFromGoneMAD)
                        syncPlayerPoweramp = findViewById(R.id.reverceFromPoweramp)
                        syncPlayerNone = findViewById(R.id.reverceFromNone)

                        val syncFromMusicBee = findViewById<CheckBox>(R.id.syncFromMusicBee)
                        syncFromMusicBee.isChecked = WifiSyncServiceSettings.syncFromMusicBee
                        syncFromMusicBee.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.syncFromMusicBee = isChecked
                            // Consider how you get 'mainWindow' here. If it's the activity,
                            // you might pass 'context as YourActivityInterface' or use a ViewModel.
                            WifiSyncServiceSettings.saveSettings(this@MainActivity) // Or pass context
                        }

                        syncToRatings = findViewById<CheckBox>(R.id.syncToRatings)
                        syncToPlayCounts = findViewById<CheckBox>(R.id.syncToPlayCounts)
                        val syncToUsingPlayer = findViewById<RadioGroup>(R.id.syncToUsingPlayer)
                        var playlistsSupported = false
                        // syncToUsingPlayer.check(R.id.reverceFromGoneMAD) // This seems to be set below

                        when (WifiSyncServiceSettings.reverseSyncPlayer) {
                            WifiSyncServiceSettings.PLAYER_GONEMAD -> {
                                playlistsSupported = true
                                syncToUsingPlayer.check(R.id.reverceFromGoneMAD)
                            }

                            WifiSyncServiceSettings.PLAYER_POWERAMP -> {
                                playlistsSupported = false
                                syncToUsingPlayer.check(R.id.reverceFromPoweramp)
                            }

                            0 -> syncToUsingPlayer.check(R.id.reverceFromNone)
                        }

                        syncToUsingPlayer.setOnCheckedChangeListener { group, checkedId ->
                            if (syncPlayerGoneMad!!.isChecked) { // Or if (checkedId == R.id.reverceFromGoneMAD)
                                WifiSyncServiceSettings.reverseSyncPlaylistsPath =
                                    "/gmmp/playlists"
                                WifiSyncServiceSettings.reverseSyncPlayer =
                                    WifiSyncServiceSettings.PLAYER_GONEMAD
                                // You'll need a way to call setPlaylistsEnabled from here.
                                // This might involve passing a lambda or having setPlaylistsEnabled
                                // operate on the views directly obtained via findViewById within this factory.
                                // For now, let's assume you'll update the views directly.
                                syncToPlaylists?.isEnabled = true
                                syncToPlaylists?.isChecked =
                                    WifiSyncServiceSettings.reverseSyncPlaylists
                                syncToPlaylistsPath?.isEnabled = true
                                syncToPlaylistsPath?.setText(WifiSyncServiceSettings.reverseSyncPlaylistsPath)
                            }
                            WifiSyncServiceSettings.saveSettings(this@MainActivity) // Or pass context
                        }

                        // Call the logic from your original setPlaylistsEnabled here,
                        // using the view references obtained above.
                        syncToPlaylists?.isEnabled = playlistsSupported
                        syncToPlaylists?.isChecked =
                            if (playlistsSupported) WifiSyncServiceSettings.reverseSyncPlaylists else false
                        syncToPlaylistsPath?.isEnabled = playlistsSupported
                        syncToPlaylistsPath?.setText(WifiSyncServiceSettings.reverseSyncPlaylistsPath)


                        syncToPlaylists?.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.reverseSyncPlaylists = isChecked
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                        }

                        syncToPlaylistsPath?.setOnEditorActionListener { v, actionId, event ->
                            WifiSyncServiceSettings.reverseSyncPlaylistsPath =
                                syncToPlaylistsPath?.text?.toString() ?: ""
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                            false
                        }

                        syncToRatings?.isChecked = WifiSyncServiceSettings.reverseSyncRatings
                        syncToRatings?.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.reverseSyncRatings = isChecked
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                        }

                        syncToPlayCounts?.isChecked =
                            WifiSyncServiceSettings.reverseSyncPlayCounts
                        syncToPlayCounts?.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.reverseSyncPlayCounts = isChecked
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                        }

                        syncPlayerGoneMad?.setOnClickListener { onGoneMADCheckClick(it) }
                        syncPlayerPoweramp?.setOnClickListener { onPowerampCheckClick(it) }
                        // ... and so on for other click listeners defined in XML
                    }

                    // Return the inflated and configured view
                    rootView
            }
        }
    }

 */

    private fun setPlaylistsEnabled(enabled: Boolean) {
        if (!enabled) {
            WifiSyncServiceSettings.reverseSyncPlaylists = false
        }
        syncToPlaylists!!.isEnabled = enabled
        syncToPlaylists!!.isChecked = WifiSyncServiceSettings.reverseSyncPlaylists
        syncToPlaylistsPath!!.isEnabled = enabled
        syncToPlaylistsPath!!.setText(WifiSyncServiceSettings.reverseSyncPlaylistsPath)
    }

    override fun onDestroy() {
        if (serverStatusThread != null) {
            serverStatusThread!!.interrupt()
            serverStatusThread = null
        }
        mainWindow = null
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        val fullSyncItem = menu.findItem(R.id.fullSyncMenuItem)
        fullSyncItem.isCheckable = true
        fullSyncItem.isChecked = true
        // val playlistSyncMenuItem = menu.findItem(R.id.playlistSyncMenuItem)
        // playlistSyncMenuItem.isCheckable = false
        // playlistSyncMenuItem.isChecked = false
        return true
    }


    private fun checkServerStatus() {
        serverStatusThread = Thread {
            try {
                var statusDisplayed = false
                while (true) {
                    if (WifiSyncService.ServerPinger.ping()) {
                        if (statusDisplayed) {
                            runOnUiThread {
                                if (serverStatusThread != null) {
                                    serverStatusThread = null
                                    findViewById<View>(R.id.syncServerStatus).visibility = View.GONE
                                }
                            }
                        }
                        break
                    }
                    if (!statusDisplayed) {
                        runOnUiThread {
                            findViewById<View>(R.id.syncServerStatus).visibility = View.VISIBLE
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
    }
}
