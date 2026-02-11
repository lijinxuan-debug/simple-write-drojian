package com.example.accounting.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.accounting.data.database.AppDatabase
import com.example.accounting.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    // 首先需要获取UserDAO实例
    private val userDAO = AppDatabase.getDatabase(application).userDao()

    // 注册状态
    val registerResult = MutableLiveData<Result<User>>()

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
                    userDAO.registerUser(newUser)

                    // 插入成功的话则返回用户对象
                    registerResult.postValue(Result.success(newUser))
                }
            } catch (e : Exception) {
                registerResult.postValue(Result.failure(e))
            }
        }
    }

    /**
     * 通过Id查询用户
     */
    fun getUserInfo(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = userDAO.getUserById(id)
                currentUser.postValue(user)
            } catch (e: Exception) {
                Log.e("获取用户数据异常", e.message.toString())
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