package kim.tkland.musicbeewifisync

import android.R.id.message
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.collectAsState

@HiltViewModel
class WifiSyncViewModel : ViewModel() {
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog
    private val _msg = MutableStateFlow("")
    val msg: StateFlow<String> = _msg
    private var thread: Thread? = null

    // 起動時にダイアログを表示し、スレッド処理を開始する
    @Composable
    fun StartThreadAndShowDialog(thread: Thread, message: String) {
        _msg.value = message // メッセージを更新
        this.thread = thread

        if (thread.isAlive) return

        viewModelScope.launch {
            doAsyncWork()
        }
    }

    fun setValues(thread: Thread, message: String) {
        _msg.value = message // メッセージを更新
        this.thread = thread
    }

    suspend fun doAsyncWork() {
        val job = CoroutineScope(Dispatchers.Default).launch {
            // バックグラウンドで時間のかかる処理
            _showDialog.value = true
            thread?.start()
            Log.d("WifiSyncViewModel", "Thread started")
            thread?.join()
            Log.d("WifiSyncViewModel", "Thread finished")
            _showDialog.value = false // ダイアログを閉じる
        }

        // スレッドをブロックせずにコルーチンの完了を待つ
        job.join()
    }

    fun cancelProcess() {
        thread?.interrupt()
        _showDialog.value = false
    }
}
