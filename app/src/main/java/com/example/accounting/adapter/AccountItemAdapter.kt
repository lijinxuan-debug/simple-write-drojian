package com.example.accounting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.data.model.AccountItem
import com.example.accounting.databinding.ItemAccountChildBinding

class AccountItemAdapter(
    private val items: List<AccountItem>,
    private val onAccountSelected: (AccountItem) -> Unit
) : RecyclerView.Adapter<AccountItemAdapter.AccountViewHolder>() {

    // 使用 ViewBinding 绑定 item_account_child.xml
    inner class AccountViewHolder(val binding: ItemAccountChildBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountChildBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            // 1. 设置图标和文字
            tvAccountName.text = item.name
            ivAccountIcon.setImageResource(item.iconRes)

            // 2. 点击事件：直接把整个对象传回给 Activity
            root.setOnClickListener {
                onAccountSelected(item)
            }
        }
    }

    override fun getItemCount() = items.size
}