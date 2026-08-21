package com.fioiu8.devinfo

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fioiu8.devinfo.data.DataStorePreferenceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferenceMigrationInstrumentedTest {

    @Test
    fun legacyUpdateValuesMigrateWithoutDeletingRollbackData() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val preferenceName = "migration_test_preferences"
            val dataStoreFileName = "migration_test.preferences_pb"
            val dataStoreFile = context.preferencesDataStoreFile(dataStoreFileName)

            context.deleteSharedPreferences(preferenceName)
            dataStoreFile.delete()
            val seeded = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit()
                .putLong("last_check_time", 42L)
                .putString("cached_tag", "release-1")
                .putString("release_name", "Release 1")
                .putString("release_body", "Release notes")
                .putString("release_url", "https://example.com/release-1")
                .putString("release_download_url", "https://example.com/release-1.apk")
                .commit()
            assertTrue(seeded)

            val repository = DataStorePreferenceRepository(context, preferenceName, dataStoreFileName)

            assertEquals(42L, repository.readLong("last_check_time"))
            assertEquals("release-1", repository.readString("cached_tag"))
            assertEquals("Release 1", repository.readString("release_name"))
            assertEquals("Release notes", repository.readString("release_body"))
            assertEquals("https://example.com/release-1", repository.readString("release_url"))
            assertEquals("https://example.com/release-1.apk", repository.readString("release_download_url"))

            val rollbackPreferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
            assertEquals(42L, rollbackPreferences.getLong("last_check_time", 0L))
            assertEquals("release-1", rollbackPreferences.getString("cached_tag", null))
            assertEquals("Release 1", rollbackPreferences.getString("release_name", null))
            assertEquals("Release notes", rollbackPreferences.getString("release_body", null))
            assertEquals("https://example.com/release-1", rollbackPreferences.getString("release_url", null))
            assertEquals("https://example.com/release-1.apk", rollbackPreferences.getString("release_download_url", null))

            assertTrue(repository.writeLong("last_check_time", 84L))
            assertEquals(84L, rollbackPreferences.getLong("last_check_time", 0L))

            context.deleteSharedPreferences(preferenceName)
            dataStoreFile.delete()
        }
    }
}
