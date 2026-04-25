package com.fioiu8.devinfo

// 导入 Android 相关类
import android.content.Context // 用于获取应用上下文，访问系统服务和缓存目录
import android.os.Build // 用于获取设备硬件和系统版本信息
import android.os.Environment // 用于访问外部存储的公共目录（如下载文件夹）
import android.provider.Settings // 用于读取系统设置值，如 Android ID
import java.io.File // 用于文件和目录操作
import java.io.FileOutputStream // 用于将数据写入文件的输出流
import java.text.SimpleDateFormat // 用于格式化日期和时间为指定的字符串格式
import java.util.* // 导入 Java 工具类，如 Date, Locale, ZipEntry
import java.util.zip.ZipEntry // 表示 ZIP 文件中的单个条目（文件或目录）
import java.util.zip.ZipOutputStream // 用于创建 ZIP 文件的输出流

// 模块导出助手类，负责生成 Magisk/KernelSU 模块的 ZIP 包
class ModuleExportHelper(private val context: Context) {

    // 核心导出方法，接收设备 ID、信息列表，并通过回调返回成功或失败结果
    fun exportModule(
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        onSuccess: (String) -> Unit, // 成功时的回调，参数为生成的文件路径
        onError: (String) -> Unit    // 失败时的回调，参数为错误信息
    ) {
        try {
            // 从系统获取设备的核心信息
            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            val device = Build.DEVICE
            val product = Build.PRODUCT
            val fingerprint = Build.FINGERPRINT
            val versionRelease = Build.VERSION.RELEASE
            val versionSdk = Build.VERSION.SDK_INT.toString()
            // 安全获取安全补丁版本 (API 23+ 才支持)
            val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH ?: ""
            } else ""

            // 生成模块 ID，将型号中的特殊字符替换为下划线，确保合法性
            val moduleId = "Device_${model.replace(Regex("[^a-zA-Z0-9_]"), "_")}"
            // 获取便于阅读的设备显示名称
            val deviceName = getDeviceDisplayName(itemsState)
            // 生成模块的显示名称
            val moduleName = "机型模拟-$deviceName"
            // 获取设备 Android ID
            val androidId = getAndroidId()
            // 使用 Android ID 的前 8 位作为模块作者名
            val author = androidId.take(8)
            // 模块版本号，基于 Android 版本
            val version = "v$versionRelease"
            // 生成模块描述信息，包含设备型号和生成时间
            val description = "这是一个自动化程序生成的机型模拟模块，用于模拟 $brand $model 设备。\\n" +
                    "生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\\n" +
                    "注意: 使用此模块具有一定风险，请自行评估。"

            // 在应用的缓存目录下创建一个临时的模块构建目录
            val tempDir = File(context.cacheDir, "magisk_module_${System.currentTimeMillis()}")
            tempDir.mkdirs() // 确保目录存在

            // 创建模块所需的子目录结构
            val commonDir = File(tempDir, "common") // 用于存放公用脚本和属性文件
            val systemDir = File(tempDir, "system") // 用于存放要替换或添加的系统文件
            commonDir.mkdirs()
            systemDir.mkdirs()

            // 1. 创建 module.prop 文件，定义模块的属性（ID、名称、作者等）
            val modulePropFile = File(tempDir, "module.prop")
            modulePropFile.writeText(buildModuleProp(
                id = moduleId,
                name = moduleName,
                author = author,
                version = version,
                description = description
            ))

            // 2. 创建 common/system.prop 文件，包含要设置的系统属性
            val systemPropFile = File(commonDir, "system.prop")
            systemPropFile.writeText(buildSystemProp(
                brand = brand,
                manufacturer = manufacturer,
                model = model,
                device = device,
                product = product,
                fingerprint = fingerprint,
                versionRelease = versionRelease,
                versionSdk = versionSdk,
                securityPatch = securityPatch
            ))

            // 3. 创建 install.sh 文件，模块安装时执行的脚本
            val installShFile = File(tempDir, "install.sh")
            installShFile.writeText(buildInstallScript(
                brand = brand,
                manufacturer = manufacturer,
                model = model
            ))

            // 4. 创建 update-binary 文件，负责模块刷入时的核心逻辑
            val updateBinaryFile = File(tempDir, "update-binary")
            updateBinaryFile.writeText(buildUpdateBinary())

            // 5. 在 system 目录下创建一个占位文件，提示用户可以在此处放置系统文件
            File(systemDir, "placeholder").writeText("# This directory will contain system files")

            // 6. 创建 common/post-fs-data.sh 文件，在系统启动早期执行的脚本
            val postFsDataFile = File(commonDir, "post-fs-data.sh")
            postFsDataFile.writeText(buildPostFsDataScript(manufacturer, model))

            // 7. 创建 common/service.sh 文件，在系统完全启动后执行的脚本
            val serviceFile = File(commonDir, "service.sh")
            serviceFile.writeText(buildServiceScript())

            // 打包所有文件为一个 ZIP 文件
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeModel = model.replace(Regex("[^a-zA-Z0-9_]"), "_") // 清理文件名中的特殊字符
            val zipFileName = "${safeModel}_${timestamp}.zip"

            // 获取公共下载目录作为保存位置
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // 确保下载目录存在
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val zipFile = File(downloadDir, zipFileName)

            // 使用 ZipOutputStream 将临时目录的所有内容打包
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                zipDirectory(tempDir, "", zipOut)
            }

            // 打包完成后，删除临时目录及其内容
            tempDir.deleteRecursively()

            // 导出成功，返回 ZIP 文件的绝对路径
            onSuccess(zipFile.absolutePath)
        } catch (e: Exception) {
            // 捕获任何异常，打印堆栈跟踪并回调错误信息
            e.printStackTrace()
            onError(e.message ?: "未知错误")
        }
    }

    // 从设备信息列表中提取制造商和型号，组合成可读的设备名称
    private fun getDeviceDisplayName(itemsState: List<ItemWithVisibility>): String {
        val manufacturer = itemsState.find { it.item.key == "制造商" }?.item?.value ?: Build.MANUFACTURER
        val model = itemsState.find { it.item.key == "型号" }?.item?.value ?: Build.MODEL
        return "$manufacturer $model"
    }

    // 安全地获取 Android ID，失败时返回默认值
    private fun getAndroidId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "0000000000000000"
        } catch (e: Exception) {
            "0000000000000000"
        }
    }

    // 构建 module.prop 文件的内容
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
        """.trimIndent() // 去除公共缩进，使生成的文件格式正确
    }

    // 构建 system.prop 文件的内容，包含用于模拟机型的各种系统属性
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
# System Properties for Device Simulation
# Generated by DeviceInfo App
# Target Device: $manufacturer $model

# Brand & Manufacturer
ro.product.brand=$brand
ro.product.manufacturer=$manufacturer

# Model & Device
ro.product.model=$model
ro.product.device=$device
ro.product.name=$product

# Build Fingerprint
ro.build.fingerprint=$fingerprint

# Version Info
ro.build.version.release=$versionRelease
ro.build.version.sdk=$versionSdk
ro.build.version.security_patch=$securityPatch

# Additional Properties
ro.build.product=$device
ro.product.board=$device
ro.product.cpu.abi=${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}
ro.product.cpu.abilist=${Build.SUPPORTED_ABIS.joinToString(",")}
ro.product.cpu.abilist32=${Build.SUPPORTED_32_BIT_ABIS.joinToString(",")}
ro.product.cpu.abilist64=${Build.SUPPORTED_64_BIT_ABIS.joinToString(",")}
        """.trimIndent()
    }

    // 构建 install.sh 脚本的内容，用于模块的安装配置和个性化提示
    private fun buildInstallScript(
        brand: String,
        manufacturer: String,
        model: String
    ): String {
        val dollar = '$' // 在模板字符串中表示美元符号的变量，避免字符串插值
        return """
#!/system/bin/sh
##########################################################################################
#
# Magisk Module Template Config Script
# by 小白杨
#
##########################################################################################
##########################################################################################
#
# Instructions:
#
# 1. Place your files into system folder (delete the placeholder file)
# 2. Fill in your module's info into module.prop
# 3. Configure the settings in this file (config.sh)
# 4. If you need boot scripts, add them into common/post-fs-data.sh or common/service.sh
# 5. Add your additional or modified system properties into common/system.prop
#
##########################################################################################

##########################################################################################
# Configs
##########################################################################################

SKIPMOUNT=false # 是否跳过挂载模块文件
PROPFILE=true   # 是否应用 common/system.prop 中的属性
POSTFSDATA=false # 是否运行 post-fs-data.sh 脚本
LATESTARTSERVICE=false # 是否在系统完全启动后运行 service.sh

##########################################################################################
# Installation Message
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
    echo " 全局机型模拟 - $manufacturer $model"
    echo "*******************************"
}

ALING

##########################################################################################
# Replace list
##########################################################################################

# 定义要替换的系统目录列表（此处为空，表示不进行替换）
REPLACE="
/system/app/Youtube
/system/priv-app/SystemUI
/system/priv-app/Settings
/system/framework
"

REPLACE="

"

##########################################################################################
# Permissions
##########################################################################################
# 模块释放函数，解压模块包中的 system 目录
on_install() {
  ui_print "- 正在释放文件"
  ui_print "- 目标设备: $manufacturer $model"
  unzip -o "${dollar}ZIPFILE" 'system/*' -d ${dollar}MODPATH >&2
}
sleep 1
# 设置文件权限的函数
set_permissions() {
  set_perm_recursive  ${dollar}MODPATH  0  0  0755  0644
}
        """.trimIndent()
    }

    // 构建 update-binary 文件的内容，这是模块刷入时最先执行的脚本
    private fun buildUpdateBinary(): String {
        val dollar = '$'
        return """
#!/sbin/sh

#################
# Initialization
#################

umask 022 # 设置默认文件权限掩码

# 定义用于输出信息给用户的函数
ui_print() { echo "${dollar}1"; }

# 检查 KernelSU 版本是否满足要求的函数
require_new_ksud() {
  ui_print "*******************************"
  ui_print " Please install KernelSU v0.6.6+ !!!"
  ui_print "*******************************"
  exit 1
}

#################
# Load util_functions
#################

# 加载 Magisk 或 KernelSU 的工具函数库
if [ -f /data/adb/ksu/util_functions.sh ]; then
  . /data/adb/ksu/util_functions.sh
elif [ -f /data/adb/magisk/util_functions.sh ]; then
  . /data/adb/magisk/util_functions.sh
else
  ui_print "! Cannot find util_functions.sh"
  exit 1
fi

#################
# Main
#################

# 如果是 KernelSU，检查其版本
if [ "${dollar}KSU" = "true" ]; then
  ksud_version="${dollar}(ksud -v)"
  if [ "${dollar}ksud_version" -lt 666 ]; then
    require_new_ksud
  fi
fi

# 解压模块文件到目标路径
ui_print "- Extracting module files"
unzip -o "${dollar}ZIPFILE" -d ${dollar}MODPATH >&2

# 如果存在 install.sh，则执行其中的逻辑和权限设置函数
if [ -f "${dollar}MODPATH/install.sh" ]; then
  . "${dollar}MODPATH/install.sh"
fi

if [ -f "${dollar}MODPATH/install.sh" ]; then
  set_permissions
fi

ui_print "- Installation complete!"
        """.trimIndent()
    }

    // 构建 post-fs-data.sh 脚本的内容，该脚本在系统启动的早期阶段执行
    private fun buildPostFsDataScript(manufacturer: String, model: String): String {
        return """
#!/system/bin/sh
# Post-fs-data script

# 可以在这里使用 resetprop 命令设置额外的系统属性
# resetprop ro.product.model "$model"
# resetprop ro.product.manufacturer "$manufacturer"
        """.trimIndent()
    }

    // 构建 service.sh 脚本的内容，该脚本在系统完全启动后执行
    private fun buildServiceScript(): String {
        val dollar = '$'
        return """
#!/system/bin/sh
# Late-start service script

# 等待系统启动完成
until [ "${dollar}(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done
        """.trimIndent()
    }

    // 递归地将目录及其所有子文件和子文件夹添加到 ZIP 输出流中
    private fun zipDirectory(dir: File, parentPath: String, zipOut: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            // 构建在 ZIP 中的条目路径
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"

            if (file.isDirectory) {
                // 如果是目录，递归处理
                zipDirectory(file, entryPath, zipOut)
            } else {
                // 如果是文件，将其添加到 ZIP 中
                zipOut.putNextEntry(ZipEntry(entryPath))
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }
}