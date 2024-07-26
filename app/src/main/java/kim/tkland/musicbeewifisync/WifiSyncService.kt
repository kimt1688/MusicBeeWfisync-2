package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.IBinder
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider.*
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.documentfile.provider.DocumentFile
import kim.tkland.musicbeewifisync.Dialog.showOkCancel
import kim.tkland.musicbeewifisync.ErrorHandler.logError
import kim.tkland.musicbeewifisync.ErrorHandler.logInfo
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.*
import java.net.*
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.xml.parsers.SAXParserFactory


class WifiSyncService : Service() {
    private val syncFileScanCount = AtomicInteger(0)
    private var settingsSyncFromMusicBee = false
    private var settingsSyncPreview = false
    private var settingsSyncIgnoreErrors = false
    private var settingsDefaultIpAddressValue: String? = null
    private var settingsDeviceName: String? = null
    private var settingsDeviceStorageIndex = 0
    private var settingsAccessPermissionsUri: Uri? = null
    private var settingsSyncCustomFiles = false
    private var settingsSyncDeleteUnselectedFiles = false
    private var settingsSyncCustomPlaylistNames: ArrayList<String>? = null
    private var settingsReverseSyncPlayer = 1
    private var settingsReverseSyncPlaylists = false
    private var settingsReverseSyncPlaylistsPath: String? = null
    private var settingsReverseSyncRatings = false
    private var settingsReverseSyncPlayCounts = false
    private var syncWorkerThread: Thread? = null
    private var storage: FileStorageAccess? = null
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        val channel = NotificationChannel(
            "MusicBeeChannel_01",
            getString(R.string.title_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this, "MusicBeeChannel_01")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.syncNotificationInfo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val stopIntent = Intent(this, WifiSyncService::class.java)
        stopIntent.action = getString(R.string.actionSyncAbort)
        val pendingStopIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_delete,
            getString(R.string.syncStop),
            pendingStopIntent
        )
        builder.addAction(stopAction)
        startForeground(FOREGROUND_ID, builder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null) {
            if (WifiSyncServiceSettings.debugMode) {
                logInfo("command", "action=$action")
            }
            if ((action == getString(R.string.actionSyncStart))) {
                syncIsRunning.set(true)
                syncPercentCompleted.set(0)
                syncProgressMessage.set("")
                syncErrorMessageId.set(0)
                syncFileScanCount.set(0)
                syncFailedFiles.clear()
                syncIteration = intent.getIntExtra(intentNameSyncIteration, 0)
                settingsDefaultIpAddressValue = intent.getStringExtra(
                    intentNameDefaultIpAddressValue
                )
                settingsDeviceName = intent.getStringExtra(intentNameDeviceName)
                settingsDeviceStorageIndex = intent.getIntExtra(intentNameDeviceStorageIndex, 0)
                BundleCompat.getParcelable(intent.extras!!, "intentNameAccessPermissionsUri", Uri::class.java)
                //settingsAccessPermissionsUri = intent.getParcelableExtra(
                //    intentNameAccessPermissionsUri, Uri::class.java
                //)
                settingsSyncPreview = intent.getBooleanExtra(intentNameSyncPreview, false)
                settingsSyncFromMusicBee = intent.getBooleanExtra(intentNameSyncFromMusicBee, true)
                settingsSyncIgnoreErrors = intent.getBooleanExtra(intentNameSyncIgnoreErrors, false)
                settingsReverseSyncPlayer = intent.getIntExtra(intentNameReverseSyncPlayer, 1)
                settingsReverseSyncPlaylists =
                    intent.getBooleanExtra(intentNameReverseSyncPlaylists, false)
                settingsReverseSyncPlaylistsPath = intent.getStringExtra(
                    intentNameReverseSyncPlaylistsPath
                )
                settingsReverseSyncRatings =
                    intent.getBooleanExtra(intentNameReverseSyncRatings, false)
                settingsReverseSyncPlayCounts = intent.getBooleanExtra(
                    intentNameReverseSyncPlayCounts, false
                )
                settingsSyncDeleteUnselectedFiles = intent.getBooleanExtra(
                    intentNameSyncDeleteUnselectedFiles, false
                )
                settingsSyncCustomFiles = intent.getBooleanExtra(intentNameSyncCustomFiles, false)
                settingsSyncCustomPlaylistNames = intent.getStringArrayListExtra(
                    intentNameSyncCustomPlaylistNames
                )
                syncWorkerThread = Thread(SynchronisationWorker(this))
                syncWorkerThread!!.start()
            } else if ((action == getString(R.string.actionSyncAbort))) {
                syncIsRunning.set(false)
                syncPercentCompleted.set(-1)
                if (syncWorkerThread != null) {
                    syncWorkerThread!!.interrupt()
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private inner class SynchronisationWorker(private val context: Context) :
        Runnable {
        private var clientSocket: Socket? = null
        private var socketInputStream: InputStream? = null
        private var socketStreamReader: DataInputStream? = null
        private var socketOutputStream: OutputStream? = null
        private var socketStreamWriter: DataOutputStream? = null
        override fun run() {
            try {
                var anyConnections =
                    tryStartSynchronisation(InetAddress.getByName(settingsDefaultIpAddressValue))
                if (!anyConnections) {
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo(
                            "worker",
                            "no connection for $settingsDefaultIpAddressValue - trying again"
                        )
                    }
                    val candidateAddresses =
                        findCandidateIpAddresses(IpAddressProviderImpl(context, null))
                    for (candidate: CandidateIpAddress in candidateAddresses) {
                        if (tryStartSynchronisation(candidate.address)) {
                            anyConnections = true
                            WifiSyncServiceSettings.defaultIpAddressValue = candidate.toString()
                            break
                        }
                    }
                }
                if (!anyConnections) {
                    syncErrorMessageId.set(R.string.errorServerNotFound)
                }
            } catch (ex: InterruptedException) {
                if (WifiSyncServiceSettings.debugMode) {
                    logError("worker", ex)
                }
            } catch (ex: Exception) {
                logError("worker", ex)
                syncErrorMessageId.set(R.string.errorServerNotFound)
            }
            syncPercentCompleted.set(-1)
            syncIsRunning.set(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
//            stopForeground(true)
            stopSelf()
        }

        @Throws(InterruptedException::class)
        private fun tryStartSynchronisation(address: InetAddress): Boolean {
            var serverLocated = false
            try {
                var socketFailRetryAttempts = 0
                loop@ while (true) {
                    //if (socketFailRetryAttempts > 0) {
                        // allow some time for the server to re-open the listener
                    //    Thread.sleep(1000)
                    //}
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("tryStart", "connecting $address, attempt=$socketFailRetryAttempts")
                    }
                    Socket().use { clientSocket ->
                        this.clientSocket = clientSocket
                        clientSocket.connect(
                            InetSocketAddress(address, serverPort),
                            socketConnectTimeout
                        )
                        if (WifiSyncServiceSettings.debugMode) {
                            logInfo("tryStart", "connected")
                        }
                        clientSocket.receiveBufferSize = 262144
                        clientSocket.sendBufferSize = 65536
                        clientSocket.setPerformancePreferences(0, 0, 1)
                        clientSocket.tcpNoDelay = true
                        clientSocket.getInputStream().use { socketInputStream ->
                            BufferedInputStream(
                                socketInputStream,
                                8192
                            ).use { bufferedSocketInputStream ->
                                DataInputStream((bufferedSocketInputStream)).use { socketStreamReader ->
                                    clientSocket.getOutputStream().use { socketOutputStream ->
                                        BufferedOutputStream(
                                            socketOutputStream,
                                            65536
                                        ).use { bufferedSocketOutputStream ->
                                            DataOutputStream(bufferedSocketOutputStream).use { socketStreamWriter ->
                                                this.socketInputStream = socketInputStream
                                                this.socketStreamReader = socketStreamReader
                                                this.socketOutputStream = socketOutputStream
                                                this.socketStreamWriter = socketStreamWriter
                                                var failedSyncFilesCount = 0
                                                try {
                                                    clientSocket.soTimeout = socketReadTimeout
                                                    val hello: String = readString()
                                                    serverLocated =
                                                        hello.startsWith(serverHelloPrefix)
                                                    if (WifiSyncServiceSettings.debugMode) {
                                                        logInfo(
                                                            "tryStart",
                                                            "hello=$serverLocated,fromMB=$settingsSyncFromMusicBee,custfiles=$settingsSyncCustomFiles,preview=$settingsSyncPreview,dev=$settingsDeviceName,$settingsDeviceStorageIndex"
                                                        )
                                                    }
                                                    if (!serverLocated) {
                                                        return false
                                                    }
                                                    writeString(clientHelloVersion)
                                                    writeString(if ((settingsSyncCustomFiles)) commandSyncToDevice else commandSyncDevice)
                                                    writeByte(if ((!settingsSyncPreview)) 0 else 1)
                                                    writeString(settingsDeviceName)
                                                    writeByte(settingsDeviceStorageIndex)
                                                    if (!settingsSyncCustomFiles) {
                                                        writeString(if ((settingsSyncFromMusicBee)) "F" else "T")
                                                        writeString("F")
                                                        writeString("0")
                                                    } else {
                                                        writeString("T")
                                                        writeString(if ((!settingsSyncDeleteUnselectedFiles)) "F" else "T")
                                                        writeString(settingsSyncCustomPlaylistNames!!.size.toString())
                                                        for (playlistName: String? in settingsSyncCustomPlaylistNames!!) {
                                                            writeString(playlistName)
                                                        }
                                                    }
                                                    writeString(syncEndOfData)
                                                    flushWriter()
                                                    val storageProfileMatched: Boolean =
                                                        (readByte().toInt() != 0)
                                                    readToEndOfCommand()
                                                    // TODO:
                                                    if (!storageProfileMatched) {
                                                        syncErrorMessageId.set(R.string.errorConfigNotMatched)
                                                        setPreviewFailed()
                                                        return true
                                                    } else {
                                                        val sdCard: File? =
                                                            FileStorageAccess.getSdCardFromIndex(
                                                                applicationContext,
                                                                settingsDeviceStorageIndex
                                                            )
                                                        if (sdCard == null) {
                                                            if (WifiSyncServiceSettings.debugMode) {
                                                                logInfo(
                                                                    "tryStart",
                                                                    "SD Card not found"
                                                                )
                                                            }
                                                            writeString(syncStatusFAIL)
                                                            flushWriter()
                                                            syncErrorMessageId.set(R.string.errorSdCardNotFound)
                                                            return true
                                                        }
                                                        storage = FileStorageAccess(
                                                            applicationContext,
                                                            sdCard.path,
                                                            settingsAccessPermissionsUri
                                                        )
                                                    }
                                                    writeString("MOUNTED")
                                                    failedSyncFilesCount = syncFailedFiles.size
                                                    syncDevice()
                                                } catch (ex: InterruptedException) {
                                                    if (WifiSyncServiceSettings.debugMode) {
                                                        logError("tryStart", ex)
                                                    }
                                                    if (storage != null) {
                                                        storage!!.waitScanFiles()
                                                    }
                                                    throw ex
                                                } catch (ex: SocketException) {
                                                    logError(
                                                        "tryStart$socketFailRetryAttempts",
                                                        ex
                                                    )
                                                    if (storage != null) {
                                                        storage!!.waitScanFiles()
                                                    }
                                                    if (socketFailRetryAttempts > 16) {
                                                        syncErrorMessageId.set(R.string.errorSyncNonSpecific)
                                                    } else {
                                                        socketFailRetryAttempts++
                                                        if (syncFailedFiles.size > failedSyncFilesCount) {
                                                            syncFailedFiles.removeAt(
                                                                failedSyncFilesCount
                                                            )
                                                        }
                                                    // TODO:pending
                                                    // continue@loop
                                                    }
                                                } catch (ex: Exception) {
                                                    logError(
                                                        "tryStart$socketFailRetryAttempts",
                                                        ex
                                                    )
                                                    if (storage != null) {
                                                        storage!!.waitScanFiles()
                                                    }
                                                    syncErrorMessageId.set(R.string.errorSyncNonSpecific)
                                                    setPreviewFailed()
                                                }
                                                return true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ex: InterruptedException) {
                if (WifiSyncServiceSettings.debugMode) {
                    logError("tryStart", ex)
                }
                throw ex
            } catch (ex: SocketTimeoutException) {
                if (WifiSyncServiceSettings.debugMode) {
                    logError("tryStart", ex)
                }
                syncErrorMessageId.set(R.string.errorServerNotFound)
                setPreviewFailed()
            } catch (ex: Exception) {
                logError("tryStart", ex)
                syncErrorMessageId.set(R.string.errorSyncNonSpecific)
                setPreviewFailed()
            }
            return serverLocated
        }

        private fun setPreviewFailed() {
            syncFromResults = null
            waitSyncResults.set()
        }

        @Throws(Exception::class)
        private fun syncDevice() {
            if (WifiSyncServiceSettings.debugMode) {
                logInfo(
                    "syncDevice",
                    "root=" + storage!!.storageRootPath + ",ignoreErrors=" + settingsSyncIgnoreErrors + ",playlists=" + settingsReverseSyncPlaylists + ",ratings=" + settingsReverseSyncRatings + ",playcount=" + settingsReverseSyncPlayCounts
                )
            }
            syncFromResults = null
            if (!settingsSyncCustomFiles) {
                syncToResults = null
                writeByte(syncIteration)
                writeByte(if ((!settingsSyncIgnoreErrors)) 0 else 1)
                writeByte(if ((!settingsReverseSyncPlaylists)) 0 else 1)
                writeByte(if ((!settingsReverseSyncRatings)) 0 else 1)
                writeByte(if ((!settingsReverseSyncPlayCounts)) 0 else 1)
                writeString(storage!!.storageRootPath)
                writeString(syncEndOfData)
            } else {
                syncToResults = ArrayList()
            }
            flushWriter()
            var command: String
            commandLoop@ while (true) {
                command = readString()
                if (WifiSyncServiceSettings.debugMode) {
                    logInfo("syncDevice", "command=$command")
                }
                if (Thread.interrupted()) {
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("syncDevice", "interrupted")
                    }
                    throw InterruptedException()
                }
                when (command) {
                    "GetCapability" -> capability
                    "GetFiles" -> files
                    "ShowDeleteConfirmation" -> showDeleteConfirmation()
                    "ReceiveFile" -> receiveFile()
                    "DeleteFiles" -> deleteFiles()
                    "DeleteFolders" -> deleteFolders()
                    "SendFile" -> sendFile()
                    "SendPlaylists" -> sendPlaylists()
                    "SendStats" -> sendStats()
                    "ShowResults" -> reverseSyncPreviewResults
                    "ShowPreviewResults" -> {
                        showSyncPreview()
                        break@commandLoop
                    }
                    "Exit" -> {
                        exitSynchronisation()
                        break@commandLoop
                    }
                    else -> break@commandLoop
                }
            }
            if (WifiSyncServiceSettings.debugMode) {
                logInfo("syncDevice", "exit")
            }
        }

        @Throws(SocketException::class)
        private fun readToEndOfCommand() = try {
            while (socketStreamReader!!.read() != 27) {}
        } catch (ex: Exception) {
            throw SocketException(ex.toString())
        }

        @Suppress("REDUNDANT_MODIFIER_IN_GETTER")
        @get:Throws(SocketException::class)
        private val availableData: Int
            private get() {
                try {
                    return socketInputStream!!.available()
                } catch (ex: Exception) {
                    throw SocketException(ex.toString())
                }
            }

        @Throws(SocketException::class)
        private fun flushWriter() {
            try {
                socketStreamWriter!!.flush()
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun readString(): String {
            try {
                return socketStreamReader!!.readUTF()
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun writeString(value: String?) {
            try {
                socketStreamWriter!!.writeUTF(value)
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun readByte(): Byte {
            try {
                return socketStreamReader!!.readByte()
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun writeByte(value: Int) {
            try {
                socketStreamWriter!!.writeByte(value)
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun readShort(): Short {
            try {
                return socketStreamReader!!.readShort()
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun readInt(): Int {
            try {
                return socketStreamReader!!.readInt()
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun writeInt(value: Int) {
            try {
                socketStreamWriter!!.writeInt(value)
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun readLong(): Long {
            try {
                return socketStreamReader!!.readLong()
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun writeLong(value: Long) {
            try {
                socketStreamWriter!!.writeLong(value)
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun readArray(buffer: ByteArray, count: Int): Int {
            try {
                return socketInputStream!!.read(buffer, 0, count)
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(SocketException::class)
        private fun writeArray(buffer: ByteArray, count: Int) {
            try {
                socketOutputStream!!.write(buffer, 0, count)
            } catch (ex: Exception) {
                throw SocketException(ex.toString())
            }
        }

        @Throws(Exception::class)
        private fun exitSynchronisation() {
            try {
                if (WifiSyncServiceSettings.debugMode) {
                    logInfo("exitSync", "fails=" + syncFailedFiles.size)
                }
                if (syncFailedFiles.size > 0 || (syncToResults != null && syncToResults!!.size > 0)) {
                    syncErrorMessageId.set(R.string.syncCompletedFail)
                }
                readToEndOfCommand()
                syncPercentCompleted.set(100)
                storage!!.waitScanFiles()
                writeString(syncStatusOK)
            } catch (ex: Exception) {
                logError("exit", ex)
                writeString(syncStatusFAIL)
            }
            flushWriter()
            clientSocket!!.shutdownInput()
        }

        @Suppress("REDUNDANT_MODIFIER_IN_GETTER")
        @get:Throws(Exception::class)
        private val capability: Unit
            private get() {
                val feature = readString()
                readToEndOfCommand()
                try {
                    when (feature) {
                        "Build" -> {
                            writeString("1.0.0")
                            writeString("1")
                            writeString("FALSE")
                        }
                        "API" -> {
                            writeString("1")
                            writeString("FALSE")
                        }
                        else -> writeString("FALSE")
                    }
                    writeString(syncStatusOK)
                } catch (ex: Exception) {
                    logError("getCapability", ex)
                    if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                        throw ex
                    }
                    writeString(syncStatusFAIL)
                }
                flushWriter()
            }

        @Suppress("REDUNDANT_MODIFIER_IN_GETTER")
        @get:Throws(Exception::class)
        private val files: Unit
            private get() {
                val folderPath = readString()
                val includeSubFolders = (readByte().toInt() == 1)
                readToEndOfCommand()
                try {
                    getFiles(folderPath, includeSubFolders)
                    writeString(syncEndOfData)
                    writeString(syncStatusOK)
                } catch (ex: Exception) {
                    logError("getFiles", ex, "path=$folderPath")
                    if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                        throw ex
                    }
                    writeString(syncEndOfData)
                    writeString("$syncStatusFAIL $ex")
                    syncErrorMessageId.set(R.string.syncCompletedFail)
                }
                flushWriter()
            }

        @Throws(Exception::class)
        private fun getFiles(folderPath: String, includeSubFolders: Boolean) {
            val contentResolver = applicationContext.contentResolver
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val storageRootPathLength = storage!!.storageRootPath.length + 1
            val folderUrl = storage!!.getFileUrl(folderPath)
            val selection = "${MediaStore.Files.FileColumns.DATE_MODIFIED} > 0"
            if (WifiSyncServiceSettings.debugMode) {
                logInfo("getFiles", "Get: $folderPath,url=$folderUrl, inc=$includeSubFolders")
                logInfo("getFiles: DATA: ", MediaStore.Files.FileColumns.DATA)
                logInfo("getFiles: selection: ", selection)
            }
            try {
                val cursor: Cursor? = contentResolver.query(
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    projection,
                    selection,
                    null, // arrayOf(folderUrl, null),
                    "")
                if (cursor == null) {
                    // logInfo("getFiles", "no cursor")
                } else {
                    if (!includeSubFolders &&  cursor.count == 0) {
                        // hack because playlists will not return any matches when querying the media store
                        val files: ArrayList<FileInfo> = storage!!.getFiles(folderPath)
                        if (WifiSyncServiceSettings.debugMode) {
                            logInfo("getFiles", "count=" + files.size)
                        }
                        for (file: FileInfo in files) {
                            writeString(storage!!.getDecodedUrl(file.filename))
                            writeLong(file.dateModified)
                        }
                        return
                    }
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("getFiles", "count=" + cursor.count)
                    }
                    val urlColumnIndex: Int =
                        cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                    val dateModifiedColumnIndex: Int =
                        cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val url: String = cursor.getString(urlColumnIndex)
                        // logInfo("Data", url)
                        if (!url.startsWith(folderUrl, true)) {
                            continue
                        }
                        if (!includeSubFolders) {
                            val filenameIndex: Int = url.lastIndexOf('/') + 1
                            if (filenameIndex > folderUrl.length) {
                                continue
                            }
                        }
                        writeString(url.substring(storageRootPathLength))
                        writeLong(cursor.getLong(dateModifiedColumnIndex))
                    }
                }
                cursor!!.close()
            } finally {
            }
        }

        @Throws(Exception::class)
        private fun receiveFile() {
            val filePath = readString()
            val fileLength = readLong()
            val fileDateModified = readLong()
            var contentUri: Uri? = null
            var os: OutputStream? = null
            syncPercentCompleted.set(readShort().toInt())
            readToEndOfCommand()
            val buffer = arrayOf(ByteArray(socketReadBufferLength), ByteArray(socketReadBufferLength))
            val readCount = IntArray(2)
            val waitRead = AutoResetEvent(false)
            val waitWrite = AutoResetEvent(true)
            var cursor: Cursor? = null
            if (WifiSyncServiceSettings.debugMode) {
                Log.d("receiveFile", "Receive: $filePath")
            }
            try {
                val contentResolver = applicationContext.contentResolver
                val ext = File(filePath).extension
                var isplaylist = false
                if (ext.isNotEmpty())
                    isplaylist = ext.equals("m3u", ignoreCase = true) || ext.equals("m3u8", ignoreCase = true) || ext.equals("wpl", ignoreCase = true)
                val mimetype: String?
                if (isplaylist) {
                    receivePlaylist(filePath, fileLength, fileDateModified)
                    return
                } else {
                    mimetype = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(ext.lowercase(Locale.getDefault()))
                }
                val separatorIndex = filePath.lastIndexOf('/') + 1
                val path = filePath.substring(0, separatorIndex)
                val name = filePath.substring(separatorIndex)
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, path)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimetype)
                    put(MediaStore.Audio.Media.IS_PENDING, false)
                }

                val audioCollection = MediaStore.Audio.Media.getContentUri(
                        MediaStore.getExternalVolumeNames(context)
                            .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
                    )
                try {
                    syncProgressMessage.set(
                        String.format(
                            getString(R.string.syncFileActionCopy),
                            filePath
                        )
                    )

                    cursor = applicationContext.contentResolver.query(
                        audioCollection,
                        arrayOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.RELATIVE_PATH,
                            MediaStore.Audio.Media.DISPLAY_NAME,
                            MediaStore.Audio.Media.DATE_MODIFIED
                        ),
                        "${MediaStore.Audio.Media.RELATIVE_PATH} = ? AND ${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.DATE_MODIFIED} > 0",
                        arrayOf(path, name),
                        null,
                        null)
                    if (cursor != null) {
                        if (cursor.count == 1) {
                            cursor.moveToFirst()
                            contentUri =
                                ContentUris.withAppendedId(audioCollection, cursor.getLong(0))
                            Log.d("UpdateContentUri:", contentUri.toString())

                            storage!!.updateFile(application as WifiSyncApp, contentUri)
                        }
                    }
                }catch (ex: Exception) {
                    Log.d("receiveFile", "${ex.message}(file=$filePath)")
                } finally {
                    if (cursor != null) cursor.close()
                }

                try {
                    if (contentUri != null) {
                        applicationContext.contentResolver.update(contentUri, values, null)
                        os = contentResolver.openOutputStream(contentUri, "wt")!!
                    } else {
                        contentUri = applicationContext.contentResolver.insert(audioCollection, values)
                        os = contentResolver.openOutputStream(contentUri!!, "wt")!!
                    }
                    os.use{fs: OutputStream? ->
                        writeString(syncStatusOK)
                        flushWriter()
                        val thread = Thread(
                            ReceiveFileReceiveLoop(
                                fileLength,
                                buffer,
                                readCount,
                                waitRead,
                                waitWrite
                            )
                        )
                        thread.start()
                        try {
                            var bytesRead: Int
                            var bufferIndex = 0
                            while (true) {
                                waitRead.waitOne()
                                bytesRead = readCount[bufferIndex]
                                if (bytesRead < 0) {
                                    throw SocketException("Error reading file")
                                } else if (bytesRead == 0) {
                                    break
                                }
                                fs?.write(buffer[bufferIndex], 0, bytesRead)
                                waitWrite.set()
                                bufferIndex = if ((bufferIndex == 1)) 0 else 1
                            }
                        } finally {
                            thread.interrupt()
                        }
                    }
                }finally {
                    //values.clear()
                    //values = ContentValues().apply {
                    //    put(MediaStore.Audio.Media.IS_PENDING, false)
                    //}
                    //contentResolver.update(contentUri!!, values, null, null)
                    os?.close()
                }
                writeString(syncStatusOK)
                flushWriter()
                storage!!.scanFile(
                    filePath,
                    fileLength,
                    fileDateModified,
                    FileStorageAccess.ACTION_ADD
                )
            } catch (ex: Exception) {
                try {
                    logError("receiveFile", ex, "file=$filePath")
                    if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                        throw ex
                    }
                    syncFailedFiles.add(
                        FileErrorInfo(
                            FileErrorInfo.ERROR_COPY,
                            filePath,
                            ex.toString()
                        )
                    )
                    while (true) {
                        Thread.sleep(100)
                        var skipBytes = availableData
                        if (skipBytes <= 0) {
                            break
                        }
                        while (skipBytes > 0) {
                            readArray(
                                buffer[0],
                                if ((skipBytes >= socketReadBufferLength)) socketReadBufferLength else skipBytes
                            )
                            skipBytes -= socketReadBufferLength
                        }
                    }
                    writeString("$syncStatusFAIL $ex")
                    flushWriter()

                } finally {
                    try {
                        storage!!.deleteFile(filePath)
                    } catch (deleteException: Exception) {
                        Log.d("receiveFile", deleteException.message!!)
                    }
                }
            }
        }

        @SuppressLint("Range")
        private fun receivePlaylist(filePath: String, fileLength: Long, fileDateModified: Long) {
            val playListCollection = MediaStore.Audio.Playlists.getContentUri(MediaStore.getExternalVolumeNames(context).toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1])
            var playlistname = File(filePath).name
            val path = File(filePath).parent!!.plus("/")
            var contentUri: Uri? = null
            if (!File(filePath).extension.equals("m3u", ignoreCase = true)){
                playlistname = "$playlistname.m3u"
            }
            //Log.d("receivePlaylist:", "Name:${playlistname}")
            //Log.d("receivePlaylist:", "Path:${path}")

            syncProgressMessage.set(
                String.format(
                    getString(R.string.syncFileActionCopy),
                    filePath
                )
            )
            var cursor: Cursor? = null
            try {
                cursor = applicationContext.contentResolver.query(
                    playListCollection,
                    arrayOf(
                        MediaStore.Audio.Playlists._ID,
                        MediaStore.Audio.Playlists.DISPLAY_NAME
                    ),
                    "${MediaStore.Audio.Playlists.DISPLAY_NAME} = ?",
                    arrayOf(playlistname),
                    null,
                    null
                )
            } catch (e: Exception){Log.d("SQLite Error", e.stackTraceToString())}
            if (cursor != null) {
                try {
                    cursor.moveToFirst()
                    if (cursor.count > 0) {
                        contentUri =
                            ContentUris.withAppendedId(playListCollection, cursor.getLong(0))
                        Log.d("Update ContentUri", filePath)
                        Log.d("Update ContentUri:", contentUri.toString())

                        storage!!.updateFile(application as WifiSyncApp, contentUri!!)
                    }
                } catch (e: Exception) {
                    Log.d("receivePlayList", e.message!!)
                    Log.d("receivePlayList", e.stackTraceToString())
                }
                cursor.close()
            }

            val buffer = arrayOf(ByteArray(socketReadBufferLength), ByteArray(socketReadBufferLength))
            val readCount = IntArray(2)
            val waitRead = AutoResetEvent(false)
            val waitWrite = AutoResetEvent(true)

            val values = ContentValues().apply {
                put(MediaStore.Audio.Playlists.DISPLAY_NAME, playlistname)
                put(MediaStore.Audio.Playlists.RELATIVE_PATH, path)
                put(MediaStore.Audio.Playlists.IS_PENDING, false)
            }

            var os: OutputStream? = null

            try {
                if (contentUri != null) {
                    applicationContext.contentResolver.update(contentUri, values, null)
                    os = contentResolver.openOutputStream(contentUri, "wt")!!
                } else {
                    contentUri = applicationContext.contentResolver.insert(playListCollection, values)
                    os = contentResolver.openOutputStream(contentUri!!, "wt")!!
                }

                os.use { fs: OutputStream? ->
                    writeString(syncStatusOK)
                    flushWriter()
                    val thread = Thread(
                        ReceiveFileReceiveLoop(
                            fileLength,
                            buffer,
                            readCount,
                            waitRead,
                            waitWrite
                        )
                    )
                    thread.start()
                    try {
                        var bytesRead: Int
                        var bufferIndex = 0
                        while (true) {
                            waitRead.waitOne()
                            bytesRead = readCount[bufferIndex]
                            if (bytesRead < 0) {
                                throw SocketException("Error reading file")
                            } else if (bytesRead == 0) {
                                break
                            }
                            fs?.write(buffer[bufferIndex], 0, bytesRead)
                            waitWrite.set()
                            bufferIndex = if ((bufferIndex == 1)) 0 else 1
                        }
                    } finally {
                        thread.interrupt()
                    }
                }
                //values.clear()
                //values = ContentValues().apply {
                //    put(MediaStore.Audio.Media.IS_PENDING, false)
                //}
                //contentResolver.update(contentUri, values, null)
                writeString(syncStatusOK)
                flushWriter()
                storage!!.scanFile(
                    filePath,
                    fileLength,
                    fileDateModified,
                    FileStorageAccess.ACTION_ADD
                )
            } catch (ex: Exception) {
                Log.d("receivePlaylist", ex.stackTraceToString())
            } finally {
                os?.close()
            }
        }

        private inner class ReceiveFileReceiveLoop(
            private val fileLength: Long,
            private val buffer: Array<ByteArray>,
            private val readCount: IntArray,
            private val waitRead: AutoResetEvent,
            private val waitWrite: AutoResetEvent
        ) : Runnable {
            override fun run() {
                var readLength = socketReadBufferLength
                var bytesRead: Int
                var remainingBytes = fileLength
                var bufferIndex = 0
                while (true) {
                    try {
                        if (remainingBytes <= 0) {
                            bytesRead = 0
                        } else {
                            if (remainingBytes < socketReadBufferLength) {
                                readLength = remainingBytes.toInt()
                            }
                            bytesRead = readArray(buffer[bufferIndex], readLength)
                        }
                    } catch (ex: Exception) {
                        bytesRead = -1
                        logError("receiveLoop", ex)
                    }
                    try {
                        waitWrite.waitOne()
                    } catch (ex: InterruptedException) {
                        bytesRead = -1
                    }
                    readCount[bufferIndex] = bytesRead
                    waitRead.set()
                    if (bytesRead <= 0) {
                        break
                    }
                    remainingBytes -= bytesRead.toLong()
                    bufferIndex = if ((bufferIndex == 1)) 0 else 1
                }
            }
        }

        @Throws(Exception::class)
        private fun sendFile() {
            val filePath = readString()
            readToEndOfCommand()
            writeString(storage!!.getFileUrl(filePath))
            val buffer = Array(2) { ByteArray(65536) }
            val readCount = IntArray(2)
            val waitRead = AutoResetEvent(false)
            val waitWrite = AutoResetEvent(true)
            val exception = arrayOfNulls<Exception>(1)
            var fileLength: Long = -1
            var remainingBytes: Long = 0
            var status: String = syncStatusOK
            try {
                storage!!.openReadStream(filePath).use { fs ->
                    fileLength = storage!!.getLength(filePath)
                    writeLong(fileLength)
                    flushWriter()
                    val thread =
                        Thread(sendFileWriteLoop(buffer, readCount, waitRead, waitWrite, exception))
                    thread.start()
                    remainingBytes = fileLength
                    try {
                        var bytesRead: Int
                        var bufferIndex = 0
                        while (true) {
                            try {
                                bytesRead = fs.read(buffer[bufferIndex], 0, 65536)
                                waitWrite.waitOne()
                            } catch (ex: InterruptedException) {
                                bytesRead = -1
                                exception[0] = ex
                            } catch (ex: Exception) {
                                logError("sendFile", ex)
                                bytesRead = -1
                                exception[0] = ex
                            }
                            readCount[bufferIndex] = bytesRead
                            waitRead.set()
                            if (exception[0] != null) {
                                throw exception[0]!!
                            } else if (bytesRead <= 0) {
                                break
                            }
                            remainingBytes -= bytesRead.toLong()
                            bufferIndex = if ((bufferIndex == 1)) 0 else 1
                        }
                    } finally {
                        thread.interrupt()
                    }
                }
            } catch (ex: Exception) {
                logError("sendFile", ex, "file=$filePath")
                if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                    throw ex
                }
                status = "$syncStatusFAIL $ex"
                if (fileLength == -1L) {
                    writeLong(0)
                } else {
                    while (remainingBytes > 0) {
                        writeArray(
                            buffer[0],
                            if ((remainingBytes >= 65536)) 65536 else remainingBytes.toInt()
                        )
                        remainingBytes -= 65536
                    }
                }
            }
            writeString(status)
            flushWriter()
        }

        private inner class sendFileWriteLoop(
            private val buffer: Array<ByteArray>,
            private val readCount: IntArray,
            private val waitRead: AutoResetEvent,
            private val waitWrite: AutoResetEvent,
            private val exception: Array<Exception?>
        ) : Runnable {
            override fun run() {
                var bufferIndex = 0
                var bytesRead: Int
                try {
                    while (true) {
                        waitRead.waitOne()
                        bytesRead = readCount[bufferIndex]
                        if (bytesRead <= 0) {
                            break
                        }
                        writeArray(buffer[bufferIndex], bytesRead)
                        waitWrite.set()
                        bufferIndex = if ((bufferIndex == 1)) 0 else 1
                    }
                } catch (ex: InterruptedException) {
                    exception[0] = ex
                } catch (ex: Exception) {
                    exception[0] = ex
                    logError("sendFileLoop", ex)
                } finally {
                    waitWrite.set()
                }
            }
        }

        @Throws(Exception::class)
        private fun showDeleteConfirmation() {
            val deleteCount = readInt()
            syncPercentCompleted.set(readShort().toInt())
            readToEndOfCommand()
            if (showOkCancelDialog(
                    String.format(
                        getString(if ((deleteCount == 1)) R.string.syncDeleteConfirm1 else R.string.syncDeleteConfirm9),
                        deleteCount
                    )
                ) == android.R.string.ok
            ) {
                writeString(syncStatusOK)
            } else {
                syncErrorMessageId.set(R.string.syncCancelled)
                writeString(syncStatusCANCEL)
            }
            flushWriter()
        }

        private fun filePathToUri(filePath: String): Uri {
            var id: Long = 0
            val cr = context.contentResolver

            val uri = MediaStore.Files.getContentUri("external")
            val projection =
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.RELATIVE_PATH)
            //val selection = MediaStore.Files.FileColumns.DISPLAY_NAME
            val selectionArgs = arrayOf(
                filePath.substring(filePath.lastIndexOf('/') + 1),
                filePath.substring(0, filePath.lastIndexOf('/') + 1),
            )

            val cursor = cr.query(
                uri, projection,
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} = ? and ${MediaStore.Files.FileColumns.RELATIVE_PATH} = ?", selectionArgs, null
            )

            if (cursor != null) {
                cursor.moveToFirst()
                do {
                    val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    id = cursor.getString(idIndex).toLong()
                } while (cursor.moveToNext())
                cursor.close()
            }
            val return_uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.getContentUri("external"),
                id
            )
            return return_uri
        }

        @Throws(Exception::class)
        private fun deleteFiles() {
            syncPercentCompleted.set(readShort().toInt())
            readToEndOfCommand()
            val status: String = syncStatusOK
            var deleteUriList: MutableList<Uri> = ArrayList()
            while (true) {
                val filePath = readString()
                if (filePath.isEmpty()) {
                    break
                }
                if (WifiSyncServiceSettings.debugMode) {
                    Log.d("deleteFiles", "Delete: $filePath")
                }
                var failMessage: String? = null
                try {
                    syncProgressMessage.set(
                        String.format(
                            getString(R.string.syncFileActionDelete),
                            filePath
                        )
                    )
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("deleteFiles", "Call deleteFile: $filePath")
                    }
                    deleteUriList.add(filePathToUri(filePath))
                    writeString(status)
                    flushWriter()
                    if (deleteUriList.size > 50) {
                        val app = (application as WifiSyncApp)
                        storage!!.deleteFile(app, deleteUriList)
                        deleteUriList = ArrayList()
                    }
                } catch (ex: Exception) {
                    logError("deleteFile", ex, "file=$filePath")
                    if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                        throw ex
                    }
                    writeString(syncStatusFAIL)
                    flushWriter()
                    failMessage = ex.toString()
                }
                if (failMessage != null) {
                    syncFailedFiles.add(
                        FileErrorInfo(
                            FileErrorInfo.ERROR_DELETE,
                            filePath,
                            failMessage
                        )
                    )
                }
            }
            if (deleteUriList.size > 0) {
                val app = (application as WifiSyncApp)
                storage!!.deleteFile(app, deleteUriList)
            }
        }

        @Throws(Exception::class)
        private fun deleteFolders() {
            syncPercentCompleted.set(readShort().toInt())
            readToEndOfCommand()
            var status: String = syncStatusOK
            while (true) {
                val folderPath = readString()
                if (folderPath.isEmpty()) {
                    break
                }
                try {
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("deleteFolder", "Delete: $folderPath")
                    }
                    syncProgressMessage.set(
                        String.format(
                            getString(R.string.syncFileActionDelete),
                            folderPath
                        )
                    )
                    if (!storage!!.deleteFolder(folderPath)) {
                        status = syncStatusFAIL
                    }
                } catch (ex: Exception) {
                    logError("deleteFolder", ex, "path=$folderPath")
                    if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                        throw ex
                    }
                    status = syncStatusFAIL
                }
            }
            writeString(status)
            flushWriter()
        }

        private fun showOkCancelDialog(prompt: String): Int {
            try {
                resultsActivityReady.waitOne()
                val app = applicationContext as WifiSyncApp
                return showOkCancel((app.currentActivity)!!, prompt)
            } catch (ex: Exception) {
                logError("showOkCancel", ex)
                return android.R.string.cancel
            }
        }

        @Throws(Exception::class)
        private fun sendPlaylists() {
            val playlistsFolderPath = readString()
            readToEndOfCommand()
            try {
                var files: ArrayList<FileInfo>? = null
                when (settingsReverseSyncPlayer) {
                    WifiSyncServiceSettings.PLAYER_GONEMAD -> files =
                        storage!!.getFiles(playlistsFolderPath)
                    WifiSyncServiceSettings.PLAYER_POWERAMP -> /*
                            //Uri playlistsUri = Uri.parse("content://com.maxmpz.audioplayer.data/playlists");
                            //String[] projection = new String[] {"_id", "playlist_path", "playlist", "mtime", "num_files"};
                            Uri playlistsUri = Uri.parse("content://com.maxmpz.audioplayer.data/playlists/7/files");
                            String[] projection = new String[] {"folder_file_id", "sort"};
                            try (Cursor cursor = getContentResolver().query(playlistsUri, projection, null, null, null)) {
                                while (cursor.moveToNext()) {
                                    String id = cursor.getString(0);
                                    String path = cursor.getString(1);
                                    //String name = cursor.getString(2);
                                    //long mTime = cursor.getLong(3);
                                    //Date xx = new Date(mTime * 1000);
                                    //String aa = cursor.getString(5);
                                    String x = path + id;
                                }
                            }
*/
                        files = ArrayList()
                }
                for (info: FileInfo in files!!) {
                    writeString(storage!!.getDecodedUrl(info.filename))
                    writeLong(info.dateModified)
                }
                writeString(syncEndOfData)
                writeString(syncStatusOK)
            } catch (ex: Exception) {
                logError("sendPlaylists", ex)
                if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                    throw ex
                }
                setPreviewFailed()
                writeString(syncEndOfData)
                writeString("$syncStatusFAIL $ex")
            }
            flushWriter()
        }

        @Throws(Exception::class)
        private fun sendStats() {
            readToEndOfCommand()

            try {
                var cachedStatsLookup: FileStatsMap
                val statsCacheFile = File(filesDir, "CachedStats.dat")
                val statsCachedFileExists = (statsCacheFile.exists())
                if (!statsCachedFileExists) {
                    cachedStatsLookup = FileStatsMap(0)
                } else {
                    FileInputStream(statsCacheFile).use { stream ->
                        BufferedInputStream(stream, 4096).use { bufferedStream ->
                            DataInputStream(bufferedStream).use { reader ->
                                val count: Int = reader.readInt()
                                cachedStatsLookup = FileStatsMap(count)
                                for (index in 0 until count) {
                                    val filename: String = reader.readUTF()
                                    val rating: Byte = reader.readByte()
                                    val lastPlayedDate: Long = reader.readLong()
                                    val playCount: Int = reader.readInt()
                                    cachedStatsLookup[filename] =
                                        FileStatsInfo(filename, rating, lastPlayedDate, playCount)
                                }
                            }
                        }
                    }
                }
                if (settingsReverseSyncPlayer == 0) {
                    writeString(syncEndOfData)
                    writeString(syncStatusOK)
                    flushWriter()
                    return
                }
                var latestStats: ArrayList<FileStatsInfo>? = null
                when (settingsReverseSyncPlayer) {
                    WifiSyncServiceSettings.PLAYER_GONEMAD -> {
                        latestStats = loadGoneMadStats()
                        if (latestStats == null) {
                            syncErrorMessageId.set(R.string.errorSyncGoneMadTrial)
                        }
                    }
                    WifiSyncServiceSettings.PLAYER_POWERAMP -> latestStats = loadPowerAmpStats()
                }
                if (latestStats == null) {
                    setPreviewFailed()
                    logError(
                        "sendStats",
                        "Unable to retrieve stats for player: $settingsReverseSyncPlayer"
                    )
                    writeString(syncEndOfData)
                    writeString("$syncStatusFAIL Unable to retrieve stats")
                } else {
                    for (latestStatsInfo: FileStatsInfo in latestStats) {
                        var incrementalPlayCount: Int
                        var ratingChanged: Boolean
                        val cachedStatsInfo = cachedStatsLookup[latestStatsInfo.fileUrl]
                        if (cachedStatsInfo == null) {
                            if (!settingsReverseSyncRatings) {
                                ratingChanged = false
                                latestStatsInfo.rating = 0
                            } else {
                                ratingChanged =
                                    (statsCachedFileExists && latestStatsInfo.rating > 0)
                            }
                            if (!settingsReverseSyncPlayCounts) {
                                latestStatsInfo.playCount = 0
                                latestStatsInfo.lastPlayedDate = 0
                                incrementalPlayCount = 0
                            } else {
                                incrementalPlayCount = latestStatsInfo.playCount
                            }
                        } else {
                            if (!settingsReverseSyncRatings) {
                                ratingChanged = false
                                latestStatsInfo.rating = cachedStatsInfo.rating
                            } else {
                                ratingChanged =
                                    (latestStatsInfo.rating > 0 && (latestStatsInfo.rating != cachedStatsInfo.rating))
                            }
                            if (!settingsReverseSyncPlayCounts) {
                                latestStatsInfo.playCount = cachedStatsInfo.playCount
                                latestStatsInfo.lastPlayedDate = cachedStatsInfo.lastPlayedDate
                                incrementalPlayCount = 0
                            } else {
                                incrementalPlayCount =
                                    latestStatsInfo.playCount - cachedStatsInfo.playCount
                            }
                        }
                        if (ratingChanged || incrementalPlayCount > 0) {
                            writeString(latestStatsInfo.fileUrl)
                            writeByte(if ((!settingsReverseSyncRatings || !ratingChanged)) 0 else latestStatsInfo.rating.toInt())
                            writeLong(if ((!settingsReverseSyncPlayCounts)) 0 else latestStatsInfo.lastPlayedDate)
                            writeInt(if ((!settingsReverseSyncPlayCounts || incrementalPlayCount <= 0)) 0 else incrementalPlayCount)
                        }
                    }
                    if (!settingsSyncPreview) {
                        FileOutputStream(statsCacheFile).use { stream ->
                            BufferedOutputStream(stream, 4096).use { bufferedStream ->
                                DataOutputStream(bufferedStream).use { writer ->
                                    val count: Int = latestStats.size
                                    writer.writeInt(count)
                                    for (index in 0 until count) {
                                        val stats: FileStatsInfo = latestStats[index]
                                        writer.writeUTF(stats.fileUrl)
                                        writer.writeByte(stats.rating.toInt())
                                        writer.writeLong(stats.lastPlayedDate)
                                        writer.writeInt(stats.playCount)
                                    }
                                    writer.flush()
                                }
                            }
                        }
                    }
                    writeString(syncEndOfData)
                    writeString(syncStatusOK)
                }
            } catch (ex: Exception) {
                Log.d("sendStats", ex.message!!)
                if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                    throw ex
                }
                setPreviewFailed()
                writeString(syncEndOfData)
                writeString("$syncStatusFAIL $ex")
            }
            flushWriter()
        }

        private fun loadPowerAmpStats(): ArrayList<FileStatsInfo> {
            val storageRootPath = storage!!.storageRootPath
            val results = ArrayList<FileStatsInfo>()
            val contentResolver = contentResolver
            val projection = arrayOf(
                "folders.path",
                "folder_files.name",
                "folder_files.rating",
                "folder_files.played_times",
                "folder_files.played_at"
            )
            contentResolver.query(
                Uri.parse("content://com.maxmpz.audioplayer.data/files"),
                projection,
                null,
                null,
                null
            ).use { cursor ->
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        var filePath: String = cursor.getString(0) + cursor.getString(1)
                        if (filePath.regionMatches(
                                0,
                                storageRootPath,
                                0,
                                storageRootPath.length,
                                ignoreCase = true
                            )
                        ) {
                            filePath = filePath.substring(storageRootPath.length + 1)
                        }
                        val rating: Byte = (cursor.getInt(2) * 20).toByte()
                        val playCount: Int = cursor.getInt(3)
                        val lastPlayed: Long = cursor.getLong(4)
                        results.add(FileStatsInfo(filePath, rating, lastPlayed, playCount))
                    }
                }
            }
            return results
        }

        private fun getPathFromUri(context: Context, uri: Uri): String? {
            // DocumentProvider
            // Log.e("Error:", "uri:" + uri.authority)
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if ("com.android.externalstorage.documents" == uri.authority) { // ExternalStorageProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    return if ("primary".equals(type, ignoreCase = true)) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    } else {
                        "/stroage/" + type + "/" + split[1]
                    }
                } else if ("com.android.providers.downloads.documents" == uri.authority) { // DownloadsProvider
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), id.toLong()
                    )
                    return getDataColumn(context, contentUri, null, null)
                } else if ("com.android.providers.media.documents" == uri.authority) { // MediaProvider
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val type = split[0]
                    val contentUri: Uri? = MediaStore.Files.getContentUri("external")
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(
                        split[1]
                    )
                    return getDataColumn(context, contentUri, selection, selectionArgs)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) { //MediaStore
                return getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
                return uri.path
            }
            return null
        }

        private fun getDataColumn(
            context: Context, uri: Uri?, selection: String?,
            selectionArgs: Array<String>?
        ): String? {
            var cursor: Cursor? = null
            val projection = arrayOf(
                MediaStore.Files.FileColumns.DATA
            )
            try {
                cursor = context.contentResolver.query(
                    uri!!, projection, selection, selectionArgs, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val cindex = cursor.getColumnIndexOrThrow(projection[0])
                    return cursor.getString(cindex)
                }
            } finally {
                cursor?.close()
            }
            return null
        }

        @Throws(Exception::class)
        private fun loadGoneMadStats(): ArrayList<FileStatsInfo>? {
            val sharedPref = getSharedPreferences("kim.tkland.musicbeewifisync.sharedpref", MODE_PRIVATE)
            val uriStr = sharedPref.getString("accesseduri", "")!!
            val targetUri = uriStr.toUri()

            val fileWriteWait = Object()
            val fileWriteStarted = AtomicBoolean(false)
            val fileWriteCompleted = AtomicBoolean(false)

            val observer: FileObserver = object : FileObserver(File(getPathFromUri(applicationContext, targetUri)!!)) {
                override fun onEvent(event: Int, path: String?) {
                    when (event) {
                        OPEN -> fileWriteStarted.set(true)
                        CLOSE_WRITE -> synchronized(fileWriteWait) {
                            fileWriteCompleted.set(true)
                            fileWriteWait.notifyAll()
                        }
                    }
                }
            }
            observer.startWatching()
            try {
                val serviceIntent = Intent()
                serviceIntent.component =
                    ComponentName("gonemad.gmmp", "gonemad.gmmp.receivers.BackupReceiver")
                serviceIntent.action = "gonemad.gmmp.action.BACKUP_STATS"
                sendBroadcast(serviceIntent)
                synchronized(fileWriteWait) {
                    fileWriteWait.wait(10000)
                    if (!fileWriteStarted.get()) {
                        return null
                    } else if (!fileWriteCompleted.get()) {
                        fileWriteWait.wait(60000)
                    }
                }
            } finally {
                observer.stopWatching()
            }

            lateinit var importdb: InputStream
            val fcd = contentResolver.openFileDescriptor(targetUri,"r")
            if (fcd != null) {
                FileInputStream(fcd.fileDescriptor).also { importdb = it }
                importdb.use {
                    val handler = GmmpStatsXmlHandler(storage!!.storageRootPath)
                    SAXParserFactory.newInstance().newSAXParser().parse(it, handler)
                    fcd.close()
                    return handler.stats
                }
            }
            return null
        }

        inner class GmmpStatsXmlHandler internal constructor(val storageRootPath: String) :
            DefaultHandler() {
            var stats = ArrayList<FileStatsInfo>()
            private var lastName: String? = null
            private var lastFileUrl = ""
            private var lastRating: Byte = 0
            private var lastPlayedDate: Long = 0
            private var lastPlayCount = 0
            override fun startElement(
                uri: String,
                localName: String,
                qName: String,
                attributes: Attributes
            ) {
                lastName = qName
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                if (qName.equals("File", ignoreCase = true)) {
                    if (lastFileUrl.regionMatches(
                            0,
                            storageRootPath,
                            0,
                            storageRootPath.length,
                            ignoreCase = true
                        )
                    ) {
                        lastFileUrl = lastFileUrl.substring(storageRootPath.length + 1)
                    }
                    stats.add(FileStatsInfo(lastFileUrl, lastRating, lastPlayedDate, lastPlayCount))
                    lastFileUrl = ""
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (lastName.equals("Uri", ignoreCase = true)) {
                    lastFileUrl += String(ch, start, length)
                } else if (lastName.equals("Rating", ignoreCase = true)) {
                    lastRating = (java.lang.Float.valueOf(String(ch, start, length)) * 20).toInt().toByte()
                } else if (lastName.equals("LastPlayed", ignoreCase = true)) {
                    lastPlayedDate = (java.lang.Long.valueOf(String(ch, start, length)))
                } else if (lastName.equals("Playcount", ignoreCase = true)) {
                    lastPlayCount = Integer.valueOf(String(ch, start, length))
                }
            }
        }

        inner class FileStatsMap internal constructor(initialCapacity: Int) :
            HashMap<String, FileStatsInfo?>(initialCapacity) {
            override fun put(key: String, value: FileStatsInfo?): FileStatsInfo? {
                return super.put(key.lowercase(), value)
            }

            override fun get(key: String): FileStatsInfo? {
                return super.get(key.lowercase())
            }

            override fun containsKey(key: String): Boolean {
                return super.containsKey(key.lowercase())
            }
        }

        @Suppress("REDUNDANT_MODIFIER_IN_GETTER")
        @get:Throws(Exception::class)
        private val reverseSyncPreviewResults: Unit
            private get() {
                syncToResults = null
                var results: ArrayList<SyncResultsInfo>? = ArrayList()
                var status: String
                try {
                    readToEndOfCommand()
                    while (true) {
                        val action = readString()
                        if (action.isEmpty()) {
                            break
                        }
                        val targetName = readString()
                        val alert = readByte()
                        val message = readString()
                        results!!.add(SyncResultsInfo(action, targetName, alert.toShort(), message))
                    }
                    status = syncStatusOK
                } catch (ex: Exception) {
                    logError("getPreview", ex)
                    if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                        throw ex
                    }
                    syncErrorMessageId.set(R.string.errorSyncNonSpecific)
                    results = null
                    status = "$syncStatusFAIL $ex"
                }
                writeString(status)
                flushWriter()
                syncToResults = results
            }

        @Throws(Exception::class)
        private fun showSyncPreview() {
            syncFromResults = null
            val results = ArrayList<SyncResultsInfo>()
            try {
                readToEndOfCommand()
                readLong() //deltaSpace =
                readLong() //estimatedBytesSendToDevice =
                while (true) {
                    val action = readString()
                    if (action.isEmpty()) {
                        break
                    }
                    val estimatedSize = readString()
                    val targetFilename = readString()
                    results.add(SyncResultsInfo(action, targetFilename, estimatedSize))
                }
                showSyncPreviewResults(results, syncStatusOK)
            } catch (ex: Exception) {
                logError("showPreview", ex)
                if (SocketException::class.java.isAssignableFrom(ex.javaClass)) {
                    throw ex
                }
                syncErrorMessageId.set(R.string.errorSyncNonSpecific)
                showSyncPreviewResults(null, "$syncStatusFAIL $ex")
            }
        }

        @Throws(Exception::class)
        private fun showSyncPreviewResults(results: ArrayList<SyncResultsInfo>?, status: String) {
            writeString(status)
            flushWriter()
            clientSocket!!.shutdownInput()
            syncFromResults = results
            waitSyncResults.set()
        }
    }

    internal class ServerPinger : Runnable {
        private var connected = false
        private val address: InetAddress
        private val scannedCount: AtomicInteger
        private val waitLock: AutoResetEvent?
        private val candidateAddresses: ArrayList<CandidateIpAddress>?

        private constructor(address: InetAddress) {
            this.address = address
            scannedCount = AtomicInteger()
            waitLock = null
            candidateAddresses = null
        }

        internal constructor(
            address: InetAddress,
            waitLock: AutoResetEvent,
            scannedCount: AtomicInteger,
            candidateAddresses: ArrayList<CandidateIpAddress>
        ) {
            this.address = address
            this.scannedCount = scannedCount
            this.waitLock = waitLock
            this.candidateAddresses = candidateAddresses
        }

        override fun run() {
            try {
                Socket().use { clientSocket ->
                    clientSocket.connect(
                        InetSocketAddress(address, serverPort),
                        socketConnectTimeout
                    )
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("ping", "socket ok=$address")
                    }
                    try {
                        clientSocket.getInputStream().use { socketInputStream ->
                            DataInputStream((socketInputStream)).use { socketStreamReader ->
                                clientSocket.getOutputStream().use { socketOutputStream ->
                                    DataOutputStream(socketOutputStream).use { socketStreamWriter ->
                                        clientSocket.setSoTimeout(socketReadTimeout)
                                        val hello: String = socketStreamReader.readUTF()
                                        if (WifiSyncServiceSettings.debugMode) {
                                            logInfo("ping", "hello=$hello")
                                        }
                                        if (hello.startsWith(serverHelloPrefix)) {
                                            socketStreamWriter.writeUTF(clientHelloVersion)
                                            socketStreamWriter.writeUTF("Ping")
                                            socketStreamWriter.writeByte(0)
                                            socketStreamWriter.writeUTF(WifiSyncServiceSettings.deviceName)
                                            socketStreamWriter.writeByte(WifiSyncServiceSettings.deviceStorageIndex)
                                            socketStreamWriter.writeUTF(syncEndOfData)
                                            socketStreamWriter.flush()
                                            val status: String = socketStreamReader.readUTF()
                                            connected = true
                                            if (WifiSyncServiceSettings.debugMode) {
                                                logInfo(
                                                    "ping",
                                                    "matched=$address,status=$status"
                                                )
                                            }
                                            if (candidateAddresses != null) {
                                                synchronized(candidateAddresses) {
                                                    candidateAddresses.add(
                                                        CandidateIpAddress(
                                                            address as Inet4Address,
                                                            (status == "OK")
                                                        )
                                                    )
                                                    waitLock!!.set()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        logError("ping", ex)
                    }
                }
            } catch (ex: Exception) {
                Log.d("run(1807)", ex.message!!)
            } finally {
                if (candidateAddresses != null) {
                    if (scannedCount.incrementAndGet() == 253) {
                        waitLock!!.set()
                    }
                }
            }
        }

        companion object {
            fun ping(): Boolean {
                try {
                    val pinger =
                        ServerPinger(InetAddress.getByName(WifiSyncServiceSettings.defaultIpAddressValue))
                    pinger.run()
                    return pinger.connected
                } catch (ex: Exception) {
                    return false
                }
            }
        }
    }

    class CandidateIpAddress internal constructor(
        val address: Inet4Address,
        val syncConfigMatched: Boolean
    ) {
        override fun toString(): String {
            return address.toString().substring(1)
        }
    }

    inner class FileStatsInfo internal constructor(
        val fileUrl: String,
        var rating: Byte,
        var lastPlayedDate: Long,
        var playCount: Int
    )

    companion object {
        val syncIsRunning = AtomicBoolean()
        val syncPercentCompleted = AtomicInteger()
        val syncErrorMessageId = AtomicInteger()
        val syncProgressMessage = AtomicReference("")
        val syncFailedFiles = ArrayList<FileErrorInfo>()
        val resultsActivityReady = AutoResetEvent(false)
        var syncIteration = 0

        @Volatile
        var syncToResults: ArrayList<SyncResultsInfo>? = null

        @Volatile
        var syncFromResults: ArrayList<SyncResultsInfo>? = null
        val waitSyncResults = AutoResetEvent(false)
        private const val socketConnectTimeout = 10000000
        private const val socketReadTimeout = 30000000
        private const val socketReadBufferLength = 131072
        private const val FOREGROUND_ID = 2938
        private const val serverPort = 27304
        private const val intentNameDefaultIpAddressValue = "defaultIpAddressValue"
        private const val intentNameDeviceName = "deviceName"
        private const val intentNameDeviceStorageIndex = "deviceStorageIndex"
        private const val intentNameAccessPermissionsUri = "accessPermissionsUri"
        private const val intentNameSyncIteration = "syncIteration"
        private const val intentNameSyncPreview = "syncPreview"
        private const val intentNameSyncFromMusicBee = "syncFromMusicBee"
        private const val intentNameSyncCustomFiles = "syncCustomFiles"
        private const val intentNameSyncDeleteUnselectedFiles = "syncDeleteUnselectedFiles"
        private const val intentNameSyncCustomPlaylistNames = "syncCustomPlaylistNames"
        private const val intentNameSyncIgnoreErrors = "syncIgnoreErrors"
        private const val intentNameReverseSyncPlayer = "reverseSyncPlayer"
        private const val intentNameReverseSyncPlaylists = "reverseSyncPlaylists"
        private const val intentNameReverseSyncPlaylistsPath = "reverseSyncPlaylistsPath"
        private const val intentNameReverseSyncRatings = "reverseSyncRatings"
        private const val intentNameReverseSyncPlayCounts = "reverseSyncPlayCounts"
        private const val commandSyncDevice = "SyncDevice"
        private const val commandSyncToDevice = "SyncToDevice"
        private const val syncStatusOK = "OK"
        private const val syncStatusFAIL = "FAIL"
        private const val syncStatusCANCEL = "CANCEL"
        private const val syncEndOfData = ""
        private const val serverHelloPrefix = "MusicBeeWifiSyncServer/"
        private const val clientHelloVersion = "MusicBeeWifiSyncClient/1.0"

        fun startSynchronisation(
            context: Context,
            iteration: Int,
            syncPreview: Boolean,
            syncIgnoreErrors: Boolean
        ) {
            if (WifiSyncServiceSettings.debugMode) {
                logInfo("startSync", "preview=$syncPreview,iteration=$iteration")
            }
            var intent: Intent
            if (syncPreview) {
                waitSyncResults.reset()
                intent = Intent(context, SyncResultsPreviewActivity::class.java)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
            } else {
                intent = Intent(context, SyncResultsStatusActivity::class.java)
                intent.flags = FLAG_ACTIVITY_NEW_TASK
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)

            intent = Intent()
            intent.setClass(context, WifiSyncService::class.java)
            intent.action = context.getString(R.string.actionSyncStart)
            intent.putExtra(
                intentNameDefaultIpAddressValue,
                WifiSyncServiceSettings.defaultIpAddressValue
            )
            intent.putExtra(intentNameDeviceName, WifiSyncServiceSettings.deviceName)
            intent.putExtra(
                intentNameDeviceStorageIndex,
                WifiSyncServiceSettings.deviceStorageIndex
            )
            intent.putExtra(
                intentNameAccessPermissionsUri,
                WifiSyncServiceSettings.accessPermissionsUri.get()
            )
            intent.putExtra(intentNameSyncIteration, iteration)
            intent.putExtra(intentNameSyncPreview, syncPreview)
            intent.putExtra(intentNameSyncFromMusicBee, WifiSyncServiceSettings.syncFromMusicBee)
            intent.putExtra(intentNameSyncCustomFiles, WifiSyncServiceSettings.syncCustomFiles)
            intent.putStringArrayListExtra(
                intentNameSyncCustomPlaylistNames,
                WifiSyncServiceSettings.syncCustomPlaylistNames
            )
            intent.putExtra(
                intentNameSyncDeleteUnselectedFiles,
                WifiSyncServiceSettings.syncDeleteUnselectedFiles
            )
            intent.putExtra(intentNameSyncIgnoreErrors, syncIgnoreErrors)
            intent.putExtra(intentNameReverseSyncPlayer, WifiSyncServiceSettings.reverseSyncPlayer)
            intent.putExtra(
                intentNameReverseSyncPlaylists,
                WifiSyncServiceSettings.reverseSyncPlaylists
            )
            intent.putExtra(
                intentNameReverseSyncPlaylistsPath,
                WifiSyncServiceSettings.reverseSyncPlaylistsPath
            )
            intent.putExtra(
                intentNameReverseSyncRatings,
                WifiSyncServiceSettings.reverseSyncRatings
            )
            intent.putExtra(
                intentNameReverseSyncPlayCounts,
                WifiSyncServiceSettings.reverseSyncPlayCounts
            )
            context.startService(intent)
        }

        @get:Throws(Exception::class)
        val musicBeePlaylists: ArrayList<String>
            get() {
                val playlists = ArrayList<String>()
                val address = InetAddress.getByName(WifiSyncServiceSettings.defaultIpAddressValue)
                Socket().use { clientSocket ->
                    clientSocket.connect(
                        InetSocketAddress(address, serverPort),
                        socketConnectTimeout
                    )
                    clientSocket.getInputStream().use { socketInputStream ->
                        DataInputStream((socketInputStream)).use { socketStreamReader ->
                            clientSocket.getOutputStream().use { socketOutputStream ->
                                DataOutputStream(socketOutputStream).use { socketStreamWriter ->
                                    clientSocket.setSoTimeout(socketReadTimeout)
                                    val hello: String = socketStreamReader.readUTF()
                                    if (hello.startsWith(serverHelloPrefix)) {
                                        socketStreamWriter.writeUTF(clientHelloVersion)
                                        socketStreamWriter.writeUTF("GetPlaylists")
                                        socketStreamWriter.writeUTF(syncEndOfData)
                                        socketStreamWriter.flush()
                                        while (true) {
                                            val playlistName: String = socketStreamReader.readUTF()
                                            if (playlistName.isEmpty()) {
                                                break
                                            }
                                            playlists.add(playlistName)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return playlists
            }

        fun getMusicBeeServerAddress(context: Context?, serverIP: InetAddress?): String? {
            val ipProvider: IpAddressProvider = IpAddressProviderImpl((context)!!, serverIP)
            val candidateAddresses = findCandidateIpAddresses(ipProvider)
            if (candidateAddresses.size == 0) {
                return null
            } else {
                val candidate = candidateAddresses[0]
                return if ((!candidate.syncConfigMatched)) "FAIL" else candidate.toString()
            }
        }

        fun findCandidateIpAddresses(ipProvider: IpAddressProvider): ArrayList<CandidateIpAddress> {
            val candidateAddresses = ArrayList<CandidateIpAddress>()
            try {
                if (WifiSyncServiceSettings.debugMode) {
                    logInfo(
                        "locate",
                        "search=" + ipProvider.ipSearchPrefix + ", exclude=" + ipProvider.deviceAddress!!.hostAddress
                    )
                }
                val waitLock = AutoResetEvent(false)
                val scannedCount = AtomicInteger(0)
                val threads = ArrayList<Thread>(200)
//                var ipAddr: InetAddress
                for (ipAddr in ipProvider) {
                    val tmp =
                        Thread(ServerPinger(ipAddr!!, waitLock, scannedCount, candidateAddresses))
                    threads.add(tmp)
                    tmp.start()
                }
                waitLock.waitOne()
                for (thread: Thread in threads) {
                    thread.interrupt()
                }
            } catch (ex: Exception) {
                logError("locate", ex)
            }
            return candidateAddresses
        }
    }
}

class AutoResetEvent(@field:Volatile private var open: Boolean) {
    private val monitor = Object()
    fun set() {
        synchronized(monitor) {
            open = true
            monitor.notifyAll()
        }
    }

    fun reset() {
        open = false
    }

    @Throws(InterruptedException::class)
    fun waitOne() {
        synchronized(monitor) {
            while (!open) {
                monitor.wait()
            }
            reset()
        }
    }
}

internal class FileStorageAccess(
    private val context: Context,
    val storageRootPath: String,
    private val storageRootPermissionedUri: Uri?
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private var rootId: String? = null
    private val isDocumentFileStorage: Boolean = (storageRootPermissionedUri != null)
    private val fileScanCount = AtomicInteger(0)
    private val fileScanWait = Object()
//    private val updateFiles = ArrayList<FileInfo>()
//    private val deleteFileUrls = ArrayList<String>()
    private val updatePlaylists = ArrayList<FileInfo>()
    private val deletePlaylistUrls = ArrayList<String>()

    init {
        if (!isDocumentFileStorage) {
            rootId = null
        } else {
            val segment = storageRootPermissionedUri!!.lastPathSegment
            rootId = segment!!.substring(0, segment.indexOf(':') + 1)
        }
        if (WifiSyncServiceSettings.debugMode) {
            logInfo(
                "storage",
                "path=" + storageRootPath + ",root=" + (if ((rootId == null)) "null" else rootId + ",uri=" + storageRootPermissionedUri.toString())
            )
        }
    }

    fun getFiles(folderPath: String): ArrayList<FileInfo> {
        val files = ArrayList<FileInfo>()
        if (isDocumentFileStorage) {
            val folderUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                storageRootPermissionedUri,
                getDocumentId(folderPath)
            )
            contentResolver.query(folderUri, arrayOf("document_id"), null, null, null)
                .use { cursor ->
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            val documentId: String = cursor.getString(0)
                            val file: DocumentFile? = DocumentFile.fromSingleUri(
                                context,
                                DocumentsContract.buildDocumentUriUsingTree(folderUri, documentId)
                            )
                            if (file!!.isFile) {
                                val fileUrl: String = file.uri.toString()
                                files.add(
                                    FileInfo(
                                        folderPath + fileUrl.substring(
                                            fileUrl.lastIndexOf(
                                                "%2F"
                                            ) + 3
                                        ), file.lastModified() / 1000
                                    )
                                )
                            }
                        }
                    }
                }
        } else {
            val folder = File(getFileUrl(folderPath))
            val folderFiles = folder.listFiles()
            if (folderFiles != null) {
                val storageRootPathLength = storageRootPath.length + 1
                for (file: File in folderFiles) {
                    if (file.isFile) {
                        files.add(
                            FileInfo(
                                file.path.substring(storageRootPathLength),
                                file.lastModified() / 1000
                            )
                        )
                    }
                }
            }
        }
        return files
    }

    @Throws(Exception::class)
    fun openReadStream(filePath: String): FileInputStream {
        if (isDocumentFileStorage) {
            val fileUri = createDocumentFile(filePath)
            return contentResolver.openAssetFileDescriptor((fileUri)!!, "r", null)!!
                .createInputStream()
        } else {
            return FileInputStream(File(getFileUrl(filePath)))
        }
    }

    /*
    @Throws(Exception::class)
    fun openWriteStream(filePath: String): FileOutputStream {
        if (isDocumentFileStorage) {
            val fileUri = createDocumentFile(filePath)
            return contentResolver.openAssetFileDescriptor((fileUri)!!, "wt", null)!!
                .createOutputStream()
        } else {
            val file = File(getFileUrl(filePath))
            val parent = file.parentFile
            if (!parent!!.exists() && !parent.mkdirs()) {
                logError("openWriteStream", "Unable to create folder: " + parent.path)
                throw FileNotFoundException()
            }
            return FileOutputStream(file)
        }
    }
     */

    fun getLength(filePath: String): Long {
        return if (isDocumentFileStorage) {
            DocumentFile.fromSingleUri(context, getDocumentFileUri(filePath))!!.length()
        } else {
            File(getFileUrl(filePath)).length()
        }
    }

    @Throws(Exception::class)
    private fun createDocumentFile(filePath: String): Uri? {
        val charIndex = filePath.lastIndexOf('/')
        if (charIndex <= 0) {
            val message = "Invalid filename: $filePath"
            logError("createFile", message)
            throw InvalidObjectException(message)
        }
        val mime = "application/octet-stream"
        val name = filePath.substring(charIndex + 1)
        val fileUri = getDocumentFileUri(filePath)
        if (documentFileExists(fileUri)) {
            return fileUri
        } else {
            val parentPath = filePath.substring(0, charIndex)
            val folderUri = createDocumentFolder(parentPath)
            try {
                return DocumentsContract.createDocument(contentResolver, (folderUri)!!, mime, name)
            } catch (ex: Exception) {
                logError(
                    "createFile",
                    "$filePath: $ex, folder=" + (folderUri?.toString()
                        ?: "null")
                )
                throw ex
            }
        }
    }

    @Throws(Exception::class)
    private fun createDocumentFolder(folderPath: String): Uri? {
        val folderUri = getDocumentFileUri(folderPath)
        if (documentFileExists(folderUri)) {
            return folderUri
        } else {
            val charIndex = folderPath.lastIndexOf('/')
            val parentPath: String
            if (charIndex == -1) {
                parentPath = ""
            } else {
                parentPath = folderPath.substring(0, charIndex)
                createDocumentFolder(parentPath)
            }
            try {
                return DocumentsContract.createDocument(
                    contentResolver,
                    getDocumentFileUri(parentPath),
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    folderPath.substring(charIndex + 1)
                )
            } catch (ex: Exception) {
                logError("createFolder", "$folderPath: $ex")
                throw ex
            }
        }
    }

    private fun documentFileExists(fileUri: Uri): Boolean {
        try {
            contentResolver.query(
                fileUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor == null) {
                    return false
                } else {
                    return (cursor.count > 0)
                }
            }
        } catch (ex: IllegalArgumentException) {
            // ignore file not found errors
        } catch (ex: Exception) {
            // not completely comfortable ignoring if this was to happen, but it replicates the Android implementation
            //if (BuildConfig.DEBUG) logInfo("exists", "$fileUri: $ex")
        }
        return false
    }

    fun deleteFile(app: WifiSyncApp, fileUris: List<Uri>): Boolean {
        try {
            try {
                app.delete(fileUris.toTypedArray(), -777)
                for (uri in fileUris) {
                    val file:File? = uriToFile(app , uri)
                    if (file!!.path.indexOf("Music/") > 0) {
                        val filePath = file.path.substring(file.path.indexOf("Music/"))
                        deleteFile(filePath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        } catch (e: Exception) {
            Log.d("deleteFile:", e.message!!)

        }
        return false
    }

    private fun uriToFile(app: WifiSyncApp, uri: Uri): File? {
        val context: Context = app.applicationContext
        val scheme = uri.scheme

        var path: String? = null
        if ("file" == scheme) {
            path = uri.path
        } else if ("content" == scheme) {
            val contentResolver = context.contentResolver
            val cursor =
                contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                path = cursor.getString(0)
                cursor.close()
            }
        }
        return if (null == path) null else File(path)
    }

    fun updateFile(app: WifiSyncApp, fileUri: Uri): Boolean {
        try {
            val updateList = arrayOf(fileUri)
            // Log.d("targetUri:", "Update target Uri: $fileUri")

            try {
                app.update(updateList, 888)
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        } catch (e: Exception) {
            Log.d("updateFile:", e.message!!)
        }
        return false
    }

    @Throws(Exception::class)
    fun deleteFile(filePath: String): Boolean {
        val fileUrl = getFileUrl(filePath)
        if (isDocumentFileStorage) {
            val fileUri = getDocumentFileUri(filePath)
            if (!documentFileExists(fileUri)) {
                return true
            }
            try {
                if (DocumentsContract.deleteDocument(contentResolver, fileUri)) {
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("deleteFile", "Delete: $filePath : true Finished")
                    }
                    return true
                } else {
                    if (WifiSyncServiceSettings.debugMode) {
                        logInfo("deleteFile", "Delete: $filePath : false")
                    }
                    return !documentFileExists(fileUri)
                }
            } catch (ex: Exception) {
                if (documentFileExists(fileUri)) {
                    throw ex
                } else {
                    return true
                }
            }
        } else {
            val file = File(fileUrl)
            if (file.delete()) {
                scanFile(filePath, 0, 0, ACTION_DELETE)
                return true
            } else {
                return !file.exists()
            }
        }
    }

    @Throws(Exception::class)
    fun deleteFolder(folderPath: String): Boolean {
        val folderUrl = getFileUrl(folderPath)
        if (isDocumentFileStorage) {
            val folderUri = getDocumentFileUri(folderPath)
            return try {
                DocumentsContract.deleteDocument(contentResolver, folderUri)
            } catch (ex: Exception) {
                if (documentFileExists(folderUri)) {
                    throw ex
                } else {
                    true
                }
            }
        } else {
            val folder = File(folderUrl)
            if (!deleteFolderFiles(folder)) {
                return false
            } else if (folder.delete()) {
                scanFile(folderPath, 0, 0, ACTION_DELETE)
                return true
            } else {
                return !folder.exists()
            }
        }
    }

    private fun deleteFolderFiles(folder: File): Boolean {
        for (file: File in folder.listFiles()!!) {
            if (file.isDirectory) {
                deleteFolderFiles(file)
            }
            if (!file.delete()) {
                return false
            }
            scanFile(file.path, 0, 0, ACTION_DELETE)
        }
        return true
    }

    private fun isPlaylistFile(filePath: String): Boolean {
        val charIndex = filePath.lastIndexOf('.')
        if (charIndex != -1) {
            val ext = filePath.substring(charIndex)
            return (ext.equals(".m3u", ignoreCase = true) || ext.equals(".m3u8", ignoreCase = true) || ext.equals(".wpl", ignoreCase = true))
        }
        return false
    }

    fun getFileUrl(filePath: String): String {
        var fP = filePath
        if (fP.regionMatches(
                0,
                storageRootPath,
                0,
                storageRootPath.length,
                ignoreCase = true
            )
        ) {
            return fP
        } else {
            if (fP.startsWith("/")) {
                fP = fP.substring(1)
            }
            return if (storageRootPath.endsWith("/")) {
                storageRootPath + fP
            } else {
                "$storageRootPath/$fP"
            }
        }
    }

    private fun getDocumentId(filePath: String): String {
        return if (!filePath.startsWith("/")) {
            rootId + filePath
        } else {
            rootId + filePath.substring(1)
        }
    }

    private fun getDocumentFileUri(filePath: String): Uri {
        return DocumentsContract.buildDocumentUriUsingTree(
            storageRootPermissionedUri,
            getDocumentId(filePath)
        )
    }

    @Throws(UnsupportedEncodingException::class)
    fun getDecodedUrl(url: String): String {
        return if (!isDocumentFileStorage) {
            url
        } else {
            URLDecoder.decode(url, "UTF-8")
        }
    }

    fun scanFile(filePath: String, fileLength: Long, fileDateModified: Long, action: Int) {
        if (isDocumentFileStorage) {
            return
        }
        val fileUrl = getFileUrl(filePath)
        if (isPlaylistFile(filePath)) {
            if (action == ACTION_DELETE) {
                deletePlaylistUrls.add(filePath)
            } else {
                updatePlaylists.add(FileInfo(filePath, fileLength, fileDateModified))
            }
        } else {
            fileScanCount.getAndIncrement()
            MediaScannerConnection.scanFile(
                context,
                arrayOf(fileUrl),
                null
            ) { _, _ ->
                fileScanCount.getAndDecrement()
                synchronized(fileScanWait) { fileScanWait.notifyAll() }
            }
        }
    }

    fun waitScanFiles() {
        if (WifiSyncServiceSettings.debugMode) {
            logInfo("waitScanFiles", "start")
        }
        try {
            if (isDocumentFileStorage) {
                // there isnt any API I know of to determine if the media-scanner is still running for document storage
                //String[] projection = new String[] {"count(*)"};
                val projection = arrayOf("count(" + MediaStore.Files.FileColumns._ID + ")")
                var lastDatabaseFileCount = -1
                for (retryCount in 0..29) {
                    var count = -1
                    contentResolver.query(
                        //MediaStore.Audio.Media.getContentUri(MediaStore.getExternalVolumeNames(context).toTypedArray()[1]),
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        projection,
                        null,
                        null,
                        null
                    ).use { cursor ->
                        if (cursor != null) {
                            cursor.moveToFirst()
                            count = cursor.getInt(0)
                        }
                    }
                    if (count == lastDatabaseFileCount) {
                        break
                    }
                    lastDatabaseFileCount = count
                    Thread.sleep(1000)
                }
            } else {
                while (fileScanCount.get() > 0) {
                    synchronized(fileScanWait) { fileScanWait.wait() }
                }
            }
        } catch (ex: Exception) {
            logError("waitScan", ex)
        }
        if (WifiSyncServiceSettings.debugMode) {
            logInfo("waitScanFiles", "done")
        }
    }

    companion object {
        const val ACTION_ADD = 0
        const val ACTION_DELETE = 1
        fun getSdCardFromIndex(context: Context, idx: Int): File? {
            //var index = idx
            try {
                val file = context.getExternalFilesDirs(Environment.DIRECTORY_MUSIC)[idx - 1]
                val path = file.path

                return File(path.substring(0, path.indexOf("/Android/")))
                /*
                for (root: File in storageRootPaths) {
                    var path = root.path
                    val charIndex = path.indexOf("/Android/")
                    if (charIndex != -1) {
                        if (index > 1) {
                            index -= 1
                        } else {
                            path = path.substring(0, charIndex)
                            return File(path)
                        }
                    }
                }*/
            } catch (ex: Exception) {
                logError("getSdCards", ex)
            }
            return null
        }
    }
}

class SyncResultsInfo {
    val direction: Short
    val action: String?
    val targetName: String?
    val estimatedSize: String?
    val alert: Short
    val message: String?

    constructor(message: String?) {
        direction = DIRECTION_NONE
        action = null
        targetName = null
        estimatedSize = null
        alert = ALERT_INFO
        this.message = message
    }

    constructor(action: String?, targetName: String?, estimateSize: String?) {
        direction = DIRECTION_FORWARD_SYNC
        this.action = action
        this.targetName = targetName
        estimatedSize = estimateSize
        alert = ALERT_INFO
        message = null
    }

    constructor(targetName: String?, message: String?) {
        direction = DIRECTION_FORWARD_SYNC
        action = null
        this.targetName = targetName
        estimatedSize = null
        alert = ALERT_SEVERE
        this.message = message
    }

    constructor(action: String?, targetName: String?, alert: Short, message: String?) {
        direction = DIRECTION_REVERSE_SYNC
        this.action = action
        this.targetName = targetName
        estimatedSize = null
        this.alert = alert
        this.message = message
    }

    companion object {
        const val DIRECTION_REVERSE_SYNC: Short = -1
        const val DIRECTION_NONE: Short = 0
        const val DIRECTION_FORWARD_SYNC: Short = 1
        const val ALERT_INFO: Short = 0
        const val ALERT_WARNING: Short = 1
        const val ALERT_SEVERE: Short = 2
    }
}

internal class FileInfo {
    val filename: String
    private val length: Long
    val dateModified: Long

    constructor(filename: String, dateModified: Long) {
        this.filename = filename
        length = 0
        this.dateModified = dateModified
    }

    constructor(filename: String, length: Long, dateModified: Long) {
        this.filename = filename
        this.length = length
        this.dateModified = dateModified
    }
}

class FileErrorInfo(val errorCategory: Int, val filename: String, val errorMessage: String) {
    companion object {
        const val ERROR_COPY = 0
        const val ERROR_DELETE = 1
    }
}

internal object WifiSyncServiceSettings {
    var defaultIpAddressValue = ""
    var deviceName: String? = Build.MODEL
    var deviceStorageIndex = 0
    private val permissionPathToSdCardMapping: MutableMap<String, String> = HashMap()
    val accessPermissionsUri = AtomicReference<Uri>()
    var syncFromMusicBee = true
    var syncCustomFiles = false
    var syncDeleteUnselectedFiles = false
    val syncCustomPlaylistNames = ArrayList<String>()
    const val PLAYER_GONEMAD = 1
    const val PLAYER_POWERAMP = 2
    var reverseSyncPlayer = 1
    var reverseSyncPlaylists = true
    var reverseSyncPlaylistsPath = ""
    var reverseSyncRatings = false
    var reverseSyncPlayCounts = false
    var debugMode = false
    var permissionsUpgraded = false
    fun loadSettings(context: Context) {
        defaultIpAddressValue = ""
        try {
            val settingsFile = File(context.filesDir, "MusicBeeWifiSyncSettings.dat")
            if (settingsFile.exists()) {
                FileInputStream(settingsFile).use { fs ->
                    DataInputStream(fs).use { reader ->
                        val version: Int = reader.readInt()
                        defaultIpAddressValue = reader.readUTF()
                        deviceName = reader.readUTF()
                        deviceStorageIndex = reader.readInt()
                        syncFromMusicBee = reader.readBoolean()
                        if (version < 5) {
                            syncFromMusicBee = true
                        }
                        var count: Int = reader.readInt()
                        while (count > 0) {
                            count--
                            val permissionsPath: String = reader.readUTF()
                            val sdCardPath: String = reader.readUTF()
                            permissionPathToSdCardMapping[permissionsPath] = sdCardPath
                        }
                        syncDeleteUnselectedFiles = reader.readBoolean()
                        count = reader.readInt()
                        while (count > 0) {
                            count--
                            syncCustomPlaylistNames.add(reader.readUTF())
                        }
                        reverseSyncPlayer = reader.readInt()
                        reverseSyncPlaylists = reader.readBoolean()
                        reverseSyncRatings = reader.readBoolean()
                        reverseSyncPlayCounts = reader.readBoolean()
                        reverseSyncPlaylistsPath = reader.readUTF()
                        if (version < 6) {
                            permissionsUpgraded = false
                        } else {
                            permissionsUpgraded = reader.readBoolean()
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logError("loadSettings", ex)
        }
    }

    fun saveSettings(context: Context?) {
        try {
            val settingsFile = File(context!!.filesDir, "MusicBeeWifiSyncSettings.dat")
            FileOutputStream(settingsFile).use { fs ->
                DataOutputStream(fs).use { writer ->
                    writer.writeInt(6)
                    writer.writeUTF(defaultIpAddressValue)
                    writer.writeUTF(deviceName)
                    writer.writeInt(deviceStorageIndex)
                    writer.writeBoolean(syncFromMusicBee)
                    writer.writeInt(permissionPathToSdCardMapping.size)
                    for (item: Map.Entry<String, String> in permissionPathToSdCardMapping.entries) {
                        writer.writeUTF(item.key)
                        writer.writeUTF(item.value)
                    }
                    writer.writeBoolean(syncDeleteUnselectedFiles)
                    writer.writeInt(syncCustomPlaylistNames.size)
                    for (playlistName: String? in syncCustomPlaylistNames) {
                        writer.writeUTF(playlistName)
                    }
                    writer.writeInt(reverseSyncPlayer)
                    writer.writeBoolean(reverseSyncPlaylists)
                    writer.writeBoolean(reverseSyncRatings)
                    writer.writeBoolean(reverseSyncPlayCounts)
                    writer.writeUTF(reverseSyncPlaylistsPath)
                    writer.writeBoolean(permissionsUpgraded)
                }
            }
        } catch (ex: Exception) {
            logError("saveSettings", ex)
        }
    }
}
