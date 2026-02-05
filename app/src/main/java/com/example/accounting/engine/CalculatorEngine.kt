package com.example.accounting.engine

object CalculatorEngine {
    fun evaluate(expression: String): Double {
        // 1. 预处理：去掉末尾多余的操作符 (比如 "93+46-" -> "93+46")
        var finalExp = expression.trim()
        while (finalExp.isNotEmpty() && "[+\\-]$".toRegex().containsMatchIn(finalExp)) {
            finalExp = finalExp.dropLast(1).trim()
        }

        if (finalExp.isEmpty()) return 0.0

        // 2. 解析 Tokens (提取数字和 + -)
        // 使用正则提取：匹配数字(带小数) 或者 符号 + -
        val pattern = """(\d*\.?\d+)|[+\-]""".toRegex()
        val tokens = pattern.findAll(finalExp).map { it.value }.toList()

        if (tokens.isEmpty()) return 0.0

        // 3. 流式计算 (因为只有加减，直接按顺序处理)
        var result = tokens[0].toDoubleOrNull() ?: 0.0

        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]      // 符号
            val nextVal = if (i + 1 < tokens.size) tokens[i + 1].toDoubleOrNull() ?: 0.0 else 0.0

            when (op) {
                "+" -> result += nextVal
                "-" -> result -= nextVal
            }
            i += 2 // 跳过符号和已经算过的数字
        }

        return result
    }
}