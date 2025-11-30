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

// Tên file DataStore Preferences dùng để lưu các thiết lập tài khoản
private const val PREFS_NAME = "account_prefs"

// Extension cho Context để truy cập DataStore
val Context.dataStore by preferencesDataStore(PREFS_NAME)

/**
 * AccountPreferences
 * ------------------
 * Lớp object dùng để:
 *  - Định nghĩa các KEY lưu trong DataStore (profile, settings, rating, login)
 *  - Cung cấp hàm get (Flow<...>) để UI đọc dữ liệu (compose collectAsState)
 *  - Cung cấp hàm suspend để lưu / cập nhật dữ liệu
 */
object AccountPreferences {

    // ============================
    // 1. KHOÁ PROFILE (thông tin cá nhân)
    // ============================
    private val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")           // Họ tên
    private val KEY_PROFILE_PHONE = stringPreferencesKey("profile_phone")         // Số điện thoại
    private val KEY_PROFILE_GENDER = stringPreferencesKey("profile_gender")       // Giới tính: "male" | "female"
    private val KEY_PROFILE_BIRTHDAY = stringPreferencesKey("profile_birthday")   // Ngày sinh dạng string
    private val KEY_PROFILE_AVATAR = stringPreferencesKey("profile_avatar_uri")   // Uri avatar (content:// ...) do user chọn

    // ============================
    // 2. KHOÁ SETTINGS (cài đặt ứng dụng)
    // ============================
    private val KEY_SETTINGS_DARK = booleanPreferencesKey("settings_dark")  // Bật/tắt Dark theme
    private val KEY_SETTINGS_NOTI = booleanPreferencesKey("settings_noti")  // Bật/tắt thông báo
    private val KEY_SETTINGS_LANG = stringPreferencesKey("settings_lang")   // Ngôn ngữ: "vi" | "en" (để tương thích phiên bản cũ)

    // ============================
    // 3. KHOÁ RATING (đánh giá ứng dụng)
    // ============================
    private val KEY_RATING_SCORE = intPreferencesKey("rating_score")                 // Điểm đánh giá (1..5)
    private val KEY_RATING_FEEDBACK = stringPreferencesKey("rating_feedback_list")   // Chuỗi nhiều feedback, nối bằng kí tự đặc biệt
    private val KEY_RATING_DELETED_BUFFER =
        stringPreferencesKey("rating_feedback_deleted_buffer")                       // Buffer tạm để lưu feedback đã xoá (phục vụ Undo)

    // ============================
    // 4. KHOÁ AUTH / LOGIN
    // ============================
    private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")  // Ghi nhớ đăng nhập
    private val KEY_LAST_EMAIL = stringPreferencesKey("last_email")     // Email cuối cùng được lưu

    // =====================================================
    // PROFILE – HÀM ĐỌC (Flow) & LƯU
    // =====================================================

    // Trả về Flow<String> tên profile; nếu chưa có → ""
    fun profileName(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_PROFILE_NAME] ?: "" }

    fun profilePhone(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_PROFILE_PHONE] ?: "" }

    fun profileGender(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_PROFILE_GENDER] ?: "" }

    fun profileBirthday(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_PROFILE_BIRTHDAY] ?: "" }

    fun profileAvatar(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_PROFILE_AVATAR] ?: "" }

    // Lưu thông tin profile cơ bản: họ tên / sđt / giới tính / ngày sinh
    suspend fun saveProfile(context: Context, name: String, phone: String, gender: String, birthday: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_NAME] = name
            prefs[KEY_PROFILE_PHONE] = phone
            prefs[KEY_PROFILE_GENDER] = gender
            prefs[KEY_PROFILE_BIRTHDAY] = birthday
        }
    }

    // Lưu đường dẫn avatar (uri dưới dạng String)
    suspend fun saveAvatar(context: Context, avatarUri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILE_AVATAR] = avatarUri
        }
    }

    // =====================================================
    // SETTINGS – HÀM ĐỌC & LƯU CÀI ĐẶT
    // =====================================================

    // Dark theme: true = bật, false = tắt (mặc định false)
    fun darkTheme(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SETTINGS_DARK] ?: false }

    // Thông báo: true = cho phép, false = tắt (mặc định true)
    fun notifications(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SETTINGS_NOTI] ?: true }

    // Ngôn ngữ: trước đây có UI, giờ giữ cho tương thích; mặc định "vi"
    fun language(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_SETTINGS_LANG] ?: "vi" }

    // Lưu toàn bộ settings một lần
    suspend fun saveSettings(context: Context, dark: Boolean, noti: Boolean, lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETTINGS_DARK] = dark
            prefs[KEY_SETTINGS_NOTI] = noti
            prefs[KEY_SETTINGS_LANG] = lang
        }
    }

    // Reset về giá trị mặc định (light theme, bật noti, tiếng Việt)
    suspend fun resetSettings(context: Context) {
        saveSettings(context, dark = false, noti = true, lang = "vi")
    }

    // =====================================================
    // AUTH / LOGIN – HÀM HỖ TRỢ GHI NHỚ ĐĂNG NHẬP
    // =====================================================

    // Flow<Boolean> trạng thái "ghi nhớ đăng nhập"
    fun rememberMe(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_REMEMBER_ME] ?: false }

    // Email cuối cùng được lưu (nếu rememberMe = true)
    fun lastEmail(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY_LAST_EMAIL] ?: "" }

    // Lưu trạng thái remember_me và last_email
    // Nếu remember = false → xoá email (set về "")
    suspend fun saveRememberMe(context: Context, remember: Boolean, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = remember
            prefs[KEY_LAST_EMAIL] = if (remember) email else ""
        }
    }

    // =====================================================
    // RATING – LƯU ĐIỂM VÀ DANH SÁCH GÓP Ý
    // =====================================================

    // Điểm rating (0 nếu chưa đánh giá)
    fun ratingScore(context: Context): Flow<Int> =
        context.dataStore.data.map { it[KEY_RATING_SCORE] ?: 0 }

    // Danh sách feedback: lưu trong DataStore dạng chuỗi, phân tách bằng kí tự đặc biệt '\u0001'
    // Khi đọc ra → tách thành List<String>
    fun ratingFeedbackList(context: Context): Flow<List<String>> =
        context.dataStore.data.map {
            it[KEY_RATING_FEEDBACK]?.split('\u0001')?.filter { s -> s.isNotBlank() } ?: emptyList()
        }

    // Buffer tạm các feedback đã xoá (dùng cho tính năng Undo)
    fun deletedBuffer(context: Context): Flow<List<String>> =
        context.dataStore.data.map {
            it[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.filter { s -> s.isNotBlank() } ?: emptyList()
        }

    // Lưu điểm rating mới
    suspend fun saveRating(context: Context, score: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_RATING_SCORE] = score }
    }

    // Thêm một feedback mới vào danh sách
    suspend fun addFeedback(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val current =
                prefs[KEY_RATING_FEEDBACK]?.split('\u0001')?.toMutableList() ?: mutableListOf()
            current.add(entry)
            // Ghép lại thành một chuỗi, phân tách bởi '\u0001'
            prefs[KEY_RATING_FEEDBACK] = current.joinToString(separator = "\u0001")
        }
    }

    // Xoá một feedback khỏi danh sách, đồng thời đưa vào buffer để có thể Undo
    suspend fun removeFeedback(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val current =
                prefs[KEY_RATING_FEEDBACK]?.split('\u0001')?.toMutableList() ?: mutableListOf()
            if (current.remove(entry)) {
                // Lưu lại danh sách mới sau khi xoá
                prefs[KEY_RATING_FEEDBACK] = current.joinToString(separator = "\u0001")
                // Thêm entry vừa xoá vào buffer
                val buf =
                    prefs[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.toMutableList()
                        ?: mutableListOf()
                buf.add(entry)
                prefs[KEY_RATING_DELETED_BUFFER] = buf.joinToString(separator = "\u0001")
            }
        }
    }

    // Phục hồi (Undo) một feedback đã xoá: lấy từ buffer đưa trở lại danh sách chính
    suspend fun undoRemove(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val buf =
                prefs[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.toMutableList()
                    ?: mutableListOf()
            if (buf.remove(entry)) {
                // Cập nhật lại buffer sau khi đã lấy ra
                prefs[KEY_RATING_DELETED_BUFFER] = buf.joinToString(separator = "\u0001")
                // Thêm entry trở lại danh sách feedback
                val current =
                    prefs[KEY_RATING_FEEDBACK]?.split('\u0001')?.toMutableList()
                        ?: mutableListOf()
                current.add(entry)
                prefs[KEY_RATING_FEEDBACK] = current.joinToString(separator = "\u0001")
            }
        }
    }

    // Xoá hẳn một entry khỏi buffer (khi người dùng không muốn Undo nữa)
    suspend fun purgeDeleted(context: Context, entry: String) {
        context.dataStore.edit { prefs ->
            val buf =
                prefs[KEY_RATING_DELETED_BUFFER]?.split('\u0001')?.toMutableList()
                    ?: mutableListOf()
            if (buf.remove(entry)) {
                prefs[KEY_RATING_DELETED_BUFFER] = buf.joinToString(separator = "\u0001")
            }
        }
    }
}
