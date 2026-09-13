package com.fioiu8.devinfo.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GitHubClient.isNewerVersion 的纯逻辑单元测试。
 *
 * 该函数决定是否向用户提示新版本：版本解析出错会造成漏提示或误提示，因此覆盖
 * v 前缀、预发布后缀、缺失段与构建元数据等情况。测试不依赖 Android 框架。
 */
class GitHubClientVersionTest {

    @Test
    fun `higher remote patch version is newer`() {
        assertTrue(GitHubClient.isNewerVersion(remoteTag = "v1.2.3", currentVersion = "1.2.2"))
    }

    @Test
    fun `lower remote version is not newer`() {
        assertFalse(GitHubClient.isNewerVersion(remoteTag = "1.2.2", currentVersion = "1.2.3"))
    }

    @Test
    fun `identical version is not newer`() {
        assertFalse(GitHubClient.isNewerVersion(remoteTag = "v1.2.3", currentVersion = "1.2.3"))
    }

    @Test
    fun `major and minor bumps are newer`() {
        assertTrue(GitHubClient.isNewerVersion(remoteTag = "v2.0.0", currentVersion = "1.9.9"))
        assertTrue(GitHubClient.isNewerVersion(remoteTag = "v1.3.0", currentVersion = "1.2.9"))
    }

    @Test
    fun `prerelease suffix is stripped before comparing`() {
        assertTrue(GitHubClient.isNewerVersion(remoteTag = "v1.2.3-beta.1", currentVersion = "1.2.2"))
        assertFalse(GitHubClient.isNewerVersion(remoteTag = "v1.2.3-rc1", currentVersion = "1.2.3"))
    }

    @Test
    fun `build metadata is ignored`() {
        assertFalse(GitHubClient.isNewerVersion(remoteTag = "v1.2.3+build.5", currentVersion = "1.2.3"))
    }

    @Test
    fun `missing segments default to zero`() {
        assertTrue(GitHubClient.isNewerVersion(remoteTag = "v1.3", currentVersion = "1.2.9"))
        assertTrue(GitHubClient.isNewerVersion(remoteTag = "v2", currentVersion = "1.9.9"))
    }

    @Test
    fun `unparsable input does not report a newer version`() {
        // 本地 dev 构建的版本名形如 dev-<sha>，不能因此提示存在新版本
        assertFalse(GitHubClient.isNewerVersion(remoteTag = "dev-abc1234", currentVersion = "1.0.0"))
        assertFalse(GitHubClient.isNewerVersion(remoteTag = "", currentVersion = "1.0.0"))
    }
}
