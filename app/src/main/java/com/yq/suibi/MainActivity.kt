package com.yq.suibi

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.yq.suibi.ui.SuibiNavHost
import com.yq.suibi.ui.theme.Palettes
import com.yq.suibi.ui.theme.SuibiTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = LocalContext.current.applicationContext as SuibiApp
            var paletteId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                paletteId = app.settings.paletteId.first()
            }

            val palette = Palettes.byId(paletteId)

            // 状态栏 / 导航栏图标跟着主题走，不跟系统走。
            // 否则浅色系统 + 深色主题时图标会看不见。
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !palette.dark
                    controller.isAppearanceLightNavigationBars = !palette.dark
                }
            }

            SuibiTheme(palette = palette) {
                SuibiNavHost(onPaletteChange = { paletteId = it })
            }
        }
    }
}