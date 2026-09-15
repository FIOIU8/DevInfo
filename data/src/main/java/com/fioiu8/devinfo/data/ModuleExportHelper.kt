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
import com.fioiu8.devinfo.core.model.ModuleExportPolicy
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
 * ├── META-INF/                               # Magisk/KernelSU 必需的签名和脚本目录
 * │   └── com/
 * │       └── google/
 * │           └── android/
 * │               ├── update-binary           # 刷机脚本（实际执行逻辑）
 * │               └── updater-script          # 刷机脚本描述（指向 update-binary）
 * │
 * ├── system/                                 # 系统文件替换目录
 * │   └── placeholder                         # 说明文件，提示可放置需要替换的系统文件
 * │
 * ├── module.prop                             # 模块信息配置文件（必需）
 * ├── system.prop                             # 系统属性配置文件（由 Magisk/KernelSU 自动加载）
 * ├── install.sh                              # 模块安装时的执行脚本
 * └── update-binary                           # 备用 update-binary（根目录版本）
 *
 * Magisk/KernelSU 模块工作原理：
 * 1. 用户通过 Magisk/KernelSU 刷入 ZIP 包
 * 2. 系统首先执行 META-INF/com/google/android/update-binary
 * 3. update-binary 加载 Magisk/KernelSU 工具函数，解压 ZIP 到 /data/adb/modules/[module_id]/
 * 4. 执行 install.sh 中的安装与权限设置函数
 * 5. 重启后 Magisk/KernelSU 加载根目录 system.prop，写入 ro.product.* 等系统属性
 *
 * 模块只在启动时通过 system.prop 生效，不注册 post-fs-data.sh / service.sh 等启动
 * 阶段脚本，也不引入任何后台进程。
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
     * @param deviceId 设备唯一标识符
     * @param itemsState 设备信息项列表
     * @param outputStream 目标输出流（由 SAF ContentResolver 提供）
     * @param onSuccess 成功回调
     * @param onError 失败回调，返回错误信息
     */
    suspend fun exportModuleToStream(
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        outputStream: OutputStream,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        policy: ModuleExportPolicy = ModuleExportPolicy.MINIMAL
    ) {
        withContext(Dispatchers.IO) {
            var directories: ModuleDirectories? = null
            try {
                val buildInfo = readDeviceBuildInfo()
                val metadata = createModuleMetadata(itemsState, buildInfo)
                directories = createModuleDirectories()

                writeModuleFiles(directories, metadata, buildInfo, deviceId, policy)
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
        val fingerprint: String,
        val versionRelease: String,
        val versionSdk: String,
        val securityPatch: String
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
        val metaInf: File,
        val system: File
    )

    private fun readDeviceBuildInfo(): DeviceBuildInfo = DeviceBuildInfo(
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        fingerprint = Build.FINGERPRINT,
        versionRelease = Build.VERSION.RELEASE,
        versionSdk = Build.VERSION.SDK_INT.toString(),
        securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty()
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
            val metaInf = File(root, "META-INF/com/google/android").also(::createDirectory)
            val system = File(root, "system").also(::createDirectory)
            return ModuleDirectories(root, metaInf, system)
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
        buildInfo: DeviceBuildInfo,
        deviceId: String,
        policy: ModuleExportPolicy
    ) {
        writeModuleProp(directories.root, metadata)
        writeSystemProp(directories.root, buildInfo, deviceId, policy)
        writeInstallScript(directories.root)
        writeRootUpdateBinary(directories.root)
        writeUpdaterScript(directories.metaInf)
        writeMetaUpdateBinary(directories.metaInf)
        writeSystemPlaceholder(directories.system)
    }

    private fun writeModuleProp(directory: File, metadata: ModuleMetadata) {
        File(directory, "module.prop").writeText(
            buildModuleProp(
                id = metadata.id,
                name = metadata.name,
                author = metadata.author,
                version = metadata.version,
                versionCode = metadata.versionCode,
                description = metadata.description
            )
        )
    }

    private fun writeSystemProp(
        directory: File,
        buildInfo: DeviceBuildInfo,
        deviceId: String,
        policy: ModuleExportPolicy
    ) {
        File(directory, "system.prop").writeText(
            buildSystemProp(
                brand = buildInfo.brand,
                manufacturer = buildInfo.manufacturer,
                model = buildInfo.model,
                device = buildInfo.device,
                product = buildInfo.product,
                fingerprint = buildInfo.fingerprint,
                versionRelease = buildInfo.versionRelease,
                versionSdk = buildInfo.versionSdk,
                securityPatch = buildInfo.securityPatch,
                deviceId = deviceId,
                policy = policy
            )
        )
    }

    private fun writeInstallScript(directory: File) {
        File(directory, "install.sh").writeText(buildInstallScript())
    }

    private fun writeRootUpdateBinary(directory: File) {
        File(directory, "update-binary").writeText(buildUpdateBinary())
    }

    private fun writeUpdaterScript(directory: File) {
        File(directory, "updater-script").writeText(buildUpdaterScript())
    }

    private fun writeMetaUpdateBinary(directory: File) {
        File(directory, "update-binary").writeText(buildMetaUpdateBinary())
    }

    private fun writeSystemPlaceholder(directory: File) {
        File(directory, "placeholder").writeText(
            "# 此目录用于存放需要替换的系统文件\n" +
                "# 例如：将文件放在 system/build.prop 会替换 /system/build.prop"
        )
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

        /** 读取应用版本失败时写入 module.prop 的兜底版本信息。 */
        private const val FALLBACK_MODULE_VERSION = "1.0.0"
        private const val FALLBACK_MODULE_VERSION_CODE = 1L

        // 文件名为热路径（每次导出多次调用），正则提为常量避免每行/每次重新编译
        private val FILENAME_SANITIZE_REGEX = Regex("[^a-zA-Z0-9_.-]")
        private val WINDOWS_DRIVE_REGEX = Regex("^[A-Za-z]:.*")
        private val REQUIRED_ZIP_ENTRIES = setOf(
            "module.prop",
            "system.prop",
            "META-INF/com/google/android/update-binary",
            "META-INF/com/google/android/updater-script"
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
         * 转义 shell 字符串值。
         */
        internal fun escapeShellValue(value: String): String {
            return buildString(value.length) {
                value.forEach { character ->
                    when (character) {
                        '\\' -> append("\\\\")
                        '\'' -> append("'\\''")
                        '\$' -> append("\\$")
                        '`' -> append("\\`")
                        '"' -> append("\\\"")
                        '\n', '\r', '\t' -> append(' ')
                        in '\u0000'..'\u001F', '\u007F' -> append(' ')
                        else -> append(character)
                    }
                }
            }
        }

        /**
         * Returns a complete single-quoted shell literal for callers that must
         * place a dynamic value in a script.
         */
        internal fun quoteShellValue(value: String): String {
            val normalized = buildString(value.length) {
                value.forEach { character ->
                    when (character) {
                        '\n', '\r', '\t' -> append(' ')
                        in '\u0000'..'\u001F', '\u007F' -> append(' ')
                        else -> append(character)
                    }
                }
            }
            return "'${normalized.replace("'", "'\\''")}'"
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
     * @param fingerprint 构建指纹
     * @param versionRelease Android 版本
     * @param versionSdk SDK 版本
     * @param securityPatch 安全补丁日期
     * @return system.prop 文件内容
     */
    private fun buildSystemProp(
        brand: String,
        manufacturer: String,
        model: String,
        device: String,
        product: String,
        fingerprint: String,
        versionRelease: String,
        versionSdk: String,
        securityPatch: String,
        deviceId: String,
        policy: ModuleExportPolicy
    ): String {
        val optionalProperties = buildString {
            if (policy.includeBuildFingerprint) {
                append("ro.build.fingerprint=${escapePropValue(fingerprint)}\n")
            }
            if (policy.includeSecurityPatch) {
                append("ro.build.version.security_patch=${escapePropValue(securityPatch)}\n")
            }
            if (policy.includeDeviceIdentifier) {
                append("devinfo.device_id=${escapePropValue(deviceId)}\n")
            }
        }.trimEnd()
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

# Build Fingerprint（构建指纹）
$optionalProperties

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

    /**
     * 构建 install.sh 脚本的内容
     * 该脚本由 update-binary 在解压模块后加载，提供 on_install() 与 set_permissions()
     * 两个入口，并打印安装信息
     *
     * @return install.sh 脚本内容
     */
    private fun buildInstallScript(): String {
        val dollar = '$'
        return """
#!/system/bin/sh
# ============================================
# Magisk/KernelSU Module Install Script
# Generated by DeviceInfo App
# ============================================

##########################################################################################
# Installation Message（安装信息显示函数）
##########################################################################################

# 输出安装信息。原实现会遍历 /data/user/0/com.coolapk.market/shared_prefs 读取第三方
# 应用的私有 SharedPreferences 提取用户名，属于以 root 身份读取他人私有数据，已移除。
show_install_banner() {
    device_name="${dollar}(getprop persist.sys.device_name)"
    echo ""
    if [ -n "${dollar}device_name" ]; then
        echo "您好！${dollar}{device_name}！"
    fi
    echo "*******************************"
    echo "    全局机型模拟模块"
    echo "    设备属性来源: system.prop"
    echo "*******************************"
    echo "  注意: 刷入后请重启设备以生效！"
    echo "*******************************"
}

# 显示安装信息
show_install_banner

##########################################################################################
# Permissions（权限设置）
##########################################################################################

# 模块安装函数。模块文件已由 update-binary 解压到 ${dollar}MODPATH，此处只确认属性已就绪，
# 不再二次解压整个 ZIP。
on_install() {
  ui_print "- 目标设备属性已写入 system.prop"
  ui_print "- 无需额外文件操作"
}

# 设置文件和目录权限的函数
set_permissions() {
  # 递归设置模块目录的权限：所有者 root，组 root，目录 755，文件 644
  set_perm_recursive ${dollar}MODPATH 0 0 0755 0644
  
  # 示例：为特定可执行文件设置执行权限
  # set_perm ${dollar}MODPATH/system/bin/some_binary 0 0 0755
}
        """.trimIndent()
    }

    /**
     * 构建 update-binary 文件的内容（根目录版本）
     * 这是一个安装脚本，负责模块的安装流程
     *
     * @return update-binary 脚本内容
     */
    private fun buildUpdateBinary(): String {
        val dollar = '$'
        return """
#!/sbin/sh

# ============================================
# Update Binary Script
# Generated by DeviceInfo App
# ============================================

#################
# Initialization
#################

umask 022  # 设置默认文件权限掩码

# 定义用于输出信息给用户的函数
ui_print() { 
    echo "${dollar}1"
}

# 检查 KernelSU 版本是否满足要求
require_new_ksud() {
  ui_print "*******************************"
  ui_print " 错误: 需要 KernelSU v0.6.6+！"
  ui_print " 请升级您的 KernelSU 版本"
  ui_print "*******************************"
  exit 1
}

# 把 "KernelSU v0.9.6" 之类的版本字符串编码为可比较的数字（major*10000+minor*100+patch）。
# 原实现直接对整串执行 [ -lt 666 ]，非纯数字输入会报 "Illegal number" 并得出错误结论。
parse_ksud_version_code() {
  version_text="${dollar}1"
  major="${dollar}(echo "${dollar}version_text" | sed -n 's/.*[vV]\([0-9][0-9]*\)\..*/\1/p')"
  minor="${dollar}(echo "${dollar}version_text" | sed -n 's/.*[vV][0-9][0-9]*\.\([0-9][0-9]*\).*/\1/p')"
  patch="${dollar}(echo "${dollar}version_text" | sed -n 's/.*[vV][0-9][0-9]*\.[0-9][0-9]*\.\([0-9][0-9]*\).*/\1/p')"
  [ -n "${dollar}major" ] || return 1
  [ -n "${dollar}minor" ] || minor=0
  [ -n "${dollar}patch" ] || patch=0
  echo "${dollar}((major * 10000 + minor * 100 + patch))"
}

#################
# Load util_functions
#################

# 加载 Magisk 或 KernelSU 的工具函数库
if [ -f /data/adb/ksu/util_functions.sh ]; then
  # KernelSU 环境
  . /data/adb/ksu/util_functions.sh
  KSU=true
elif [ -f /data/adb/magisk/util_functions.sh ]; then
  # Magisk 环境
  . /data/adb/magisk/util_functions.sh
  KSU=false
else
  ui_print "! 错误: 找不到 Magisk/KernelSU 工具函数库"
  ui_print "! 请确保您已安装 Magisk 或 KernelSU"
  exit 1
fi

#################
# Main
#################

# 如果是 KernelSU，检查版本是否足够新。0.6.6 编码为 606；
# 解析失败时跳过校验，避免因版本输出格式未知而阻断安装。
if [ "${dollar}KSU" = "true" ]; then
  ksud_version="${dollar}(ksud -v 2>/dev/null)"
  ksud_version_code="${dollar}(parse_ksud_version_code "${dollar}ksud_version")"
  if [ -n "${dollar}ksud_version_code" ] && [ "${dollar}ksud_version_code" -lt 606 ]; then
    require_new_ksud
  fi
  ui_print "- KernelSU 版本检测通过"
else
  ui_print "- Magisk 环境检测通过"
fi

# 解压模块文件到目标路径
ui_print "- 正在解压模块文件..."
unzip -o "${dollar}ZIPFILE" -d "${dollar}MODPATH" >&2

# 如果存在 install.sh，则加载并执行其中的配置和权限设置函数
if [ -f "${dollar}MODPATH/install.sh" ]; then
  ui_print "- 正在执行安装脚本..."
  . "${dollar}MODPATH/install.sh"

  # 用 command -v 判断函数是否存在。原实现依赖 type 的输出文案（grep 'function'），
  # 在不同 shell 的本地化输出下不可靠。
  if command -v on_install >/dev/null 2>&1; then
    on_install
  fi

  if command -v set_permissions >/dev/null 2>&1; then
    set_permissions
  fi
fi

ui_print "- 模块安装完成！"
ui_print "- 请重启设备以使模块生效"
        """.trimIndent()
    }

    /**
     * 构建 updater-script 文件的内容（META-INF 目录）
     * 这是刷机脚本的描述文件，通常只是注释
     *
     * @return updater-script 文件内容
     */
    private fun buildUpdaterScript(): String {
        return """
#MAGISK
# ============================================
# Magisk/KernelSU Module Updater Script
# Generated by DeviceInfo App
# ============================================
# 
# 此文件为兼容性文件，实际安装逻辑由 update-binary 处理
# Magisk/KernelSU 会自动执行同目录下的 update-binary
#
# ============================================
        """.trimIndent()
    }

    /**
     * 构建 META-INF 目录中的 update-binary
     * 这是 Magisk/KernelSU 首先执行的主脚本
     *
     * @return update-binary 脚本内容
     */
    private fun buildMetaUpdateBinary(): String {
        val dollar = '$'
        return """
#!/sbin/sh

# ============================================
# META-INF Update Binary
# Magisk/KernelSU Module Entry Point
# Generated by DeviceInfo App
# ============================================

umask 022

# 输出信息函数
ui_print() {
    echo "${dollar}1"
}

ui_print "================================="
ui_print "    DeviceInfo 机型模拟模块"
ui_print "================================="

# 确定模块安装路径
if [ -z "${dollar}{MODPATH:-}" ]; then
    ui_print "! 错误: 安装环境未提供 MODPATH"
    exit 1
fi

# ZIPFILE 由 Magisk/KernelSU 安装器提供。原实现只判断文件是否存在，不存在时整段
# 安装逻辑被跳过却仍输出"安装完成"，属于静默空装。
if [ -z "${dollar}{ZIPFILE:-}" ] || [ ! -f "${dollar}ZIPFILE" ]; then
    ui_print "! 错误: 安装环境未提供模块 ZIP 路径"
    exit 1
fi

# 检查并加载工具函数
if [ -f /data/adb/ksu/util_functions.sh ]; then
    ui_print "- 检测到 KernelSU 环境"
    . /data/adb/ksu/util_functions.sh
elif [ -f /data/adb/magisk/util_functions.sh ]; then
    ui_print "- 检测到 Magisk 环境"
    . /data/adb/magisk/util_functions.sh
else
    ui_print "! 错误: 未检测到 Magisk 或 KernelSU"
    ui_print "! 请确保您的设备已正确安装 Magisk/KernelSU"
    exit 1
fi

# 优先执行模块根目录的 update-binary（本模块的实际安装脚本）。
# 变量名不使用 TMPDIR，避免覆盖系统同名环境变量。
MODULE_TMPDIR="${dollar}(mktemp -d)"
if [ -z "${dollar}MODULE_TMPDIR" ]; then
    ui_print "! 错误: 无法创建临时目录"
    exit 1
fi

unzip -o "${dollar}ZIPFILE" "update-binary" -d "${dollar}MODULE_TMPDIR" >/dev/null 2>&1

if [ -f "${dollar}MODULE_TMPDIR/update-binary" ]; then
    ui_print "- 正在执行主安装脚本..."
    . "${dollar}MODULE_TMPDIR/update-binary"
else
    # 兜底：按标准流程解压全部文件并执行 install.sh
    ui_print "- 正在执行标准安装流程..."
    unzip -o "${dollar}ZIPFILE" -d "${dollar}MODPATH" >&2

    if [ -f "${dollar}MODPATH/install.sh" ]; then
        . "${dollar}MODPATH/install.sh"
        if command -v set_permissions >/dev/null 2>&1; then
            set_permissions
        fi
    fi
fi

rm -rf "${dollar}MODULE_TMPDIR"

ui_print "================================="
ui_print "- 模块安装流程完成！"
ui_print "- 请重启设备以使修改生效"
ui_print "================================="
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
     * ├── META-INF/com/google/android/
     * │   ├── update-binary
     * │   └── updater-script
     * ├── system/
     * │   └── placeholder
     * ├── module.prop
     * ├── system.prop
     * ├── install.sh
     * └── update-binary
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
