package kim.tkland.musicbeewifisync

import android.R.id.message
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kim.tkland.musicbeewifisync.ErrorHandler.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay


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

    suspend fun waitForThread() = coroutineScope { // coroutineScopeを作成
        _msg.value = message.toString() // メッセージを更新

        launch { // 子コルーチン①
            _showDialog.value = true
            thread?.start()
            Log.d("WifiSyncViewModel", "Thread started")
            thread?.join()
            Log.d("WifiSyncViewModel", "Thread finished")
        }
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
