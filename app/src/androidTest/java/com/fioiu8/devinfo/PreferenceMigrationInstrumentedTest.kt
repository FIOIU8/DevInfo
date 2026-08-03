package com.fioiu8.devinfo

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferenceMigrationInstrumentedTest {

    @Test
    fun legacyUpdateValuesMigrateWithoutDeletingRollbackData() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferenceName = "migration_test_preferences"
        val dataStoreFileName = "migration_test.preferences_pb"
        val dataStoreFile = context.preferencesDataStoreFile(dataStoreFileName)

        context.deleteSharedPreferences(preferenceName)
        dataStoreFile.delete()
        val seeded = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit()
            .putLong("last_check_time", 42L)
            .putString("cached_tag", "release-1")
            .commit()
        assertTrue(seeded)

        val repository = DataStorePreferenceRepository(context, preferenceName, dataStoreFileName)

        assertEquals(42L, repository.readLong("last_check_time"))
        assertEquals("release-1", repository.readString("cached_tag"))

        val rollbackPreferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
        assertEquals(42L, rollbackPreferences.getLong("last_check_time", 0L))
        assertEquals("release-1", rollbackPreferences.getString("cached_tag", null))

        assertTrue(repository.writeLong("last_check_time", 84L))
        assertEquals(84L, rollbackPreferences.getLong("last_check_time", 0L))

        context.deleteSharedPreferences(preferenceName)
        dataStoreFile.delete()
    }
}
