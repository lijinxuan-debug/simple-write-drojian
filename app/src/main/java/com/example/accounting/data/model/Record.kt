package com.example.accounting.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,          // 金额
    val type: Int,               // 0:支出, 1:收入
    val categoryName: String,    // 分类：餐饮、交通等
    val paymentMethod: String,   // 付款方式：支付宝、微信、银行卡、现金
    val timestamp: Long,         // 用于周/月/年统计
    val dateStr: String,         // 格式化日期 "2024-05-20"
    val monthStr: String,        // 格式化月份 "2024-05"，方便关联预算表
    val remark: String = ""      // 备注
)