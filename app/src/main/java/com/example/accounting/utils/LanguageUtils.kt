package com.example.accounting.utils

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageUtils {
    private const val SP_NAME = "language_prefs"
    private const val KEY_LANG = "selected_language"

    // 切换语言的核心方法
    fun setLanguage(context: Context, lang: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 官方方案：系统自动处理持久化和切换
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
            localeManager.applicationLocales = LocaleList.forLanguageTags(lang)
        } else {
            // Android 12 及以下：手动存 SP + AppCompatDelegate
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(KEY_LANG, lang).apply()

            // 使用支持库的方案来切换
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(lang)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    // 获取当前语言（用于弹窗默认选中项）
    fun getCurrentLanguage(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) "zh" else locales[0].language
        } else {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            sp.getString(KEY_LANG, "zh") ?: "zh"
        }
    }
}