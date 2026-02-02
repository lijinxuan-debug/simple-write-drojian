package com.example.accounting

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.example.accounting.databinding.ActivityRegisterBinding
import com.example.accounting.util.SmartRedis

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渲染标题颜色
        binding.registerTitle.text = buildSpannedString {
            append(getString(R.string.register))
            color(Color.parseColor("#FBB03B")) {
                append(getString(R.string.simple_accounting))
            }
            append(getString(R.string.account))
        }
        // 邮箱、验证码、密码进行实时监测
        emailValid()
        verifyValid()
        passwordValid()

        // 获取验证码
        binding.gainVerify.setOnClickListener { gainVerify() }

        // 注册时检测是否有误
        binding.registerLoginBtn.setOnClickListener {registerValid()}
    }

    private fun passwordValid() {
        binding.password.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.passwordLayout.error = null
            }
        })
    }

    private fun verifyValid() {
        binding.verificationCode.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.verificationCodeLayout.error = null
            }
        })
    }

    private fun emailValid() {
        binding.email.addTextChangedListener(object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 1. 只要一动，立刻清除红色报错
                binding.emailLayout.error = null

                // 实时检查是否包含非法字符（如中文或特殊符号）
                val input = s.toString()
                if (input.any { it.isWhitespace() }) {
                    binding.emailLayout.error = "邮箱不能包含空格"
                }
            }
        })
    }

    private fun gainVerify() {
        // 暂时使用hashmap存储
        val email = binding.email.text.toString().trim()

        // 先简单校验邮箱格式（之前写的正则）
        if (!"^[a-z0-9.]{6,30}@gmail.com$".toRegex().matches(email)) {
            binding.emailLayout.error = "请输入有效的 Gmail"
            return
        }

        // 模拟生成 6 位验证码并存入你的 LruCache
        val code = (100000..999999).random().toString()
        SmartRedis.set(email, code, 30) // 存入 30 秒有效

        // 模拟发送
        Toast.makeText(this, "验证码已发送：$code", Toast.LENGTH_LONG).show()

        // 启动倒计时
        startCountDown()
    }

    private fun startCountDown() {
        // 禁用按钮，防止倒计时期间重复点击
        binding.gainVerify.isEnabled = false
        binding.gainVerify.alpha = 0.5f // 变淡，视觉上表示禁用

        countDownTimer = object : CountDownTimer(30000, 1000) {
            // 每秒触发一次
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                binding.gainVerify.text = "${secondsLeft}s 后重发"
            }

            // 倒计时结束
            override fun onFinish() {
                binding.gainVerify.isEnabled = true
                binding.gainVerify.alpha = 1.0f
                binding.gainVerify.text = "获取验证码"
            }
        }.start()
    }

    private fun registerValid() {
        val email = binding.email.text.toString()
        val verify = binding.verificationCode.text.toString()
        val password = binding.password.text.toString()

        // google 邮箱的正则表达式
        val gmailRegex = "^[a-z0-9.]{6,30}@gmail.com$".toRegex()

        var logo = false
        // 校验邮箱
        if (email.isEmpty()) {
            binding.emailLayout.error = "邮箱不能为空"
            logo = true
        } else if (!gmailRegex.matches(email.trim().lowercase())) {
            binding.emailLayout.error = "邮箱格式有误，暂只支持google邮箱"
            logo = true
        }

        // 校验验证码
        if (verify.isEmpty()) {
            binding.verificationCodeLayout.error = "验证码不能为空"
            logo = true
        } else if (SmartRedis.get(email) != verify) {
            binding.verificationCodeLayout.error = "验证码错误或已过期"
            logo = true
        }

        // 校验密码
        if (password.isEmpty()) {
            binding.passwordLayout.error = "密码不能为空"
            logo = true
        }

        if (logo) return

        // TODO

        Toast.makeText(this,"注册成功，已跳转至主页", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 防止内存泄漏
        countDownTimer?.cancel()
    }
}