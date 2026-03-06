package com.example.accounting.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    private var mToast: Toast? = null

    fun show(context: Context, text: String) {
        // 1. 取消上一个正在显示的 Toast
        mToast?.cancel()
        
        // 2. 创建新的 Toast
        // 注意：这里建议使用 context.applicationContext 防止内存泄漏
        mToast = Toast.makeText(context.applicationContext, text, Toast.LENGTH_SHORT)
        
        // 3. 显示
        mToast?.show()
    }
}