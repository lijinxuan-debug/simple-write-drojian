package com.example.accounting.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BillViewModel : ViewModel() {
    // 计算的结果，即金额数据
    val amount = MutableLiveData<String>("0.00")

    // 计算过程 (如: 50+50)
    val calculationExpression = MutableLiveData<String>("")

    // 单位提示 (如: 万)
    val unitHint = MutableLiveData<String>("")

    // 单位显示状态 (true为可见，false为不可见但占位)
    val unitHintVisible = MutableLiveData<Boolean>(false)

    // 键盘显示状态 (Activity 控制隐藏，Fragment 监听)
    val isKeyboardVisible = MutableLiveData<Boolean>(true)

    // 记录需要切换到的页面索引：-1 表示不切换，0 表示支出，1 表示收入
    val jumpToPage = MutableLiveData<Int>(-1)

    // 更新金额的方法
    fun updateAmount(newAmount: String) {
        amount.value = newAmount
    }

    fun updateKeyboardVisible(newKeyboardVisible: Boolean) {
        isKeyboardVisible.value = newKeyboardVisible
    }

    fun updateUnitHint(newUnitHint: String) {
        unitHint.value = newUnitHint
    }

    fun updateCalculationExpression(newCalculationExpression: String) {
        calculationExpression.value = newCalculationExpression
    }

    fun updateUnitHintVisible(newUnitHintVisible: Boolean) {
        unitHintVisible.value = newUnitHintVisible
    }
}