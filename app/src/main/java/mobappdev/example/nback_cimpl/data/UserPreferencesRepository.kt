package mobappdev.example.nback_cimpl.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class SaveResult {
    Saved,
    Unchanged,
    Failed,
}

interface HighScoreStore {
    val highscore: Flow<Int>
    suspend fun saveIfHigher(score: Int): SaveResult
}

/**
 * This repository provides a way to interact with the DataStore api,
 * with this API you can save key:value pairs
 *
 * Currently this file contains only one thing: getting the highscore as a flow
 * and writing to the highscore preference.
 * (a flow is like a waterpipe; if you put something different in the start,
 * the end automatically updates as long as the pipe is open)
 *
 * Date: 25-08-2023
 * Version: Skeleton code version 1.0
 * Author: Yeetivity
 *
 */

class UserPreferencesRepository (
    private val dataStore: DataStore<Preferences>
) : HighScoreStore {
    private companion object {
        val HIGHSCORE = intPreferencesKey("highscore")
        const val TAG = "UserPreferencesRepo"
    }

    override val highscore: Flow<Int> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map { preferences ->
            preferences[HIGHSCORE] ?: 0
        }

    override suspend fun saveIfHigher(score: Int): SaveResult {
        return try {
            var wrote = false
            dataStore.edit { preferences ->
                val current = preferences[HIGHSCORE] ?: 0
                wrote = score > current
                if (wrote) {
                    preferences[HIGHSCORE] = score
                }
            }
            if (wrote) SaveResult.Saved else SaveResult.Unchanged
        } catch (error: IOException) {
            Log.e(TAG, "Error writing preferences", error)
            SaveResult.Failed
        }
    }
}