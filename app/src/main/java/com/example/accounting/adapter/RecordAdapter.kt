package com.example.accounting.adapter

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
import com.example.accounting.data.model.RecordListItem
import com.example.accounting.databinding.ItemBillRecordBinding
import com.example.accounting.databinding.ItemDateHeaderBinding // 假设你新建了这个极简布局
import com.example.accounting.engine.GlideEngine
import com.google.android.material.imageview.ShapeableImageView
import java.text.DecimalFormat

class RecordAdapter(
    private val onEdit: (Record) -> Unit
) : ListAdapter<RecordListItem, RecyclerView.ViewHolder>(RecordDiffCallback) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is RecordListItem.Header -> TYPE_HEADER
            is RecordListItem.Item -> TYPE_ITEM
        }
    }

    // --- 1. 定义两个不同的 ViewHolder ---

    // 日期头 ViewHolder
    class HeaderViewHolder(private val binding: ItemDateHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: RecordListItem.Header) {
            binding.tvDateTitle.text = header.dateStr
        }
    }

    // 账单项 ViewHolder
    inner class RecordViewHolder(val binding: ItemBillRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: Record) {
            binding.apply {
                // 设置图标
                val resId = root.context.resources.getIdentifier(record.categoryIcon, "drawable", root.context.packageName)
                ivCategoryIcon.setImageResource(resId)

                tvCategoryName.text = record.categoryName
                tvAccountInfo.text = "${record.paymentMethod} · ${record.timeStr}"
                tvRemark.text = record.remark

                // 图片显示 (建议后续按我说的优化 addView 问题)
                layoutImages.removeAllViews()
                if (record.images.isNotEmpty()) {
                    layoutImages.visibility = View.VISIBLE
                    record.images.forEach { path ->
                        val iv = ShapeableImageView(root.context).apply {
                            val size = (80 * resources.displayMetrics.density).toInt()
                            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = 4.dpToPx() }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        layoutImages.addView(iv)
                        GlideEngine.createGlideEngine().loadImage(root.context, path, iv)
                    }
                } else {
                    layoutImages.visibility = View.GONE
                }

                // 金额处理
                val isExpense = record.type == 0
                val amountValue = record.amount.toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                tvAmount.text = "${if (isExpense) "-" else "+"}${DecimalFormat("#,##0.00").format(amountValue)}"
                tvAmount.setTextColor(ContextCompat.getColor(root.context, if (isExpense) R.color.expenditure_red else R.color.revenue_green))

                data.setOnClickListener { onEdit(record) }
            }
        }
    }

    // --- 2. 修改创建和绑定逻辑 ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemDateHeaderBinding.inflate(inflater, parent, false))
        } else {
            RecordViewHolder(ItemBillRecordBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val data = getItem(position)
        if (holder is HeaderViewHolder && data is RecordListItem.Header) {
            holder.bind(data)
        } else if (holder is RecordViewHolder && data is RecordListItem.Item) {
            holder.bind(data.record)
        }
    }

    // 辅助扩展
    private fun Int.dpToPx() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
}

// --- 3. 彻底重写 DiffCallback ---

object RecordDiffCallback : DiffUtil.ItemCallback<RecordListItem>() {
    override fun areItemsTheSame(oldItem: RecordListItem, newItem: RecordListItem): Boolean {
        if (oldItem::class != newItem::class) return false
        return when {
            oldItem is RecordListItem.Header && newItem is RecordListItem.Header ->
                oldItem.dateStr == newItem.dateStr
            oldItem is RecordListItem.Item && newItem is RecordListItem.Item ->
                oldItem.record.id == newItem.record.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: RecordListItem, newItem: RecordListItem): Boolean {
        return oldItem == newItem
    }
}