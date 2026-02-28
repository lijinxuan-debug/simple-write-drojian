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
import com.example.accounting.utils.SpUtil
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
    private var selectedMonth = LocalDate.now().monthValue

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
        loadTimeRange {
            initTimePicker()
        }

        // 初始化图表的样式（关掉网格、右轴等）
        initLineChart()

        // 开始观察数据并刷新图表
        observeData()

        binding.tvTypeSelector.setOnClickListener {
            showTypePopup()
        }
    }

    /**
     * 获取当前的所有数据的起始结束时间
     */
    private fun loadTimeRange(onComplete: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val timeRange = viewModel.getTimeRangeInfo(SpUtil.getUserId(requireContext()))
            if (timeRange != null) {
                val minDate = Instant.ofEpochMilli(timeRange.first)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                val maxDate = Instant.ofEpochMilli(timeRange.second)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                dataStartYear = minDate.year
                dataStartMonth = minDate.monthValue
                dataEndYear = maxDate.year
                dataEndMonth = maxDate.monthValue
            }
            // 时间范围加载完成后执行回调
            onComplete()
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
            val selectedIndex = newList.indexOfFirst { it.year == selectedYear && it.month == selectedMonth }
            if (selectedIndex != -1) {
                binding.rvTimePicker.scrollToPosition(selectedIndex)
            }
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

            // 默认选中当前月份
            tabs.add(TimeTab(label, y, m, y == currentYear && m == currentMonth))
        }
        return tabs
    }

    /**
     * 初始化时间选择
     */
    private fun initTimePicker() {
        val timeAdapter = TimePickerAdapter { selectedTab ->
            selectedYear = selectedTab.year
            selectedMonth = selectedTab.month
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

                // 重置选中位置，避免索引冲突
                timeAdapter.resetSelectedPosition()

                val newList = if (currentPeriodMode == 0) getDynamicMonthList() else getDynamicYearList()
                timeAdapter.submitList(newList)

                val selectedIndex = newList.indexOfFirst { it.isSelected }
                if (selectedIndex != -1) {
                    binding.rvTimePicker.scrollToPosition(selectedIndex)
                }

                // 刷新图表
                updateUIBySelection()
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

            // 默认选中今年
            tabs.add(TimeTab(label, year, 1, year == currentYear))
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
                setDrawAxisLine(false)
                enableGridDashedLine(10f, 10f, 0f)
            }
            setExtraOffsets(0f, 120f, 0f, 30f)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statsRecordsFlow.collect { records ->
                // 1. 先把全量数据存起来
                allRecords = records

                // 2. 检查时间范围是否变化
                val minTimestamp = records.minOfOrNull { it.timestamp }
                val maxTimestamp = records.maxOfOrNull { it.timestamp }

                val timeRangeChanged = minTimestamp != dataStartTimestamp || maxTimestamp != dataEndTimestamp

                // 3. 更新时间范围标记
                dataStartTimestamp = minTimestamp
                dataEndTimestamp = maxTimestamp

                // 4. 如果时间范围变化了，重新加载时间选择器
                if (timeRangeChanged && records.isNotEmpty()) {
                    loadTimeRange {
                        // 时间选择器已更新，恢复当前选中状态
                        updateTimePickerSelection()
                    }
                }

                // 5. 更新图表
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

//    private fun updateLineChart(records: List<Record>) {
//        // 1. 这里的 records 已经是 updateUIBySelection 过滤后的单类型数据了
//        val allDates = records.map { it.dateStr }.distinct().sorted()
//
//        if (allDates.isEmpty()) {
//            binding.lineChart.clear()
//            binding.layoutEmptyChart.visibility = View.VISIBLE
//            binding.lineChart.visibility = View.GONE
//            return
//        }
//
//        // 有数据时显示图表
//        binding.layoutEmptyChart.visibility = View.GONE
//        binding.lineChart.visibility = View.VISIBLE
//
//        val entries = ArrayList<Entry>()
//        val dateLabels = ArrayList<String>()
//
//        var sum = 0f
//        allDates.forEachIndexed { index, date ->
//            // 2. 直接计算当前日期下的所有金额总和（因为类型已经过滤过了）
//            val daySum = records.filter { it.dateStr == date }
//                .sumOf { it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO }.toFloat()
//
//            sum+=daySum
//            entries.add(Entry(index.toFloat(), daySum))
//            dateLabels.add(date.split(" ")[0])
//        }
//
//        // 设置总金额
//        binding.tvTotalLabel.text = if (currentType == 0) "总支出：${sum}" else "总收入：${sum}"
//        // 先获取当前月份的天数
//        val lengthOfMonth = YearMonth.of(selectedYear, selectedMonth).lengthOfMonth()
//        // 设置平均值
//        binding.tvAverageLabel.text = if (currentPeriodMode == 0) {
//            "平均值：%.2f".format(sum.toDouble() / lengthOfMonth)
//        } else {
//            "总收入：%.2f".format(sum.toDouble() / 12)
//        }
//
//        // 3. 根据 currentType 设置不同的样式
//        val label = if (currentType == 0) "支出" else "收入"
//        val themeColor = if (currentType == 0) "#FF4081" else "#4CAF50"
//
//        val dataSet = LineDataSet(entries, label).apply {
//            color = Color.parseColor(themeColor)
//            setCircleColor(Color.parseColor(themeColor))
//            lineWidth = 2.5f
//            circleRadius = 4f
//            mode = LineDataSet.Mode.LINEAR
//        }
//
//        binding.lineChart.data = LineData(dataSet)
//
//        val marker = CustomMarkerView(
//            context = requireContext(),
//            dateLabels = allDates,     // 传入“密码本”
//            allRecords = allRecords,   // 传入“原始数据库记录”
//            currentType = currentType  // 传入当前是支出还是收入
//        )
//
//        // 之所以先将图表传给气泡是为了防止待会气泡溢出边界
//        marker.chartView = binding.lineChart
//        // 设置图表气泡内容
//        binding.lineChart.marker = marker
//
//        // X 轴配置保持不变
//        binding.lineChart.xAxis.apply {
//            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
//                override fun getFormattedValue(value: Float): String {
//                    return dateLabels.getOrNull(value.toInt()) ?: ""
//                }
//            }
//        }
//
//        binding.lineChart.animateX(800, Easing.EaseInOutQuart)
//    }

    private fun updateLineChart(records: List<Record>)
    {
        if (currentPeriodMode == 0) {
            updateLineChartByMonth(records)
        } else {
            updateLineChartByYear(records)
        }
    }

    private fun updateLineChartByMonth(records:
                                       List<Record>) {
        val allDates = records.map { it.dateStr
        }.distinct().sorted()

        if (allDates.isEmpty()) {
            binding.lineChart.clear()
            binding.layoutEmptyChart.visibility =
                View.VISIBLE
            binding.lineChart.visibility = View.GONE
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

        binding.tvTotalLabel.text = if (currentType ==
            0) "总支出：${sum}" else "总收入：${sum}"
        val lengthOfMonth = YearMonth.of(selectedYear,
            selectedMonth).lengthOfMonth()
        binding.tvAverageLabel.text =
            "平均值：%.2f".format(sum.toDouble() /
                    lengthOfMonth)

        updateChartData(entries, dateLabels, sum,
            allRecords)
    }

    private fun updateLineChartByYear(records:
                                      List<Record>) {
        val monthGroups = records.groupBy {
            val localDate =
                Instant.ofEpochMilli(it.timestamp)

                    .atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.monthValue
        }

        val allMonths = (1..12).toList()

        if (monthGroups.isEmpty()) {
            binding.lineChart.clear()
            binding.layoutEmptyChart.visibility =
                View.VISIBLE
            binding.lineChart.visibility = View.GONE
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
            entries.add(Entry(index.toFloat(),
                monthSum))
            dateLabels.add("${month}月")
        }

        binding.tvTotalLabel.text = if (currentType ==
            0) "总支出：${sum}" else "总收入：${sum}"
        binding.tvAverageLabel.text =
            "平均值：%.2f".format(sum.toDouble() / 12)

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