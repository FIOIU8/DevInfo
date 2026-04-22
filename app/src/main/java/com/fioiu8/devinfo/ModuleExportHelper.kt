package com.fioiu8.devinfo

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModuleExportHelper(private val context: Context) {

    fun exportModule(
        deviceId: String,
        itemsState: List<ItemWithVisibility>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            val device = Build.DEVICE
            val product = Build.PRODUCT
            val fingerprint = Build.FINGERPRINT
            val versionRelease = Build.VERSION.RELEASE
            val versionSdk = Build.VERSION.SDK_INT.toString()
            val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH ?: ""
            } else ""

            val moduleId = "Device_${model.replace(Regex("[^a-zA-Z0-9_]"), "_")}"
            val deviceName = getDeviceDisplayName(itemsState)
            val moduleName = "机型模拟-$deviceName"
            val androidId = getAndroidId()
            val author = androidId.take(8)
            val version = "v$versionRelease"
            val description = "这是一个自动化程序生成的机型模拟模块，用于模拟 $brand $model 设备。\\n" +
                    "生成时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\\n" +
                    "注意: 使用此模块具有一定风险，请自行评估。"

            val tempDir = File(context.cacheDir, "magisk_module_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val commonDir = File(tempDir, "common")
            val systemDir = File(tempDir, "system")
            commonDir.mkdirs()
            systemDir.mkdirs()

            // 1. module.prop
            val modulePropFile = File(tempDir, "module.prop")
            modulePropFile.writeText(buildModuleProp(
                id = moduleId,
                name = moduleName,
                author = author,
                version = version,
                description = description
            ))

            // 2. common/system.prop
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

            // 3. install.sh
            val installShFile = File(tempDir, "install.sh")
            installShFile.writeText(buildInstallScript(
                brand = brand,
                manufacturer = manufacturer,
                model = model
            ))

            // 4. update-binary
            val updateBinaryFile = File(tempDir, "update-binary")
            updateBinaryFile.writeText(buildUpdateBinary())

            // 5. placeholder
            File(systemDir, "placeholder").writeText("# This directory will contain system files")

            // 6. post-fs-data.sh
            val postFsDataFile = File(commonDir, "post-fs-data.sh")
            postFsDataFile.writeText(buildPostFsDataScript(manufacturer, model))

            // 7. service.sh
            val serviceFile = File(commonDir, "service.sh")
            serviceFile.writeText(buildServiceScript())

            // 打包 ZIP
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeModel = model.replace(Regex("[^a-zA-Z0-9_]"), "_")
            val zipFileName = "Magisk_${safeModel}_${timestamp}.zip"
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val zipFile = File(downloadDir, zipFileName)

            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                zipDirectory(tempDir, "", zipOut)
            }

            tempDir.deleteRecursively()

            onSuccess(zipFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.message ?: "未知错误")
        }
    }

    private fun getDeviceDisplayName(itemsState: List<ItemWithVisibility>): String {
        val manufacturer = itemsState.find { it.item.key == "制造商" }?.item?.value ?: Build.MANUFACTURER
        val model = itemsState.find { it.item.key == "型号" }?.item?.value ?: Build.MODEL
        return "$manufacturer $model"
    }

    private fun getAndroidId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "0000000000000000"
        } catch (e: Exception) {
            "0000000000000000"
        }
    }

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

    private fun buildInstallScript(
        brand: String,
        manufacturer: String,
        model: String
    ): String {
        val dollar = '$'
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

SKIPMOUNT=false
PROPFILE=true
POSTFSDATA=false
LATESTARTSERVICE=false

##########################################################################################
# Installation Message
##########################################################################################

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
on_install() {
  ui_print "- 正在释放文件"
  ui_print "- 目标设备: $manufacturer $model"
  unzip -o "${dollar}ZIPFILE" 'system/*' -d ${dollar}MODPATH >&2
}
sleep 1
am start -a android.intent.action.VIEW -d tg://resolve?domain=ALING521 >/dev/null 2>&1
set_permissions() {
  set_perm_recursive  ${dollar}MODPATH  0  0  0755  0644
}
        """.trimIndent()
    }

    private fun buildUpdateBinary(): String {
        val dollar = '$'
        return """
#!/sbin/sh

#################
# Initialization
#################

umask 022

ui_print() { echo "${dollar}1"; }

require_new_ksud() {
  ui_print "*******************************"
  ui_print " Please install KernelSU v0.6.6+ !!!"
  ui_print "*******************************"
  exit 1
}

#################
# Load util_functions
#################

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

if [ "${dollar}KSU" = "true" ]; then
  ksud_version="${dollar}(ksud -v)"
  if [ "${dollar}ksud_version" -lt 666 ]; then
    require_new_ksud
  fi
fi

ui_print "- Extracting module files"
unzip -o "${dollar}ZIPFILE" -d ${dollar}MODPATH >&2

if [ -f "${dollar}MODPATH/install.sh" ]; then
  . "${dollar}MODPATH/install.sh"
fi

if [ -f "${dollar}MODPATH/install.sh" ]; then
  set_permissions
fi

ui_print "- Installation complete!"
        """.trimIndent()
    }

    private fun buildPostFsDataScript(manufacturer: String, model: String): String {
        return """
#!/system/bin/sh
# Post-fs-data script

# Additional properties can be set here
# resetprop ro.product.model "$model"
# resetprop ro.product.manufacturer "$manufacturer"
        """.trimIndent()
    }

    private fun buildServiceScript(): String {
        val dollar = '$'
        return """
#!/system/bin/sh
# Late-start service script

until [ "${dollar}(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done
        """.trimIndent()
    }

    private fun zipDirectory(dir: File, parentPath: String, zipOut: ZipOutputStream) {
        dir.listFiles()?.forEach { file ->
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"

            if (file.isDirectory) {
                zipDirectory(file, entryPath, zipOut)
            } else {
                zipOut.putNextEntry(ZipEntry(entryPath))
                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }
}