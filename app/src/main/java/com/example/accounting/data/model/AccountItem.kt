package com.example.accounting.data.model

// 单个账户
data class AccountItem(
    val id: Int,
    val groupName: String, // 所属组：虚拟账户、储蓄账户等
    val name: String,
    val iconRes: Int
)
