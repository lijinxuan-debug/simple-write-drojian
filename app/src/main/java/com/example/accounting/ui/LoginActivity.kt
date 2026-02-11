package com.example.accounting.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.ViewModelProvider
import com.example.accounting.MainActivity
import com.example.accounting.R
import com.example.accounting.databinding.ActivityLoginBinding
import com.example.accounting.utils.SpUtil
import com.example.accounting.viewmodel.UserViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // 对登录结果进行监听
        userViewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                Toast.makeText(this,"登录成功", Toast.LENGTH_SHORT).show()
                SpUtil.saveUserId(this,user.id)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }.onFailure { e ->
                Toast.makeText(this,e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 自动登录
        if (SpUtil.getUserId(this) != -1L) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 先对标题进行监听
        binding.loginTitle.text = buildSpannedString {
            append(getString(R.string.welcome))
            color(Color.parseColor("#FBB03B")) {
                append(getString(R.string.app_name))
            }
        }

        // 对注册页面进行跳转
        binding.signup.setOnClickListener {
            val loginToRegister = Intent(this, RegisterActivity::class.java)
            startActivity(loginToRegister)
        }

        // 对忘记密码页面进行跳转
        binding.forgetPassword.setOnClickListener {
            val loginToForget = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(loginToForget)
        }

        // 登录按钮进行监听
        binding.login.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = binding.email.text.toString()
        val password = binding.password.text.toString()

        if (!"^[a-z0-9.]{6,30}@gmail.com$".toRegex().matches(email)) {
            binding.emailLayout.error = "请输入有效的 Gmail"
            return
        }

        userViewModel.login(email,password)
    }
}