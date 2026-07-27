package com.fioiu8.devinfo.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.fioiu8.devinfo.R
import com.fioiu8.devinfo.model.UiStyle
import com.fioiu8.devinfo.ui.theme.LocalUiStyle

@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val htmlString = stringResource(
        id = R.string.about_source_link,
        "<b><a href=\"https://github.com/FIOIU8/DevInfo\">GitHub</a></b>"
    )
    val state = AboutUiState(
        title = stringResource(R.string.about_app),
        appName = stringResource(R.string.app_name),
        versionName = versionName,
        links = extractLinks(htmlString) + LinkInfo(
            fullText = stringResource(R.string.view_gpl_license),
            url = "https://www.gnu.org/licenses/gpl-3.0.txt",
        ),
    )
    val actions = AboutScreenActions(
        onBack = onBack,
        onOpenLink = uriHandler::openUri,
    )

    when (LocalUiStyle.current) {
        UiStyle.MIUIX -> AboutScreenMiuix(state, actions)
        UiStyle.MATERIAL3 -> AboutScreenMaterial(state, actions)
    }
}
