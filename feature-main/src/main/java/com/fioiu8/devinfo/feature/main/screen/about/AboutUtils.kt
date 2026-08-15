package com.fioiu8.devinfo.feature.main.screen.about

import android.text.Spanned
import android.text.style.URLSpan
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.text.HtmlCompat

@Immutable
data class LinkInfo(
    val fullText: String,
    val url: String
)

fun extractLinks(html: String): List<LinkInfo> {
    val lines = html.split("<br/>", "<br>", "\n")
    val result = mutableListOf<LinkInfo>()

    for (line in lines) {
        val spanned: Spanned = HtmlCompat.fromHtml(line, HtmlCompat.FROM_HTML_MODE_LEGACY)
        val spans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        val text = spanned.toString().trim()

        if (spans.isEmpty() && text.isNotBlank()) {
            Log.w("AboutUtils", "extractLinks: line has no URL span: $text")
        }

        for (span in spans) {
            val url = span.url
            result.add(LinkInfo(text, url))
        }
    }
    return result
}
