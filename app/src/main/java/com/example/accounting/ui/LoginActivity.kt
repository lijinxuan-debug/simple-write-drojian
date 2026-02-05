package com.example.accounting.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.accounting.R
import com.example.accounting.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }
}