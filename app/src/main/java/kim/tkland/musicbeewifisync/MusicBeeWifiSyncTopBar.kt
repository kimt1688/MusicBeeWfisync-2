package kim.tkland.musicbeewifisync

import android.R.color.white
import android.content.Intent
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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicBeeWifiSyncTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = colorResource(R.color.colorPrimary),
        //colorOnContainer = colorResource(white),
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
    appBarTitle: MutableState<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean)-> Unit,
    showDeleteAllPlaylistsDialog: MutableState<Boolean>,
    showFullScanDialogShow: Boolean,
    onShowFullScanDialogShowChange: (Boolean)-> Unit,
    isFullSync: Boolean)
{
    val resources = LocalResources.current
    val context = LocalContext.current

    MusicBeeWifiSyncTopBar(
        title = {
            Box(
            ) {
                Text(
                    text = appBarTitle.value,
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
                modifier = Modifier.background(Color(resources.getColor(white))),
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
                                        checkmarkColor = Color(resources.getColor(android.R.color.white)),
                                        uncheckedColor = Color(resources.getColor(android.R.color.black)),
                                        checkedColor = Color(resources.getColor(R.color.colorAccent)),                                                    ),
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
                            startActivity(context, intent, null)
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
                                        checkmarkColor = Color(resources.getColor(android.R.color.white)),
                                        uncheckedColor = Color(resources.getColor(android.R.color.black)),
                                        checkedColor = Color(resources.getColor(R.color.colorAccent)),                                                ),
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
                    text = { Text(resources.getString(R.string.menuFullScanFiles)) },
                    onClick = {
                        onExpandedChange(false)
                        onShowFullScanDialogShowChange(true)
                    }
                )
                DropdownMenuItem(
                    text = { Text(resources.getString(R.string.menuAllPlaylistsDelete)) },
                    onClick = {
                        onExpandedChange(false)
                        onShowFullScanDialogShowChange(true)
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
    )
}
