package com.example.accounting.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.accounting.R
import com.example.accounting.adapter.RecordAdapter
import com.example.accounting.databinding.FragmentHomeBinding
import com.example.accounting.ui.ManuallyActivity
import com.example.accounting.viewmodel.BillViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.getValue


class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding ?= null
    private val binding get() = _binding!!

    // 记录菜单是否展开
    private var isMenuExpanded = false

    private val recordAdapter = RecordAdapter()

    private val viewModel : BillViewModel by viewModels()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
            // 同时调用按钮回来动画
            collapseMenu()
        }

        // 配置recyclerview
        binding.rvRecordList.apply {
            adapter = recordAdapter
        }

        // 观察liveData
        viewModel.allRecords.observe(viewLifecycleOwner) { newList ->
            recordAdapter.submitList(newList)
        }

        // 更新支出和收入
        viewModel.allExpense.observe(viewLifecycleOwner) { expenseStr ->
            binding.expenseAmount.text = expenseStr
        }

        viewModel.allIncome.observe(viewLifecycleOwner) { incomeStr ->
            binding.incomeAmount.text = incomeStr
        }

    }

    private fun showWheelDatePicker(isStartDate: Boolean) {
        val pvTime = TimePickerBuilder(requireContext()) { date, _ ->
            // 1. 将 java.util.Date 安全转换为 java.time.LocalDate
            val localDate = Instant.ofEpochMilli(date.time)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // 2. 使用线程安全的 DateTimeFormatter 格式化
            val dateString = localDate.format(dateFormatter)

            // 3. 更新 UI
            if (isStartDate) {
                binding.tvStartDate.text = dateString
                // 更新 ViewModel 逻辑...
            } else {
                binding.tvEndDate.text = dateString
                // 更新 ViewModel 逻辑...
            }
        }
            .setType(booleanArrayOf(true, true, true, false, false, false))
            .setCancelText("取消")
            .setSubmitText("确定")
            .setTitleText("选择日期")
            // 设置符合图中的黄色风格
            .setSubmitColor(ContextCompat.getColor(requireContext(), R.color.bill_add))
            .build()

        pvTime.show()
    }

    private fun toggleMenu() {
        if (isMenuExpanded) {
            // 收起菜单
            collapseMenu()
        } else {
            // 展开菜单
            expandMenu()
        }
    }

    private fun expandMenu() {
        // 只要调用了展开，状态必须是true
        isMenuExpanded = true

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
        // 只要调用了收起，状态必须是false
        isMenuExpanded = false

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