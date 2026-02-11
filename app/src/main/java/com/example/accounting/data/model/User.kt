package com.example.accounting.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 是账号也是昵称
    val email: String,
    // 账号创建时间
    val createTime: Long = System.currentTimeMillis(),
    // 密码
    val password: String,
    // 头像
    val avatar: String = "default_avatar"
)