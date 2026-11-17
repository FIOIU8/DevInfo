/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo

import android.app.Application
import com.fioiu8.devinfo.data.identifiers.OaidInitializer

/**
 * 在进程启动时预热 OAID/AAID 读取。
 *
 * 厂商服务通常需要提前绑定；这里只在本机注册，不发起网络请求。
 */
class DevInfoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OaidInitializer.register(this)
    }
}
