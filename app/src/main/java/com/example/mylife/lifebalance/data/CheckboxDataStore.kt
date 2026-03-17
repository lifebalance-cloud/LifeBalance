package com.example.mylife.lifebalance.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "lifebalance_settings")

class LifeBalanceRepository(private val context: Context) {

    private val CHECKBOX_KEY = booleanPreferencesKey("checkbox_state")

    val isChecked: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CHECKBOX_KEY] ?: false
        }

    suspend fun saveCheckedState(checked: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[CHECKBOX_KEY] = checked
        }
    }
}
