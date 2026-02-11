package com.example.accounting.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.lifecycle.ViewModelProvider
import com.example.accounting.MainActivity
import com.example.accounting.R
import com.example.accounting.databinding.ActivityRegisterBinding
import com.example.accounting.utils.SmartRedis
import com.example.accounting.utils.SpUtil
import com.example.accounting.viewmodel.UserViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding

    private var countDownTimer: CountDownTimer? = null

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 之所以传Class对象是为了节省内存，如果存在则直接复用，不存在可反射创建实例
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // 渲染标题颜色
        binding.registerTitle.text = buildSpannedString {
            append(getString(R.string.register))
            color(Color.parseColor("#FBB03B")) {
                append(getString(R.string.simple_accounting))
            }
            append(getString(R.string.account))
        }

        // 注册结果监听
        userViewModel.registerResult.observe(this) { result ->
            result.onSuccess { user ->
                Toast.makeText(this,"登录成功", Toast.LENGTH_SHORT).show()
                // 同时将当前用户持久化，保证下次自动登录即可
                SpUtil.saveUserId(this,user.id)
                // 登录成功直接跳转到主页
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }.onFailure { e ->
                Toast.makeText(this,e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 邮箱、验证码、密码进行实时监测光标
        emailValid()
        verifyValid()
        passwordValid()

        // 获取验证码
        binding.gainVerify.setOnClickListener { gainVerify() }

        // 最后输入密码后直接点击软键盘
        binding.password.setOnEditorActionListener { v, actionId, event ->
            // 判断点击的是否是“完成”动作
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 调用注册方法
                registerValid()
                true
            } else {
                false
            }
        }

        // 注册时检测是否有误
        binding.registerLoginBtn.setOnClickListener {registerValid()}
    }

    private fun passwordValid() {
        binding.password.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.passwordLayout.error = null
            }
        }
    }

    private fun verifyValid() {
        binding.verificationCode.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.verificationCodeLayout.error = null
            }
        }
    }

    private fun emailValid() {
        binding.email.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.emailLayout.error = null
            }
        }
    }

    private fun gainVerify() {
        binding.root.requestFocus()

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
        sendVerifyNotification(code)

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
        // 失去所有焦点
        binding.root.requestFocus()

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

        // 注册用户
        userViewModel.register(email,password)

        Toast.makeText(this,"注册成功，已跳转至主页", Toast.LENGTH_SHORT).show()
    }

    private fun sendVerifyNotification(code: String) {
        val channelId = "verify_channel"
        val notificationId = System.currentTimeMillis().toInt()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 1. Android 8.0+ 必须创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "验证码通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于接收单机版模拟验证码"
            }
            manager.createNotificationChannel(channel)
        }

        // 弹出权限通知栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                // 弹出系统权限请求对话框
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 2. 构建通知内容
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("验证码服务")
            .setContentText("您的验证码是：$code，请在30秒内输入。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 点击后自动消失

        // 3. 发送
        manager.notify(notificationId, builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        // 防止内存泄漏
        countDownTimer?.cancel()
    }
}