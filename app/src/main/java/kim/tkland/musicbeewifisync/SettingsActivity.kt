package kim.tkland.musicbeewifisync

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuCompat
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.jar.Manifest

class SettingsActivity : WifiSyncBaseActivity() {
    private var initialSetup = false
    private var locateServerButton: Button? = null
    private var settingsWaitIndicator: ProgressBar? = null
    private var settingsLocateServerNoConfig: TextView? = null
    private var settingsStorageOptions: RadioGroup? = null
    private var settingsStorageSdCard1: RadioButton? = null
    private var settingsStorageSdCard2: RadioButton? = null
    private var settingsGrantAccessButton: Button? = null
    private var settingsDebugMode: CheckBox? = null
    private var settingsDeviceNamePrompt: TextView? = null
    private var settingsDeviceName: EditText? = null
    private var settingsServerIpOverride: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        // PermissionsHandler.demandInternalStorageAccessPermissions(this)
        initialSetup = WifiSyncServiceSettings.defaultIpAddressValue.length == 0
        locateServerButton = findViewById(R.id.locateServerButton)
        settingsWaitIndicator = findViewById(R.id.settingsWaitIndicator)
        settingsLocateServerNoConfig = findViewById(R.id.settingsLocateServerNoConfig)
        val settingsStoragePrompt = findViewById<TextView>(R.id.settingsStoragePrompt)
        settingsStorageOptions = findViewById(R.id.settingsStorageOptions)
        settingsStorageOptions?.let{it.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { _, _ -> showGrantAccessButton() })}
        val settingsStorageInternal = findViewById<RadioButton>(R.id.settingsStorageInternal)
        settingsStorageSdCard1 = findViewById(R.id.settingsStorageSdCard1)
        settingsGrantAccessButton = findViewById(R.id.settingsGrantAccessButton)
        settingsDebugMode = findViewById(R.id.settingsDebugMode)
        settingsDebugMode?.setChecked(WifiSyncServiceSettings.debugMode)
        settingsDebugMode?.let{it.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, _ ->
            WifiSyncServiceSettings.debugMode = settingsDebugMode?.isChecked!!
        })}
        settingsDeviceNamePrompt = findViewById(R.id.settingsDeviceNamePrompt)
        settingsDeviceName = findViewById(R.id.settingsDeviceName)
        val externalSdCardCount = getExternalFilesDirs(null).size - 1
        var sdCard1:RadioButton? = settingsStorageSdCard1
        //var sdCard2:RadioButton? = settingsStorageSdCard2
        if (externalSdCardCount == 0) {
            settingsStorageInternal.isChecked = true
            sdCard1?.setChecked(false)
            //sdCard2?.let{it.setVisibility(View.GONE)}
        } else {
            if (externalSdCardCount == 1) {
                if (WifiSyncServiceSettings.deviceStorageIndex ==2) {
                    sdCard1?.setChecked(true)
                    //sdCard2?.let{it.setVisibility(View.GONE)}
                } else {
                    settingsStorageInternal.isChecked = true
                    //if (WifiSyncServiceSettings.deviceStorageIndex ==0) {
                    //sdCard1?.setText(R.string.settingsStorageSdCard1)
                    //sdCard2?.let{it.setText(R.string.settingsStorageSdCard2)}
                    //sdCard2?.let{it.setEnabled(false)}
                    //sdCard2?.let{it.setVisibility(View.VISIBLE)
                    //}
                }
            } else {
                settingsStorageInternal.isChecked = false
                WifiSyncServiceSettings.deviceStorageIndex =1
                sdCard1?.setText(R.string.settingsStorageSdCard1)
                //sdCard2?.let{it.setText(R.string.settingsStorageSdCard2)}
                //sdCard2?.let{it.setVisibility(View.VISIBLE)}
                //sdCard2?.let{it.setChecked(WifiSyncServiceSettings.deviceStorageIndex == 2)}
            }
        }
        var debugMode: CheckBox? = settingsDebugMode
        var serverButton: Button? = locateServerButton
        if (initialSetup) {
            WifiSyncServiceSettings.debugMode = true
            debugMode?.let{it.setVisibility(View.GONE)}
        } else {
            val actionBar = supportActionBar
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.setTitle(R.string.title_activity_settings2)
            }
            findViewById<View>(R.id.settingsInfo0).visibility = View.GONE
            findViewById<View>(R.id.settingsInfo1).visibility = View.GONE
            findViewById<View>(R.id.settingsInfo2).visibility = View.GONE
            serverButton?.let{it.setVisibility(View.GONE)}
            debugMode?.let{it.setVisibility(View.VISIBLE)}
            settingsStoragePrompt.setText(R.string.settingsStorageSettingsPrompt)
            if (!Build.MODEL.equals(WifiSyncServiceSettings.deviceName, ignoreCase = true)) {
                showNoConfigMatchedSettings()
            }
        }
        // パーミッションの付与確認
        if (allPermissionsGranted()) {
            // Toast.makeText(this,"既にパーミッションが許可されています", Toast.LENGTH_LONG).show()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSION
            )
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1
        private val REQUIRED_PERMISSION =
             arrayOf(android.Manifest.permission.READ_MEDIA_AUDIO)
    }

    /** 全てのパーミッションｋが許可されているかのチェック */
    private fun allPermissionsGranted() = REQUIRED_PERMISSION.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (allPermissionsGranted()) {
                // Toast.makeText(this, "パーミッションが許可されました。", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this,
                    "パーミッションが許可されませんでした。(-_-;)",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        if (initialSetup) {
            WifiSyncServiceSettings.debugMode = false
        } else {
            WifiSyncServiceSettings.deviceStorageIndex =
                if (settingsStorageSdCard1!!.isChecked) 2 else 1
//                if (settingsStorageSdCard1!!.isChecked) 1 else 0
//                if (settingsStorageSdCard1!!.isChecked) 1 else if (settingsStorageSdCard2!!.isChecked) 2 else 0
            WifiSyncServiceSettings.saveSettings(this)
        }
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

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

    fun onLocateServerButton_Click(view: View) {
        settingsLocateServerNoConfig!!.visibility = View.GONE
        WifiSyncServiceSettings.deviceStorageIndex =
            if (settingsStorageSdCard1!!.isChecked) 2 else 1
//            if (settingsStorageSdCard1!!.isChecked) 1 else 0
//            if (settingsStorageSdCard1!!.isChecked) 1 else if (settingsStorageSdCard2!!.isChecked) 2 else 0
        WifiSyncServiceSettings.saveSettings(mainWindow)
        settingsWaitIndicator!!.visibility = View.VISIBLE
        val locateServerThread = Thread(Runnable {
            val serverIPAddress = WifiSyncService.getMusicBeeServerAddress(mainWindow, null)
            runOnUiThread {
                if (mainWindow != null) {
                    settingsWaitIndicator!!.visibility = View.INVISIBLE
                    locateServerButton!!.isEnabled = true
                    locateServerButton!!.setTextColor(buttonTextEnabledColor)
                    if (serverIPAddress == null) {
                        val errorDialog = AlertDialog.Builder(mainWindow!!)
                        errorDialog.setMessage(getText(R.string.errorServerNotFound))
                        errorDialog.setPositiveButton(android.R.string.ok, null)
                        errorDialog.show()
                    } else if (serverIPAddress == getString(R.string.syncStatusFAIL)) {
                        settingsLocateServerNoConfig!!.visibility = View.VISIBLE
                        showNoConfigMatchedSettings()
                        settingsDeviceNamePrompt!!.visibility = View.GONE
                    } else {
                        WifiSyncServiceSettings.defaultIpAddressValue = serverIPAddress
                        WifiSyncServiceSettings.saveSettings(mainWindow)
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
            WifiSyncServiceSettings.saveSettings(mainWindow)
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        return true
    }

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
}
