package com.fioiu8.devinfo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 更新检查器 — 封装 GitHub API + 12 小时缓存。
 */
class UpdateChecker(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UpdateState.IDLE)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _releaseInfo = MutableStateFlow<GitHubClient.ReleaseInfo?>(null)
    val releaseInfo: StateFlow<GitHubClient.ReleaseInfo?> = _releaseInfo.asStateFlow()

    /** 当前是否正在检查 */
    val isChecking: Boolean get() = _state.value == UpdateState.CHECKING

    /** 开始异步检查 */
    suspend fun check(currentVersion: String) {
        if (!BuildConfig.IS_OFFICIAL) return

        _state.value = UpdateState.CHECKING

        // 12小时缓存
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        val now = System.currentTimeMillis()
        if (now - lastCheck < CACHE_DURATION_MS) {
            val cachedTag = prefs.getString(KEY_CACHED_TAG, currentVersion) ?: currentVersion
            if (!GitHubClient.isNewerVersion(cachedTag, currentVersion)) {
                _state.value = UpdateState.UP_TO_DATE
                return
            }
        }

        when (val result = GitHubClient.getLatestRelease()) {
            is GitHubClient.ApiResult.Success -> {
                val info = result.data
                _releaseInfo.value = info
                prefs.edit()
                    .putLong(KEY_LAST_CHECK, now)
                    .putString(KEY_CACHED_TAG, info.tagName)
                    .apply()
                _state.value = if (info.tagName.isNotBlank() &&
                    GitHubClient.isNewerVersion(info.tagName, currentVersion)
                ) {
                    UpdateState.NEW_VERSION_AVAILABLE
                } else {
                    UpdateState.UP_TO_DATE
                }
            }
            is GitHubClient.ApiResult.Error -> {
                _state.value = UpdateState.ERROR
            }
        }
    }

    /** 重置状态 */
    fun reset() { _state.value = UpdateState.IDLE }

    // ── 常量 ──

    companion object {
        private const val PREFS_NAME = "devinfo_update"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_CACHED_TAG = "cached_tag"
        private const val CACHE_DURATION_MS = 12 * 60 * 60 * 1000L // 12 小时
    }
}

enum class UpdateState {
    IDLE,            // 未检查
    CHECKING,        // 正在检查
    UP_TO_DATE,      // 已是最新
    NEW_VERSION_AVAILABLE, // 有新版本
    ERROR            // 检查失败
}
