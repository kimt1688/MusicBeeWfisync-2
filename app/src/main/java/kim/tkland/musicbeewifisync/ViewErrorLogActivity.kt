package kim.tkland.musicbeewifisync

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class ViewErrorLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ComposeView(applicationContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        setContent {
            ViewErrorLog()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ViewErrorLog() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        //val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
                    title = {
                        Box( // Wrap the Text in a Box
                        ) {
                            Text(
                                text = getString(R.string.title_activity_view_error_log),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
                        Button(
                            modifier = Modifier.border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(R.color.colorButtonTextEnabled)),
                                shape = androidx.compose.ui.graphics.RectangleShape
                            ),
                            shape = androidx.compose.ui.graphics.RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(getColor(R.color.colorButtonBackground)),
                                contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                            ),
                            onClick = {
                                val clipboard =
                                    getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                val logData = ErrorHandler.log
                                if (!logData.isNullOrEmpty()) {
                                    val clipData =
                                        ClipData.newPlainText("plain text", logData)
                                    clipboard.setPrimaryClip(clipData)
                            }
                        }) {
                            Icon(
                                modifier = Modifier.border(
                                                        width = 2.dp, // 枠線の幅
                                                        color = Color(getColor(R.color.colorButtonTextEnabled)), // 枠線の色
                                                    )
                                                    .background(Color(getColor(R.color.colorButtonBackground))),
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy to Clipboard",
                                tint = Color(getColor(R.color.colorButtonTextEnabled)),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding)
                    .padding(statusBarPadding)

                ) {
                val errorLog = ErrorHandler.log
                if (!errorLog.isNullOrEmpty()) {
                    item {
                        Text(errorLog)
                    }
                } else {
                    item {
                        Text("")
                    }
                }
            }
        }
    }
}
