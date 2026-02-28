package com.example.accounting.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.accounting.data.database.AppDatabase
import com.example.accounting.data.model.Record
import com.example.accounting.data.model.RecordListItem
import com.example.accounting.utils.CategoryAndAccountData
import com.example.accounting.utils.SpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    // 当前编辑的账单ID（默认为0）
    var id : Long = 0L
    // 计算的结果，即金额数据
    val amount = MutableLiveData<String>("0.00")

    // 计算过程 (如: 50+50)
    val calculationExpression = MutableLiveData<String>("")

    // 单位提示 (如: 万)
    val unitHint = MutableLiveData<String>("")

    // 单位显示状态 (true为可见，false为不可见但占位)
    val unitHintVisible = MutableLiveData<Boolean>(false)

    // 键盘显示状态 (Activity 控制隐藏，Fragment 监听)
    val isKeyboardVisible = MutableLiveData<Boolean>(false)

    // 记录需要切换到的页面索引：-1 表示不切换，0 表示支出，1 表示收入
    val jumpToPage = MutableLiveData<Int>(-1)

    // 接下来的分类、备注、时间、账户都要分两套，因为支出和收入并不同
    // 其中账户和分类信息都是默认为所有数据第一个，时间也是默认当天
    // --- 支出部分 ---
    val expenseDate = MutableLiveData(System.currentTimeMillis())
    val expenseCategoryItem = MutableLiveData(CategoryAndAccountData.expenseCategories[0].items[0])
    val expenseAccount = MutableLiveData(CategoryAndAccountData.accountGroups[0].items[0])
    val expenseRemark = MutableLiveData("")
    val expenseImage = MutableLiveData<List<String>>(emptyList())

    // --- 收入部分 ---
    val incomeDate = MutableLiveData(System.currentTimeMillis())
    val incomeCategoryItem = MutableLiveData(CategoryAndAccountData.incomeCategories[0].items[0]) // 注意这里应该是 incomeCategories
    val incomeAccount = MutableLiveData(CategoryAndAccountData.accountGroups[0].items[0])
    val incomeRemark = MutableLiveData("")
    val incomeImage = MutableLiveData<List<String>>(emptyList())

    val billSaveResult = MutableLiveData<Result<String>>()

    val billDeleteResult = MutableLiveData<Result<String>>()

    private val formater = DecimalFormat("#,##0.00")

    private val currentYear = MutableStateFlow(LocalDate.now().year)
    private val currentMonth = MutableStateFlow(LocalDate.now().monthValue)

    // 1. 先定义一个内部私有的原始流，它是所有数据的源头
    @OptIn(ExperimentalCoroutinesApi::class)
    val rawRecordsFlow: Flow<List<Record>> = combine(currentYear, currentMonth) { year, month ->
        getMonthRange(year, month)
    }.flatMapLatest { (startTime, endTime) ->
        recordDao.selectRecordsByMonth(SpUtil.getUserId(application), startTime, endTime)
    }.distinctUntilChanged() // 只有当数据库内容真正改变时才触发下游

    // 2. 专门给 RecyclerView 使用的流（带 Header）
    val monthRecords: Flow<List<RecordListItem>> = rawRecordsFlow.map { records ->
        val uiList = mutableListOf<RecordListItem>()
        var lastDate = ""
        records.forEach { record ->
            if (record.dateStr != lastDate) {
                uiList.add(RecordListItem.Header(record.dateStr))
                lastDate = record.dateStr
            }
            uiList.add(RecordListItem.Item(record))
        }
        uiList
    }

    // 3. 总支出计算（直接用原始流，不用 filterIsInstance，效率最高）
    val allExpense: Flow<String> = rawRecordsFlow.map { records ->
        val sum = records.filter { it.type == 0 } // 0 为支出
            .sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }
        formater.format(sum)
    }

    // 4. 总收入计算（直接用原始流）
    val allIncome: Flow<String> = rawRecordsFlow.map { records ->
        val sum = records.filter { it.type == 1 } // 1 为收入
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

    fun updateExpense(record: Record) {
        expenseRemark.value = record.remark
        expenseImage.value = record.images
        expenseDate.value = record.timestamp
        // 使用你之前在 CategoryAndAccountData 里写的查找方法
        CategoryAndAccountData.getCategoryById(record.categoryId)?.let {
            expenseCategoryItem.value = it
        }
        CategoryAndAccountData.getAccountById(record.accountId)?.let {
            expenseAccount.value = it
        }
    }

    fun updateIncome(record: Record) {
        incomeRemark.value = record.remark
        incomeImage.value = record.images
        incomeDate.value = record.timestamp
        CategoryAndAccountData.getCategoryById(record.categoryId)?.let {
            incomeCategoryItem.value = it
        }
        CategoryAndAccountData.getAccountById(record.accountId)?.let {
            incomeAccount.value = it
        }
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

    private fun getYearRange(year: Int): Pair<Long, Long> {
        val start = LocalDate.of(year, 1, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val end = LocalDate.of(year, 1, 1)
            .plusYears(1) // 增加一年，即到次年1月1日0点
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        return Pair(start, end)
    }

    val statsYear = MutableStateFlow(2026)
    val statsMonth = MutableStateFlow(2)
    val statsMode = MutableStateFlow(0)

    // 专门用来用到使用栏这一块的
    @OptIn(ExperimentalCoroutinesApi::class)
    val statsRecordsFlow: Flow<List<Record>> = combine(statsYear, statsMonth, statsMode) { y, m, mode ->
        if (mode == 0) getMonthRange(y, m) else getYearRange(y)
    }.flatMapLatest { (start, end) ->
        recordDao.selectRecordsByMonth(SpUtil.getUserId(application), start, end)
    }.distinctUntilChanged()

    // 独立的切换方法
    fun changeStatsDate(year: Int, month: Int, mode: Int) {
        statsMode.value = mode
        statsYear.value = year
        statsMonth.value = month
    }

    /**
     * 获取用户账单的时间范围信息
     * @param userId 用户ID
     * @return Pair<最早时间戳, 最晚时间戳>，如果没有数据返回 null
     */
    suspend fun getTimeRangeInfo(userId: Long): Pair<Long, Long>? {
        val minTimestamp = recordDao.getMinTimestamp(userId)
        val maxTimestamp = recordDao.getMaxTimestamp(userId)
        return if (minTimestamp != null && maxTimestamp != null) {
            Pair(minTimestamp, maxTimestamp)
        } else null
    }

    /**
     * 编辑完将账单保存到room数据库
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

    /**
     * 删除对应的账单
     */
    fun deleteRecord(recordId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recordDao.deleteRecord(recordId)
                billDeleteResult.postValue(Result.success("账单已删除"))
            } catch (e: Exception) {
                Log.e("删除账单出现错误",e.message.toString())
                billDeleteResult.postValue(Result.failure(Exception("账单无法删除")))
            }
        }
    }

}