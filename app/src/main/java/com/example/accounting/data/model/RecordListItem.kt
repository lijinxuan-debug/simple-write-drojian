package com.example.accounting.data.model

sealed class RecordListItem {
    // 日期头类型
    data class Header(val dateStr: String) : RecordListItem()
    // 真实的账单数据类型
    data class Item(val record: Record) : RecordListItem()
}