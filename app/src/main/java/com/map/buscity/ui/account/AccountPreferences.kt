package com.map.buscity.ui.account

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFS_NAME = "account_prefs"

val Context.dataStore by preferencesDataStore(PREFS_NAME)

object AccountPreferences {
    // Profile
    private val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")
    private val KEY_PROFILE_PHONE = stringPreferencesKey("profile_phone")
    private val KEY_PROFILE_GENDER = stringPreferencesKey("profile_gender") // "male" | "female"
    private val KEY_PROFILE_BIRTHDAY = stringPreferencesKey("profile_birthday")
    private val KEY_PROFILE_AVATAR = stringPreferencesKey("profile_avatar_uri") // persisted user selected avatar (content:// uri)

    // Settings
    private val KEY_SETTINGS_DARK = booleanPreferencesKey("settings_dark")
    private val KEY_SETTINGS_NOTI = booleanPreferencesKey("settings_noti")
    private val KEY_SETTINGS_LANG = stringPreferencesKey("settings_lang") // "vi" | "en"

    // Rating
    private val KEY_RATING_SCORE = intPreferencesKey("rating_score")
    private val KEY_RATING_FEEDBACK = stringPreferencesKey("rating_feedback_list") // pipe-separated JSON-lite
    private val KEY_RATING_DELETED_BUFFER = stringPreferencesKey("rating_feedback_deleted_buffer") // temp store for undo

    // Auth / login
    private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
    private val KEY_LAST_EMAIL = stringPreferencesKey("last_email")

    // Profile getters
    fun profileName(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_PROFILE_NAME] ?: "" }
    fun profilePhone(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_PROFILE_PHONE] ?: "" }
    fun profileGender(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_PROFILE_GENDER] ?: "" }
    fun profileBirthday(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_PROFILE_BIRTHDAY] ?: "" }
    fun profileAvatar(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_PROFILE_AVATAR] ?: "" }

    suspend fun saveProfile(context: Context, name: String, phone: String, gender: String, birthday: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_NAME] = name
            prefs[KEY_PROFILE_PHONE] = phone
            prefs[KEY_PROFILE_GENDER] = gender
            prefs[KEY_PROFILE_BIRTHDAY] = birthday
        }
    }

    suspend fun saveAvatar(context: Context, avatarUri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_AVATAR] = avatarUri
        }
    }

    // Settings getters
    fun darkTheme(context: Context): Flow<Boolean> = context.dataStore.data.map { it[KEY_SETTINGS_DARK] ?: false }
    fun notifications(context: Context): Flow<Boolean> = context.dataStore.data.map { it[KEY_SETTINGS_NOTI] ?: true }
    // language no longer exposed in UI; keep for backward compatibility
    fun language(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_SETTINGS_LANG] ?: "vi" }

    suspend fun saveSettings(context: Context, dark: Boolean, noti: Boolean, lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETTINGS_DARK] = dark
            prefs[KEY_SETTINGS_NOTI] = noti
            prefs[KEY_SETTINGS_LANG] = lang
        }
    }

    suspend fun resetSettings(context: Context) {
        saveSettings(context, dark = false, noti = true, lang = "vi")
    }

    // Auth helpers
    fun rememberMe(context: Context): Flow<Boolean> = context.dataStore.data.map { it[KEY_REMEMBER_ME] ?: false }
    fun lastEmail(context: Context): Flow<String> = context.dataStore.data.map { it[KEY_LAST_EMAIL] ?: "" }

    suspend fun saveRememberMe(context: Context, remember: Boolean, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = remember
            prefs[KEY_LAST_EMAIL] = if (remember) email else ""
        }
    }

    // Rating getters
    fun ratingScore(context: Context): Flow<Int> = context.dataStore.data.map { it[KEY_RATING_SCORE] ?: 0 }
    // Store feedback list as lines separated by \u0001 to keep delimiter safe
    fun ratingFeedbackList(context: Context): Flow<List<String>> = context.dataStore.data.map {
        it[KEY_RATING_FEEDBACK]?.split('\u0001')?.filter { s -> s.isNotBlank() } ?: emptyList()
    }
    fun deletedBuffer(context: Context): Flow<List<String>> = context.dataStore.data.map {
        it[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.filter { s -> s.isNotBlank() } ?: emptyList()
    }

    suspend fun saveRating(context: Context, score: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_RATING_SCORE] = score }
    }

    suspend fun addFeedback(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RATING_FEEDBACK]?.split('\u0001')?.toMutableList() ?: mutableListOf()
            current.add(entry)
            prefs[KEY_RATING_FEEDBACK] = current.joinToString(separator = "\u0001")
        }
    }

    suspend fun removeFeedback(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_RATING_FEEDBACK]?.split('\u0001')?.toMutableList() ?: mutableListOf()
            if (current.remove(entry)) {
                prefs[KEY_RATING_FEEDBACK] = current.joinToString(separator = "\u0001")
                // buffer for possible undo
                val buf = prefs[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.toMutableList() ?: mutableListOf()
                buf.add(entry)
                prefs[KEY_RATING_DELETED_BUFFER] = buf.joinToString(separator = "\u0001")
            }
        }
    }

    suspend fun undoRemove(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val buf = prefs[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.toMutableList() ?: mutableListOf()
            if (buf.remove(entry)) {
                prefs[KEY_RATING_DELETED_BUFFER] = buf.joinToString(separator = "\u0001")
                // restore
                val current = prefs[KEY_RATING_FEEDBACK]?.split('\u0001')?.toMutableList() ?: mutableListOf()
                current.add(entry)
                prefs[KEY_RATING_FEEDBACK] = current.joinToString(separator = "\u0001")
            }
        }
    }

    suspend fun purgeDeleted(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val buf = prefs[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.toMutableList() ?: mutableListOf()
            if (buf.remove(entry)) {
                prefs[KEY_RATING_DELETED_BUFFER] = buf.joinToString(separator = "\u0001")
            }
        }
    }
}
