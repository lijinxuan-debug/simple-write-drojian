package com.example.accounting.utils

import android.content.Context

object SpUtil {
    private const val PREF_NAME = "accounting_user_prefs"
    private const val KEY_USER_ID = "current_user_id"

    // 保存登录状态
    fun saveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    // 获取当前登录 ID
    fun getUserId(context: Context): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1L)
    }

    // 退出登录时清除
    fun clear(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}