package com.yq.suibi.data

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 极简 WebDAV 客户端。只用到 MKCOL / PUT / GET 三个动作,
 * 覆盖坚果云、Nextcloud、群晖这类标准 WebDAV 服务。
 */
class WebDavClient(private val config: WebDavConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val auth: String = Credentials.basic(config.username, config.password, Charsets.UTF_8)

    private fun base(): String = config.server.trim().trimEnd('/')

    private fun urlOf(path: String): String {
        val p = path.trim('/')
        return if (p.isEmpty()) base() else "${base()}/$p"
    }

    private fun request(method: String, path: String, body: RequestBody? = null): Request =
        Request.Builder()
            .url(urlOf(path))
            .header("Authorization", auth)
            .method(method, body)
            .build()

    /** 返回一句给人看的结论。 */
    fun test(): String {
        val target = config.remoteDir.trim('/').ifBlank { "suibi" }
        val resp = runCatching { client.newCall(request("PROPFIND", "")).execute() }
            .getOrElse { return "连接失败：${it.message ?: it.javaClass.simpleName}" }

        resp.use { r ->
            if (r.code == 401 || r.code == 403) return "认证失败，请检查账号与应用密码（HTTP ${r.code}）"
            if (r.code != 207 && r.code != 200 && r.code != 301 && r.code != 302) {
                return "服务器返回 HTTP ${r.code}"
            }
        }

        val mk = runCatching { client.newCall(request("MKCOL", target)).execute() }
            .getOrElse { return "目录创建失败：${it.message}" }

        mk.use { r ->
            return when (r.code) {
                201 -> "连接成功，已创建目录 /$target"
                405 -> "连接成功，目录 /$target 已存在"
                401, 403 -> "认证失败，请检查账号与应用密码"
                409 -> "父目录不存在（HTTP 409），请确认远程目录层级"
                else -> "连接成功（HTTP ${r.code}）"
            }
        }
    }

    /** 逐级创建目录。已存在（405）视为成功。 */
    fun ensureDir(path: String): Boolean {
        var cur = ""
        for (part in path.trim('/').split('/').filter { it.isNotBlank() }) {
            cur = if (cur.isEmpty()) part else "$cur/$part"
            val r = runCatching { client.newCall(request("MKCOL", cur)).execute() }.getOrNull()
                ?: return false
            r.use { if (it.code != 201 && it.code != 405 && it.code != 200) return false }
        }
        return true
    }

    fun upload(path: String, bytes: ByteArray): Pair<Boolean, String> {
        val body = bytes.toRequestBody("application/octet-stream".toMediaType())
        val r = runCatching { client.newCall(request("PUT", path, body)).execute() }
            .getOrElse { return false to "上传失败：${it.message ?: it.javaClass.simpleName}" }
        r.use {
            val ok = it.isSuccessful || it.code == 201 || it.code == 204
            return if (ok) true to "上传成功" else false to "上传失败：HTTP ${it.code}"
        }
    }

    fun download(path: String): ByteArray? {
        val r = runCatching { client.newCall(request("GET", path)).execute() }.getOrNull() ?: return null
        r.use { return if (it.isSuccessful) it.body?.bytes() else null }
    }
}