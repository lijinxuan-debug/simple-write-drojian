package com.example.accounting.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.accounting.R
import com.example.accounting.adapter.RecordAdapter
import com.example.accounting.databinding.DialogWheelDateBinding
import com.example.accounting.databinding.FragmentHomeBinding
import com.example.accounting.ui.ManuallyActivity
import com.example.accounting.viewmodel.BillViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlin.getValue


class HomeFragment : Fragment() {
    private var _binding : FragmentHomeBinding ?= null
    private val binding get() = _binding!!

    // 记录菜单是否展开
    private var isMenuExpanded = false

    private val viewModel : BillViewModel by viewModels()

    private val yearFormatter = DateTimeFormatter.ofPattern("yyyy年")
    private val monthFormatter = DateTimeFormatter.ofPattern("MM月")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置日期为当前时间
        val now = LocalDate.now()
        binding.tvHeaderYear.text = now.format(yearFormatter)
        binding.tvHeaderMonth.text = now.format(monthFormatter)

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

        // 配置适配器
        val recordAdapter = RecordAdapter { record ->
            val intent = Intent(requireContext(), ManuallyActivity::class.java)
            intent.putExtra("recordData",Gson().toJson(record))
            startActivity(intent)
        }

        // 配置recyclerview
        binding.rvRecordList.apply {
            adapter = recordAdapter
        }

        // 使用协程去收集流Flow
        lifecycleScope.launch {
            // 这个repeat的作用是节省资源
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察账单列表流
                launch {
                    viewModel.monthRecords.collect { records ->
                        if (records.isEmpty()) {
                            binding.llEmptyView.visibility = View.VISIBLE
                            binding.rvRecordList.visibility = View.GONE
                        } else {
                            binding.llEmptyView.visibility = View.GONE
                            binding.rvRecordList.visibility = View.VISIBLE
                            recordAdapter.submitList(records)
                        }
                    }
                }
                // 2. 观察总支出流
                launch {
                    viewModel.allExpense.collect { expenseStr ->
                        binding.expenseAmount.text = expenseStr
                    }
                }

                // 3. 观察总收入流
                launch {
                    viewModel.allIncome.collect { incomeStr ->
                        binding.incomeAmount.text = incomeStr
                    }
                }
            }
        }

        // 设置开始时间点击：选完后自动弹结束时间
        binding.layoutMonthPicker.setOnClickListener {
            showMonthPicker()
        }

    }

    /**
     * 弹出年月选择器（适配新的右上角布局）
     */
    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun showMonthPicker() {
        val dialog = BottomSheetDialog(requireContext())
        // 确保你的 dialog 布局已经去掉了 npDay
        val dialogBinding = DialogWheelDateBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val calendar = Calendar.getInstance()

        // 1. 初始化年份滚轮
        dialogBinding.npYear.apply {
            minValue = 2000
            maxValue = 2100
            // 尝试从当前 header 获取年份，如果没有则用系统年份
            value = binding.tvHeaderYear.text.toString().replace("年", "").toIntOrNull()
                ?: calendar.get(Calendar.YEAR)
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        // 2. 初始化月份滚轮
        dialogBinding.npMonth.apply {
            minValue = 1
            maxValue = 12
            // 尝试从当前 header 获取月份，如果没有则用系统月份
            value = binding.tvHeaderMonth.text.toString().replace("月", "").toIntOrNull()
                ?: (calendar.get(Calendar.MONTH) + 1)
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        }

        // 3. 确定按钮逻辑
        dialogBinding.btnConfirm.setOnClickListener {
            val selectedYear = dialogBinding.npYear.value
            val selectedMonth = dialogBinding.npMonth.value

            // 更新 UI 显示
            binding.tvHeaderYear.text = "${selectedYear}年"
            binding.tvHeaderMonth.text = String.format("%02d月", selectedMonth)

            // 【核心】在这里触发数据刷新逻辑
            viewModel.changeDate(selectedYear,selectedMonth)

            dialog.dismiss()
        }

        dialog.show()
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