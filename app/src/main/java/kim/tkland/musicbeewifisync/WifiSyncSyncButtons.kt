package kim.tkland.musicbeewifisync

import android.R.color.white
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

class WifiSyncSyncButtons(
    val onPreviewButtonClick: () -> Unit,
    val onSyncNowButtonClick: () -> Unit
) {
    @Composable
    fun BottomBarContent() {

        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        val context = LocalContext.current
        val resources = context.resources

        Row(
            // Or a Compose Row
            modifier = Modifier
                .fillMaxWidth()
                .padding(navigationBarPadding),
        ) {
            Button(
                modifier = Modifier
                    .weight(0.5f)
                    .height(80.dp)
                    .border(
                        width = 2.dp, // 枠線の幅
                        color = Color(resources.getColor(white)), // 枠線の色
                    ),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(resources.getColor(R.color.colorButtonBackground)),
                    contentColor = Color(resources.getColor(R.color.colorButtonTextEnabled))
                ),
                onClick = {
                    onPreviewButtonClick()
                }) {
                Modifier.weight(1f)
                Text(resources.getString(R.string.syncPreview), fontSize = 24.sp)
            }
            Button(
                modifier = Modifier
                    .weight(0.5f)
                    .height(80.dp)
                    .border(
                        width = 2.dp, // 枠線の幅
                        color = Color(resources.getColor(white)), // 枠線の色
                    ),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(resources.getColor(R.color.colorButtonBackground)),
                    contentColor = Color(resources.getColor(R.color.colorButtonTextEnabled))
                ),
                onClick = {
                    onSyncNowButtonClick()
                }) {
                Modifier.weight(1f)

                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = "Sync",
                )
                Text(resources.getString(R.string.syncNow), fontSize = 24.sp)
            }
        }
    }
}
