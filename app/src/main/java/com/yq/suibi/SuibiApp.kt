package com.yq.suibi

import android.app.Application
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SuibiApp : Application() {

    /** 应用级作用域：ViewModel 销毁后仍能完成落库，避免退出时丢最后一次编辑。 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}