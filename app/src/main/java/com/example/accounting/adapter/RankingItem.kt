package com.example.accounting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.databinding.ItemRankingBinding // 确保包名正确

// RankingItem 是我们要显示的数据模型
data class RankingItem(
    val icon: String,
    val name: String,
    val amount: String,
    val percent: Float
)

class RankingAdapter : ListAdapter<RankingItem, RankingAdapter.ViewHolder>(DiffCallback) {

    // 使用 ViewBinding 绑定 item_ranking.xml
    inner class ViewHolder(val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            // 绑定文字
            tvRankName.text = item.name
            tvRankAmount.text = item.amount
            tvRankPercent.text = "${String.format("%.1f", item.percent)}%"
            
            // 绑定进度条 (ProgressBar)
            pbRankBar.progress = item.percent.toInt()
            
            // 动态设置图标 (假设图标存放在 drawable 中)
            val resId = root.context.resources.getIdentifier(item.icon, "drawable", root.context.packageName)
            if (resId != 0) {
                ivRankIcon.setImageResource(resId)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<RankingItem>() {
        override fun areItemsTheSame(oldItem: RankingItem, newItem: RankingItem) = oldItem.name == newItem.name
        override fun areContentsTheSame(oldItem: RankingItem, newItem: RankingItem) = oldItem == newItem
    }
}