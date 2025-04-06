package kim.tkland.musicbeewifisync

import android.os.Bundle

fun WifiSyncAlertDialogThread(thread: Thread, dialog: WifiSyncAlertDialog) : WifiSyncAlertDialogThread {
    return WifiSyncAlertDialogThread(thread, dialog, null)
}

class WifiSyncAlertDialogThread {
    var dialog: WifiSyncAlertDialog? = null
    var thread: Thread? = null
    constructor() {
    }

    constructor(target: Runnable, dialog: WifiSyncAlertDialog, bundle: Bundle?): this()  {
        this.dialog = dialog
        this.thread = Thread(target)
    }
}
