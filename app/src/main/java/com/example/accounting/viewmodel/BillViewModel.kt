package com.example.accounting.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.accounting.data.database.AppDatabase
import com.example.accounting.data.model.AccountItem
import com.example.accounting.data.model.CategoryItem
import com.example.accounting.data.model.Record
import com.example.accounting.utils.CategoryAndAccountData
import com.example.accounting.utils.SpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.Exception
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.ZoneId

class BillViewModel(application: Application) : AndroidViewModel(application) {
    // 获取recordDao实例
    private val recordDao = AppDatabase.getDatabase(application).recordDao()
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

    // 接下来的分类、备注、时间、账户都要分两套，因为支出和收入并不同
    // 其中账户和分类信息都是默认为所有数据第一个，时间也是默认当天
    var expenseDate : Long = System.currentTimeMillis()
    var expenseCategoryItem : CategoryItem = CategoryAndAccountData.expenseCategories[0].items[0]
    var expenseAccount : AccountItem = CategoryAndAccountData.accountGroups[0].items[0]
    var expenseRemark : String = ""
    var expenseImage : List<String> = emptyList()

    var incomeDate : Long = System.currentTimeMillis()
    var incomeCategoryItem : CategoryItem = CategoryAndAccountData.expenseCategories[0].items[0]
    var incomeAccount : AccountItem = CategoryAndAccountData.accountGroups[0].items[0]
    var incomeRemark : String = ""
    var incomeImage : List<String> = emptyList()

    val billSaveResult = MutableLiveData<Result<String>>()

    private val formater = DecimalFormat("#,##0.00")

    private val currentYear = MutableStateFlow(LocalDate.now().year)
    private val currentMonth = MutableStateFlow(LocalDate.now().monthValue)

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthRecords: Flow<List<Record>> = combine(currentYear,currentMonth) { year, month ->
        getMonthRange(year,month)
    }.flatMapLatest { (startTime,endTime) ->
        recordDao.selectRecordsByMonth(SpUtil.getUserId(application),startTime,endTime)
    }

    // 总支出
    val allExpense: Flow<String> = monthRecords.map { list ->
        val sum = list.filter { it.type == 0 } // 过滤出支出
            .sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
        formater.format(sum)
    }

    // 总收入
    val allIncome: Flow<String> = monthRecords.map { list ->
        val sum = list.filter { it.type == 1 } // 过滤出收入
            .sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
        formater.format(sum)
    }

    fun changeDate(year: Int, month: Int) {
        currentYear.value = year
        currentMonth.value = month
    }

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

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val start = LocalDate.of(year,month,1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val end = LocalDate.of(year,month,1)
            .plusMonths(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        return Pair(start,end)
    }

    /**
     * 将账单存到room数据库
     */
    fun insertRecord(record: Record) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recordDao.insertRecord(record)
                billSaveResult.postValue(Result.success("账单保存成功"))
            } catch (e: Exception) {
                Log.e("保存账单出现错误",e.message.toString())
                billSaveResult.postValue(Result.failure(Exception("保存账单错误")))
            }
        }
    }

}