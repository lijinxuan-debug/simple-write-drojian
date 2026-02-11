package com.example.accounting.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class RecordPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // 固定为 2 页：0 是支出，1 是收入
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        // 每次滑动到新页面时，根据位置创建一个 Fragment 实例
        return RecordFragment.newInstance(position)
    }
}