package com.example.accounting.utils

import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.DecimalFormat

class WanValueFormatter : ValueFormatter() {
    // 使用更简洁的格式，防止由于数字太长（如 10.00亿）导致图表空间又被挤压
    private val format = DecimalFormat("###,###,##0.00")

    override fun getFormattedValue(value: Float): String {
        val absoluteValue = Math.abs(value)

        return when {
            // 1. 判断是否大于等于 1 亿 (100,000,000)
            absoluteValue >= 100_000_000f -> {
                format.format(value / 100_000_000f) + "亿"
            }
            // 2. 判断是否大于等于 1 万 (10,000)
            absoluteValue >= 10_000f -> {
                format.format(value / 10_000f) + "万"
            }
            // 3. 原数显示
            else -> {
                format.format(value)
            }
        }
    }
}