package kim.tkland.musicbeewifisync

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings.ACTION_REQUEST_MANAGE_MEDIA
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuCompat
import java.io.File
import androidx.core.content.edit
import androidx.core.net.toUri
import kim.tkland.musicbeewifisync.MainActivity.RadioOption

class SettingsActivity : WifiSyncBaseActivity() {
    private var initialSetup = false
    private var locateServerButton: Button? = null
    private var settingsLocateServerNoConfig: TextView? = null
    private var settingsStorageOptions: RadioGroup? = null
    private var settingsStorageSdCard1: RadioButton? = null
    private var settingsDebugMode: CheckBox? = null
    private var settingsDeviceNamePrompt: TextView? = null
    private var settingsDeviceName: EditText? = null
    private val PERMISSION_READ_EXTERNAL_STORAGE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initialSetup = WifiSyncServiceSettings.defaultIpAddressValue.isEmpty()
        //setContentView(R.layout.activity_settings)
        /*
        locateServerButton = findViewById(R.id.locateServerButton)
        settingsLocateServerNoConfig = findViewById(R.id.settingsLocateServerNoConfig)
        val settingsStoragePrompt = findViewById<TextView>(R.id.settingsStoragePrompt)
        settingsStorageOptions = findViewById(R.id.settingsStorageOptions)
        settingsStorageOptions?.let { it.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, _ -> showGrantAccessButton() }) }
        val settingsStorageInternal = findViewById<RadioButton>(R.id.settingsStorageInternal)
        settingsStorageSdCard1 = findViewById(R.id.settingsStorageSdCard1)
        settingsDebugMode = findViewById(R.id.settingsDebugMode)
         */
        //settingsDebugMode?.setChecked(WifiSyncServiceSettings.debugMode)
        //settingsDebugMode?.let {
        //    it.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, _ ->
        //        WifiSyncServiceSettings.debugMode = settingsDebugMode?.isChecked!!
        //    })
        //}
        //settingsDeviceNamePrompt = findViewById(R.id.settingsDeviceNamePrompt)
        //settingsDeviceName = findViewById(R.id.settingsDeviceName)
        // val externalSdCardCount = 2
        // val sdCard1: RadioButton? = settingsStorageSdCard1
        //if (externalSdCardCount == 0) {
        //settingsStorageInternal.isChecked = true
        //sdCard1?.setChecked(false)
        //} else {
        //if (externalSdCardCount == 1) {
        //if (WifiSyncServiceSettings.deviceStorageIndex == 2) {
        //sdCard1?.setChecked(true)
        //} else {
        //settingsStorageInternal.isChecked = true
        //}
        //} else {
        //settingsStorageInternal.isChecked = false
        //WifiSyncServiceSettings.deviceStorageIndex = 1
        //sdCard1?.setText(R.string.settingsStorageSdCard1)
        //}
        //}
        //val debugMode: CheckBox? = settingsDebugMode
        //val serverButton: Button? = locateServerButton
        WifiSyncServiceSettings.deviceStorageIndex == 2
        if (initialSetup) {
            WifiSyncServiceSettings.debugMode = true
            setContent {
                FirstSettingView()
            }
            //debugMode?.let { it.visibility = View.GONE }

            // ここでパミッションチェックか？2024/7/20 8:20
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    requestPermissionForReadWrite()
                }
            } else {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    requestPermissionForReadWrite()
                }
            }
            val intent = Intent(ACTION_REQUEST_MANAGE_MEDIA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData("package:kim.tkland.musicbeewifisync".toUri())
            startActivity(intent)
            val sharedPref =
                getSharedPreferences("kim.tkland.musicbeewifisync.sharedpref", MODE_PRIVATE)
            val uriStr = sharedPref.getString("accesseduri", "")
            val stats = File("/storage/emulated/0/gmmp/stats.xml")
            if (stats.exists()) {
                if (uriStr.isNullOrEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.statsSelect)
                        .setMessage(R.string.statsSelectMessage)
                        .setPositiveButton("OK") { _, _ ->
                            // OKボタン押下時に実行したい処理を記述
                            // ここでファイルの追加処理か？ 2024/7/30 5:30
                            // OnStart()で複数回Toastが出たのでここに戻す
                            // 2024/8/1 Android14で報告があったのでOS分岐をなくす
                            // 2024/8/3 ストレージパーミッションが必要かもしれないからパーミッション追加の後にしてみる
                            // 2024/8/3 8:50 onCreate()でファイルがリストアップされないのでここに戻す
                            // 2024/8/11 8:13 onStart()でファイルがリストアップされないのでここにしてみる
                            // 2024/8/11 11:05 launcher.launchの終了待ちをして実行、決定版か？
                            // 2024/8/13 8:08 処理自体をブロッキングにしたのでとりあえずOK
                            listNewFiles()
                            launcher.launch(setLaunchIntent())
                        }
                        .create()
                        .show()
                }
            }
        } else {
            setContent {
                OptionSettingView()
            }
            //val actionBar = supportActionBar
            //if (actionBar != null) {
            //    actionBar.setDisplayHomeAsUpEnabled(true)
            //    actionBar.setTitle(R.string.title_activity_settings2)
            //}
            /*
            findViewById<View>(R.id.settingsInfo0).visibility = View.GONE
            findViewById<View>(R.id.settingsInfo1).visibility = View.GONE
            findViewById<View>(R.id.settingsInfo2).visibility = View.GONE
            serverButton?.let { it.visibility = View.GONE }
            debugMode?.let { it.visibility = View.VISIBLE }
            settingsStoragePrompt.setText(R.string.settingsStorageSettingsPrompt)
            if (!Build.MODEL.equals(WifiSyncServiceSettings.deviceName, ignoreCase = true)) {
                showNoConfigMatchedSettings()
            }
        }
        setActivityRoot(this)
             */
            //setSupportActionBar(findViewById(R.id.my_toolbar))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FirstSettingView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        var isSyncFromMusicBeeChecked by remember { mutableStateOf(WifiSyncServiceSettings.syncFromMusicBee) }
        var isSyncToPlaycountsChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayCounts) }
        var isSyncToRatingChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncRatings) }
        var isSyncToPlaylistsChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlaylists) }

        var initialStorage by remember { mutableIntStateOf(value = 1) }

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
                            getString(R.string.title_activity_settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Row( // Or a Compose Row
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            WifiSyncServiceSettings.defaultIpAddressValue =
                                WifiSyncService.getMusicBeeServerAddress(applicationContext, null)
                                    .toString()
                            WifiSyncServiceSettings.deviceStorageIndex = initialStorage
                            WifiSyncServiceSettings.saveSettings(applicationContext)
                            // WifiSyncServiceSettings.saveSettings(mainWindow)
                            val intent = Intent(mainWindow, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)
                        }) {

                        Text(getString(R.string.settingsLocate), fontSize = 24.sp)
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
                Row() {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp, end = 15.dp, top = 10.dp),
                        text = applicationContext.getString(R.string.settingsLocateServerInfo0),
                        color = colorResource(R.color.colorError),
                        fontSize = 20.sp
                    )

                }
                Row() {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp, end = 15.dp, top = 10.dp),
                        text = applicationContext.getString(R.string.settingsLocateServerInfo1),
                        fontSize = 16.sp
                    )

                }
                Row() {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 15.dp, end = 15.dp, top = 12.dp),
                        text = applicationContext.getString(R.string.settingsLocateServerInfo2),
                        fontSize = 16.sp
                    )

                }
                Row() {
                    StorageRadioGroup(initialStorage, 20)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OptionSettingView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        var isSyncFromMusicBeeChecked by remember { mutableStateOf(WifiSyncServiceSettings.syncFromMusicBee) }
        var isSyncToPlaycountsChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlayCounts) }
        var isSyncToRatingChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncRatings) }
        var isSyncToPlaylistsChecked by remember { mutableStateOf(WifiSyncServiceSettings.reverseSyncPlaylists) }

        var initialStorage by remember { mutableIntStateOf(value = WifiSyncServiceSettings.deviceStorageIndex) }

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
                            getString(R.string.title_activity_settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    StorageRadioGroup(initialStorage, 20)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, top = 100.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    CheckableRow(
                        text = applicationContext.getString(R.string.settingsDebugMode),
                        checked = WifiSyncServiceSettings.debugMode,
                        onCheckedChange = {
                            WifiSyncServiceSettings.debugMode = it
                            WifiSyncServiceSettings.saveSettings(applicationContext)
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun CheckableRow(text: String, checked: Boolean, onCheckedChange: (Boolean)->Unit) {
        MaterialTheme {
            var checked by remember { mutableStateOf(checked) }
            Row(
                modifier = Modifier
                    .toggleable(
                        value = checked,
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
                    onCheckedChange = onCheckedChange
                )
                Text(text, fontSize = 20.sp)
            }
        }
    }

    data class RadioOption(val text: String, val value: String, val index: Int)

    @Composable
    fun StorageRadioGroup(initialStorage: Int, fontSize: Int) {
        val options = listOf(
            RadioOption(
                getString(R.string.settingsStorageInternal),
                "Internal",
                index = 1
            ),
            RadioOption(
                getString(R.string.settingsStorageExternal),
                "SD Card",
                index = 2
            ),
        )

        val (selectedOption, onOptionSelected) = remember { mutableIntStateOf(initialStorage) }

        Column {
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        //.padding(8.dp)
                        .selectable(
                            selected = (option.index == selectedOption),
                            onClick = {
                                onOptionSelected(option.index)
                                WifiSyncServiceSettings.deviceStorageIndex = option.index
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
                            WifiSyncServiceSettings.deviceStorageIndex = option.index
                        }
                    )
                    Text(text = option.text, fontSize = fontSize.sp)
                }
            }
        }
    }
    // アクティビティの結果に対するコールバックの登録
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // アクティビティ結果NG
            return@registerForActivityResult
        } else {
            // アクティビティ結果OK
            try {
                val mUri = result.data?.data
                clearAllPersistedUriPermissions(applicationContext)
                contentResolver.takePersistableUriPermission(
                    mUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val preferences = applicationContext.getSharedPreferences(
                    "kim.tkland.musicbeewifisync.sharedpref",
                    MODE_PRIVATE
                )
                preferences.edit(commit = true) { putString("accesseduri", mUri.toString()) }
            } catch (e: Exception) {
                Log.d("launcher", e.message!!)
            }
        }
    }

    private fun clearAllPersistedUriPermissions(context: Context) {
        try {
            val contentResolver = context.contentResolver
            for (uriPermission in contentResolver.persistedUriPermissions) {
                applicationContext.contentResolver.releasePersistableUriPermission(
                    /* uri = */       uriPermission.uri,
                    /* modeFlags = */ Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        } catch (e: Throwable) {
            // just to be safe...
            e.printStackTrace()
        }
    }

    private fun requestPermissionForReadWrite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.MANAGE_MEDIA,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                ), PERMISSION_READ_EXTERNAL_STORAGE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                ), PERMISSION_READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun setLaunchIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/xml"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/storage/emulated/0/gmmp")
        }
        return intent
    }

    override fun onDestroy() {
        /*
        if (initialSetup) {
            WifiSyncServiceSettings.debugMode = false
        } else {
            WifiSyncServiceSettings.deviceStorageIndex =
                if (settingsStorageSdCard1!!.isChecked) 2 else 1
            WifiSyncServiceSettings.saveSettings(applicationContext)
            // WifiSyncServiceSettings.saveSettings(this)
        }
         */
        super.onDestroy()
    }

    //override fun onSupportNavigateUp(): Boolean {
    //    finish()
    //    return true
    //}

    @Suppress("REDUNDANT_MODIFIER_IN_GETTER")
    private val checkedStorageTypeButton: Int
        private get() {
            when (settingsStorageOptions!!.checkedRadioButtonId) {
                R.id.settingsStorageSdCard1 -> return 2
            }
            return 1
        }

    private fun showGrantAccessButton() {
    }

    fun onLocateServerButton_Click() {
        //settingsLocateServerNoConfig!!.visibility = View.GONE
        //WifiSyncServiceSettings.deviceStorageIndex =
        //    if (settingsStorageSdCard1!!.isChecked) 2 else 1
        WifiSyncServiceSettings.saveSettings(applicationContext)
        // WifiSyncServiceSettings.saveSettings(mainWindow)
        // settingsWaitIndicator!!.visibility = View.VISIBLE
        val locateServerThread = Thread(Runnable {
            val serverIPAddress = WifiSyncService.getMusicBeeServerAddress(mainWindow, null)
            runOnUiThread {
                if (mainWindow != null) {
                    // settingsWaitIndicator!!.visibility = View.INVISIBLE
                    //locateServerButton!!.isEnabled = true
                    //locateServerButton!!.setTextColor(buttonTextEnabledColor)
                    if (serverIPAddress == null) {
                        val errorDialog = AlertDialog.Builder(mainWindow!!)
                        errorDialog.setMessage(getText(R.string.errorServerNotFound))
                        errorDialog.setPositiveButton(android.R.string.ok, null)
                        errorDialog.show()
                    } else if (serverIPAddress == getString(R.string.syncStatusFAIL)) {
                        //settingsLocateServerNoConfig!!.visibility = View.VISIBLE
                        showNoConfigMatchedSettings()
                        //settingsDeviceNamePrompt!!.visibility = View.GONE
                    } else {
                        WifiSyncServiceSettings.defaultIpAddressValue = serverIPAddress
                        WifiSyncServiceSettings.saveSettings(applicationContext)
                        // WifiSyncServiceSettings.saveSettings(mainWindow)
                        val intent = Intent(mainWindow, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }
                }
            }
        })
        locateServerThread.start()
    }

    private fun showNoConfigMatchedSettings() {
        settingsDeviceNamePrompt!!.visibility = View.VISIBLE
        settingsDeviceName!!.setText(WifiSyncServiceSettings.deviceName)
        settingsDeviceName!!.visibility = View.VISIBLE
        settingsDeviceName!!.setOnEditorActionListener { _, _, _ ->
            WifiSyncServiceSettings.deviceName = settingsDeviceName!!.text.toString()
            WifiSyncServiceSettings.saveSettings(applicationContext)
            // WifiSyncServiceSettings.saveSettings(mainWindow)
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
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
        }
        return super.onOptionsItemSelected(item)
    }

    fun setActivityRoot(c: Activity) {
        var v: View = (c.findViewById<ViewGroup>(android.R.id.content)!!).getChildAt(0)

        var sv = ScrollView(c)
        var lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        sv.setLayoutParams(lp)

        (v.parent as ViewGroup).removeAllViews()

        sv.addView(v)
        c.addContentView(sv, lp)
    }
}
