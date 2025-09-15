package kim.tkland.musicbeewifisync

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class WifiSyncAlertDialog : DialogFragment() {
    var thread: WifiSyncAlertDialogThread? = null
    var msg: String? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val msgView = layoutInflater.inflate(R.layout.dialog_sync_progress, null)
        val dialogText = msgView.findViewById<TextView>(R.id.textView)
        dialogText.text = this.msg
        return activity?.let {
            // Use the Builder class for convenient dialog construction.
            val builder = AlertDialog.Builder(it)
            builder.setView(msgView)
                .setTitle(R.string.app_name)
                .setNegativeButton(R.string.syncCancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        dialog.dismiss()
                        this.thread!!.thread!!.interrupt()
                    })
            // Create the AlertDialog object and return it.
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
