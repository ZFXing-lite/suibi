package com.yq.suibi

import android.app.Application
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.AttachmentStore
import com.yq.suibi.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SuibiApp : Application() {

    /** 应用级作用域：ViewModel 销毁后仍能完成落库，避免退出时丢最后一次编辑。 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        sweepOrphanAttachments()
    }

    /**
     * 回收孤儿附件文件。
     *
     * 删笔记/删讨论走的是外键 CASCADE，数据库行没了，但磁盘上的
     * 文件不会自己消失。启动时对一次账，把没人引用的清掉。
     */
    private fun sweepOrphanAttachments() {
        appScope.launch {
            runCatching {
                val referenced = db.attachmentDao().getAll().map { it.relPath }.toSet()
                AttachmentStore.sweepOrphans(this@SuibiApp, referenced)
            }
        }
    }
}