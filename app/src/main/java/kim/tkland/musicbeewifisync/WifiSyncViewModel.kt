package kim.tkland.musicbeewifisync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class WifiSyncViewModel : ViewModel() {
    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog
    private val _msg = MutableStateFlow("")
    val msg: StateFlow<String> = _msg
    
    private var thread: Thread? = null

    /**
     * スレッドとメッセージを設定し、ダイアログの表示フラグを立てる。
     * 実際の処理開始は doAsyncWork() で行われる。
     */
    fun setValues(thread: Thread, message: String) {
        _msg.value = message
        this.thread = thread
        _showDialog.value = true // ダイアログを表示状態にする
    }

    /**
     * バックグラウンドスレッドの実行と完了待ちを行う。
     * CreateProgressDialog内のLaunchedEffectなどから呼び出されることを想定。
     */
    suspend fun doAsyncWork() {
        if (thread == null) {
            Log.w("WifiSyncViewModel", "doAsyncWork called but thread is null")
            _showDialog.value = false
            return
        }

        withContext(Dispatchers.IO) {
            try {
                if (thread?.state == Thread.State.NEW) {
                    Log.d("WifiSyncViewModel", "Starting thread: ${thread?.name}")
                    thread?.start()
                }
                
                Log.d("WifiSyncViewModel", "Waiting for thread: ${thread?.name}")
                thread?.join()
                Log.d("WifiSyncViewModel", "Thread finished: ${thread?.name}")
            } catch (e: Exception) {
                Log.e("WifiSyncViewModel", "Error in doAsyncWork", e)
            } finally {
                // 処理が終わったらダイアログを閉じる
                _showDialog.value = false
            }
        }
    }

    fun cancelProcess() {
        Log.d("WifiSyncViewModel", "Cancelling process")
        thread?.interrupt()
        _showDialog.value = false
    }
}
