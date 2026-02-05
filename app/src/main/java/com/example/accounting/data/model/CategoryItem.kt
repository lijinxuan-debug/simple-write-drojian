package com.example.accounting.data.model

// 单个小分类（如：进货支出）
data class CategoryItem(
    val id: Int,
    val groupName: String,
    val name: String,
    val iconRes: Int
)