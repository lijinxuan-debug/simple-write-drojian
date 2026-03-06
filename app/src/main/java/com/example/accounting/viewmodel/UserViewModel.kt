package com.example.accounting.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.accounting.data.database.AppDatabase
import com.example.accounting.data.model.User
import com.example.accounting.utils.SpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserViewModel(application: Application) : AndroidViewModel(application) {

    // 首先需要获取UserDAO实例
    private val userDAO = AppDatabase.getDatabase(application).userDao()

    // 注册状态
    val registerResult = MutableLiveData<Result<Long>>()

    // 登录状态
    val loginResult = MutableLiveData<Result<User>>()

    // 查询用户
    val currentUser = MutableLiveData<User?>()

    /**
     * 注册相关逻辑
     */
    fun register(email: String, password: String) {
        // 创建协程执行IO密集型任务
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 先检查邮箱是否已经存在
                val user = userDAO.getUserByEmail(email)

                if (user != null) {
                    // 如果已经存在则通知UI线程失败消息
                    registerResult.postValue(Result.failure(Exception("该邮箱已被注册")))
                } else {
                    val newUser = User(
                        email = email,
                        password = password
                    )

                    // 插入数据库
                    val userId = userDAO.registerUser(newUser)
                    // 插入成功的话则返回用户对象
                    registerResult.postValue(Result.success(userId))
                }
            } catch (e : Exception) {
                registerResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 通过Id查询用户
     */
    fun getUserInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDAO.getUserById(SpUtil.getUserId(getApplication()))
                currentUser.postValue(user)
            } catch (e: Exception) {
                Log.e("获取用户数据异常", e.message.toString())
            }
        }
    }

    /**
     * 存储用户头像路径到数据库
     */
    suspend fun saveUserAvatar(avatar: String): Boolean {
        // 切换到 IO 线程并【等待】其结果返回
        return withContext(Dispatchers.IO) {
            try {
                val userId = SpUtil.getUserId(getApplication())
                val rowsAffected = userDAO.updateAvatarById(avatar, userId)

                // 如果受影响行数 > 0，说明更新成功
                rowsAffected > 0
            } catch (e: Exception) {
                Log.e("存储失败", e.message.toString())
                false // 发生异常返回 false
            }
        }
    }

    /**
     * 登录账号
     */
    fun login(email: String,password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDAO.getUserByEmail(email)
                if (user == null) {
                    loginResult.postValue(Result.failure(Exception("用户不存在")))
                    return@launch
                }
                if (user.password != password) {
                    loginResult.postValue(Result.failure(Exception("密码错误")))
                    return@launch
                }
                // 登录成功
                loginResult.postValue(Result.success(user))

            } catch (e: Exception) {
                Log.e("登录异常",e.message.toString())
            }
        }
    }
}