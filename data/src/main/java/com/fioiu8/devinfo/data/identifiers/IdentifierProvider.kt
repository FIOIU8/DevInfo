/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.data.identifiers

import android.app.Application
import android.content.Context
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.gzuliyujiang.oaid.DeviceIdentifier
import com.github.gzuliyujiang.oaid.IGetter
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 一次读取得到的匿名设备标识。null 表示当前设备或系统不支持。 */
data class IdentifierSnapshot(
    val oaid: String?,
    val aaid: String?,
)

interface IdentifierProvider {
    suspend fun read(): IdentifierSnapshot
}

/** 测试与无厂商服务的默认实现。 */
object NoopIdentifierProvider : IdentifierProvider {
    override suspend fun read(): IdentifierSnapshot = IdentifierSnapshot(oaid = null, aaid = null)
}

/**
 * 进程启动时的 OAID 预热入口。
 *
 * 厂商服务通常需要提前绑定；这里只在本机注册，不发起网络请求。失败时静默降级为
 * “不可用”，不影响应用启动。
 */
object OaidInitializer {
    fun register(application: Application) {
        runCatching { DeviceIdentifier.register(application) }
    }
}

/**
 * 基于 Android_CN_OAID 的实现：OAID 走 MSA/厂商接口，AAID 走 GMS 广告标识。
 *
 * 只在本机读取，不发起网络请求；每次刷新最多等待 [READ_TIMEOUT_MS]，超时按不可用处理，
 * 避免厂商服务挂起时拖慢设备信息采集。
 */
class AndroidCnOaidProvider(private val context: Context) : IdentifierProvider {

    override suspend fun read(): IdentifierSnapshot = withContext(Dispatchers.IO) {
        coroutineScope {
            val oaid = async { readIdentifier { getter -> DeviceID.getByMsa(context, getter) } }
            val aaid = async { readIdentifier { getter -> DeviceID.getByGms(context, getter) } }
            IdentifierSnapshot(oaid = oaid.await(), aaid = aaid.await())
        }
    }

    private suspend fun readIdentifier(fetcher: (IGetter) -> Unit): String? =
        withTimeoutOrNull(READ_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val getter = object : IGetter {
                    override fun onOAIDGetComplete(result: String) {
                        if (continuation.isActive) {
                            continuation.resume(result.trim().takeIf { it.isNotEmpty() })
                        }
                    }

                    override fun onOAIDGetError(error: Exception) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                runCatching { fetcher(getter) }.onFailure {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }

    private companion object {
        const val READ_TIMEOUT_MS = 1_500L
    }
}
