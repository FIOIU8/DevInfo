/*
 * Copyright (C) 2026 FIOIU8
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.fioiu8.devinfo.data
import com.fioiu8.devinfo.data.R

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * GitHub API 客户端 — 获取最新 Release 信息。
 * 所有网络请求均在 IO 线程进行。
 */
object GitHubClient {

    private const val TAG = "GitHubClient"
    private const val BASE = "https://api.github.com"
    private const val OWNER = "FIOIU8"
    private const val REPO = "DevInfo"
    private const val MAX_RESPONSE_BYTES = 1024 * 1024
    private const val MAX_LOG_BODY_LENGTH = 512

    /** Resolved error messages from string resources. Populated lazily on first use. */
    @Volatile private var errorMessageRelease: String? = null
    @Volatile private var errorMessageNetwork: String? = null

    private fun ensureMessages(context: Context) {
        if (errorMessageRelease == null) {
            val res = context.applicationContext.resources
            val release = res.getString(R.string.error_fetch_release)
            val network = res.getString(R.string.error_network)
            errorMessageNetwork = network
            // errorMessageRelease 是并发方的判断哨兵，必须最后写入：
            // 其他线程一旦观察到它非空就会直接读取 errorMessageNetwork
            errorMessageRelease = release
        }
    }

    /** GitHub Release 信息 */
    data class ReleaseInfo(
        val tagName: String,
        val name: String,
        val body: String,
        val htmlUrl: String,
        /** 首个 .apk 资源的下载链接，若没有则为 null */
        val downloadUrl: String?
    )

    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val message: String) : ApiResult<Nothing>()
    }

    /** 比较远程 tag 与当前版本是否为更高版本（semver 格式） */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remote = parseVersion(remoteTag)
        val current = parseVersion(currentVersion)
        for (i in 0..2) {
            if (remote[i] > current[i]) return true
            if (remote[i] < current[i]) return false
        }
        return false
    }

    /** 解析 semver 字符串为三元组，缺失部分默认为 0 */
    private fun parseVersion(tag: String): IntArray {
        // 先剥离预发布/构建后缀（如 -beta.1、+build.2），否则 "3-beta" 解析为 0
        val cleaned = tag.removePrefix("v").removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
        val parts = cleaned.split(".").map { it.toIntOrNull() ?: 0 }
        return intArrayOf(
            parts.getOrElse(0) { 0 },
            parts.getOrElse(1) { 0 },
            parts.getOrElse(2) { 0 }
        )
    }

    suspend fun getLatestRelease(context: Context): ApiResult<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            ensureMessages(context)
            val json = getJson("/repos/$OWNER/$REPO/releases/latest") ?: run {
                return@withContext ApiResult.Error(errorMessageRelease ?: "Failed to fetch release")
            }
            val assets = json.optJSONArray("assets")
            val downloadUrl = assets
                ?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
                ?.firstOrNull { it.optString("name", "").endsWith(".apk") }
                ?.optString("browser_download_url")
            ApiResult.Success(
                ReleaseInfo(
                    tagName = json.optString("tag_name", ""),
                    name = json.optString("name", ""),
                    body = json.optString("body", ""),
                    htmlUrl = json.optString("html_url", ""),
                    downloadUrl = downloadUrl
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "getLatestRelease failed", e)
            ApiResult.Error(e.message ?: errorMessageNetwork ?: "Network error")
        }
    }

    // ── HTTP ──

    /** 通用 GET + 解析方法，抽取 getJson 的公共错误处理逻辑 */
    private inline fun <T> fetchAndParse(path: String, parse: (String) -> T): T? {
        val (code, body) = httpGet(path)
        if (code != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "GET $path → $code: ${body?.take(MAX_LOG_BODY_LENGTH)}")
            return null
        }
        if (body.isNullOrBlank()) return null
        return try { parse(body) } catch (_: Exception) { null }
    }

    private fun getJson(path: String): JSONObject? = fetchAndParse(path) { JSONObject(it) }

    /** 返回 (responseCode, body) */
    private fun httpGet(path: String): Pair<Int, String?> {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE$path")
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "DevInfo-App")
            val token = System.getenv("GITHUB_TOKEN")
            if (!token.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            val stream = try {
                conn.inputStream
            } catch (_: Exception) {
                conn.errorStream
            }
            val body = stream?.let(::readResponseBody)
            return Pair(code, body)
        } catch (e: Exception) {
            Log.e(TAG, "HTTP GET $path failed", e)
            return Pair(-1, e.message)
        } finally {
            conn?.disconnect()
        }
    }

    private fun readResponseBody(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0
        input.use { stream ->
            while (true) {
                val bytesRead = stream.read(buffer)
                if (bytesRead < 0) break
                totalBytes += bytesRead
                if (totalBytes > MAX_RESPONSE_BYTES) {
                    throw IOException("Response body exceeds $MAX_RESPONSE_BYTES bytes")
                }
                output.write(buffer, 0, bytesRead)
            }
        }
        return output.toByteArray().toString(StandardCharsets.UTF_8)
    }
}
