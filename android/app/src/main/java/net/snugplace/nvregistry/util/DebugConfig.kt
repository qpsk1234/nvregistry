package net.snugplace.nvregistry.util

import android.content.Context

/**
 * アプリ全体のデバッグログ有効/無効を管理するシングルトン。
 * SharedPreferencesで永続化するので再起動後も設定が維持される。
 */
object DebugConfig {

    private const val PREF_NAME = "nvregistry_prefs"
    private const val KEY_DEBUG = "debug_enabled"

    // メモリキャッシュ（Context不要な箇所でも参照できるよう）
    var isEnabled: Boolean = false
        private set

    fun init(context: Context) {
        isEnabled = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEBUG, false)
    }

    fun toggle(context: Context): Boolean {
        isEnabled = !isEnabled
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEBUG, isEnabled)
            .apply()
        return isEnabled
    }
}
