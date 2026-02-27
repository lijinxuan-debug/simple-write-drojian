package com.example.accounting.data.model

data class TimeTab(
    val label: String,    // 用于显示的文字
    val year: Int,
    val month: Int,
    var isSelected: Boolean = false
)