package kim.tkland.musicbeewifisync

import android.R.color.white
import android.app.Activity.RESULT_OK
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit

private fun setLaunchIntent(): Intent {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "text/xml"
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/storage/emulated/0/gmmp")
    }
    return intent
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicBeeWifiSyncTopBar(
    title: @Composable () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = colorResource(R.color.colorPrimary),
        titleContentColor = colorResource(white),
        navigationIconContentColor = colorResource(white),
        actionIconContentColor = colorResource(white),
        scrolledContainerColor = colorResource(white)
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = colors,
            modifier = Modifier.padding(contentPadding),
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreenTopBar(
    appBarTitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean)-> Unit,
    showFullScanDialog:()-> Unit,
    // showDeleteAllPlaylistsDialog: ()-> Unit,
    isFullSync: Boolean)
{
    val context = LocalContext.current
    val resources = context.resources

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            // アクティビティ結果NG
            return@rememberLauncherForActivityResult
        } else {
            // アクティビティ結果OK
            try {
                val mUri = result.data?.data
                if (mUri != null) {
                    val contentResolver = context.contentResolver
                    for (uriPermission in contentResolver.persistedUriPermissions) {
                        contentResolver.releasePersistableUriPermission(
                            uriPermission.uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    contentResolver.takePersistableUriPermission(
                        mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    val preferences = context.getSharedPreferences(
                        "kim.tkland.musicbeewifisync.sharedpref",
                        MODE_PRIVATE
                    )
                    preferences.edit(commit = true) { putString("accesseduri", mUri.toString()) }
                }
            } catch (e: Exception) {
                Log.d("launcher", e.message ?: "")
            }
        }
    }

    MusicBeeWifiSyncTopBar(
        title = {
            Box(
            ) {
                Text(
                    text = appBarTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            IconButton(onClick = { onExpandedChange(!expanded) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
            }
            DropdownMenu(
                modifier = Modifier.background(Color(ContextCompat.getColor(context,white))),
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(resources.getString(R.string.menuWifiFullSync))
                            }
                            if (isFullSync) {
                                Checkbox(
                                    checked = true,
                                    colors = CheckboxDefaults.colors(
                                        checkmarkColor = Color(
                                            ContextCompat.getColor(
                                                context,
                                                android.R.color.white
                                            )
                                        ),
                                        uncheckedColor = Color(
                                            ContextCompat.getColor(
                                                context,
                                                android.R.color.black
                                            )
                                        ),
                                        checkedColor = Color(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.colorAccent
                                            )
                                        ),
                                    ),
                                    onCheckedChange = { isChecked ->
                                    }
                                )
                            }
                        }
                    },
                    onClick = {
                        if(!isFullSync) {
                            val intent = Intent(
                                context,
                                MainActivity::class.java
                            )
                            intent.putExtra("fullSync", true)
                            onExpandedChange(false)
                            context.startActivity(intent)
                        } else {
                            onExpandedChange(false)
                        }
                    }
                )
                DropdownMenuItem(
                    onClick = {
                        if (isFullSync) {
                            val intent = Intent(
                                context,
                                PlaylistSyncActivity::class.java
                            )
                            intent.putExtra("playlistSync", true)
                            onExpandedChange(false)
                            context.startActivity(intent)
                        } else {
                            onExpandedChange(false)
                        }
                    },
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(resources.getString(R.string.menuWifiPlaylistSync))
                            }
                            if (!isFullSync) {
                                Checkbox(
                                    checked = true,
                                    colors = CheckboxDefaults.colors(
                                        checkmarkColor = Color(
                                            ContextCompat.getColor(
                                                context,
                                                android.R.color.white
                                            )
                                        ),
                                        uncheckedColor = Color(
                                            ContextCompat.getColor(
                                                context,
                                                android.R.color.black
                                            )
                                        ),
                                        checkedColor = Color(
                                            ContextCompat.getColor(
                                                context,
                                                R.color.colorAccent
                                            )
                                        ),
                                    ),
                                    onCheckedChange = { }
                                )
                            }
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.menuSyncSettings)) },
                    onClick = {
                        val intent = Intent(
                            context,
                            SettingsActivity::class.java
                        )
                        onExpandedChange(false)
                        context.startActivity(intent)
                    }
                )
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.selectStats)) },
                    onClick = {
                        onExpandedChange(false)
                        launcher.launch(setLaunchIntent())
                    }
                )
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.menuFullScanFiles)) },
                    onClick = {
                        onExpandedChange(false)
                        showFullScanDialog()
                    }
                )
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.menuWifiSyncLog)) },
                    onClick = {
                        val intent = Intent(
                            context,
                            ViewErrorLogActivity::class.java
                        )
                        onExpandedChange(false)
                        context.startActivity(intent)
                    }
                )
                // for Test(delete playlists)
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.menudeletePlaylists)) },
                    onClick = {
                        val intent = Intent(
                            context,
                            DeletePlaylistActivity::class.java
                        )
                        onExpandedChange(false)
                        context.startActivity(intent)
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreenTopBar(
    appBarTitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean)-> Unit)
{
    val context = LocalContext.current
    val resources = context.resources

    MusicBeeWifiSyncTopBar(
        title = {
            Box(
            ) {
                Text(
                    text = appBarTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                }
                DropdownMenu(
                    modifier = Modifier.background(Color(ContextCompat.getColor(context, white))),
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(resources.getString(R.string.menuSyncSettings)) },
                        onClick = {
                            val intent = Intent(
                                context,
                                SettingsActivity::class.java
                            )
                            onExpandedChange(false)
                            context.startActivity(intent)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(resources.getString(R.string.menuWifiSyncLog)) },
                        onClick = {
                            val intent = Intent(
                                context,
                                ViewErrorLogActivity::class.java
                            )
                            onExpandedChange(false)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    )
}
