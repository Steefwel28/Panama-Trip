package com.panamatrip.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Única instancia de DataStore de la app (modo invitado + preferencias). */
val Context.panamaTripPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "panama_trip_preferences",
)
