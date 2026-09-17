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

// 导入 Android 相关类
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.fioiu8.devinfo.core.model.ItemWithVisibility
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 模块导出助手类，负责生成 Magisk/KernelSU 模块的 ZIP 包。
 *
 * 生成的模块 ZIP 包结构如下（文件名由调用方通过系统文件选择器决定）：
 *
 * DevInfo_<机型>.zip                          # 模块压缩包
 * │
 * ├── system/                                 # 系统文件替换目录
 * │   └── placeholder                         # 说明文件，提示可放置需要替换的系统文件
 * │
 * ├── module.prop                             # 模块信息配置文件（必需）
 * └── system.prop                             # 系统属性配置文件（由 Magisk/KernelSU 自动加载）
 *
 * 刻意不产出任何安装脚本（既无 META-INF/com/google/android/update-binary，也无 install.sh）：
 * 本模块只需要 module.prop 与 system.prop，Magisk 与 KernelSU 的内置安装器
 * （KernelSU 为 ksud module install）已经能完整处理这种模块。
 *
 * 此前的实现内嵌了三份 shell 脚本，环境探测依赖 /data/adb/ksu/util_functions.sh。
 * KernelSU 3.x 已不再提供该文件，模块因此在 KernelSU 上必然报「未检测到 Magisk 或
 * KernelSU」并零解压退出，功能完全不可用（已在 KernelSU 3.2.5 实机复现）。改为不产出
 * 安装脚本后，安装交由各 root 方案自身的安装器，不再依赖特定版本的文件布局。
 *
 * 重启后 Magisk/KernelSU 加载模块根目录的 system.prop，写入 ro.product.* 等系统属性。
 * 模块只在启动时生效，不注册 post-fs-data.sh / service.sh 等启动阶段脚本，
 * 也不引入任何后台进程。
 */
class ModuleExportHelper(private val context: Context) {
    private val locale: Locale = context.resources.configuration.locales[0]

    /**
     * 基于 SAF 的导出方法：将 ZIP 写入给定的 OutputStream。
     * 生成过程在 cacheDir 完成临时文件创建，最后写入流式输出。
     *
     * 注意：内部的 ZipOutputStream 会关闭传入的 [outputStream]，因此调用方在
     * 本方法返回后不应继续写入该流（重复关闭是安全的，但不要依赖它仍可写）。
     *
     * @param itemsState 设备信息项列表
     * @param outputStream 目标输出流（由 SAF ContentResolver 提供）
     * @param onSuccess 成功回调
     * @param onError 失败回调，返回错误信息
     */
    suspend fun exportModuleToStream(
        itemsState: List<ItemWithVisibility>,
        outputStream: OutputStream,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        withContext(Dispatchers.IO) {
            var directories: ModuleDirectories? = null
            try {
                val buildInfo = readDeviceBuildInfo()
                val metadata = createModuleMetadata(itemsState, buildInfo)
                directories = createModuleDirectories()

                writeModuleFiles(directories, metadata, buildInfo)
                writeZipArchive(directories.root, outputStream)
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (e: Exception) {
                onError(e.message ?: context.getString(R.string.error_unknown))
            } finally {
                directories?.root?.deleteRecursively()
            }
        }
    }

    private data class DeviceBuildInfo(
        val model: String,
        val manufacturer: String,
        val brand: String,
        val device: String,
        val product: String,
        val versionRelease: String,
        val versionSdk: String
    )

    private data class ModuleMetadata(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val versionCode: Long,
        val description: String
    )

    private data class AppVersion(
        val name: String,
        val code: Long
    )

    private data class ModuleDirectories(
        val root: File,
        val system: File
    )

    private fun readDeviceBuildInfo(): DeviceBuildInfo = DeviceBuildInfo(
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        versionRelease = Build.VERSION.RELEASE,
        versionSdk = Build.VERSION.SDK_INT.toString()
    )

    private fun createModuleMetadata(
        itemsState: List<ItemWithVisibility>,
        buildInfo: DeviceBuildInfo
    ): ModuleMetadata {
        val moduleId = "Device_${sanitizeIdentifier(buildInfo.model)}"
        val deviceName = getDeviceDisplayName(itemsState)
        val moduleName = context.getString(R.string.module_export_name, deviceName)
        val author = "DevInfo"
        // 模块版本取应用自身的版本名与 versionCode。此前写入的是 Android 版本号且
        // versionCode 恒为 1，Magisk 既无法比较模块版本，也无法提示模块升级。
        val appVersion = readAppVersion()
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(Date())
        val description = context.getString(
            R.string.module_export_description,
            buildInfo.brand,
            buildInfo.model,
            generatedAt
        )
        return ModuleMetadata(
            id = moduleId,
            name = moduleName,
            author = author,
            version = appVersion?.name ?: FALLBACK_MODULE_VERSION,
            versionCode = appVersion?.code ?: FALLBACK_MODULE_VERSION_CODE,
            description = description
        )
    }

    /** 读取应用自身的版本名与 versionCode，供 module.prop 使用。 */
    private fun readAppVersion(): AppVersion? = runCatching {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0),
        )
        val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() }
            ?: return@runCatching null
        AppVersion(name = versionName, code = packageInfo.longVersionCode)
    }.getOrNull()

    private fun createModuleDirectories(): ModuleDirectories {
        val root = java.nio.file.Files.createTempDirectory(
            context.cacheDir.toPath(),
            "module-export-"
        ).toFile()
        try {
            val system = File(root, "system").also(::createDirectory)
            return ModuleDirectories(root, system)
        } catch (e: Exception) {
            root.deleteRecursively()
            throw e
        }
    }

    private fun createDirectory(directory: File) {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException(context.getString(R.string.error_create_export_dir))
        }
    }

    private fun writeModuleFiles(
        directories: ModuleDirectories,
        metadata: ModuleMetadata,
        buildInfo: DeviceBuildInfo
    ) {
        File(directories.root, "module.prop").writeText(
            buildModuleProp(
                id = metadata.id,
                name = metadata.name,
                author = metadata.author,
                version = metadata.version,
                versionCode = metadata.versionCode,
                description = metadata.description
            )
        )
        File(directories.root, "system.prop").writeText(
            buildSystemProp(
                brand = buildInfo.brand,
                manufacturer = buildInfo.manufacturer,
                model = buildInfo.model,
                device = buildInfo.device,
                product = buildInfo.product,
                versionRelease = buildInfo.versionRelease,
                versionSdk = buildInfo.versionSdk
            )
        )
        File(directories.system, "placeholder").writeText(SYSTEM_PLACEHOLDER)
    }

    /**
     * 从设备信息列表中提取制造商和型号，组合成可读的设备名称
     *
     * @param itemsState 设备信息项列表
     * @return 格式为 "制造商 型号" 的设备名称
     */
    private fun getDeviceDisplayName(itemsState: List<ItemWithVisibility>): String {
        // DeviceInfoItem.key stores the resource entry name, not its localized text.
        // 一次 associateBy 构建查找表，避免对列表两次线性扫描。
        val byKey = itemsState.associateBy({ it.item.key }, { it.item.value })
        val manufacturer = byKey["device_manufacturer"] ?: Build.MANUFACTURER
        val model = byKey["device_model"] ?: Build.MODEL
        return sanitizeDisplayValue("$manufacturer $model")
    }

    companion object {
        private const val FALLBACK_FILE_NAME = "module-export"

        /** system/ 目录的占位说明，提示该目录用于替换系统文件。 */
        private const val SYSTEM_PLACEHOLDER =
            "# 此目录用于存放需要替换的系统文件\n" +
                "# 例如：将文件放在 system/build.prop 会替换 /system/build.prop"

        /** 读取应用版本失败时写入 module.prop 的兜底版本信息。 */
        private const val FALLBACK_MODULE_VERSION = "1.0.0"
        private const val FALLBACK_MODULE_VERSION_CODE = 1L

        // 文件名为热路径（每次导出多次调用），正则提为常量避免每行/每次重新编译
        private val FILENAME_SANITIZE_REGEX = Regex("[^a-zA-Z0-9_.-]")
        private val WINDOWS_DRIVE_REGEX = Regex("^[A-Za-z]:.*")
        private val REQUIRED_ZIP_ENTRIES = setOf(
            "module.prop",
            "system.prop"
        )

        /**
         * 转义 .prop 文件中的值：转义换行、回车、反斜杠和等号。
         */
        internal fun escapePropValue(value: String): String {
            return buildString(value.length) {
                value.forEach { character ->
                    when (character) {
                        '\\' -> append("\\\\")
                        '=' -> append("\\=")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        in '\u0000'..'\u001F', '\u007F' -> append('_')
                        else -> append(character)
                    }
                }
            }
        }

        /**
         * 净化文件名：只保留字母、数字、下划线、连字符和点。
         */
        internal fun sanitizeFileName(name: String): String {
            if (name == "." || name == "..") return FALLBACK_FILE_NAME
            var sanitized = name.replace(FILENAME_SANITIZE_REGEX, "_")
            sanitized = sanitized.replace("..", "_")
            return sanitized.ifBlank { FALLBACK_FILE_NAME }
                .takeUnless { it == "." || it == ".." }
                ?: FALLBACK_FILE_NAME
        }

        fun createExportFileName(model: String): String {
            return sanitizeFileName("DevInfo_${sanitizeIdentifier(model)}.zip")
        }

        internal fun isSafeZipEntryName(entryName: String): Boolean {
            if (entryName.isEmpty() || entryName.any(::isUnsafeEntryCharacter)) return false
            if (entryName.startsWith('/') || entryName.startsWith('\\')) return false
            if (entryName.matches(WINDOWS_DRIVE_REGEX)) return false
            if (entryName.contains('\\')) return false
            val path = entryName.removeSuffix("/")
            if (path.isEmpty()) return false
            return path.split('/').none { it.isEmpty() || it == "." || it == ".." }
        }

        internal fun validateZipEntries(entryNames: Collection<String>) {
            require(entryNames.all(::isSafeZipEntryName)) { "ZIP entry 名称不安全" }
            val files = entryNames.map { it.removeSuffix("/") }.toSet()
            require(REQUIRED_ZIP_ENTRIES.all(files::contains)) { "ZIP 缺少必要文件" }
        }

        private fun sanitizeIdentifier(value: String): String {
            return sanitizeFileName(value).replace('.', '_').ifBlank { FALLBACK_FILE_NAME }
        }

        private fun sanitizeDisplayValue(value: String): String {
            return buildString(value.length) {
                value.forEach { character ->
                    when (character) {
                        '\n', '\r', '\t' -> append(' ')
                        in '\u0000'..'\u001F', '\u007F' -> append(' ')
                        else -> append(character)
                    }
                }
            }.trim().ifBlank { "Device" }
        }

        private fun isUnsafeEntryCharacter(character: Char): Boolean {
            return character.code <= 0x1F || character.code == 0x7F ||
                Character.isSurrogate(character)
        }
    }

    /**
     * 构建 module.prop 文件的内容
     * 这是 Magisk/KernelSU 模块的必需文件，定义了模块的元数据
     *
     * @param id 模块唯一标识符
     * @param name 模块显示名称
     * @param author 模块作者
     * @param version 模块版本字符串
     * @param versionCode 模块版本码，Magisk 用它比较模块版本
     * @param description 模块描述信息
     * @return module.prop 文件内容
     */
    private fun buildModuleProp(
        id: String,
        name: String,
        author: String,
        version: String,
        versionCode: Long,
        description: String
    ): String {
        return """
id=${escapePropValue(id)}
name=${escapePropValue(name)}
version=${escapePropValue(version)}
versionCode=$versionCode
author=${escapePropValue(author)}
description=${escapePropValue(description)}
        """.trimIndent()
    }

    /**
     * 构建 system.prop 文件的内容
     * 该文件中的属性会在系统启动时被 Magisk 自动注入到系统属性中
     *
     * @param brand 品牌
     * @param manufacturer 制造商
     * @param model 型号
     * @param device 设备代号
     * @param product 产品名称
     * @param versionRelease Android 版本
     * @param versionSdk SDK 版本
     * @return system.prop 文件内容
     */
    private fun buildSystemProp(
        brand: String,
        manufacturer: String,
        model: String,
        device: String,
        product: String,
        versionRelease: String,
        versionSdk: String
    ): String {
        val supportedAbis = Build.SUPPORTED_ABIS.joinToString(",") { escapePropValue(it) }
        val supported32BitAbis = Build.SUPPORTED_32_BIT_ABIS.joinToString(",") { escapePropValue(it) }
        val supported64BitAbis = Build.SUPPORTED_64_BIT_ABIS.joinToString(",") { escapePropValue(it) }
        return """
# ============================================
# System Properties for Device Simulation
# Generated by DeviceInfo App
# ============================================
# Generation Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(Date())}
# ============================================

# Brand & Manufacturer（品牌和制造商）
ro.product.brand=${escapePropValue(brand)}
ro.product.manufacturer=${escapePropValue(manufacturer)}

# Model & Device（型号和设备代号）
ro.product.model=${escapePropValue(model)}
ro.product.device=${escapePropValue(device)}
ro.product.name=${escapePropValue(product)}

# Version Info（版本信息）
ro.build.version.release=${escapePropValue(versionRelease)}
ro.build.version.sdk=${escapePropValue(versionSdk)}

# Additional Properties（附加属性）
ro.build.product=${escapePropValue(device)}
ro.product.board=${escapePropValue(device)}
ro.product.cpu.abi=${escapePropValue(Build.SUPPORTED_ABIS.firstOrNull().orEmpty())}
ro.product.cpu.abilist=$supportedAbis
ro.product.cpu.abilist32=$supported32BitAbis
ro.product.cpu.abilist64=$supported64BitAbis
        """.trimIndent()
    }

    private fun writeZipArchive(root: File, outputStream: OutputStream) {
        // 先只收集条目名并校验，再写入目标流。原实现是边写边收集、写完后才校验：
        // 校验失败时通过 SAF 选中的文件已经写入了完整字节，无法回滚。
        // 收集名称只做 listFiles 遍历、不读取文件内容，代价可忽略。
        val entryNames = collectZipEntryNames(root)
        validateZipEntries(entryNames)

        ZipOutputStream(outputStream).use { zipOut ->
            zipDirectory(root, "", zipOut)
            zipOut.finish()
        }
    }

    /**
     * 递归收集 ZIP 条目名（目录条目以 "/" 结尾），用于写入前的结构校验。
     *
     * @param dir 要收集的目录
     * @param parentPath ZIP 中的父路径
     */
    private fun collectZipEntryNames(dir: File, parentPath: String = ""): List<String> {
        val children = dir.listFiles()?.sortedBy(File::getName)
            ?: throw IOException(context.getString(R.string.error_read_export_dir))
        return buildList {
            children.forEach { file ->
                val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
                require(isSafeZipEntryName(entryPath)) { "ZIP entry 名称不安全" }
                if (file.isDirectory) {
                    add("$entryPath/")
                    addAll(collectZipEntryNames(file, entryPath))
                } else {
                    add(entryPath)
                }
            }
        }
    }

    /**
     * 递归地将目录及其所有子文件和子文件夹写入 ZIP 输出流。每个条目写入前都会再次
     * 校验名称安全，避免调用方绕过 [collectZipEntryNames] 的预校验。
     *
     * ZIP 文件结构示例：
     * module.zip
     * ├── system/
     * │   └── placeholder
     * ├── module.prop
     * └── system.prop
     *
     * @param dir 要打包的目录
     * @param parentPath ZIP 中的父路径
     * @param zipOut ZIP 输出流
     */
    private fun zipDirectory(dir: File, parentPath: String, zipOut: ZipOutputStream) {
        val children = dir.listFiles()?.sortedBy(File::getName)
            ?: throw IOException(context.getString(R.string.error_read_export_dir))
        children.forEach { file ->
            // 构建在 ZIP 中的条目路径
            // 如果是根目录，直接使用文件名；否则添加父路径前缀
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            require(isSafeZipEntryName(entryPath)) { "ZIP entry 名称不安全" }

            if (file.isDirectory) {
                // 如果是目录，在 ZIP 中添加目录条目（以 / 结尾）
                zipOut.putNextEntry(ZipEntry("$entryPath/"))
                zipOut.closeEntry()
                // 递归处理子目录
                zipDirectory(file, entryPath, zipOut)
            } else {
                // 如果是文件，添加到 ZIP 中
                zipOut.putNextEntry(ZipEntry(entryPath))
                file.inputStream().use { input ->
                    input.copyTo(zipOut)  // 将文件内容复制到 ZIP 流
                }
                zipOut.closeEntry()
            }
        }
    }
}
