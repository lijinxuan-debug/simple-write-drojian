package com.example.accounting.utils

import java.text.DecimalFormat
import kotlin.math.abs

object CalcUtil {

    private const val MAX_LIMIT = 999999999.99
    private const val BOUNDARY = 1000000000.0

    /**
     * 保底过滤：确保任何数字进入 UI 前都在正负 10 亿范围内
     */
    private fun coerceResult(value: Double): Double {
        if (value.isInfinite() || value.isNaN()) return 0.0
        return when {
            value >= BOUNDARY -> MAX_LIMIT
            value <= -BOUNDARY -> -MAX_LIMIT
            else -> value
        }
    }

    /**
     * 格式化大金额：带千分位 (用于 tvAmount)
     */
    fun formatAmount(result: Double): String {
        val finalResult = coerceResult(result)
        val df = DecimalFormat("#,##0.00")
        return df.format(finalResult)
    }

    /**
     * 格式化计算值：纯数字 (用于 tvCalculation)
     */
    fun formatForCalculation(result: Double): String {
        val finalResult = coerceResult(result)
        // 使用 %.2f 确保不生成千分位逗号
        return String.format("%.2f", finalResult)
    }

    /**
     * 根据金额大小获取单位描述
     */
    fun getUnitHint(result: Double): String {
        val absValue = abs(coerceResult(result))
        return when {
            absValue >= 100000000.0 -> "亿"
            absValue >= 10000000.0 -> "千万"
            absValue >= 1000000.0 -> "百万"
            absValue >= 100000.0 -> "十万"
            absValue >= 10000.0 -> "万"
            else -> ""
        }
    }
}