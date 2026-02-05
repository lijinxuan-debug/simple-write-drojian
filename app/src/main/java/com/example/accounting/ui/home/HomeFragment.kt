package com.example.accounting.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.accounting.R
import com.example.accounting.databinding.FragmentHomeBinding
import com.example.accounting.ui.ManuallyActivity

class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding ?= null
    private val binding get() = _binding!!

    // 记录菜单是否展开
    private var isMenuExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 给添加账单按钮添加点击事件
        binding.fabAdd.setOnClickListener {
            toggleMenu()
        }

        // 手动导入添加跳转
        binding.itemManuallyAddBtn.setOnClickListener {
            val intent = Intent(requireContext(), ManuallyActivity::class.java)
            startActivity(intent)
        }

    }

    private fun toggleMenu() {
        if (!isMenuExpanded) {
            // 展开菜单
            expandMenu()
        } else {
            // 收起菜单
            collapseMenu()
        }
        isMenuExpanded = !isMenuExpanded
    }

    private fun expandMenu() {
        // 1. 图标切换：加号变叉号
        binding.fabAdd.setImageResource(R.drawable.ic_close_custom)

        // 2. 显示容器
        binding.llSubMenu.visibility = View.VISIBLE

        // 3. 动画：从下方 50dp 的位置淡入并滑向原位
        binding.llSubMenu.alpha = 0f
        binding.llSubMenu.translationY = 50f
        binding.llSubMenu.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300) // 300毫秒，非常丝滑
            .start()

    }

    private fun collapseMenu() {
        // 1. 图标切换：叉号变回加号
        binding.fabAdd.setImageResource(R.drawable.ic_add_custom)

        // 2. 动画：向下位移并消失
        binding.llSubMenu.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(300)
            .withEndAction {
                // 动画结束后完全隐藏，不占点击空间
                binding.llSubMenu.visibility = View.GONE
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}