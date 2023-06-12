package com.w2sv.filenavigator.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey

object PreferencesKey {
    val SHOWED_MANAGE_EXTERNAL_STORAGE_RATIONAL =
        booleanPreferencesKey("showedManageExternalStorageRational")

    val DISABLE_LISTENER_ON_LOW_BATTERY =
        booleanPreferencesKey("disableListenerOnLowBattery")
}