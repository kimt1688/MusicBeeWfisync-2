package kim.tkland.musicbeewifisync

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

abstract class SyncResultsBaseActivity : AppCompatActivity() {
    @JvmField
    protected var mainWindow: SyncResultsBaseActivity? = this
    @JvmField
    protected var infoColor = 0
    @JvmField
    protected var errorColor = 0
    @JvmField
    protected var warningColor = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        this.enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        infoColor = ContextCompat.getColor(this, R.color.colorButtonTextDisabled)
        errorColor = ContextCompat.getColor(this, R.color.colorWarning)
        warningColor = ContextCompat.getColor(this, R.color.colorWarning)
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.

            v.updateLayoutParams() {
                v.left = (insets.left).toInt()
                v.bottom = (insets.bottom).toInt()
                v.right = (insets.right).toInt()
            }
            v.updatePadding(
                left = bars.left,
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
            )

            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onDestroy() {
        mainWindow = null
        WifiSyncService.syncFromResults = null
        WifiSyncService.syncToResults = null
        super.onDestroy()
    }

    @Suppress("NAME_SHADOWING")
    protected fun showResults(
        resultsListView: ListView,
        resultsToData: ArrayList<SyncResultsInfo>?,
        resultsFromData: ArrayList<SyncResultsInfo>?
    ) {
        var resultsToData = resultsToData
        var resultsFromData = resultsFromData
        val maxResults = 256
        if (resultsToData == null) {
            resultsToData = ArrayList()
        }
        if (resultsFromData == null) {
            resultsFromData = ArrayList()
        }
        val resultsToDataCount = resultsToData.size
        val resultsFromDataCount = resultsFromData.size
        val filteredPreviewData: ArrayList<SyncResultsInfo>
        if (resultsToDataCount + resultsFromDataCount < maxResults + 16) {
            filteredPreviewData = ArrayList(resultsToDataCount + resultsFromDataCount)
            filteredPreviewData.addAll(resultsToData)
            filteredPreviewData.addAll(resultsFromData)
        } else {
            filteredPreviewData = ArrayList(maxResults + 2)
            var filteredPreviewFromCount = resultsFromDataCount
            var filteredPreviewToCount = resultsToDataCount
            if (resultsToDataCount < maxResults / 4) {
                filteredPreviewFromCount = maxResults - resultsToDataCount
            } else if (resultsFromDataCount < maxResults / 4) {
                filteredPreviewToCount = maxResults - resultsFromDataCount
            } else {
                val scaling =
                    maxResults.toDouble() / (resultsToDataCount + resultsFromDataCount).toDouble()
                filteredPreviewToCount *= scaling.toInt()
                filteredPreviewFromCount *= scaling.toInt()
            }
            for (index in 0 until filteredPreviewToCount) {
                filteredPreviewData.add(resultsToData[index])
            }
            if (filteredPreviewToCount < resultsToDataCount) {
                filteredPreviewData.add(
                    SyncResultsInfo(
                        String.format(
                            getString(R.string.syncPreviewMoreResults),
                            resultsToDataCount - filteredPreviewToCount
                        )
                    )
                )
            }
            for (index in 0 until filteredPreviewFromCount) {
                filteredPreviewData.add(resultsFromData[index])
            }
            if (filteredPreviewFromCount < resultsFromDataCount) {
                filteredPreviewData.add(
                    SyncResultsInfo(
                        String.format(
                            getString(R.string.syncPreviewMoreResults),
                            resultsFromDataCount - filteredPreviewFromCount
                        )
                    )
                )
            }
        }
        val adapter: ArrayAdapter<SyncResultsInfo?> = object : ArrayAdapter<SyncResultsInfo?>(
            mainWindow?.applicationContext!!,
            R.layout.row_item_sync_results,
            R.id.syncResultsLine1,
            filteredPreviewData as List<SyncResultsInfo?>
        )
        {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val syncResultsDirectionIcon =
                    view.findViewById<ImageView>(R.id.syncResultsDirectionIcon)
                val syncResultsStatusIcon =
                    view.findViewById<ImageView>(R.id.syncResultsStatusIcon)
                val syncResultsLine1 = view.findViewById<TextView>(R.id.syncResultsLine1)
                val syncResultsLine2 = view.findViewById<TextView>(R.id.syncResultsLine2)
                val info = filteredPreviewData[position]
                if (info.direction == SyncResultsInfo.DIRECTION_NONE) {
                    // more results message
                    syncResultsStatusIcon.visibility = View.GONE
                    syncResultsDirectionIcon.visibility = View.GONE
                    syncResultsLine1.visibility = View.GONE
                    syncResultsLine2.text = info.message
                } else if (info.direction == SyncResultsInfo.DIRECTION_REVERSE_SYNC || info.alert != SyncResultsInfo.ALERT_INFO) {
                    // reverse sync action
                    syncResultsDirectionIcon.visibility = View.VISIBLE
                    syncResultsDirectionIcon.setImageResource(if (info.direction == SyncResultsInfo.DIRECTION_REVERSE_SYNC) R.drawable.ic_arrow_back else R.drawable.ic_arrow_forward)
                    val color: Int
                    if (info.alert == SyncResultsInfo.ALERT_INFO) {
                        syncResultsStatusIcon.visibility = View.GONE
                        color = infoColor
                    } else {
                        color =
                            if (info.alert == SyncResultsInfo.ALERT_WARNING) warningColor else errorColor
                        syncResultsStatusIcon.visibility = View.VISIBLE
                        syncResultsStatusIcon.setImageResource(android.R.drawable.stat_notify_error)
                        syncResultsStatusIcon.setColorFilter(color)
                    }
                    syncResultsDirectionIcon.setColorFilter(infoColor)
                    syncResultsLine1.setTextColor(color)
                    syncResultsLine1.text = info.targetName
                    syncResultsLine2.text = info.message
                } else {
                    // sync action
                    syncResultsDirectionIcon.visibility = View.VISIBLE
                    syncResultsDirectionIcon.setImageResource(R.drawable.ic_arrow_forward)
                    syncResultsDirectionIcon.setColorFilter(infoColor)
                    syncResultsStatusIcon.visibility = View.GONE
                    syncResultsLine1.setTextColor(infoColor)
                    syncResultsLine1.text = info.targetName
                    syncResultsLine2.text =
                        if (info.estimatedSize!!.isEmpty()) info.action else "${info.action} - ${info.estimatedSize}"
                }
                return view
            }
        }
        resultsListView.adapter = adapter
    }
}
