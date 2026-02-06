package com.example.accounting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.data.model.AccountGroup
import com.example.accounting.data.model.AccountItem
import com.example.accounting.databinding.ItemAccountGroupBinding

class AccountGroupAdapter(
    private val groups: List<AccountGroup>,
    private val onAccountSelected: (AccountItem) -> Unit
) : RecyclerView.Adapter<AccountGroupAdapter.GroupViewHolder>() {

    // 使用 ViewBinding 绑定 item_account_group.xml
    inner class GroupViewHolder(val binding: ItemAccountGroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemAccountGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.binding.apply {
            // 1. 设置分组标题（例如：储蓄账户）
            tvGroupName.text = group.groupName

            // 2. 配置嵌套的 RecyclerView
            rvAccountItems.apply {
                // 账户是垂直排列的，所以用 LinearLayoutManager
                layoutManager = LinearLayoutManager(context)
                // 挂载子适配器，并将点击事件继续向上传递
                adapter = AccountItemAdapter(group.items,onAccountSelected)
                
                // 性能优化：子列表不需要独立滚动
                isNestedScrollingEnabled = false 
            }
        }
    }

    override fun getItemCount() = groups.size
}