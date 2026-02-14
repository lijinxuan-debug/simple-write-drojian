package com.example.accounting.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.accounting.data.model.User

@Dao
interface UserDao {
    // 插入新用户
    @Insert
    suspend fun registerUser(user: User)

    // 根据邮箱查询用户（注册前检查邮箱是否被占用）
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    // 根据用户ID查询用户
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

}