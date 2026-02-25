package com.example.accounting.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                  // 账单唯一主键，由数据库自动生成

    val amount: String,                // 交易金额（例如：88.50）

    val userId: Long,                  // 这个账单的用户id

    val type: Int,                     // 账单类型：0 代表支出，1 代表收入

    val categoryIcon: String,          // 分类图标文件昵称

    // --- 分类相关 ---
    val categoryId: Int,               // 分类唯一 ID（对应 CategoryItem 的 id，用于后期按分类统计和过滤）

    val categoryName: String,          // 分类显示名称（例如：“员工工资”、“主营业务收入”）

    val categoryGroupName: String,     // 分类所属的分组名称（例如：“人工支出”、“营业收入”）

    // --- 账户相关 ---
    val accountId: Int,                // 账户唯一 ID（对应 AccountItem 的 id，用于计算某个账户的余额）

    val paymentMethod: String,         // 账户名称/支付方式（例如：“现金账户”、“支付宝”、“银行卡”）

    // --- 时间相关 ---
    val timestamp: Long,               // 原始时间戳（毫秒级，用于精确排序和周/月/年等逻辑运算）

    val dateStr: String,               // 格式化后的日期字符串（例如：“2月12日 周四”，用于界面分组显示和简单的 SQL 模糊查询）

    val timeStr: String,               // 格式化后的时间字符串

    // --- 备注与附件 ---
    val remark: String = "",           // 用户填写的备注信息，默认为空字符串

    val images: List<String> = emptyList() // 账单关联的图片路径列表（存储在私有目录下的路径）
) : Parcelable