package com.pcapplusplus.toyvpn

import android.content.Context

interface SharedPreferencesProvider {
    fun getSavedText(key: String): String

    fun saveText(
        key: String,
        value: String,
    )
}

class DefaultSharedPreferencesProvider(private val context: Context, private val appKey: String) : SharedPreferencesProvider {
    override fun getSavedText(key: String): String {
        val sharedPreferences = context.getSharedPreferences(appKey, Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, "") ?: ""
    }

    override fun saveText(
        key: String,
        value: String,
    ) {
        val sharedPreferences = context.getSharedPreferences(appKey, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }
}
