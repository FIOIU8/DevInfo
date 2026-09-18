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

import android.content.Context
import com.fioiu8.devinfo.core.model.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 更新检查器 — 封装 GitHub API + 12 小时缓存。
 */
class UpdateChecker(
    private val context: Context,
    private val isOfficialBuild: Boolean
) {

    // 构造 DataStore 仓库不做任何 I/O（I/O 失败已在仓库内部按读/写分别兜底），
    // 因此这里无需额外的降级分支。
    private val cacheRepository: PreferenceRepository = DataStorePreferenceRepository(context)

    private val _state = MutableStateFlow(UpdateState.IDLE)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _releaseInfo = MutableStateFlow<GitHubClient.ReleaseInfo?>(null)
    val releaseInfo: StateFlow<GitHubClient.ReleaseInfo?> = _releaseInfo.asStateFlow()

    private val checkMutex = Mutex()

    /** 开始异步检查 */
    suspend fun check(currentVersion: String): Unit = checkMutex.withLock {
        if (!isOfficialBuild) return

        _state.value = UpdateState.CHECKING

        // 12小时缓存
        val now = System.currentTimeMillis()
        val lastCheck = PreferenceValidators.validLastCheckTimeOrZero(
            cacheRepository.readLong(UpdateCacheKeys.LAST_CHECK) ?: 0L,
            now,
        )
        if (now - lastCheck < CACHE_DURATION_MS) {
            val cachedTag = cacheRepository.readString(UpdateCacheKeys.CACHED_TAG) ?: currentVersion
            if (!GitHubClient.isNewerVersion(cachedTag, currentVersion)) {
                _state.value = UpdateState.UP_TO_DATE
                return
            }
            // 缓存窗口内且缓存 tag 已是更新版本：直接用缓存的发布信息离线复现
            // 更新对话框。原实现会在这种最常见场景下每次启动都请求网络，
            // 与缓存设计初衷相悖
            readCachedReleaseInfo(cachedTag)?.let { info ->
                _releaseInfo.value = info
                _state.value = UpdateState.NEW_VERSION_AVAILABLE
                return
            }
        }

        when (val result = GitHubClient.getLatestRelease(context)) {
            is GitHubClient.ApiResult.Success -> {
                val info = result.data
                _releaseInfo.value = info
                cacheRepository.writeBatch(
                    mapOf(
                        UpdateCacheKeys.LAST_CHECK to PreferenceValue.LongValue(now),
                        UpdateCacheKeys.CACHED_TAG to PreferenceValue.StringValue(info.tagName),
                        UpdateCacheKeys.RELEASE_NAME to PreferenceValue.StringValue(info.name),
                        UpdateCacheKeys.RELEASE_BODY to PreferenceValue.StringValue(info.body),
                        UpdateCacheKeys.RELEASE_URL to PreferenceValue.StringValue(info.htmlUrl),
                        UpdateCacheKeys.RELEASE_DOWNLOAD_URL to PreferenceValue.StringValue(info.downloadUrl.orEmpty()),
                    ),
                )
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

    private suspend fun readCachedReleaseInfo(
        cachedTag: String
    ): GitHubClient.ReleaseInfo? {
        val name = cacheRepository.readString(UpdateCacheKeys.RELEASE_NAME) ?: return null
        val htmlUrl = cacheRepository.readString(UpdateCacheKeys.RELEASE_URL)?.takeIf { it.isNotBlank() } ?: return null
        return GitHubClient.ReleaseInfo(
            tagName = cachedTag,
            name = name,
            body = cacheRepository.readString(UpdateCacheKeys.RELEASE_BODY).orEmpty(),
            htmlUrl = htmlUrl,
            downloadUrl = cacheRepository.readString(UpdateCacheKeys.RELEASE_DOWNLOAD_URL)?.takeIf { it.isNotBlank() }
        )
    }

    /** 重置状态 */
    fun reset() { _state.value = UpdateState.IDLE }

    // ── 常量 ──

    companion object {
        private const val CACHE_DURATION_MS = 12 * 60 * 60 * 1000L // 12 小时
    }
}
