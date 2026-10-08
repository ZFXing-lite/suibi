package com.yq.suibi.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yq.suibi.data.AppDatabase
import com.yq.suibi.data.SettingsStore
import com.yq.suibi.data.WebDavConfig
import com.yq.suibi.ui.common.ConfirmDialog
import com.yq.suibi.ui.common.absoluteTime
import com.yq.suibi.ui.common.vmFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavScreen(
    db: AppDatabase,
    store: SettingsStore,
    onBack: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val vm: SettingsViewModel = viewModel(
        factory = vmFactory { SettingsViewModel(appContext, db, store) }
    )
    val saved by vm.config.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remoteDir by remember { mutableStateOf("suibi") }
    var autoBackup by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    LaunchedEffect(saved.server, saved.username, saved.password, saved.remoteDir, saved.autoBackup) {
        if (!loaded) {
            server = saved.server
            username = saved.username
            password = saved.password
            remoteDir = saved.remoteDir
            autoBackup = saved.autoBackup
            loaded = true
        }
    }

    fun current() = WebDavConfig(
        server = server,
        username = username,
        password = password,
        remoteDir = remoteDir,
        autoBackup = autoBackup,
        intervalDays = saved.intervalDays,
        lastBackupAt = saved.lastBackupAt,
        lastBackupStatus = saved.lastBackupStatus
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 备份") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.save(current())
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "把整个笔记库打包上传到你的网盘。支持坚果云、Nextcloud、群晖等标准 WebDAV 服务。数据始终以本地为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text("服务器地址") },
                placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("账号") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("应用密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = remoteDir,
                onValueChange = { remoteDir = it },
                label = { Text("远程目录") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("自动备份", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "每 ${saved.intervalDays} 天提醒一次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(checked = autoBackup, onCheckedChange = { autoBackup = it })
                }
            }

            if (saved.lastBackupAt > 0L) {
                Text(
                    text = "上次备份：${absoluteTime(saved.lastBackupAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { vm.test(current()) },
                    enabled = busy == Busy.NONE,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("测试连接")
                }
                Button(
                    onClick = { vm.backupNow() },
                    enabled = busy == Busy.NONE,
                    modifier = Modifier.weight(1f)
                ) {
                    if (busy == Busy.BACKING_UP) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp).width(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(
                            Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("立即备份")
                }
            }

            OutlinedButton(
                onClick = { confirmRestore = true },
                enabled = busy == Busy.NONE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("从云端恢复（覆盖本地）")
            }

            if (busy != Busy.NONE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(14.dp).width(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = when (busy) {
                            Busy.TESTING -> "正在测试连接…"
                            Busy.BACKING_UP -> "正在备份…"
                            Busy.RESTORING -> "正在恢复…"
                            Busy.NONE -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (message.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "从云端恢复",
            message = "会用云端的备份文件覆盖当前全部数据。本地现有的笔记会丢失。确定继续？",
            confirmText = "覆盖恢复",
            onDismiss = { confirmRestore = false },
            onConfirm = {
                confirmRestore = false
                vm.save(current())
                vm.restore()
            }
        )
    }
}