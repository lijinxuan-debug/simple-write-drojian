package com.example.accounting.ui

import android.annotation.SuppressLint
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.distinctUntilChanged
import com.example.accounting.R
import com.example.accounting.adapter.RecordPagerAdapter
import com.example.accounting.data.model.Record
import com.example.accounting.databinding.ActivityManuallyBinding
import com.example.accounting.databinding.LayoutDialogDeleteBinding
import com.example.accounting.engine.CalculatorEngine
import com.example.accounting.utils.CalcUtil
import com.example.accounting.utils.FileUtil.copyImagesToPrivateStorage
import com.example.accounting.utils.SpUtil
import com.example.accounting.viewmodel.BillViewModel
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class ManuallyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManuallyBinding

    private val viewModel: BillViewModel by viewModels()

    // 详情或存储用（完整日期）
    private val groupDateFormatter = DateTimeFormatter.ofPattern("MM月dd日 E", Locale.getDefault())

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityManuallyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.billDeleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { e ->
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 这里需要先判断是修改还是新增
        val json = intent.getStringExtra("recordData")
        val currentRecord = Gson().fromJson(json, Record::class.java)

        // 初始化并监听所有计算按钮
        initCalculatorButton()

        // 这里的适配器指的是固定的数据
        val adapter = RecordPagerAdapter(this)
        binding.mainViewPager.adapter = adapter

        // 如果不为空那就说明为编辑
        if (currentRecord != null) {
            switchEdit(currentRecord)
        }

        TabLayoutMediator(binding.topTabLayout, binding.mainViewPager) { tab, position ->
            tab.text =
                if (position == 0) getString(R.string.expenditure) else getString(R.string.revenue)
        }.attach()

        // 监听返回按钮
        binding.ivBack.setOnClickListener { finish() }
        binding.ivTextBack.setOnClickListener { finish() }

        // 监听保存按钮
        binding.tvSave.setOnClickListener {
            saveRecordToDatabase()
        }

        binding.btnEditDone.setOnClickListener {
            saveRecordToDatabase()
        }

        // 监听删除按钮
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }

    /**
     * 显示删除确认弹窗
     */
    private fun showDeleteConfirmDialog() {
        // 1. 拿到自定义弹窗的 Binding 实例
        val dialogBinding = LayoutDialogDeleteBinding.inflate(layoutInflater)

        // 2. 构建并创建 Dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        // 3. 去掉 Dialog 默认的背景（处理圆角白边）
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 4. 设置点击事件
        with(dialogBinding) {
            tvCancel.setOnClickListener {
                dialog.dismiss()
            }

            tvConfirmDelete.setOnClickListener {
                // 执行删除逻辑，传入 ID
                viewModel.deleteRecord(viewModel.id)
                dialog.dismiss()
                // 注意：这里不要写 finish()，我们要等观察者回调
            }
        }

        // 5. 显示并设置变暗
        dialog.show()
        dialog.window?.setDimAmount(0.5f)
    }

    private fun switchEdit(record: Record) {
        viewModel.updateAmount(record.amount)
        // 更换到支出、收入页面
        binding.mainViewPager.currentItem = record.type
        // 先将头部更换账单给隐藏掉
        binding.topTabLayout.visibility = View.GONE
        // 同时将当前的账单id传递过去
        viewModel.id = record.id
        // 底部的完成和删除按钮也要显现
        binding.llEditActions.visibility = View.VISIBLE
        if (record.type == 0) {
            viewModel.updateExpense(record)
        } else {
            viewModel.updateIncome(record)
        }
    }

    private fun saveRecordToDatabase() {
        val currentPage = binding.mainViewPager.currentItem // 0:支出, 1:收入

        // 1. 获取金额（处理 0.00 的情况）
        val amountStr = viewModel.amount.value ?: "0.00"
        // 获取金额之后还需要消除逗号
        val amountVal = amountStr.replace(",", "")

        // 2. 根据当前页面映射对应的属性
        val (rawDate, rawCategory, rawAccount, rawRemark, rawImages) = if (currentPage == 0) {
            // 支出
            FiveTuple(
                viewModel.expenseDate.value ?: System.currentTimeMillis(),
                viewModel.expenseCategoryItem.value!!,
                viewModel.expenseAccount.value!!,
                viewModel.expenseRemark.value ?: "",
                viewModel.expenseImage.value ?: emptyList()
            )
        } else {
            // 收入
            FiveTuple(
                viewModel.incomeDate.value ?: System.currentTimeMillis(),
                viewModel.incomeCategoryItem.value!!,
                viewModel.incomeAccount.value!!,
                viewModel.incomeRemark.value ?: "",
                viewModel.incomeImage.value ?: emptyList()
            )
        }

        // 3. 将图片复制到私有目录（关键：解决 I/O 错误）
        val privateImagePaths = copyImagesToPrivateStorage(this, rawImages)

        // 4. 处理日期格式化（使用你之前定义的 fullDateFormatter）
        val instant = Instant.ofEpochMilli(rawDate)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

        val dateString = localDateTime.format(groupDateFormatter)

        val timeString = localDateTime.format(timeFormatter)

        // 获取分类图标昵称
        val categoryIcon = this.resources.getResourceEntryName(rawCategory.iconRes)

        // 5. 构建 Record 对象
        val record = Record(
            id = viewModel.id,
            amount = amountVal,
            type = currentPage, // 0或1
            userId = SpUtil.getUserId(this),
            categoryIcon = categoryIcon,
            categoryId = rawCategory.id,
            categoryName = rawCategory.name,
            categoryGroupName = rawCategory.groupName,
            accountId = rawAccount.id,
            paymentMethod = rawAccount.name,
            timestamp = rawDate,
            dateStr = dateString,
            timeStr = timeString,
            remark = rawRemark,
            images = privateImagePaths // 存入私有目录路径列表
        )
//        Log.e(
//            "RecordDetail", """
//    |--- 账单详情 ---
//    |金额: ${record.amount}
//    |类型: ${if (record.type == 0) "支出" else "收入"}
//    |图标Id: ${record.categoryIcon}
//    |分类Id: ${record.categoryId}
//    |分类名称: ${record.categoryName}
//    |分类分组: ${record.categoryGroupName}
//    |账户Id: ${record.accountId}
//    |支付方式: ${record.paymentMethod}
//    |时间戳: ${record.timestamp}
//    |日期字符串: ${record.dateStr}
//    |备注: ${record.remark}
//    |图片路径: ${record.images.joinToString(", ")}
//    |----------------
//""".trimMargin()
//        )

        // 6. 调用 ViewModel 的插入方法
        viewModel.insertRecord(record)

        // 同时将id进行清零
        viewModel.id = 0L

        // 7. 保存成功后关闭页面且跳转到主页
        finish()
    }

    // 辅助类用于解构赋值
    data class FiveTuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    /**
     * 初始化所有计算按钮
     */
    private fun initCalculatorButton() {
        // 1. 点击下拉箭头收起键盘
        binding.btnHideKeyboard.setOnClickListener {
            calculateFinalResult()
        }

        // 定义基础数字和运算符按键映射
        val commonButtons = listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9, binding.buttonPoint,
            binding.buttonPlus, binding.buttonMinus
        )

        // 监听键盘状态
        viewModel.isKeyboardVisible.distinctUntilChanged().observe(this) { isVisible ->
            if (isVisible) {
                showKeyboardWithAnim()
            } else {
                hideKeyboardWithAnim()
            }
        }

        // 批量设置点击监听
        commonButtons.forEach { btn ->
            btn.setOnClickListener {
                appendToCalcDisplay(btn.text.toString())
            }
        }

        // 2. 清除键 (C)
        binding.buttonClear.setOnClickListener {
            viewModel.updateCalculationExpression("") // 清空过程
            viewModel.updateAmount(getString(R.string.init_amount))  // 重置金额
            viewModel.updateUnitHintVisible(false) // 清除单位
        }

        // 3. 退格键 (删除图标)
        binding.backspace.setOnClickListener {
            val current = viewModel.calculationExpression.value ?: ""
            if (current.isNotEmpty()) {
                viewModel.updateCalculationExpression(current.dropLast(1))
                tryLivePreview()
            }
        }

        // 4. 完成键 (计算最终结果并清空过程)
        binding.btnDone.setOnClickListener {
            calculateFinalResult()
        }

    }

    // 收起键盘的动画函数
    private fun hideKeyboardWithAnim() {
        binding.llKeyboardWrapper.animate()
            .translationY(binding.llKeyboardWrapper.height.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.llKeyboardWrapper.visibility = View.GONE
            }
            .start()
    }

    // 弹出键盘的动画函数
    private fun showKeyboardWithAnim() {
        binding.llKeyboardWrapper.visibility = View.VISIBLE
        binding.llKeyboardWrapper.translationY = binding.llKeyboardWrapper.height.toFloat()
        binding.llKeyboardWrapper.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    @SuppressLint("SetTextI18n")
    private fun appendToCalcDisplay(str: String) {
        val current = viewModel.calculationExpression.value!!
        // 只保留加减正则
        val isOperatorRegex = "[+\\-]".toRegex()
        val lastPart = current.split(isOperatorRegex).last()

        if (str == "0" && (lastPart == "0" || lastPart == "")) {
            return
        }

        // --- 1. 预判未来结果是否超标 ---
        if (str.matches("[0-9.]".toRegex())) {
            val futureExpression = current + str
            try {
                val futureResult = CalculatorEngine.evaluate(futureExpression)
                if (abs(futureResult) >= 1000000000.0) return
            } catch (_: Exception) {
                // 异常通常是 10+ 这种不完整表达式，忽略即可
            }
        }

        // 小数点处理
        if (str == ".") {
            if (lastPart.contains(".")) return
            if (current.isEmpty() || current.last().toString().matches(isOperatorRegex)) {
                viewModel.updateCalculationExpression(current + "0.")
                tryLivePreview() // 补0后也要预览
                return
            }
        }

        // 小数位限制：小数点后只能有两位
        if (lastPart.contains(".") && str.matches("[0-9]".toRegex())) {
            val decimalPart = lastPart.substringAfter(".")
            if (decimalPart.length >= 2) return
        }

        // 运算符处理 (只处理 + -)
        if (str.matches(isOperatorRegex)) {
            if (current.isEmpty()) return
            // 重复点击切换运算符
            if (current.last().toString().matches(isOperatorRegex)) {
                viewModel.updateCalculationExpression(current.dropLast(1) + str)
                return
            }
        }

        viewModel.updateCalculationExpression(current + str)
        // 预算结果
        tryLivePreview()
    }

    /**
     * 实时预览结果：用户每输入一个数字，上方大金额实时变动
     */
    private fun tryLivePreview() {
        val expression = viewModel.calculationExpression.value ?: ""

        if (expression.isEmpty()) {
            viewModel.updateAmount(getString(R.string.init_amount))
            // 空的时候隐藏单位
            viewModel.updateUnitHintVisible(false)
            return
        }

        try {
            // 调用计算引擎
            val result = CalculatorEngine.evaluate(expression)
            // 使用格式化工具：千分位 + 两位小数
            viewModel.updateAmount(CalcUtil.formatAmount(result))

            // 这里是新增的单位显示逻辑
            val unit = CalcUtil.getUnitHint(result)
            if (unit.isNotEmpty()) {
                viewModel.updateUnitHint(unit)
                viewModel.updateUnitHintVisible(true)
            } else {
                viewModel.updateUnitHintVisible(false)
            }

        } catch (_: Exception) {
            // 解析中（如末尾是运算符时）忽略异常
        }
    }

    /**
     * 点击完成：计算结果并清空下方过程
     */
    private fun calculateFinalResult() {
        // 消失动画
        viewModel.updateKeyboardVisible(false)

        val expression = viewModel.calculationExpression.value ?: ""
        if (expression.isEmpty()) return

        try {
            val result = CalculatorEngine.evaluate(expression)
            if (result < 0) {
                // 触发提示，询问用户建议
                checkNegativeResult(result)
            } else {
                // 兜底
                val finalResult = if (result >= 1000000000.0) 999999999.99 else result
                viewModel.updateAmount(CalcUtil.formatAmount(finalResult))
                // 计算结束，输入框不能清除，避免后续用户还想计算，同时需要使用decimal格式化，有小数最多保留两位，没有则不保留
                viewModel.updateCalculationExpression(DecimalFormat("#.##").format(finalResult))
            }
        } catch (_: Exception) {
            viewModel.updateAmount(getString(R.string.init_amount))
        }
    }

    /**
     * 核心逻辑：检查结果并弹出转换建议
     */
    private fun checkNegativeResult(result: Double) {
        // 直接用 Fragment 自己的身份标识 pageType
        // 假设 0 是支出，1 是收入
        val currentTabIsExpense = viewModel.jumpToPage.value == 0

        // 支出出现负数
        if (currentTabIsExpense && result < 0) {
            showSwitchDialog(
                title = "发现一笔“反向支出”",
                message = "当前计算结果为负数，是否要将其转为 [收入] 记录？",
                targetTabPosition = 1, // 收入页
                absoluteValue = abs(result)
            )
        }
        // 收入出现负数
        else if (!currentTabIsExpense && result < 0) {
            showSwitchDialog(
                title = "这笔收入是负的？",
                message = "看起来这是一笔开销，需要直接切换到 [支出] 页面并记录吗？",
                targetTabPosition = 0, // 支出页
                absoluteValue = abs(result)
            )
        }
    }

    private fun showSwitchDialog(
        title: String,
        message: String,
        targetTabPosition: Int,
        absoluteValue: Double
    ) {
        // 1. 准备数据：一个是带逗号的展示值，一个是不带逗号的计算值
        val displayValue = CalcUtil.formatAmount(absoluteValue)
        val calculationValue = CalcUtil.formatForCalculation(absoluteValue)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确认转换") { _, _ ->

                // 【核心】通过 ViewModel 同步所有状态
                // 更新金额 LiveData，这样两边的 Fragment 都会显示正数
                viewModel.updateAmount(displayValue)

                // 更新计算过程，防止用户删一个字符又变回负数
                viewModel.calculationExpression.value = calculationValue

                // 【切换】发出指令让 Activity 拨动 ViewPager2
                viewModel.jumpToPage.value = targetTabPosition

                // 提示用户
                Toast.makeText(this, "已为您切换状态", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("不用了", null)
            .show()
    }
}