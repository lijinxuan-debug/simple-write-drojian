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
import com.example.accounting.adapter.TimePickerAdapter
import com.example.accounting.data.model.Record
import com.example.accounting.data.model.TimeTab
import com.example.accounting.databinding.FragmentStatisticsBinding
import com.example.accounting.databinding.LayoutTypePopupBinding
import com.example.accounting.utils.WanValueFormatter
import com.example.accounting.viewmodel.BillViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class StatisticsFragment : Fragment() {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    // 引入你的 ViewModel
    private val viewModel: BillViewModel by viewModels()

    // 0 代表支出，1 代表收入
    private var currentType = 0

    // 新增：暂存从 ViewModel 拿到的全量数据
    private var allRecords: List<Record> = emptyList()

    // 0 代表月，1 代表年
    private var currentPeriodMode = 0

    // 存储当前选中的年/月（用于恢复选中状态）
    private var selectedYear = LocalDate.now().year
    private var selectedMonth = 1

    private var selectedYearOfMonth = LocalDate.now().year
    private var selectedMonthOfMonth = LocalDate.now().monthValue

    // 存储时间范围信息
    private var dataStartYear: Int? = null
    private var dataStartMonth: Int? = null
    private var dataEndYear: Int? = null
    private var dataEndMonth: Int? = null

    // 存储时间戳用于检测时间范围变化
    private var dataStartTimestamp: Long? = null
    private var dataEndTimestamp: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 先获取时间范围信息，再初始化时间选择器
        initTimePicker()

        // 初始化图表的样式（关掉网格、右轴等）
        initLineChart()

        // 开始观察数据并刷新图表
        observeData()

        binding.tvTypeSelector.setOnClickListener {
            showTypePopup()
        }
    }

    /**
     * 更新时间选择器列表（保持当前选中状态）
     */
    private fun updateTimePickerSelection() {
        val timeAdapter = binding.rvTimePicker.adapter as? TimePickerAdapter ?: return

        val newList = if (currentPeriodMode == 0) getDynamicMonthList() else getDynamicYearList()
        timeAdapter.submitList(newList)

        // 滚动到当前选中项
        binding.rvTimePicker.post {
            val selectedIndex = newList.indexOfFirst { (it.year == selectedYear && it.month == selectedMonth) || (it.year == selectedYearOfMonth && it.month == selectedMonthOfMonth) }
            if (selectedIndex != -1) {
                binding.rvTimePicker.scrollToPosition(selectedIndex)
            }
        }
    }

    private fun showTypePopup() {
        // 使用 View Binding 加载布局
        val popupBinding = LayoutTypePopupBinding.inflate(layoutInflater)

        // 创建 PopupWindow
        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

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

        // 背景变暗处理
        toggleBackgroundAlpha(0.7f)
        // 上拉窗取消了则取消变暗，恢复原状
        popupWindow.setOnDismissListener { toggleBackgroundAlpha(1.0f) }

        // 依附对象弹出
        popupWindow.showAsDropDown(binding.headerLayout)
    }

    private fun getDynamicMonthList(): List<TimeTab> {
        val tabs = mutableListOf<TimeTab>()
        val now = LocalDate.now()
        val currentYear = now.year
        val currentMonth = now.monthValue

        // 如果没有数据，只显示本月
        if (dataStartYear == null || dataStartMonth == null || dataEndYear == null || dataEndMonth == null) {
            tabs.add(TimeTab("本月", currentYear, currentMonth, true))
            return tabs
        }

        val startYear = dataStartYear!!
        val startMonth = dataStartMonth!!
        val endYear = dataEndYear!!
        val endMonth = dataEndMonth!!

        // 计算总月数
        val totalMonths = (endYear - startYear) * 12 + (endMonth - startMonth)

        // 生成从开始月份到结束月份的列表
        for (i in 0..totalMonths) {
            val date = LocalDate.of(startYear, startMonth, 1).plusMonths(i.toLong())
            val y = date.year
            val m = date.monthValue

            // 逻辑处理显示文字
            val label = when {
                y == currentYear && m == currentMonth -> "本月"
                y == currentYear && m == currentMonth - 1 -> "上月"
                y != currentYear -> "${y}-${m}月" // 跨年显示年份
                else -> "${String.format("%02d", m)}月" // 同年显示 03月 这种格式
            }

            // 默认选中当前月份（如果已经有了则选择已经选中的月份）
            tabs.add(TimeTab(label, y, m, m == selectedMonthOfMonth && y == selectedYearOfMonth))
        }
        return tabs
    }

    /**
     * 初始化时间选择
     */
    private fun initTimePicker() {
        val timeAdapter = TimePickerAdapter { selectedTab ->
            if (currentPeriodMode == 0) {
                selectedYearOfMonth = selectedTab.year
                selectedMonthOfMonth = selectedTab.month
            } else {
                selectedYear = selectedTab.year
            }
            viewModel.changeStatsDate(selectedTab.year, selectedTab.month, currentPeriodMode)
        }

        binding.rvTimePicker.adapter = timeAdapter

        val initialList = getDynamicMonthList()

        // 这里的post是等待recyclerview渲染就绪
        binding.rvTimePicker.post {
            timeAdapter.submitList(initialList) {
                // 获得选中的图标
                val selectedIndex = initialList.indexOfFirst { it.isSelected }
                if (selectedIndex != -1) {
                    binding.rvTimePicker.scrollToPosition(selectedIndex)
                }
            }
        }

        // 处理年/月按钮切换
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                // 查看是当前选中的是月份还是年份
                currentPeriodMode = if (checkedId == binding.btnMonth.id) 0 else 1

                val newList = if (currentPeriodMode == 0) getDynamicMonthList() else getDynamicYearList()
                timeAdapter.submitList(newList)

                val selectedIndex = newList.indexOfFirst { it.isSelected }
                if (selectedIndex != -1) {
                    binding.rvTimePicker.scrollToPosition(selectedIndex)
                }

                // 根据当前模式，获取对应模式下应该查询的年、月
                val targetYear = if (currentPeriodMode == 0) selectedYearOfMonth else selectedYear
                val targetMonth = if (currentPeriodMode == 0) selectedMonthOfMonth else 1 // 年模式下传1即可（有ViewModel保险）

                // 通知 ViewModel 去查数据库
                viewModel.changeStatsDate(targetYear, targetMonth, currentPeriodMode)
            }
        }
    }

    private fun getDynamicYearList(): List<TimeTab> {
        val tabs = mutableListOf<TimeTab>()
        val now = LocalDate.now()
        val currentYear = now.year

        // 如果没有数据，只显示本年
        if (dataStartYear == null || dataEndYear == null) {
            tabs.add(TimeTab("本年", currentYear, 1, true))
            return tabs
        }

        val startYear = dataStartYear!!
        val endYear = dataEndYear!!

        // 生成从开始年份到结束年份的列表
        for (year in startYear..endYear) {
            // 逻辑处理显示文字
            val label = when {
                year == currentYear -> "今年"
                year == currentYear - 1 -> "去年"
                year == currentYear + 1 -> "明年"
                else -> "${year}年"
            }

            // 默认选中今年（已经选择了则对等于selectedYear）
            tabs.add(TimeTab(label, year, 1, year == selectedYear))
        }
        return tabs
    }

    private fun updateUIBySelection() {
        // 统一过滤当前需要的数据（是支出还是收入）
        val filtered = allRecords.filter { it.type == currentType }

        // 更新图表
        updateLineChart(filtered)
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
            setTouchEnabled(true)
            setScaleEnabled(false)
            isScaleXEnabled = false
            isScaleYEnabled = false
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false

            isDragEnabled = true
            isHighlightPerDragEnabled = true // 启用拖拽时自动高亮

            // 气泡隐藏任务
            val hideMarkerRunnable = Runnable {
                highlightValue(null)
            }

            // 添加手势监听器
            setOnChartGestureListener(object : com.github.mikephil.charting.listener.OnChartGestureListener {

                override fun onChartGestureStart(
                    me: android.view.MotionEvent?,
                    lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?
                ) {
                    // 触摸开始，取消隐藏任务
                    post { removeCallbacks(hideMarkerRunnable) }
                }

                override fun onChartGestureEnd(
                    me: android.view.MotionEvent?,
                    lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?
                ) {
                    // 触摸结束，2秒后隐藏气泡
                    postDelayed(hideMarkerRunnable, 2000L)
                }

                override fun onChartLongPressed(me: android.view.MotionEvent?) {
                    // 长按时，取消隐藏任务
                    post { removeCallbacks(hideMarkerRunnable) }
                }

                override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}

                override fun onChartSingleTapped(me: android.view.MotionEvent?) {}

                override fun onChartFling(
                    me1: android.view.MotionEvent?,
                    me2: android.view.MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ) {
                }

                override fun onChartScale(me: android.view.MotionEvent?, scaleX: Float, scaleY: Float) {}

                override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {
                    // 滑动时，实时高亮最近的数据点
                    me?.let {
                        val highlight = getHighlightByTouchPoint(it.x, it.y)
                        if (highlight != null) {
                            highlightValue(highlight)
                        }
                    }
                }
            })

            // X轴的相关配置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                labelRotationAngle = -45f
                granularity = 1f
                textSize = 10f
                textColor = Color.parseColor("#999999")
            }

            axisRight.isEnabled = false
            axisLeft.apply {
                valueFormatter = WanValueFormatter()
                setDrawAxisLine(false)
                enableGridDashedLine(10f, 10f, 0f)
                // 强制地基为0
                axisMinimum = 0f
            }
            // 图表偏移量
            setExtraOffsets(0f, 120f, 0f, 30f)
        }
    }

    private fun observeData() {
        // 任务 A：观察支出时间范围的变化（更新顶部的滚轮/选择器）
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timeRangeFlow.collect { range ->
                if (range != null) {
                    // 1. 解析时间
                    val minDate = Instant.ofEpochMilli(range.first)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val maxDate = Instant.ofEpochMilli(range.second)
                        .atZone(ZoneId.systemDefault()).toLocalDate()

                    // 2. 更新范围变量
                    dataStartYear = minDate.year
                    dataStartMonth = minDate.monthValue
                    dataEndYear = maxDate.year
                    dataEndMonth = maxDate.monthValue

                    // 3. 刷新时间选择器（比如数据从2024增加到了2025，选择器要多出一项）
                    updateTimePickerSelection()
                }
            }
        }

        // 任务 B：观察账单记录的变化（更新中间的图表）
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statsRecordsFlow.collect { records ->
                allRecords = records
                if (allRecords.isNotEmpty()) {
                    updateUIBySelection()
                } else {
                    binding.lineChart.clear()
                    binding.layoutEmptyChart.visibility = View.VISIBLE
                    binding.lineChart.visibility = View.GONE
                }
            }
        }
    }

    private fun updateLineChart(records: List<Record>)
    {
        if (currentPeriodMode == 0) {
            updateLineChartByMonth(records)
        } else {
            updateLineChartByYear(records)
        }
    }

    private fun updateLineChartByMonth(records: List<Record>) {
        // 日期升序排列
        val allDates = records.map { it.dateStr
        }.distinct().sorted()

        if (allDates.isEmpty()) {
            binding.lineChart.clear()
            binding.layoutEmptyChart.visibility =
                View.VISIBLE
            binding.lineChart.visibility = View.GONE
            // 同时总支出和总收入也需要归零
            binding.tvTotalLabel.text = if (currentType ==
                0) "总支出：0.00" else "总收入：0.00"
            binding.tvAverageLabel.text = "日平均值：0.00"
            return
        }

        binding.layoutEmptyChart.visibility = View.GONE
        binding.lineChart.visibility = View.VISIBLE

        val entries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()
        var sum = 0f

        allDates.forEachIndexed { index, date ->
            val daySum = records.filter { it.dateStr ==
                    date }
                .sumOf { it.amount.toBigDecimalOrNull()
                    ?: java.math.BigDecimal.ZERO }.toFloat()
            sum += daySum
            entries.add(Entry(index.toFloat(), daySum))
            dateLabels.add(date.split(" ")[0])
        }

        val formatted = "%.2f".format(sum)

        binding.tvTotalLabel.text = if (currentType ==
            0) "总支出：${formatted}" else "总收入：${formatted}"

        val lengthOfMonth = YearMonth.of(selectedYearOfMonth,
            selectedMonthOfMonth).lengthOfMonth()
        binding.tvAverageLabel.text =
            "日平均值：%.2f".format(sum.toDouble() /
                    lengthOfMonth)

        updateChartData(entries, dateLabels, sum,
            allRecords)
    }

    private fun updateLineChartByYear(records: List<Record>) {
        // 按照月份先进行分组，后续计算需要
        val monthGroups = records.groupBy {
            val localDate =
                Instant.ofEpochMilli(it.timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.monthValue
        }

        val allMonths = (1..12).toList()

        if (monthGroups.isEmpty()) {
            binding.lineChart.clear()
            binding.layoutEmptyChart.visibility = View.VISIBLE
            binding.lineChart.visibility = View.GONE
            binding.tvTotalLabel.text = if (currentType == 0) "总支出：0.00" else "总收入：0.00"
            binding.tvAverageLabel.text = "月平均值：0.00"
            return
        }

        binding.layoutEmptyChart.visibility = View.GONE
        binding.lineChart.visibility = View.VISIBLE

        val entries = ArrayList<Entry>()
        val dateLabels = ArrayList<String>()
        var sum = 0f

        allMonths.forEachIndexed { index, month ->
            val monthSum = monthGroups[month]?.sumOf {
                it.amount.toBigDecimalOrNull() ?:
                java.math.BigDecimal.ZERO
            }?.toFloat() ?: 0f
            sum += monthSum
            // 这是因为linechart不认识“1月”只认识索引。
            entries.add(Entry(index.toFloat(),
                monthSum))
            dateLabels.add("${month}月")
        }

        val formatted = "%.2f".format(sum)

        binding.tvTotalLabel.text = if (currentType ==
            0) "总支出：${formatted}" else "总收入：${formatted}"

        binding.tvAverageLabel.text =
            "月平均值：%.2f".format(sum.toDouble() / 12)

        updateChartData(entries, dateLabels, sum,
            records)
    }

    private fun updateChartData(
        entries: ArrayList<Entry>,
        dateLabels: ArrayList<String>,
        sum: Float,
        records: List<Record>
    ) {
        val label = if (currentType == 0) "支出" else
            "收入"
        val themeColor = if (currentType == 0)
            "#FF4081" else "#4CAF50"

        val dataSet = LineDataSet(entries, label).apply{
            color = Color.parseColor(themeColor)

            setCircleColor(Color.parseColor(themeColor))
            lineWidth = 2.5f
            circleRadius = 4f
            mode = LineDataSet.Mode.LINEAR
        }

        binding.lineChart.data = LineData(dataSet)

        val marker = CustomMarkerView(
            context = requireContext(),
            dateLabels = dateLabels,
            allRecords = records,
            currentType = currentType
        )

        marker.chartView = binding.lineChart
        binding.lineChart.marker = marker

        binding.lineChart.xAxis.apply {
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dateLabels.getOrNull(value.toInt()) ?: ""
                }
            }
        }

        binding.lineChart.animateX(800,
            Easing.EaseInOutQuart)
    }

    // 记得销毁 Binding 防止内存泄露
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}