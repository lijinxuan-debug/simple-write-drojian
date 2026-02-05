package com.example.accounting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.data.model.CategoryGroup
import com.example.accounting.data.model.CategoryItem
import com.example.accounting.databinding.ItemCategoryGroupBinding

class CategoryGroupAdapter(
    private val groups: List<CategoryGroup>,
    private val onItemSelected: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryGroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(val binding: ItemCategoryGroupBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemCategoryGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.binding.apply {
            tvGroupName.text = group.groupName

            // 初始化子 RecyclerView
            rvSubItems.layoutManager = GridLayoutManager(root.context, 4)
            rvSubItems.adapter = CategoryItemAdapter(group.items, onItemSelected)
        }
    }

    override fun getItemCount() = groups.size
}