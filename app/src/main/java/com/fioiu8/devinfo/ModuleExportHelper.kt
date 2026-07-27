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

// 导入 Android 相关类
import android.content.Context
import android.os.Build
import android.os.Environment
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.ItemWithVisibility
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 模块导出助手类，负责生成 Magisk/KernelSU 模块的 ZIP 包
 *
 * 生成的模块 ZIP 包结构如下：
 *
 * Device_XXX_20240101_120000.zip              # 模块压缩包
 * │
 * ├── META-INF/                               # Magisk/KernelSU 必需的签名和脚本目录
 * │   └── com/
 * │       └── google/
 * │           └── android/
 * │               ├── update-binary           # 刷机脚本（实际执行逻辑）
 * │               └── updater-script          # 刷机脚本描述（指向 update-binary）
 * │
 * ├── common/                                 # 公共文件目录
 * │   ├── system.prop                         # 系统属性配置文件（会被 Magisk 自动加载）
 * │   ├── post-fs-data.sh                     # 文件系统挂载后执行的脚本（early boot）
 * │   └── service.sh                          # 系统完全启动后执行的后台服务脚本
 * │
 * ├── system/                                 # 系统文件替换目录
 * │   └── (可选的系统文件，用于替换 /system 下的文件)
 * │
 * ├── module.prop                             # 模块信息配置文件（必需）
 * ├── install.sh                              # 模块安装时的执行脚本
 * └── update-binary                           # 备用 update-binary（根目录版本）
 *
 * Magisk/KernelSU 模块工作原理：
 * 1. 用户通过 Magisk/KernelSU 刷入 ZIP 包
 * 2. 系统首先执行 META-INF/com/google/android/update-binary
 * 3. update-binary 加载模块配置，解压文件到 /data/adb/modules/[module_id]/
 * 4. 应用 common/system.prop 中的系统属性
 * 5. 根据配置执行 post-fs-data.sh 和 service.sh
 * 6. 重启后模块生效
 */
class ModuleExportHelper(private val context: Context) {

    /**
     * 核心导出方法，生成完整的 Magisk/KernelSU 模块 ZIP 包
     *
     * @param deviceId 设备唯一标识符
     * @param itemsState 设备信息项列表（用于获取用户选择的设备信息）
     * @param onSuccess 成功回调，返回生成的 ZIP 文件路径
     * @param onError 失败回调，返回错误信息
     */
    fun exportModule(
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val buildInfo = readDeviceBuildInfo()
            val metadata = createModuleMetadata(deviceId, itemsState, buildInfo)
            val directories = createModuleDirectories()

            writeModuleFiles(directories, metadata, buildInfo)
            val zipFile = createModuleArchive(directories.root, buildInfo.model)
            directories.root.deleteRecursively()
            onSuccess(zipFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.message ?: "未知错误")
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
        val description: String
    )

    private data class ModuleDirectories(
        val root: File,
        val metaInf: File,
        val common: File,
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
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        buildInfo: DeviceBuildInfo
    ): ModuleMetadata {
        val moduleId = "Device_${buildInfo.model.replace(Regex("[^a-zA-Z0-9_]"), "_")}"
        val deviceName = getDeviceDisplayName(itemsState)
        val moduleName = context.getString(R.string.module_export_name, deviceName)
        val author = deviceId.take(8).ifBlank { "DevInfo" }
        val version = "v${buildInfo.versionRelease}"
        val generatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
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
            version = version,
            description = description
        )
    }

    private fun createModuleDirectories(): ModuleDirectories {
        val root = File(context.cacheDir, "magisk_module_${System.currentTimeMillis()}")
        root.mkdirs()

        val metaInf = File(root, "META-INF/com/google/android")
        metaInf.mkdirs()

        val common = File(root, "common")
        common.mkdirs()

        val system = File(root, "system")
        system.mkdirs()

        return ModuleDirectories(root, metaInf, common, system)
    }

    private fun writeModuleFiles(
        directories: ModuleDirectories,
        metadata: ModuleMetadata,
        buildInfo: DeviceBuildInfo
    ) {
        writeModuleProp(directories.root, metadata)
        writeSystemProp(directories.common, buildInfo)
        writeInstallScript(directories.root, buildInfo)
        writeRootUpdateBinary(directories.root)
        writeUpdaterScript(directories.metaInf)
        writeMetaUpdateBinary(directories.metaInf)
        writeSystemPlaceholder(directories.system)
        writePostFsData(directories.common, buildInfo)
        writeServiceScript(directories.common)
    }

    private fun writeModuleProp(directory: File, metadata: ModuleMetadata) {
        File(directory, "module.prop").writeText(
            buildModuleProp(
                id = metadata.id,
                name = metadata.name,
                author = metadata.author,
                version = metadata.version,
                description = metadata.description
            )
        )
    }

    private fun writeSystemProp(directory: File, buildInfo: DeviceBuildInfo) {
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
                securityPatch = buildInfo.securityPatch
            )
        )
    }

    private fun writeInstallScript(directory: File, buildInfo: DeviceBuildInfo) {
        File(directory, "install.sh").writeText(
            buildInstallScript(
                brand = buildInfo.brand,
                manufacturer = buildInfo.manufacturer,
                model = buildInfo.model
            )
        )
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

    private fun writePostFsData(directory: File, buildInfo: DeviceBuildInfo) {
        File(directory, "post-fs-data.sh").writeText(
            buildPostFsDataScript(buildInfo.manufacturer, buildInfo.model)
        )
    }

    private fun writeServiceScript(directory: File) {
        File(directory, "service.sh").writeText(buildServiceScript())
    }

    private fun createModuleArchive(tempDir: File, model: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val safeModel = model.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val zipFileName = "${safeModel}_${timestamp}.zip"
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        val zipFile = File(downloadDir, zipFileName)
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            zipDirectory(tempDir, "", zipOut)
        }
        return zipFile
    }

    /**
     * 从设备信息列表中提取制造商和型号，组合成可读的设备名称
     *
     * @param itemsState 设备信息项列表
     * @return 格式为 "制造商 型号" 的设备名称
     */
    private fun getDeviceDisplayName(itemsState: List<ItemWithVisibility>): String {
        // DeviceInfoItem.key stores the resource entry name, not its localized text.
        val manufacturer = itemsState
            .find { it.item.key == "device_manufacturer" }
            ?.item
            ?.value
            ?: Build.MANUFACTURER
        val model = itemsState
            .find { it.item.key == "device_model" }
            ?.item
            ?.value
            ?: Build.MODEL
        return "$manufacturer $model"
    }

    /**
     * 构建 module.prop 文件的内容
     * 这是 Magisk/KernelSU 模块的必需文件，定义了模块的元数据
     *
     * @param id 模块唯一标识符
     * @param name 模块显示名称
     * @param author 模块作者
     * @param version 模块版本字符串
     * @param description 模块描述信息
     * @return module.prop 文件内容
     */
    private fun buildModuleProp(
        id: String,
        name: String,
        author: String,
        version: String,
        description: String
    ): String {
        return """
id=$id
name=$name
version=$version
versionCode=1
author=$author
description=$description
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
        securityPatch: String
    ): String {
        return """
# ============================================
# System Properties for Device Simulation
# Generated by DeviceInfo App
# ============================================
# Target Device: $manufacturer $model
# Generation Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
# ============================================

# Brand & Manufacturer（品牌和制造商）
ro.product.brand=$brand
ro.product.manufacturer=$manufacturer

# Model & Device（型号和设备代号）
ro.product.model=$model
ro.product.device=$device
ro.product.name=$product

# Build Fingerprint（构建指纹）
ro.build.fingerprint=$fingerprint

# Version Info（版本信息）
ro.build.version.release=$versionRelease
ro.build.version.sdk=$versionSdk
ro.build.version.security_patch=$securityPatch

# Additional Properties（附加属性）
ro.build.product=$device
ro.product.board=$device
ro.product.cpu.abi=${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}
ro.product.cpu.abilist=${Build.SUPPORTED_ABIS.joinToString(",")}
ro.product.cpu.abilist32=${Build.SUPPORTED_32_BIT_ABIS.joinToString(",")}
ro.product.cpu.abilist64=${Build.SUPPORTED_64_BIT_ABIS.joinToString(",")}
        """.trimIndent()
    }

    /**
     * 构建 install.sh 脚本的内容
     * 该脚本在模块安装时执行，用于设置模块配置、显示信息等
     *
     * @param brand 品牌
     * @param manufacturer 制造商
     * @param model 型号
     * @return install.sh 脚本内容
     */
    private fun buildInstallScript(
        brand: String,
        manufacturer: String,
        model: String
    ): String {
        val dollar = '$'
        return """
#!/system/bin/sh
# ============================================
# Magisk/KernelSU Module Install Script
# Generated by DeviceInfo App
# ============================================

##########################################################################################
# Configs（配置选项）
##########################################################################################

SKIPMOUNT=false          # 是否跳过挂载模块文件到系统分区
PROPFILE=true            # 是否应用 common/system.prop 中的系统属性
POSTFSDATA=false         # 是否在第一阶段启动时执行 post-fs-data.sh
LATESTARTSERVICE=false   # 是否在系统完全启动后执行 service.sh

##########################################################################################
# Installation Message（安装信息显示函数）
##########################################################################################

# 尝试获取酷安用户名，用于个性化安装问候
function get_coolapk_user_name(){
    for i in /data/user/0/com.coolapk.market/shared_prefs/*preferences*.xml
    do
        username="${dollar}(grep '<string name="username">' "${dollar}{i}" | sed 's/.*"username">//g;s/<.*//g')"
        if [[ -n "${dollar}{username}" ]];then
            echo "${dollar}{username}"
            break
        fi
    done
}

# 输出个性化的安装信息
function ALING(){
    echo ""
    if test -n "${dollar}(getprop persist.sys.device_name)" ;then
        echo "您好！${dollar}(getprop persist.sys.device_name)！"
    elif test "${dollar}(get_coolapk_user_name)" != "" ;then
        echo "您好！${dollar}(get_coolapk_user_name)！"
    elif test -n "${dollar}(pm list users | cut -d : -f2 )" ;then
        echo "您好！ ${dollar}(pm list users | cut -d : -f2 )！"
    fi
    echo "*******************************"
    echo "    全局机型模拟 - $manufacturer $model"
    echo "    品牌: $brand"
    echo "*******************************"
    echo "  注意: 刷入后请重启设备以生效！"
    echo "*******************************"
}

# 显示安装信息
ALING

##########################################################################################
# Replace List（替换列表）
##########################################################################################

# 定义要替换的系统目录列表
# 格式：每个目录一行，Magisk 会将这些目录替换为模块中的对应目录
REPLACE="
"

##########################################################################################
# Permissions（权限设置）
##########################################################################################

# 模块释放函数：解压模块包中的 system 目录到模块安装目录
on_install() {
  ui_print "- 正在释放文件..."
  ui_print "- 目标设备: $manufacturer $model"
  unzip -o "${dollar}ZIPFILE" 'system/*' -d ${dollar}MODPATH >&2
  sleep 1
  ui_print "- 文件释放完成！"
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

# 如果是 KernelSU，检查版本是否足够新
if [ "${dollar}KSU" = "true" ]; then
  ksud_version="${dollar}(ksud -v 2>/dev/null)"
  if [ -n "${dollar}ksud_version" ] && [ "${dollar}ksud_version" -lt 666 ]; then
    require_new_ksud
  fi
  ui_print "- KernelSU 版本检测通过"
else
  ui_print "- Magisk 环境检测通过"
fi

# 解压模块文件到目标路径
ui_print "- 正在解压模块文件..."
unzip -o "${dollar}ZIPFILE" -d ${dollar}MODPATH >&2

# 如果存在 install.sh，则加载并执行其中的配置和权限设置函数
if [ -f "${dollar}MODPATH/install.sh" ]; then
  ui_print "- 正在执行安装脚本..."
  . "${dollar}MODPATH/install.sh"
  
  # 执行 on_install 函数（如果存在）
  if type on_install 2>/dev/null | grep -q 'function'; then
    on_install
  fi
  
  # 执行 set_permissions 函数（如果存在）
  if type set_permissions 2>/dev/null | grep -q 'function'; then
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
if [ -z "${dollar}MODPATH" ]; then
    MODPATH="${dollar}MODPATH"
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

# 检查模块根目录是否存在 update-binary，如果存在则调用它
if [ -f "${dollar}ZIPFILE" ]; then
    # 临时解压模块根目录的 update-binary 并执行
    # 创建临时目录
    TMPDIR="${dollar}(mktemp -d)"
    
    # 提取模块根目录的 update-binary
    unzip -o "${dollar}ZIPFILE" "update-binary" -d "${dollar}TMPDIR" >&2 2>/dev/null
    
    if [ -f "${dollar}TMPDIR/update-binary" ]; then
        ui_print "- 正在执行主安装脚本..."
        . "${dollar}TMPDIR/update-binary"
    else
        # 执行标准安装流程
        ui_print "- 正在执行标准安装流程..."
        
        # 解压所有模块文件
        unzip -o "${dollar}ZIPFILE" -d ${dollar}MODPATH >&2
        
        # 如果存在 install.sh，执行权限设置
        if [ -f "${dollar}MODPATH/install.sh" ]; then
            . "${dollar}MODPATH/install.sh"
            if type set_permissions 2>/dev/null | grep -q 'function'; then
                set_permissions
            fi
        fi
    fi
    
    # 清理临时目录
    rm -rf "${dollar}TMPDIR"
fi

ui_print "================================="
ui_print "- 模块安装流程完成！"
ui_print "- 请重启设备以使修改生效"
ui_print "================================="
        """.trimIndent()
    }

    /**
     * 构建 post-fs-data.sh 脚本的内容
     * 该脚本在文件系统挂载后、系统服务启动前执行
     * 执行时机：早期启动阶段
     *
     * @param manufacturer 制造商
     * @param model 型号
     * @return post-fs-data.sh 脚本内容
     */
    private fun buildPostFsDataScript(manufacturer: String, model: String): String {
        return """
#!/system/bin/sh
# ============================================
# post-fs-data.sh
# 执行时机：文件系统挂载后，系统服务启动前
# Generated by DeviceInfo App
# ============================================

# 使用 resetprop 命令可以设置只读系统属性
# resetprop 比 setprop 更强，可以修改只读属性

# 示例：设置设备型号（如果需要覆盖 system.prop 中的设置）
# resetprop ro.product.model "$model"
# resetprop ro.product.manufacturer "$manufacturer"

# 注意：
# 1. 此脚本在系统启动早期执行，此时部分服务可能尚未启动
# 2. 脚本执行时间应尽可能短，避免延迟系统启动
# 3. 除非有特殊需求，否则建议使用 common/system.prop 设置属性

# 记录脚本执行日志（调试用）
# echo "post-fs-data.sh executed at \$(date)" >> /data/local/tmp/module_debug.log
        """.trimIndent()
    }

    /**
     * 构建 service.sh 脚本的内容
     * 该脚本在系统完全启动后以后台服务方式运行
     * 执行时机：系统启动完成后（后期启动阶段）
     *
     * @return service.sh 脚本内容
     */
    private fun buildServiceScript(): String {
        val dollar = '$'
        return """
#!/system/bin/sh
# ============================================
# service.sh
# 执行时机：系统完全启动后（后台服务）
# Generated by DeviceInfo App
# ============================================

# 等待系统完全启动完成
# sys.boot_completed=1 表示系统已完全启动
until [ "${dollar}(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# 系统启动完成后再等待几秒，确保所有服务都已就绪
sleep 3

# 在此处添加需要在系统启动后执行的任务
# 例如：
# - 设置额外的系统属性
# - 启动后台进程
# - 修改文件权限等

# 示例：记录模块已加载
# echo "Device simulation module loaded at \$(date)" >> /data/local/tmp/module.log

# 返回 0 表示脚本执行成功
exit 0
        """.trimIndent()
    }

    /**
     * 递归地将目录及其所有子文件和子文件夹添加到 ZIP 输出流中
     *
     * ZIP 文件结构示例：
     * module.zip
     * ├── META-INF/
     * │   └── com/
     * │       └── google/
     * │           └── android/
     * │               ├── update-binary
     * │               └── updater-script
     * ├── common/
     * │   ├── system.prop
     * │   ├── post-fs-data.sh
     * │   └── service.sh
     * ├── system/
     * │   └── placeholder
     * ├── module.prop
     * ├── install.sh
     * └── update-binary
     *
     * @param dir 要打包的目录
     * @param parentPath ZIP 中的父路径
     * @param zipOut ZIP 输出流
     */
    private fun zipDirectory(dir: File, parentPath: String, zipOut: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            // 构建在 ZIP 中的条目路径
            // 如果是根目录，直接使用文件名；否则添加父路径前缀
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"

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
