package com.example.accounting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.accounting.data.model.Message
import com.example.accounting.databinding.ItemMessageAiBinding
import com.example.accounting.databinding.ItemMessageLoadingBinding
import com.example.accounting.databinding.ItemMessageUserBinding

class MessageAdapter() : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {
    private val TYPE_USER = 1
    private val TYPE_AI = 2
    private val TYPE_LOADING = 3

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return when {
            message.isLoading -> TYPE_LOADING
            message.isMine -> TYPE_USER
            else -> TYPE_AI
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(ItemMessageUserBinding.inflate(inflater, parent, false))
            TYPE_AI -> AIViewHolder(ItemMessageAiBinding.inflate(inflater, parent, false))
            // 这里必须对应你那个带 ProgressBar 的布局
            TYPE_LOADING -> LoadingViewHolder(
                ItemMessageLoadingBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        if (holder is UserViewHolder) {
            holder.binding.tvMessageUser.text = item.content
        } else if (holder is AIViewHolder) {
            holder.binding.tvMessageAi.text = item.content
        }
    }
}

class UserViewHolder(val binding: ItemMessageUserBinding) :
    RecyclerView.ViewHolder(binding.root)

class AIViewHolder(val binding: ItemMessageAiBinding) : RecyclerView.ViewHolder(binding.root)
class LoadingViewHolder(val binding: ItemMessageLoadingBinding) :
    RecyclerView.ViewHolder(binding.root)

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(
        oldItem: Message,
        newItem: Message
    ): Boolean {
        return oldItem.content == newItem.content && oldItem.isMine == newItem.isMine && oldItem.timestamp == newItem.timestamp && oldItem.isLoading == newItem.isLoading
    }

    override fun areContentsTheSame(
        oldItem: Message,
        newItem: Message
    ): Boolean {
        return oldItem == newItem
    }

}