package com.iptv.player.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文件日志：同时输出到 Logcat、app 内部目录与共享外部存储。
 * 共享存储路径：/sdcard/Android/data/com.iptv.player/files/dev.log，
 * 用项目根目录的 get-devlog.cmd 可一键拉到电脑。
 */
object FileLogger {

    private const val TAG = "IPTV"
    private const val MAX_SIZE = 512L * 1024
    private val scope = CoroutineScope(Dispatchers.IO)
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val files = mutableListOf<File>()
    private val urlRegex = Regex("""https?://[^\s,\])}"]+""", RegexOption.IGNORE_CASE)
    private val addressRegex = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?\b""")
    private val sensitiveParamRegex = Regex("""(?i)(token|access_token|refresh_token|key|api_key|auth|authorization|password|passwd|secret)=([^&\s]+)""")
    private val pathFieldRegex = Regex("""(?i)\b(path|url)=([^\s,)\]]+)""")

    /** 内部存储路径（run-as 可读取，最完整）。 */
    fun internalPath(): String? = files.firstOrNull()?.absolutePath

    /** 共享存储路径（无需 root，文件管理器可见）。 */
    fun externalPath(): String? = files.getOrNull(1)?.absolutePath

    fun init(context: Context) {
        val internal = File(context.filesDir, "dev.log")
        val external = context.getExternalFilesDir(null)?.let { File(it, "dev.log") }
        files.clear()
        files.add(internal)
        if (external != null) files.add(external)
        i("FileLogger", "internal log: ${internal.absolutePath}")
        if (external != null) i("FileLogger", "external log: ${external.absolutePath}")
    }

    fun d(tag: String, msg: String) = write("D", tag, msg)
    fun i(tag: String, msg: String) = write("I", tag, msg)
    fun w(tag: String, msg: String) = write("W", tag, msg)
    fun e(tag: String, msg: String) = write("E", tag, msg)

    private fun write(level: String, tag: String, msg: String) {
        val safeMsg = sanitize(msg)
        val priority = when (level) {
            "E" -> Log.ERROR
            "W" -> Log.WARN
            "D" -> Log.DEBUG
            else -> Log.INFO
        }
        Log.println(priority, "$TAG/$tag", safeMsg)
        if (files.isEmpty()) return
        val line = "[${fmt.format(Date())}] $level/$tag: $safeMsg\n"
        scope.launch {
            try {
                files.forEach { f ->
                    if (f.length() > MAX_SIZE) {
                        f.writeBytes(line.toByteArray())
                    } else {
                        f.appendText(line)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun sanitize(raw: String): String {
        var msg = raw
            .replace(sensitiveParamRegex) { match -> "${match.groupValues[1]}=[redacted]" }
            .replace(urlRegex) { match -> redactUrl(match.value) }
            .replace(addressRegex, "[address redacted]")
            .replace(pathFieldRegex) { match ->
                val key = match.groupValues[1]
                val value = match.groupValues[2]
                if (value.startsWith("[") || value == "[redacted]") {
                    "$key=$value"
                } else {
                    "$key=${redactPath(value)}"
                }
            }
        return msg
    }

    private fun redactUrl(url: String): String {
        val lower = url.lowercase(Locale.ROOT)
        val label = when {
            "/api/nas/" in lower -> "[nas url redacted]"
            "/api/stream/" in lower || "/api/ts/" in lower || "/api/relay/" in lower -> "[stream url redacted]"
            else -> "[url redacted]"
        }
        return label
    }

    private fun redactPath(path: String): String {
        val decoded = runCatching { URLDecoder.decode(path, "UTF-8") }.getOrDefault(path)
        val fileName = decoded
            .trimEnd('/')
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .takeIf { it.isNotBlank() }
        return fileName?.let { "[path redacted: $it]" } ?: "[path redacted]"
    }
}
