package com.example.accounting.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // 必须是这个路径！

class Converters {
    private val gson = Gson() // 服用gson

    @TypeConverter
    fun fromString(value: String?): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        // 加上判空处理，防止数据库里有空值导致崩溃
        return gson.fromJson(value ?: "[]", listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }
}