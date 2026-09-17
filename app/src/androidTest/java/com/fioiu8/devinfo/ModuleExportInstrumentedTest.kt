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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fioiu8.devinfo.data.ModuleExportHelper
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 导出模块的结构契约。
 *
 * 模块刻意不产出自定义安装脚本：KernelSU 3.x 不再提供 util_functions.sh，自带
 * update-binary 的模块会因环境探测失败而拒绝安装（报「未检测到 Magisk 或 KernelSU」
 * 且零解压退出）。这里锁定该契约，防止自定义安装脚本被重新加回来。
 */
@RunWith(AndroidJUnit4::class)
class ModuleExportInstrumentedTest {

    @Test
    fun exportedModuleCarriesNoCustomInstallerAndKeepsRequiredFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val helper = ModuleExportHelper(context)
        val output = ByteArrayOutputStream()
        var failure: String? = null

        runBlocking {
            helper.exportModuleToStream(
                itemsState = emptyList(),
                outputStream = output,
                onSuccess = {},
                onError = { failure = it },
            )
        }
        assertNull("导出失败: $failure", failure)

        val bytes = output.toByteArray()
        val entries = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.map { it.name }.toList()
        }

        assertTrue("缺少 module.prop: $entries", entries.contains("module.prop"))
        assertTrue("缺少 system.prop: $entries", entries.contains("system.prop"))
        assertFalse("不应再产出 META-INF 安装脚本: $entries", entries.any { it.startsWith("META-INF") })
        assertFalse("不应再产出 install.sh: $entries", entries.contains("install.sh"))
        assertFalse("不应再产出根级 update-binary: $entries", entries.contains("update-binary"))

        // 落盘一份，供真机上用 ksud module install 做实际安装验证。
        val staged = File(context.cacheDir, STAGED_MODULE_FILE)
        staged.writeBytes(bytes)
        assertTrue("落盘失败", staged.length() > 0)
    }

    private companion object {
        const val STAGED_MODULE_FILE = "module-export-instrumented.zip"
    }
}
