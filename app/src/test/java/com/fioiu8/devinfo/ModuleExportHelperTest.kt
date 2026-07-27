package com.fioiu8.devinfo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ModuleExportHelper 的纯逻辑单元测试。
 * 测试输入转义、文件名净化和模块内容生成。
 *
 * 注意：这些测试不依赖 Android 框架，可在 JVM 上运行。
 */
class ModuleExportHelperTest {

    // ── escapePropValue ──────────────────────────────────────────────

    @Test
    fun `escapePropValue should escape equals sign`() {
        // "foo=bar" -> "foo\=bar"  (\= is prop escape for literal =)
        assertEquals("foo\\=bar", ModuleExportHelper.escapePropValue("foo=bar"))
    }

    @Test
    fun `escapePropValue should escape newlines`() {
        // input has actual newline, output has \n as two chars
        assertEquals("line1\\nline2", ModuleExportHelper.escapePropValue("line1\nline2"))
    }

    @Test
    fun `escapePropValue should escape carriage returns`() {
        assertEquals("text\\r", ModuleExportHelper.escapePropValue("text\r"))
    }

    @Test
    fun `escapePropValue should escape backslashes`() {
        // input "foo\bar" -> "foo\\bar"
        assertEquals("foo\\\\bar", ModuleExportHelper.escapePropValue("foo\\bar"))
    }

    @Test
    fun `escapePropValue should handle empty string`() {
        assertEquals("", ModuleExportHelper.escapePropValue(""))
    }

    @Test
    fun `escapePropValue should handle normal text unchanged`() {
        assertEquals("SM-S9080", ModuleExportHelper.escapePropValue("SM-S9080"))
    }

    // ── escapeShellValue ─────────────────────────────────────────────

    @Test
    fun `escapeShellValue should escape single quotes`() {
        // "it's" -> "it'\''s"
        assertEquals("it'\\''s", ModuleExportHelper.escapeShellValue("it's"))
    }

    @Test
    fun `escapeShellValue should escape dollar sign`() {
        assertEquals("price is 5 dollars", ModuleExportHelper.escapeShellValue("price is 5 dollars"))
    }

    @Test
    fun `escapeShellValue should escape backticks`() {
        assertEquals("\\`ls\\`", ModuleExportHelper.escapeShellValue("`ls`"))
    }

    @Test
    fun `escapeShellValue should replace newlines with spaces`() {
        assertEquals("a b", ModuleExportHelper.escapeShellValue("a\nb"))
    }

    @Test
    fun `escapeShellValue should remove carriage returns`() {
        assertEquals("ab", ModuleExportHelper.escapeShellValue("a\rb"))
    }

    @Test
    fun `escapeShellValue should handle empty string`() {
        assertEquals("", ModuleExportHelper.escapeShellValue(""))
    }

    // ── sanitizeFileName ─────────────────────────────────────────────

    @Test
    fun `sanitizeFileName should keep safe characters`() {
        assertEquals("SM-S9080_20240728.zip", ModuleExportHelper.sanitizeFileName("SM-S9080_20240728.zip"))
    }

    @Test
    fun `sanitizeFileName should replace unsafe characters with underscore`() {
        assertEquals("a_b_c", ModuleExportHelper.sanitizeFileName("a/b:c"))
    }

    @Test
    fun `sanitizeFileName should handle empty string`() {
        assertEquals("", ModuleExportHelper.sanitizeFileName(""))
    }

    @Test
    fun `sanitizeFileName should handle spaces`() {
        assertEquals("hello_world", ModuleExportHelper.sanitizeFileName("hello world"))
    }

    @Test
    fun `sanitizeFileName should handle unicode`() {
        assertEquals("__", ModuleExportHelper.sanitizeFileName("中文"))
    }
}
