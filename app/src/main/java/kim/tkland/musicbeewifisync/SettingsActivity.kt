package kim.tkland.musicbeewifisync

import android.Manifest
import android.R.color.black
import android.R.color.white
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings.ACTION_REQUEST_MANAGE_MEDIA
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.runBlocking

class SettingsActivity : WifiSyncBaseActivity("") {
    private var initialSetup = false
    private var isfirst = true
    var showErrorDialog1 = mutableStateOf(false)
    var showErrorDialog2 = mutableStateOf(false)
    var initialStorage = mutableIntStateOf(value = 1)

    // Initialize the launcher in onCreate or an earlier lifecycle method
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or update the UI.
                Log.d("Permission", "Permission granted")
                // You might want to re-compose or update state here
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied.
                Log.d("Permission", "Permission denied")
                // You might want to show a rationale or guide the user to settings.
            }
        }

    private val requestMultiplePermissionsLauncher =//
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Log.d("Permission", "$permissionName granted")
                } else {
                    Log.d("Permission", "$permissionName denied")
                }
            }
        }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WifiSyncServiceSettings.loadSettings(this)

        initialSetup = WifiSyncServiceSettings.defaultIpAddressValue.isEmpty()

        setContent {
            val currentIP = WifiSyncServiceSettings.defaultIpAddressValue
            val newIPAddressState = rememberTextFieldState(currentIP, TextRange(currentIP.length))

            if (initialSetup) {
                if (isfirst) {
                    val intent = Intent(ACTION_REQUEST_MANAGE_MEDIA)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData("package:kim.tkland.musicbeewifisync".toUri())
                    startActivity(intent)
                    isfirst = false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.READ_MEDIA_AUDIO
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        RequestPermissionForReadWrite() // Request if denied
                    } else {
                        // Permission granted, proceed with button action
                        //performBottomBarButtonAction()
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_DENIED
                    ) {
                        RequestPermissionForReadWrite() // Request if denied
                    } else {
                        // Permission granted, proceed with button action
                        //performBottomBarButtonAction()
                    }
                }

                val sharedPref =
                    getSharedPreferences("kim.tkland.musicbeewifisync.sharedpref", MODE_PRIVATE)
                val uriStr = sharedPref.getString("accesseduri", "")
                val stats = File(WifiSyncServiceSettings.gmmpStatsFile)

                if (stats.exists()) {
                    if (uriStr.isNullOrEmpty()) {
                        //showDialog.value = true
                        launcher.launch(setLaunchIntent())
                        FirstSettingView(newIPAddressState)
                    }
                }

                val viewModel: WifiSyncViewModel = viewModel()

                getMusicFiles()
                viewModel.setValues(
                    getMusicFilesThread,
                    getString(R.string.progressDialogMessage)
                )
                runBlocking {
                    setContent {
                        CreateProgressDialog(viewModel)
                        FirstSettingView(newIPAddressState)
                    }
                }
            } else {
                OptionSettingView(newIPAddressState)
            }
        }
    }

    @Composable
    override fun CustomView() {}

    /*
    fun processGMMPStatsXML() {
        val stats = File(WifiSyncServiceSettings.gmmpStatsFile)
        if (stats.exists()) {
            val mUri: Uri = stats.path.toUri()
            clearAllPersistedUriPermissions(applicationContext)
            contentResolver.takePersistableUriPermission(
                mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
     */

    @Composable
    fun StatsAlertDialog(showDialog: MutableState<Boolean>, onConfirm: () -> Unit) {
        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text(text = getString(R.string.statsSelect)) },
                text = { Text(text = getString(R.string.statsSelectMessage)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onConfirm()
                            showDialog.value = false
                    }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }

    @Composable
    @SuppressLint("SuspiciousIndentation")
    private fun RequestPermissionForReadWrite() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN ->{
                // For Android 17 (API 37) and above
                val context = LocalContext.current
                val mediaPermissions = arrayOf( // Define the array of permissions
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.ACCESS_LOCAL_NETWORK
                )

                var allMediaPermissionsGranted = true
                for (permission in mediaPermissions) {
                    // Don't check MANAGE_MEDIA with checkSelfPermission
                    if (permission == Manifest.permission.MANAGE_MEDIA) continue
                    if (ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        allMediaPermissionsGranted = false
                        break
                    }
                }
                if (allMediaPermissionsGranted) {
                    Log.d("Permission", "All media permissions already granted")
                } else {
                    // Directly request the permission
                    Log.d("Permission", "Requesting READ_MEDIA_AUDIO")
                    val mediaPermissions: MutableList<String> = MutableList<String>(0) { "" }
                    mediaPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    mediaPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    mediaPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                    mediaPermissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                    mediaPermissions.add(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    requestMultiplePermissionsLauncher.launch(mediaPermissions.toTypedArray())
                }

            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // For Android 13 (API 33) and above, request READ_MEDIA_AUDIO
                val context = LocalContext.current
                val mediaPermissions = arrayOf( // Define the array of permissions
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    // Manifest.permission.MANAGE_MEDIA, // MANAGE_MEDIA is special, handle separately
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                )

                var allMediaPermissionsGranted = true
                for (permission in mediaPermissions) {
                    // Don't check MANAGE_MEDIA with checkSelfPermission
                    if (permission == Manifest.permission.MANAGE_MEDIA) continue
                    if (ContextCompat.checkSelfPermission(
                            context,
                            permission
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        allMediaPermissionsGranted = false
                        break
                    }
                }
                if (allMediaPermissionsGranted) {
                    Log.d("Permission", "All media permissions already granted")
                } else {
                    // Directly request the permission
                    Log.d("Permission", "Requesting READ_MEDIA_AUDIO")
                    val mediaPermissions: MutableList<String> = MutableList<String>(0) { "" }
                    mediaPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    mediaPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    mediaPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                    mediaPermissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                    requestMultiplePermissionsLauncher.launch(mediaPermissions.toTypedArray())
                }
            }

            else -> {
                // For Android 11 (API 30) and above, for all files access (if truly needed)
                // or specific media permissions.
                // MANAGE_EXTERNAL_STORAGE is a special permission that needs to be handled differently.
                // For typical media access, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO are preferred.
                // If you need general external storage, check READ_EXTERNAL_STORAGE

                val permissionsToRequest = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                )

                var allPermissionsGranted = true
                for (permission in permissionsToRequest) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            permission // Check one permission at a time
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        allPermissionsGranted = false
                        break
                    }
                }
                if (allPermissionsGranted) {
                    Log.d("Permission", "All required storage permissions already granted")
                } else {
                    Log.d("Permission", "Requesting READ_EXTERNAL_STORAGE")
                    val mediaPermissions: MutableList<String> = MutableList<String>(0) { "" }
                    mediaPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    mediaPermissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
                    requestMultiplePermissionsLauncher.launch(mediaPermissions.toTypedArray())
                }
            }
        }

        // Handle MANAGE_MEDIA separately if needed (as in your original code)
        // This is a special permission and is handled via an Intent, not the standard permission request flow
        if (checkSelfPermission(Manifest.permission.MANAGE_MEDIA) == PackageManager.PERMISSION_DENIED) {
            // This part seems correct in your original code, launching an intent
            val intent = Intent(ACTION_REQUEST_MANAGE_MEDIA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData("package:kim.tkland.musicbeewifisync".toUri())
            // Consider using a launcher for this intent too, if you need to handle its result
            startActivity(intent)
        }
    }

    @Composable
    private fun ShowServerNotFoundDialog(showErrorState: MutableState<Boolean>, newIPAddressState: TextFieldState, isFirst: Boolean) {
        val openAlertDialog = remember { mutableStateOf(showErrorState.value) }

        when {
            openAlertDialog.value -> AlertDialog(
                onDismissRequest = { showErrorState.value = false },
                title = { Text(getString(R.string.syncErrorHeader)) },
                text = { Text(getString(R.string.errorServerNotFound)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showErrorState.value = false
                            openAlertDialog.value = false
                            setContent {
                                if (isFirst) {
                                    FirstSettingView(newIPAddressState)
                                } else {
                                    OptionSettingView(newIPAddressState)
                                }
                            }
                        }
                    ) {
                        Text(getString(android.R.string.ok))
                    }
                },
                dismissButton = null
            )
        }
    }

    @Composable
    private fun ShowNoConfigMatchedDialog(showErrorState: MutableState<Boolean>, newIPAddressState: TextFieldState, isFirst: Boolean) {
        val openAlertDialog = remember { mutableStateOf(showErrorState.value) }

        when {
            openAlertDialog.value -> AlertDialog(
                onDismissRequest = { showErrorState.value = false },
                title = { Text(getString(R.string.syncErrorHeader)) },
                text = { Text(getString(R.string.errorLocateServerNoConfig)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showErrorState.value = false
                            openAlertDialog.value = false
                            setContent {
                                if (isFirst) {
                                    FirstSettingView(newIPAddressState)
                                } else {
                                    OptionSettingView(newIPAddressState)
                                }
                            }
                        }
                    ) {
                        Text(getString(android.R.string.ok))
                    }
                },
                dismissButton = null
            )
        }
    }



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FirstSettingView(newIPAddressState: TextFieldState) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

        val context = LocalContext.current
        var expanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
                    title = {
                        Text(
                            getString(R.string.title_activity_settings),
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
                                modifier = Modifier.background(Color(getColor(white))),
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
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
                Row(
                    // Or a Compose Row
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding),
                ) {
                    Button(
                        modifier = Modifier
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
                        onClick = {
                            // Locate Server automatically
                            WifiSyncServiceSettings.deviceStorageIndex = initialStorage.intValue
                            WifiSyncServiceSettings.saveSettings(applicationContext)
                            
                            // We can use a simple thread or Coroutine for this
                            val locateServerThread = Thread {
                                val serverIPAddress = WifiSyncService.getMusicBeeServerAddress(context, null)
                                runOnUiThread {
                                    if (serverIPAddress == null) {
                                        showErrorDialog1.value = true
                                    } else if (serverIPAddress == getString(R.string.syncStatusFAIL)) {
                                        showErrorDialog2.value = true
                                    } else {
                                        WifiSyncServiceSettings.defaultIpAddressValue = serverIPAddress
                                        WifiSyncServiceSettings.saveSettings(applicationContext)
                                        val intent = Intent(context, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                            locateServerThread.start()
                        }
                    ) {
                        Text(getString(R.string.settingsLocate), fontSize = 24.sp)
                    }
                }
            }
        ) { innerPadding ->
            if (showErrorDialog1.value) {
                ShowServerNotFoundDialog(showErrorDialog1, newIPAddressState, true)
            }
            if (showErrorDialog2.value) {
                ShowNoConfigMatchedDialog(showErrorDialog2, newIPAddressState, true)
            }
            Column(
                modifier = Modifier
                    .background(Color(getColor(white)))
                    .padding(innerPadding),
                //.padding(statusBarPadding),
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
                    StorageRadioGroup(initialStorage.intValue, 20)
                }
                // 2025/09/17 有効にしてみる->問題ない
                Row(modifier = Modifier.padding(top = 30.dp)) {
                    CheckableRow(
                        text = applicationContext.getString(R.string.settingsDebugMode),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OptionSettingView(newIPAddressState: TextFieldState) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        val context = LocalContext.current

        var initialStorage by remember { mutableIntStateOf(value = WifiSyncServiceSettings.deviceStorageIndex) }

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        var expanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
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
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                            }
                            DropdownMenu(
                                modifier = Modifier.background(Color(getColor(white))),
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
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
                Row(
                    // Or a Compose Row
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding),
                ) {
                    Button(
                        modifier = Modifier
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
                        onClick = {
                            val serverIPAddress = newIPAddressState.text.toString()
                            if (serverIPAddress.isEmpty()) {
                                showErrorDialog1.value = true
                            } else {
                                WifiSyncServiceSettings.defaultIpAddressValue = serverIPAddress
                                WifiSyncServiceSettings.saveSettings(applicationContext)
                                val intent = Intent(context, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    ) {
                        Text(getString(R.string.settingsLocate), fontSize = 24.sp)
                    }
                }
            }
        ) { innerPadding ->
            if (showErrorDialog1.value) {
                ShowServerNotFoundDialog(showErrorDialog1, newIPAddressState, false)
            }
            if (showErrorDialog2.value) {
                ShowNoConfigMatchedDialog(showErrorDialog2, newIPAddressState, false)
            }
            Column(
                modifier = Modifier
                    .background(Color(getColor(white)))
                    .padding(innerPadding)
                    .padding(statusBarPadding)
                    .padding(navigationBarPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 15.dp, end = 15.dp, top = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    StorageRadioGroup(initialStorage, 20)
                }
                Row(
                    modifier = Modifier
                        .padding(start = 15.dp, end = 15.dp, top = 100.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    CheckableRow(
                        text = applicationContext.getString(R.string.settingsDebugMode),
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(start = 15.dp, end = 15.dp, top = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    TextField(
                        state = newIPAddressState,
                        label = { Text(applicationContext.getString(R.string.titleOfIPAddress)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White
                        ),
                    )
                }
            }
        }
    }

    @Composable
    private fun CheckableRow(text: String) {
        var debugMode by remember { mutableStateOf(value = WifiSyncServiceSettings.debugMode) }

        Row(
            modifier = Modifier
                .toggleable(
                    value = debugMode,
                    role = Role.Checkbox,
                    onValueChange = {
                        debugMode = it
                        WifiSyncServiceSettings.debugMode = it
                    }
                )
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Checkbox(
                checked = debugMode,
                colors = CheckboxDefaults.colors(
                    checkmarkColor = Color(getColor(white)),
                    uncheckedColor = Color(getColor(black)),
                    checkedColor = Color(getColor(R.color.colorAccent)),
                ),
                onCheckedChange = null
            )
            Text(text, fontSize = 20.sp)
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
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(getColor(R.color.colorAccent)), // 選択時の色
                            unselectedColor = Color(getColor(R.color.colorButtonBackground)) // 非選択時の色
                        ),
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
                    /* modeFlags = */
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        } catch (e: Throwable) {
            // just to be safe...
            e.printStackTrace()
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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun showNoConfigMatchedSettings() {
        //WifiSyncServiceSettings.deviceName = settingsDeviceName!!.text.toString()
        WifiSyncServiceSettings.saveSettings(applicationContext)
        false
    }
}
