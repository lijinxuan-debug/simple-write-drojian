package com.example.accounting.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.R
import com.example.accounting.data.model.Record
import com.example.accounting.databinding.ItemBillRecordBinding
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
            tvTime.text = record.dateStr
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
            tvAmount.setTextColor(ContextCompat.getColor(root.context, colorRes))
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