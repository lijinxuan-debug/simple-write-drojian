package com.example.accounting.ui.statistics

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.accounting.adapter.RankingAdapter
import com.example.accounting.adapter.RankingItem
import com.example.accounting.adapter.TimePickerAdapter
import com.example.accounting.data.model.Record
import com.example.accounting.data.model.TimeTab
import com.example.accounting.databinding.FragmentStatisticsBinding
import com.example.accounting.databinding.LayoutTypePopupBinding
import com.example.accounting.viewmodel.BillViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.time.LocalDate

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    // 引入你的 ViewModel
    private val viewModel: BillViewModel by viewModels()

    // 引入adapter
    private val rankingAdapter = RankingAdapter()

    // 0 代表支出，1 代表收入
    private var currentType = 0

    // 新增：暂存从 ViewModel 拿到的全量数据
    private var allRecords: List<Record> = emptyList()

    // 0 代表月，1 代表年
    private var currentPeriodMode = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initTimePicker()

        // 初始化图表的样式（关掉网格、右轴等）
        initLineChart()

        // 开始观察数据并刷新图表
        observeData()

        binding.tvTypeSelector.setOnClickListener {
            showTypePopup()
        }
    }

    private fun showTypePopup() {
        // 1. 使用 View Binding 加载布局
        // 假设你的布局文件名是 layout_type_popup.xml
        val popupBinding = LayoutTypePopupBinding.inflate(layoutInflater)

        // 2. 创建 PopupWindow
        val popupWindow = PopupWindow(
            popupBinding.root, // 使用 binding.root 获取根 View
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // 3. 直接通过 popupBinding 访问组件，不需要 findViewById
        popupBinding.apply {
            // 4. 初始化勾选显示
            ivExpenseCheck.visibility = if (currentType == 0) View.VISIBLE else View.GONE
            ivIncomeCheck.visibility = if (currentType == 1) View.VISIBLE else View.GONE

            // 5. 设置点击“支出”
            itemExpense.setOnClickListener {
                if (currentType != 0) {
                    currentType = 0
                    binding.tvTypeSelector.text = "支出 ▼"
                    updateUIBySelection()
                }
                popupWindow.dismiss()
            }

            // 6. 设置点击“收入”
            itemIncome.setOnClickListener {
                if (currentType != 1) {
                    currentType = 1
                    binding.tvTypeSelector.text = "收入 ▼"
                    updateUIBySelection()
                }
                popupWindow.dismiss()
            }
        }

        // 7. 背景变暗处理
        toggleBackgroundAlpha(0.7f)
        popupWindow.setOnDismissListener { toggleBackgroundAlpha(1.0f) }

        // 8. 弹出
        popupWindow.showAsDropDown(binding.tvTypeSelector)
    }

    private fun getDynamicMonthList(): List<TimeTab> {
        val tabs = mutableListOf<TimeTab>()
        val now = LocalDate.now()

        // 生成从 -6 到 +6 的月份
        for (i in -6..6) {
            val date = now.plusMonths(i.toLong())
            val y = date.year
            val m = date.monthValue

            // 逻辑处理显示文字
            val label = when {
                i == 0 -> "本月"
                i == -1 -> "上月"
                y != now.year -> "${y}-${m}月" // 跨年显示年份
                else -> "${String.format("%02d", m)}月" // 同年显示 03月 这种格式
            }

            // 默认选中 i == 0 (本月)
            tabs.add(TimeTab(label, y, m, i == 0))
        }
        return tabs
    }


    // 初始化 RecyclerView
    private fun initTimePicker() {
        val timeAdapter = TimePickerAdapter { selectedTab ->
            viewModel.changeStatsDate(selectedTab.year, selectedTab.month, currentPeriodMode)
        }

        // 绑定适配器
        binding.rvTimePicker.adapter = timeAdapter

        // 初始加载：显示月份列表并直接定位到“本月”
        val initialList = getDynamicMonthList()
        timeAdapter.submitList(initialList)

        TODO()

        binding.rvTimePicker.scrollToPosition(6)

        // 处理年/月按钮切换
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentPeriodMode = if (checkedId == binding.btnMonth.id) 0 else 1

                val newList = if (currentPeriodMode == 0) getDynamicMonthList() else getDynamicYearList()
                timeAdapter.submitList(newList)

                val selectedIndex = newList.indexOfFirst { it.isSelected }
                if (selectedIndex != -1) {
                    binding.rvTimePicker.scrollToPosition(selectedIndex)
                }
            }
        }
    }

    private fun getDynamicYearList(): List<TimeTab> {
        val tabs = mutableListOf<TimeTab>()
        val now = LocalDate.now()
        val currentYear = now.year

        // 生成前后 2 年，共 5 年的数据（比如 2024, 2025, 2026, 2027, 2028）
        for (i in -2..2) {
            val targetYear = currentYear + i

            // 逻辑处理显示文字
            val label = when (i) {
                0 -> "本年"
                -1 -> "去年"
                1 -> "明年"
                else -> "${targetYear}年"
            }

            // 默认选中 i == 0 (本年)
            // 这里的 month 传 1 即可，因为 getYearRange 只用到 year 字段
            tabs.add(TimeTab(label, targetYear, 1, i == 0))
        }
        return tabs
    }

    private fun updateUIBySelection() {
        // 统一过滤当前需要的数据（是支出还是收入）
        val filtered = allRecords.filter { it.type == currentType }

        // 更新图表
        updateLineChart(filtered)

        // 更新排行榜
        updateRankingList(filtered)
    }

    private fun toggleBackgroundAlpha(alpha: Float) {
        // 获取当前 Activity 的窗口属性
        val lp = requireActivity().window.attributes
        // 设置透明度（比如 0.7f 就是让背景变暗 30%）
        lp.alpha = alpha
        // 将修改后的属性应用回窗口
        requireActivity().window.attributes = lp
    }

    private fun initLineChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            // 这里允许触摸响应主要是为了后续的气泡显示
            setTouchEnabled(true)
            setScaleEnabled(false) // 禁止缩放，防止和滑动冲突

            // 下面主要是为了防止折线图变大变小
            setScaleEnabled(false)       // 禁用双向缩放
            isScaleXEnabled = false      // 禁用 X 轴缩放
            isScaleYEnabled = false      // 禁用 Y 轴缩放
            setPinchZoom(false)          // 禁用 X、Y 轴同时缩放
            isDoubleTapToZoomEnabled = false // 【重点】禁用双击缩放

            isDragEnabled = false        // 禁用拖拽平移

            // X轴的相关配置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                labelRotationAngle = -45f

                granularity = 1f // 确保每个标签都显示
                textSize = 10f   // 根据需要微调字体大小
                textColor = Color.parseColor("#999999")
            }

            axisRight.isEnabled = false // 隐藏右轴
            // 调整左边的那根竖轴线
            axisLeft.apply {
                setDrawAxisLine(false)
                enableGridDashedLine(10f, 10f, 0f)
            }
            // 为日期文字腾出位置空间
            setExtraOffsets(0f,0f,0f,30f)
        }
    }

    private fun updateRankingList(records: List<Record>) {
        // 1. 筛选出“支出”类型的账单 (假设 type == 0 是支出)
        val expenseRecords = records.filter { it.type == 0 }

        // 2. 计算支出的总金额（用于计算每个分类占的百分比）
        val totalAmount = expenseRecords.sumOf {
            it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        }

        // 3. 按“分类名称”分组，并计算每个分类的总和
        val rankingItems = expenseRecords.groupBy { it.categoryName }
            .map { (categoryName, list) ->
                // 计算该分类的总支出
                val categorySum = list.sumOf {
                    it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                }

                // 计算百分比 (分类额 / 总额 * 100)
                val percent = if (totalAmount > java.math.BigDecimal.ZERO) {
                    categorySum.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP).toFloat() * 100
                } else 0f

                // 创建适配器需要的 RankingItem 对象
                RankingItem(
                    icon = list.first().categoryIcon, // 拿到该分类的图标名
                    name = categoryName,
                    amount = categorySum.toString(),
                    percent = percent
                )
            }
            .sortedByDescending { it.amount.toBigDecimal() } // 按金额从大到小排序

        // 4. 将计算好的列表交给适配器
        rankingAdapter.submitList(rankingItems)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statsRecordsFlow.collect { records ->
                // 1. 先把全量数据存起来
                allRecords = records

                // 2. 只有在有数据的时候才去更新 UI
                if (allRecords.isNotEmpty()) {
                    updateUIBySelection()
                } else {
                    binding.lineChart.clear()
                    rankingAdapter.submitList(emptyList())
                }
            }
        }
    }

    private fun updateLineChart(records: List<Record>) {
        // 1. 这里的 records 已经是 updateUIBySelection 过滤后的单类型数据了
        val allDates = records.map { it.dateStr }.distinct().sorted()
        val entries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()

        var sum = 0f
        allDates.forEachIndexed { index, date ->
            // 2. 直接计算当前日期下的所有金额总和（因为类型已经过滤过了）
            val daySum = records.filter { it.dateStr == date }
                .sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }.toFloat()

            sum+=daySum
            entries.add(Entry(index.toFloat(), daySum))
            dateLabels.add(date.split(" ")[0])
        }

        // 设置总金额
        binding.tvTotalLabel.text = sum.toString()
        // 设置平均值


        if (allDates.isEmpty()) {
            binding.lineChart.clear()
            return
        }

        // 3. 根据 currentType 设置不同的样式
        val label = if (currentType == 0) "支出" else "收入"
        val themeColor = if (currentType == 0) "#FF4081" else "#4CAF50"

        val dataSet = LineDataSet(entries, label).apply {
            color = Color.parseColor(themeColor)
            setCircleColor(Color.parseColor(themeColor))
            lineWidth = 2.5f
            circleRadius = 4f
            mode = LineDataSet.Mode.LINEAR
        }

        binding.lineChart.data = LineData(dataSet)

        val marker = CustomMarkerView(
            context = requireContext(),
            dateLabels = allDates,     // 传入“密码本”
            allRecords = allRecords,   // 传入“原始数据库记录”
            currentType = currentType  // 传入当前是支出还是收入
        )

        // 3. 将其关联到图表实例
        marker.chartView = binding.lineChart
        binding.lineChart.marker = marker

        // 4. X 轴配置保持不变
        binding.lineChart.xAxis.apply {
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateLabels.getOrNull(value.toInt()) ?: ""
                }
            }
        }

        binding.lineChart.animateX(800, Easing.EaseInOutQuart)
    }

    // 记得销毁 Binding 防止内存泄露
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}