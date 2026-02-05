package com.example.accounting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.data.model.CategoryItem
import com.example.accounting.databinding.ItemCategoryItemBinding

class CategoryItemAdapter(
    private val items: List<CategoryItem>,
    private val onItemSelected: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryItemAdapter.ItemViewHolder>() {

    class ItemViewHolder(val binding: ItemCategoryItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemCategoryItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvItemName.text = item.name
            ivItemIcon.setImageResource(item.iconRes)
            
            // 点击整个 Item 回调
            root.setOnClickListener { onItemSelected(item) }
        }
    }

    override fun getItemCount() = items.size
}