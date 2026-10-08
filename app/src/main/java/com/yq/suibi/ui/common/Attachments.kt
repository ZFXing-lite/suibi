package com.yq.suibi.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.yq.suibi.data.Attachment
import com.yq.suibi.data.AttachmentKind
import com.yq.suibi.data.AttachmentStore
import java.io.File

/**
 * 选图 / 选文件 / 拍照的三个入口。
 *
 * 拍照走 TakePicture：先在本应用缓存里建一个临时文件，拍照结果写进去，
 * 再用 FileProvider 授权给相机应用。
 */
@Composable
fun rememberAttachmentPickers(
    context: Context,
    onPicked: (Uri) -> Unit
): AttachmentPickers {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPicked) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // 让这份权限跨进程重启也有效
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onPicked(it)
        }
    }

    return AttachmentPickers(
        pickImage = {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        pickFile = { filePicker.launch(arrayOf("*/*")) }
    )
}

data class AttachmentPickers(
    val pickImage: () -> Unit,
    val pickFile: () -> Unit
)

/** 用系统相机拍照，结果落到应用缓存目录。 */
fun launchCamera(context: Context, onShot: (Uri) -> Unit) {
    try {
        val dir = File(context.cacheDir, "camera").apply { if (!exists()) mkdirs() }
        val file = File(dir, "shot-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        context.startActivity(intent)
        onShot(uri)
    } catch (_: Exception) {
    }
}

/* ---------- 展示 ---------- */

/** 图片横排预览，点开大图，右上角可删。 */
@Composable
fun AttachmentStrip(
    attachments: List<Attachment>,
    onRemove: (Attachment) -> Unit,
    onOpen: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attachments.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments, key = { it.id }) { a ->
            AttachmentThumb(a = a, onRemove = { onRemove(a) }, onOpen = { onOpen(a) })
        }
    }
}

@Composable
private fun AttachmentThumb(
    a: Attachment,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = Modifier.size(96.dp)) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .size(96.dp)
                .clip(shape)
                .clickable(onClick = onOpen)
        ) {
            if (a.kind == AttachmentKind.IMAGE) {
                AsyncImage(
                    model = AttachmentStore.resolve(context, a.relPath),
                    contentDescription = a.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Icon(
                        SI.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = a.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 删除角标
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRemove)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    SI.Close,
                    contentDescription = "移除附件",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** 附件的小标签，用在列表页显示「有几张图」。 */
@Composable
fun AttachmentBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            SI.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.height(13.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}