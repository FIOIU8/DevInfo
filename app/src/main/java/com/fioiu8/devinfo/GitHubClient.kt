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

package com.fioiu8.devinfo

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub API 客户端 — 获取 release、contributors、languages。
 * 所有网络请求均在 IO 线程进行。
 */
object GitHubClient {

    private const val TAG = "GitHubClient"
    private const val BASE = "https://api.github.com"
    private const val OWNER = "FIOIU8"
    private const val REPO = "DevInfo"

    /** Resolved error messages from string resources. Populated lazily on first use. */
    private var errorMessageRelease: String? = null
    private var errorMessageContributors: String? = null
    private var errorMessageLanguages: String? = null
    private var errorMessageNetwork: String? = null

    private fun ensureMessages(context: Context) {
        if (errorMessageRelease == null) {
            val res = context.applicationContext.resources
            errorMessageRelease = res.getString(R.string.error_fetch_release)
            errorMessageContributors = res.getString(R.string.error_fetch_contributors)
            errorMessageLanguages = res.getString(R.string.error_fetch_languages)
            errorMessageNetwork = res.getString(R.string.error_network)
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

    /** GitHub 仓库贡献者 */
    data class Contributor(
        val login: String,
        val avatarUrl: String,
        val htmlUrl: String,
        val contributions: Int
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
        val cleaned = tag.removePrefix("v").removePrefix("V")
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
                return@withContext ApiResult.Error(errorMessageRelease!!)
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
            ApiResult.Error(e.message ?: errorMessageNetwork!!)
        }
    }

    suspend fun getContributors(context: Context): ApiResult<List<Contributor>> = withContext(Dispatchers.IO) {
        try {
            ensureMessages(context)
            val arr = getJsonArray("/repos/$OWNER/$REPO/contributors?per_page=10") ?: run {
                return@withContext ApiResult.Error(errorMessageContributors!!)
            }
            val list = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Contributor(
                    login = obj.optString("login", ""),
                    avatarUrl = obj.optString("avatar_url", ""),
                    htmlUrl = obj.optString("html_url", ""),
                    contributions = obj.optInt("contributions", 0)
                )
            }
            ApiResult.Success(list)
        } catch (e: Exception) {
            Log.e(TAG, "getContributors failed", e)
            ApiResult.Error(e.message ?: errorMessageNetwork!!)
        }
    }

    suspend fun getLanguages(context: Context): ApiResult<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            ensureMessages(context)
            val json = getJson("/repos/$OWNER/$REPO/languages") ?: run {
                return@withContext ApiResult.Error(errorMessageLanguages!!)
            }
            val map = mutableMapOf<String, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optInt(key, 0)
            }
            ApiResult.Success(map)
        } catch (e: Exception) {
            Log.e(TAG, "getLanguages failed", e)
            ApiResult.Error(e.message ?: errorMessageNetwork!!)
        }
    }

    // ── HTTP ──

    /** 通用 GET + 解析方法，抽取 getJson/getJsonArray 的公共错误处理逻辑 */
    private inline fun <T> fetchAndParse(path: String, parse: (String) -> T): T? {
        val (code, body) = httpGet(path)
        if (code != HttpURLConnection.HTTP_OK) {
            Log.w(TAG, "GET $path → $code: $body")
            return null
        }
        if (body.isNullOrBlank()) return null
        return try { parse(body) } catch (_: Exception) { null }
    }

    private fun getJson(path: String): JSONObject? = fetchAndParse(path) { JSONObject(it) }

    private fun getJsonArray(path: String): JSONArray? = fetchAndParse(path) { JSONArray(it) }

    /** 返回 (responseCode, body) */
    private fun httpGet(path: String): Pair<Int, String?> {
        return try {
            val url = URL("$BASE$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "DevInfo-App")
            val token = System.getenv("GITHUB_TOKEN")
            if (!token.isNullOrBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
            }
            val code = conn.responseCode
            val body = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
            }
            conn.disconnect()
            Pair(code, body)
        } catch (e: Exception) {
            Log.e(TAG, "HTTP GET $path failed", e)
            Pair(-1, e.message)
        }
    }
}
