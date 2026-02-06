package com.example.accounting.data.model

// 账户组
data class AccountGroup(
    val id: Int,
    val groupName: String,
    val items: List<AccountItem>
)
