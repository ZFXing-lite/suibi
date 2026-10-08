package com.yq.suibi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.BackupManager
import com.yq.suibi.data.SettingsStore
import com.yq.suibi.data.WebDavClient
import com.yq.suibi.data.WebDavConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Busy { NONE, TESTING, BACKING_UP, RESTORING }

class SettingsViewModel(
    private val db: AppDatabase,
    private val store: SettingsStore
) : ViewModel() {

    val config: StateFlow<WebDavConfig> = store.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WebDavConfig())

    private val _busy = MutableStateFlow(Busy.NONE)
    val busy: StateFlow<Busy> = _busy.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    fun save(config: WebDavConfig) {
        viewModelScope.launch { store.save(config) }
    }

    fun test(config: WebDavConfig) {
        if (!config.isConfigured) {
            _message.value = "请先填写服务器地址、账号和密码"
            return
        }
        viewModelScope.launch {
            _busy.value = Busy.TESTING
            store.save(config)
            val result = withContext(Dispatchers.IO) { WebDavClient(config).test() }
            _message.value = result
            _busy.value = Busy.NONE
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            val cfg = store.config.first()
            if (!cfg.isConfigured) {
                _message.value = "请先配置 WebDAV"
                return@launch
            }
            _busy.value = Busy.BACKING_UP
            val result = withContext(Dispatchers.IO) {
                val client = WebDavClient(cfg)
                if (!client.ensureDir(cfg.remoteDir)) {
                    "无法创建远程目录 /${cfg.remoteDir}"
                } else {
                    val bytes = BackupManager.export(db)
                    val (ok, msg) = client.upload("${cfg.remoteDir}/${BackupManager.FILE_NAME}", bytes)
                    if (ok) "$msg（${bytes.size / 1024} KB）" else msg
                }
            }
            _message.value = result
            val ok = !result.startsWith("无法") && !result.startsWith("上传失败")
            store.markBackup(System.currentTimeMillis(), result)
            if (ok) _message.value = "备份完成 · $result"
            _busy.value = Busy.NONE
        }
    }

    fun restore() {
        viewModelScope.launch {
            val cfg = store.config.first()
            if (!cfg.isConfigured) {
                _message.value = "请先配置 WebDAV"
                return@launch
            }
            _busy.value = Busy.RESTORING
            val result = withContext(Dispatchers.IO) {
                val bytes = WebDavClient(cfg)
                    .download("${cfg.remoteDir}/${BackupManager.FILE_NAME}")
                    ?: return@withContext "云端没有找到备份文件"
                BackupManager.restore(db, bytes)
            }
            _message.value = result
            _busy.value = Busy.NONE
        }
    }

    fun clearMessage() {
        _message.value = ""
    }
}