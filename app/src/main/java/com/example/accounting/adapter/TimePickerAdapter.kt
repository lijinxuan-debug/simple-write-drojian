package com.example.accounting.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.data.model.TimeTab
import com.example.accounting.databinding.ItemTimePickerBinding

class TimePickerAdapter(
    private val onItemSelected: (TimeTab) -> Unit
) : ListAdapter<TimeTab, TimePickerAdapter.ViewHolder>(TimeTabDiffCallback()) {

    // 内部维护选中的位置，用于 UI 表现
    private var selectedPosition = -1

    class ViewHolder(val binding: ItemTimePickerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimePickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        // 第一次加载时初始化 selectedPosition
        if (item.isSelected && selectedPosition == -1) {
            selectedPosition = holder.bindingAdapterPosition
        }

        holder.binding.tvMonthItem.apply {
            text = item.label

            // 状态表现逻辑
            if (position == selectedPosition) {
                setTextColor(Color.parseColor("#333333"))
                setTypeface(null, Typeface.BOLD)
                textSize = 16f
            } else {
                setTextColor(Color.parseColor("#999999"))
                setTypeface(null, Typeface.NORMAL)
                textSize = 14f
            }
        }

        holder.binding.root.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION && currentPos != selectedPosition) {
                val oldPos = selectedPosition
                selectedPosition = currentPos

                // DiffUtil 配合这些手动刷新可以实现精准的点击动画
                notifyItemChanged(oldPos)
                notifyItemChanged(selectedPosition)

                scrollToCenter(holder.binding.root, currentPos)
                onItemSelected(item)
            }
        }
    }

    private fun scrollToCenter(view: View, position: Int) {
        val recyclerView = view.parent as? RecyclerView ?: return
        val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val offset = recyclerView.width / 2 - view.width / 2
        manager.scrollToPositionWithOffset(position, offset)
    }

    // 核心：DiffUtil 回调定义
    class TimeTabDiffCallback : DiffUtil.ItemCallback<TimeTab>() {
        override fun areItemsTheSame(oldItem: TimeTab, newItem: TimeTab): Boolean {
            // 比较唯一标识（年+月）
            return oldItem.year == newItem.year && oldItem.month == newItem.month
        }

        override fun areContentsTheSame(oldItem: TimeTab, newItem: TimeTab): Boolean {
            // 比较内容是否一致
            return oldItem == newItem
        }
    }
}