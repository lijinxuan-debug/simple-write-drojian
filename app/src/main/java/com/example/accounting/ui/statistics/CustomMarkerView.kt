package com.example.accounting.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.view.LayoutInflater
import com.example.accounting.R
import com.example.accounting.data.model.Record
import com.example.accounting.databinding.ItemMarkerDetailBinding
import com.example.accounting.databinding.LayoutChartMarkerBinding
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView(
    context: Context,
    private val dateLabels: List<String>, // 这里的 labels 就是你的 allDates 完整日期列表
    private val allRecords: List<Record>, // 原始数据库记录
    private val currentType: Int          // 0 或 1
) : MarkerView(context, R.layout.layout_chart_marker) {

    private val binding = LayoutChartMarkerBinding.bind(this)
    /**
     * 每次点击圆点时，此方法都会被触发。
     */
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        binding.tvMarkerTitle.text = "只显示最大3笔交易"

        val date = dateLabels.getOrNull(e.x.toInt()) ?: ""

        val top3Records = allRecords.filter { it ->
            // 匹配日期 且 匹配当前显示的类型（支出或收入）
            it.dateStr.contains(date) && it.type == currentType
        }
            .sortedByDescending { it ->
                // 按金额转成 BigDecimal 排序（防止 String 排序导致 "10" 小于 "2" 的问题）
                it.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            }
            .take(3) // 只取前三条

        // 先清空气泡，避免发生叠加
        binding.llDetailContainer.removeAllViews()

        top3Records.forEach { record ->
            val itemBinding = ItemMarkerDetailBinding.inflate(LayoutInflater.from(context),
                binding.llDetailContainer,
                false)

            itemBinding.apply {
                // 利用类加载器去获得资源图标
                val resId = root.context.resources.getIdentifier(record.categoryIcon, "drawable", root.context.packageName)
                ivItemIcon.setImageResource(resId)
                tvItemAmount.text = record.amount
                tvItemRemark.text = record.remark
                tvItemDate.text = record.dateStr.split(" ")[0]
            }

            // 将指定的View添加到主容器里面
            binding.llDetailContainer.addView(itemBinding.root)

        }

        val sum = e.y
        if (currentType == 0) {
            binding.tvMarkerTotal.text = "当日总支出：${sum}"
        } else {
            binding.tvMarkerTotal.text = "当日总收入：${sum}"
        }

        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val mWidth = measuredWidth.toFloat()
        val cWidth = chartView?.width?.toFloat() ?: 0f
        val tx = highlight?.xPx ?: 0f

        val idealLeft = tx - mWidth / 2f
        var correctionOffset = 0f

        if (idealLeft < 0) {
            correctionOffset = -idealLeft // 靠左超出了，向右修正
        } else if (idealLeft + mWidth > cWidth) {
            correctionOffset = cWidth - (idealLeft + mWidth) // 靠右超出了，向左修正
        }

        binding.vMarkerArrow.translationX = -correctionOffset

        super.refreshContent(e, highlight)
    }

    /**
     * 关键方法：控制气泡相对于圆点的偏移位置。
     * 如果不重写，气泡左上角会对着圆点，挡住视线。
     */
    override fun getOffset(): MPPointF {
        // 返回一个 MPPointF 对象：
        // 第一个参数：-(width / 2f) 使气泡在水平方向居中
        // 第二个参数：-height 使气泡完全显示在圆点上方
        return MPPointF((-(width / 2f)), (-height).toFloat())
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val mOffset = MPPointF()

        // 1. 获取气泡最新的测量尺寸
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val mWidth = measuredWidth.toFloat()
        val mHeight = measuredHeight.toFloat()

        // --- X轴 避障计算 (左右) ---
        // 默认居中：圆点坐标 - 一半宽度
        var xOffset = -mWidth / 2f

        if (posX + xOffset < 0) {
            // 碰到左边界：强制让气泡左边对齐屏幕(0)，偏移量就是 -posX
            xOffset = -posX
        } else if (posX + mWidth / 2f > chartView.width) {
            // 碰到右边界：强制让气泡右边对齐屏幕，偏移量就是 宽度差
            xOffset = chartView.width - posX - mWidth
        }
        mOffset.x = xOffset

        // --- Y轴 避障计算 (上下) ---
        // 默认在上方：向上移动一整个高度 + 一点间隙
        val spacing = 20f // 气泡距离圆点的间隙
        var yOffset = -mHeight - spacing

        // 判断上方空间是否足够 (posY 是圆点距离顶部的距离)
        if (posY + yOffset < 0) {
            // 上方空间不足，气泡翻转到圆点下方
            yOffset = spacing + 10f // 10f 是考虑到圆点本身的大小，往下挪一点
        }
        mOffset.y = yOffset

        // --- 记录这个偏移，供箭头旋转/平移使用 ---
        // 计算气泡框被“挤压”了多少，让箭头去补偿
        // 正常的居中偏移应该是 -mWidth/2，现在的偏移是 xOffset
        val arrowCorrection = xOffset - (-mWidth / 2f)
        binding.vMarkerArrow.translationX = -arrowCorrection

        // 如果气泡翻转到了下方，箭头也得反转 (可选)
        if (yOffset > 0) {
            binding.vMarkerArrow.rotation = 180f

            val arrowSize = 12f * context.resources.displayMetrics.density
            binding.vMarkerArrow.translationY = -mHeight - arrowSize
        } else {
            binding.vMarkerArrow.rotation = 0f
            binding.vMarkerArrow.translationY = 0f
        }

        return mOffset
    }
}