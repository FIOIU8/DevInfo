/*
 * Copyright (C) 2026 FIOIU8
 *
 * SPDX-License-Identifier: MIT
 */

package com.fioiu8.devinfo.feature.main.screen.about
import com.fioiu8.devinfo.feature.main.R

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 关于页内用于触发内置许可证弹窗的伪链接，不会真正交给系统打开。 */
private const val LICENSE_URI = "devinfo://license/mit"

@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showLicense by remember { mutableStateOf(false) }

    val htmlString = stringResource(
        id = R.string.about_source_link,
        "<b><a href=\"https://github.com/FIOIU8/DevInfo\">GitHub</a></b>"
    )
    val state = AboutUiState(
        title = stringResource(R.string.about_app),
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        links = extractLinks(htmlString) + LinkInfo(
            fullText = stringResource(R.string.view_license),
            url = LICENSE_URI,
        ),
    )
    val actions = AboutScreenActions(
        onBack = onBack,
        onOpenLink = { url ->
            if (url == LICENSE_URI) {
                showLicense = true
            } else {
                uriHandler.openUri(url)
            }
        },
    )

    AboutScreenMaterial(state, actions)

    if (showLicense) {
        LicenseDialog(
            title = stringResource(R.string.license_title),
            onDismiss = { showLicense = false },
        )
    }
}

/**
 * 展示随 APK 一同分发的许可证全文，因此断网时同样可读。
 *
 * 读取 raw 资源属于阻塞 I/O，放在 IO 调度器执行，避免占用主线程。
 */
@Composable
private fun LicenseDialog(
    title: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val licenseText by produceState(initialValue = "") {
        value = withContext(Dispatchers.IO) {
            context.resources.openRawResource(R.raw.license)
                .bufferedReader()
                .use { reader -> reader.readText() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = licenseText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}
