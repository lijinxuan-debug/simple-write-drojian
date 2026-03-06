package com.example.accounting.data.model

import java.sql.Timestamp

data class Message(
    val content: String,
    val isMine: Boolean, // true是用户发送的，false则是机器发送的。
    val timestamp: Long = System.currentTimeMillis(), // 设置时间戳的目的就是为了防止用户问一样的问题
    val isLoading: Boolean = false, // 默认是普通消息，只有思考中才为 true
)
