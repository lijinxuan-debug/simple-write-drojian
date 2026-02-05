package com.example.accounting.data.model

// 分类组（如：货品材料）
data class CategoryGroup(
    val id: Int,
    val groupName: String,
    val items: List<CategoryItem>
)