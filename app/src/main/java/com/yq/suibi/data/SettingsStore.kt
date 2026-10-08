package com.yq.suibi.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.suibiStore by preferencesDataStore(name = "suibi_settings")

data class WebDavConfig(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val remoteDir: String = "suibi",
    val autoBackup: Boolean = false,
    val intervalDays: Int = 7,
    val lastBackupAt: Long = 0L,
    val lastBackupStatus: String = ""
) {
    val isConfigured: Boolean
        get() = server.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

/** 「通用」里的可调项。 */
data class GeneralConfig(
    /** 删除前是否要求二次确认。 */
    val confirmDelete: Boolean = true,
    /** 讨论区默认是否展开。 */
    val discussionsExpanded: Boolean = true,
    /** 列表里的时间用相对（3 分钟前）还是绝对（10-05 14:22）。 */
    val relativeTime: Boolean = true,
    /** 正文字号倍率，1.0 为标准。 */
    val fontScale: Float = 1.0f,
    /** 导出长图宽度，px。 */
    val exportWidth: Int = 1080
)

class SettingsStore(private val context: Context) {

    private object K {
        val server = stringPreferencesKey("webdav_server")
        val user = stringPreferencesKey("webdav_user")
        val pass = stringPreferencesKey("webdav_pass")
        val dir = stringPreferencesKey("webdav_dir")
        val auto = booleanPreferencesKey("auto_backup")
        val interval = intPreferencesKey("interval_days")
        val lastAt = longPreferencesKey("last_backup_at")
        val lastStatus = stringPreferencesKey("last_backup_status")
        val palette = stringPreferencesKey("palette_id")

        val confirmDelete = booleanPreferencesKey("general_confirm_delete")
        val discussionsExpanded = booleanPreferencesKey("general_discussions_expanded")
        val relativeTime = booleanPreferencesKey("general_relative_time")
        val fontScale = intPreferencesKey("general_font_scale_pct")
        val exportWidth = intPreferencesKey("general_export_width")
    }

    /** 当前主题 id，默认「宣纸」。 */
    val paletteId: Flow<String> = context.suibiStore.data.map { p ->
        p[K.palette] ?: "xuanzhi"
    }

    suspend fun setPalette(id: String) {
        context.suibiStore.edit { it[K.palette] = id }
    }

    /** 「通用」配置。 */
    val general: Flow<GeneralConfig> = context.suibiStore.data.map { p ->
        GeneralConfig(
            confirmDelete = p[K.confirmDelete] ?: true,
            discussionsExpanded = p[K.discussionsExpanded] ?: true,
            relativeTime = p[K.relativeTime] ?: true,
            fontScale = (p[K.fontScale] ?: 100) / 100f,
            exportWidth = p[K.exportWidth] ?: 1080
        )
    }

    suspend fun setConfirmDelete(v: Boolean) = context.suibiStore.edit { it[K.confirmDelete] = v }
    suspend fun setDiscussionsExpanded(v: Boolean) = context.suibiStore.edit { it[K.discussionsExpanded] = v }
    suspend fun setRelativeTime(v: Boolean) = context.suibiStore.edit { it[K.relativeTime] = v }
    suspend fun setFontScale(v: Float) = context.suibiStore.edit { it[K.fontScale] = (v * 100).toInt() }
    suspend fun setExportWidth(v: Int) = context.suibiStore.edit { it[K.exportWidth] = v }

    val config: Flow<WebDavConfig> = context.suibiStore.data.map { p ->
        WebDavConfig(
            server = p[K.server] ?: "",
            username = p[K.user] ?: "",
            password = CryptoBox.decrypt(p[K.pass] ?: ""),
            remoteDir = p[K.dir] ?: "suibi",
            autoBackup = p[K.auto] ?: false,
            intervalDays = p[K.interval] ?: 7,
            lastBackupAt = p[K.lastAt] ?: 0L,
            lastBackupStatus = p[K.lastStatus] ?: ""
        )
    }

    suspend fun save(config: WebDavConfig) {
        context.suibiStore.edit { p ->
            p[K.server] = config.server.trim()
            p[K.user] = config.username.trim()
            p[K.pass] = CryptoBox.encrypt(config.password)
            p[K.dir] = config.remoteDir.trim().trim('/').ifBlank { "suibi" }
            p[K.auto] = config.autoBackup
            p[K.interval] = config.intervalDays
        }
    }

    suspend fun markBackup(at: Long, status: String) {
        context.suibiStore.edit { p ->
            p[K.lastAt] = at
            p[K.lastStatus] = status
        }
    }
}