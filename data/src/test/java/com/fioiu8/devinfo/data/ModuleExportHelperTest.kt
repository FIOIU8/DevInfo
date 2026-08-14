package com.fioiu8.devinfo.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `escapePropValue should neutralize control characters`() {
        assertEquals("a_b\\tb\\nb\\rc", ModuleExportHelper.escapePropValue("a\u0000b\tb\nb\rc"))
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
        assertEquals("a b", ModuleExportHelper.escapeShellValue("a\rb"))
    }

    @Test
    fun `escapeShellValue should handle empty string`() {
        assertEquals("", ModuleExportHelper.escapeShellValue(""))
    }

    @Test
    fun `quoteShellValue should create a safe single quoted literal`() {
        assertEquals("'a'\\''b c'", ModuleExportHelper.quoteShellValue("a'b\nc"))
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
        assertEquals("module-export", ModuleExportHelper.sanitizeFileName(""))
    }

    @Test
    fun `sanitizeFileName should handle spaces`() {
        assertEquals("hello_world", ModuleExportHelper.sanitizeFileName("hello world"))
    }

    @Test
    fun `sanitizeFileName should handle unicode`() {
        assertEquals("__", ModuleExportHelper.sanitizeFileName("中文"))
    }

    @Test
    fun `sanitizeFileName should remove path traversal segments`() {
        assertEquals("__etc_passwd", ModuleExportHelper.sanitizeFileName("../etc/passwd"))
        assertEquals("module-export", ModuleExportHelper.sanitizeFileName(".."))
    }

    @Test
    fun `createExportFileName should return a safe zip filename`() {
        val name = ModuleExportHelper.createExportFileName("../Model:One\n")

        assertEquals("DevInfo___Model_One_.zip", name)
        assertTrue(name.endsWith(".zip"))
        assertFalse(name.contains('/'))
        assertFalse(name.contains('\\'))
    }

    @Test
    fun `zip entry validation should reject unsafe paths`() {
        assertTrue(ModuleExportHelper.isSafeZipEntryName("META-INF/com/google/android/update-binary"))
        assertTrue(ModuleExportHelper.isSafeZipEntryName("system/"))
        assertFalse(ModuleExportHelper.isSafeZipEntryName("../module.prop"))
        assertFalse(ModuleExportHelper.isSafeZipEntryName("/module.prop"))
        assertFalse(ModuleExportHelper.isSafeZipEntryName("C:/module.prop"))
        assertFalse(ModuleExportHelper.isSafeZipEntryName("system\\module.prop"))
        assertFalse(ModuleExportHelper.isSafeZipEntryName("system/\u0000.prop"))
        assertFalse(ModuleExportHelper.isSafeZipEntryName("system/\uD800.prop"))
    }

    @Test
    fun `zip validation should require fixed module files`() {
        val entries = listOf(
            "module.prop",
            "system.prop",
            "META-INF/com/google/android/update-binary",
            "META-INF/com/google/android/updater-script",
            "system/"
        )

        ModuleExportHelper.validateZipEntries(entries)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zip validation should reject a missing required file`() {
        ModuleExportHelper.validateZipEntries(
            listOf(
                "module.prop",
                "system.prop",
                "META-INF/com/google/android/update-binary"
            )
        )
    }

    @Test
    fun `zip entry list can be validated after reading archive`() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            listOf(
                "module.prop",
                "system.prop",
                "META-INF/com/google/android/update-binary",
                "META-INF/com/google/android/updater-script"
            ).forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.closeEntry()
            }
        }

        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries += entry.name
            }
        }

        ModuleExportHelper.validateZipEntries(entries)
    }
}
