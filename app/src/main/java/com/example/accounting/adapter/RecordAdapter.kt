package com.example.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.R
import com.example.accounting.data.model.Record
import com.example.accounting.databinding.ItemBillRecordBinding
import com.example.accounting.engine.GlideEngine
import com.google.android.material.imageview.ShapeableImageView
import java.text.DecimalFormat

class RecordAdapter : ListAdapter<Record, RecordAdapter.RecordViewHolder>(RecordDiffCallback) {

    class RecordViewHolder(val binding: ItemBillRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemBillRecordBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return RecordViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = getItem(position)
        holder.binding.apply {
            val iconName = record.categoryIcon
            val context = root.context

            // 日期分组
            if (position == 0) {
                // 第一条数据，必须显示日期
                layoutDateHeader.visibility = View.VISIBLE
                tvDateTitle.text = record.dateStr
            } else {
                // 获取上一条数据
                val prevRecord = getItem(position - 1)
                if (record.dateStr == prevRecord.dateStr) {
                    // 如果日期相同，隐藏日期头
                    layoutDateHeader.visibility = View.GONE
                } else {
                    // 日期不同，显示日期头
                    layoutDateHeader.visibility = View.VISIBLE
                    tvDateTitle.text = record.dateStr
                }
            }

            // 根据名字查当前版本的资源 ID
            val resId = root.context.resources.getIdentifier(
                iconName,
                "drawable",
                root.context.packageName
            )

            // 设置图标图片
            ivCategoryIcon.setImageResource(resId)
            // 设置分类名称
            tvCategoryName.text = record.categoryName
            // 设置时间显示
            tvDateTitle.text = record.dateStr
            // 显示账单类别和精确时间
            tvAccountInfo.text = "${record.paymentMethod} · ${record.timeStr}"
            // 显示图片
            layoutImages.removeAllViews()

            if (record.images.isNotEmpty()) {
                layoutImages.visibility = View.VISIBLE
                record.images.forEach { path ->
                    val iv = ShapeableImageView(context).apply {
                        // 【核心修复】必须设置 LayoutParams，否则宽高默认为 0
                        val size = (80 * resources.displayMetrics.density).toInt() // 80dp 转 px
                        layoutParams = LinearLayout.LayoutParams(size, size).apply {
                            marginEnd = (4 * resources.displayMetrics.density).toInt() // 4dp 间距
                        }

                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    // 3. 【核心修复】必须添加到容器中，否则界面上看不见
                    layoutImages.addView(iv)

                    GlideEngine.createGlideEngine().loadImage(context,path,iv)
                }
            } else {
                layoutImages.visibility = View.GONE
            }

            // 设置金额显示，这里必须要区分是收入还是支出
            val isExpense = record.type == 0
            val prefix = if (isExpense) "-" else "+"

            val amountValue = record.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
            val formatter = DecimalFormat("#,##0.00")
            val thousandSeparatorAmount = formatter.format(amountValue)

            tvAmount.text = "$prefix${thousandSeparatorAmount}"
            // 同时还要设置金额的颜色
            val colorRes = if (isExpense) {
                R.color.revenue_green
            } else {
                R.color.expenditure_red
            }
            tvAmount.setTextColor(ContextCompat.getColor(context, colorRes))
            // 设置备注
            tvRemark.text = record.remark
        }
    }

    // 定义对比原则（DiffUtil）
    object RecordDiffCallback : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(
            oldItem: Record,
            newItem: Record
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Record,
            newItem: Record
        ): Boolean {
            return oldItem == newItem // 这里面是因为data class的 == 会自动比较所有字段
        }

    }
}