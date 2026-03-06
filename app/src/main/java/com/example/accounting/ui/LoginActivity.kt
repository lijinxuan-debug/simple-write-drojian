package com.example.accounting.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
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

    private var mToast: Toast ?= null

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

        // 对输入框和密码进行输入监听，以便消除提示
        binding.email.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.emailLayout.error = null
                binding.emailLayout.isErrorEnabled = false
            }
        }

        binding.password.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.passwordLayout.error = null
                binding.passwordLayout.isErrorEnabled = false
            }
        }

        // 登录按钮进行监听
        binding.login.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = binding.email.text.toString()
        val password = binding.password.text.toString()

        // 失去焦点
        currentFocus?.clearFocus()

        // 强制收起软件盘
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.login.windowToken,0)

        if (!"^[a-z0-9.]{6,30}@gmail.com$".toRegex().matches(email)) {
            binding.emailLayout.error = "请输入有效的 Gmail"
            return
        } else if (password.isEmpty()) {
            binding.passwordLayout.error = "密码不可为空"
            return
        }

        if (binding.protocolCheckbox.isChecked) {
            userViewModel.login(email,password)
        } else {
            showToast("请先同意用户协议")
        }

    }

    private fun showToast(text: String) {
        mToast?.cancel()

        mToast = Toast.makeText(this,text, Toast.LENGTH_SHORT)
        mToast?.show()
    }
}