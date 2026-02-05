package com.example.accounting.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.accounting.R
import com.example.accounting.databinding.ActivityManuallyBinding
import com.example.accounting.engine.CalculatorEngine
import com.example.accounting.utils.CalcUtils
import androidx.core.view.isGone
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.accounting.databinding.LayoutDialogCameraBinding
import com.example.accounting.engine.GlideEngine
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnExternalPreviewEventListener
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.io.File
import kotlin.math.abs

class ManuallyActivity : AppCompatActivity() {

    private var imageSelectList = ArrayList<LocalMedia>()
    private lateinit var binding: ActivityManuallyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityManuallyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化并监听所有计算按钮
        initCalculatorButton()

        // 监听导航栏
        initTabListener()

        // 监听返回按钮
        binding.ivBack.setOnClickListener { finish() }
        binding.ivTextBack.setOnClickListener { finish() }

        // 监听照相按钮
        binding.llCamera.setOnClickListener {
            showCameraBottomSheet()
        }

        // 当选择图片之后显示这个
        binding.ivPreview.setOnClickListener {
            // 只有当列表里有图片时才进入预览
            if (imageSelectList.isNotEmpty()) {
                PictureSelector.create(this)
                    .openPreview() // 打开预览模式
                    .setImageEngine(GlideEngine.createGlideEngine()) // 必须设置图片引擎
                    .setExternalPreviewEventListener(object : OnExternalPreviewEventListener {
                        // 【核心：监听预览界面的删除动作】
                        override fun onPreviewDelete(position: Int) {
                            // 当用户在预览界面点击“删除”时，同步更新我们本地的全局列表
                            imageSelectList.removeAt(position)

                            // 检查是否删光了
                            if (imageSelectList.isEmpty()) {
                                // 如果图删完了，切换回“加号”状态
                                binding.ivPreview.visibility = View.GONE
                                binding.llCameraDefault.visibility = View.VISIBLE
                            } else {
                                // 如果还有图，把缩略图更新为剩下的第一张
                                updateThumbnail(imageSelectList[0].path)
                            }
                        }

                        override fun onLongPressDownload(
                            context: Context?,
                            media: LocalMedia?
                        ): Boolean {
                            // 长按功能暂不设计
                            return false
                        }

                    })
                    // 启动预览：参数分别是 (起始位置, 是否显示删除, 数据源)
                    .startActivityPreview(0, true, imageSelectList)
            }
        }
    }

    // 辅助方法：更新主界面缩略图
    private fun updateThumbnail(path: String) {
        Glide.with(this)
            .load(path)
            .transform(CenterCrop(), RoundedCorners(15))
            .into(binding.ivPreview)
    }

    private fun initTabListener() {
        binding.topTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 根据选中的位置判断是支出(0)还是收入(1)
                val isExpense = tab?.position == 0
                val targetColor = if (isExpense) getColor(R.color.revenue_green) else getColor(R.color.expenditure_red)

                // 一键切换所有相关 UI 颜色
                updateUIThemeColor(targetColor)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    /**
     * 统一修改所有颜色
     */
    private fun updateUIThemeColor(color: Int) {
        // 金额大数字颜色
        binding.tvAmount.setTextColor(color)

        // 下划线颜色
        binding.lineAmount.setBackgroundColor(color)
    }

    /**
     * 初始化所有计算按钮
     */
    private fun initCalculatorButton() {
        // 1. 点击下拉箭头收起键盘
        binding.btnHideKeyboard.setOnClickListener {
            calculateFinalResult()
        }

        // 2. 点击金额显示区域，再次弹出键盘
        // 假设你的金额区域布局 ID 是 cl_amount_area
        binding.clAmountArea.setOnClickListener {
            if (binding.llKeyboardWrapper.isGone) {
                binding.llKeyboardWrapper.visibility = View.VISIBLE
                // 显示动画
                showKeyboardWithAnim()
                // 同时使得输入横线变粗
                binding.lineAmount.layoutParams.height = 4.dpToPx()
                binding.lineAmount.requestLayout()
            }
        }

        // 定义基础数字和运算符按键映射
        val commonButtons = listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9, binding.buttonPoint,
            binding.buttonPlus, binding.buttonMinus
        )

        // 批量设置点击监听
        commonButtons.forEach { btn ->
            btn.setOnClickListener {
                appendToCalcDisplay(btn.text.toString())
            }
        }

        // 2. 清除键 (C)
        binding.buttonClear.setOnClickListener {
            binding.tvCalculation.text = "" // 清空过程
            binding.tvAmount.text = getString(R.string.init_amount)  // 重置金额
            binding.tvUnitHint.visibility = View.INVISIBLE // 清除单位
        }

        // 3. 退格键 (删除图标)
        binding.backspace.setOnClickListener {
            val current = binding.tvCalculation.text.toString()
            if (current.isNotEmpty()) {
                binding.tvCalculation.text = current.dropLast(1)
                tryLivePreview()
                // 删除后也要保证视野不丢失
                scrollToBottom()
            }
        }

        // 4. 完成键 (计算最终结果并清空过程)
        binding.btnDone.setOnClickListener {
            calculateFinalResult()
        }

        // 刚开始单位不展示
        binding.tvUnitHint.visibility = View.INVISIBLE

    }

    private fun scrollToBottom() {
        // 需要使用 post，因为文字改变后布局刷新需要时间，直接滚动会滚不到位
        binding.hsvCalculation.post {
            binding.hsvCalculation.fullScroll(View.FOCUS_RIGHT)
        }
    }

    // 收起键盘的动画函数
    private fun hideKeyboardWithAnim() {
        binding.llKeyboardWrapper.animate()
            .translationY(binding.llKeyboardWrapper.height.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.llKeyboardWrapper.visibility = View.GONE
            }
            .start()
    }

    // 弹出键盘的动画函数
    private fun showKeyboardWithAnim() {
        binding.llKeyboardWrapper.visibility = View.VISIBLE
        binding.llKeyboardWrapper.translationY = binding.llKeyboardWrapper.height.toFloat()
        binding.llKeyboardWrapper.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }

    @SuppressLint("SetTextI18n")
    private fun appendToCalcDisplay(str: String) {
        val current = binding.tvCalculation.text.toString()
        // 只保留加减正则
        val isOperatorRegex = "[+\\-]".toRegex()
        val lastPart = current.split(isOperatorRegex).last()

        // --- 1. 预判未来结果是否超标 ---
        if (str.matches("[0-9.]".toRegex())) {
            val futureExpression = current + str
            try {
                val futureResult = CalculatorEngine.evaluate(futureExpression)
                if (abs(futureResult) >= 1000000000.0) return
            } catch (_: Exception) {
                // 异常通常是 10+ 这种不完整表达式，忽略即可
            }
        }

        // 小数点处理
        if (str == ".") {
            if (lastPart.contains(".")) return
            if (current.isEmpty() || current.last().toString().matches(isOperatorRegex)) {
                binding.tvCalculation.text = current + "0."
                tryLivePreview() // 补0后也要预览
                return
            }
        }

        // 小数位限制：小数点后只能有两位
        if (lastPart.contains(".") && str.matches("[0-9]".toRegex())) {
            val decimalPart = lastPart.substringAfter(".")
            if (decimalPart.length >= 2) return
        }

        // 运算符处理 (只处理 + -)
        if (str.matches(isOperatorRegex)) {
            if (current.isEmpty()) return
            // 重复点击切换运算符
            if (current.last().toString().matches(isOperatorRegex)) {
                binding.tvCalculation.text = current.dropLast(1) + str
                return
            }
        }

        binding.tvCalculation.text = current + str
        // 预算结果
        tryLivePreview()
        // 进行滚动
        scrollToBottom()
    }

    /**
     * 实时预览结果：用户每输入一个数字，上方大金额实时变动
     */
    private fun tryLivePreview() {
        val expression = binding.tvCalculation.text.toString()

        if (expression.isEmpty()) {
            binding.tvAmount.text = getString(R.string.init_amount)
            // 空的时候隐藏单位
            binding.tvUnitHint.visibility = View.INVISIBLE
            return
        }

        try {
            // 调用计算引擎
            val result = CalculatorEngine.evaluate(expression)
            // 使用格式化工具：千分位 + 两位小数
            binding.tvAmount.text = CalcUtils.formatAmount(result)

            // 这里是新增的单位显示逻辑
            val unit = CalcUtils.getUnitHint(result)
            if (unit.isNotEmpty()) {
                binding.tvUnitHint.text = unit
                binding.tvUnitHint.visibility = View.VISIBLE
            } else {
                binding.tvUnitHint.visibility = View.INVISIBLE
            }

        } catch (_: Exception) {
            // 解析中（如末尾是运算符时）忽略异常
        }
    }

    /**
     * 点击完成：计算结果并清空下方过程
     */
    private fun calculateFinalResult() {
        binding.llKeyboardWrapper.visibility = View.GONE
        // 消失动画
        hideKeyboardWithAnim()
        // 同时输入横线变细
        binding.lineAmount.layoutParams.height = 1.5f.dpToPx()
        binding.lineAmount.requestLayout()

        val expression = binding.tvCalculation.text.toString()
        if (expression.isEmpty()) return

        try {
            val result = CalculatorEngine.evaluate(expression)
            if (result < 0) {
                // 触发提示，询问用户建议
                checkNegativeResult(result)
            } else {
                // 兜底
                val finalResult = if (result >= 1000000000.0) 999999999.99 else result
                binding.tvAmount.text = CalcUtils.formatAmount(finalResult)
                // 计算结束，输入框不能清除，避免后续用户还想计算
                binding.tvCalculation.text = CalcUtils.formatForCalculation(finalResult)
                // 将计算器收起
                hideKeyboardWithAnim()
            }
        } catch (_: Exception) {
            binding.tvAmount.text = getString(R.string.init_amount)
        }
    }

    /**
     * 核心逻辑：检查结果并弹出转换建议
     */
    private fun checkNegativeResult(result: Double) {
        val currentTabIsExpense = binding.topTabLayout.selectedTabPosition == 0

        // 支出出现负数
        if (currentTabIsExpense && result < 0) {
            showSwitchDialog(
                title = "发现一笔“反向支出”",
                message = "当前计算结果为负数，是否要将其转为 [收入] 记录？",
                targetTabPosition = 1, // 收入页
                absoluteValue = abs(result)
            )
        }
        // 收入出现负数
        else if (!currentTabIsExpense && result < 0) {
            showSwitchDialog(
                title = "这笔收入是负的？",
                message = "看起来这是一笔开销，需要直接切换到 [支出] 页面并记录吗？",
                targetTabPosition = 0, // 支出页
                absoluteValue = abs(result)
            )
        }
    }

    private fun showSwitchDialog(title: String, message: String, targetTabPosition: Int, absoluteValue: Double) {
        // 大金额：1,234.56 (带逗号)
        val displayValue = CalcUtils.formatAmount(absoluteValue)
        // 过程框：1234.56 (无逗号)
        val calculationValue = CalcUtils.formatForCalculation(absoluteValue)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确转换") { _, _ ->
                // 自动切换 Tab（更改颜色）
                binding.topTabLayout.getTabAt(targetTabPosition)?.select()

                // 将负数转为正数并更新显示
                binding.tvAmount.text = displayValue

                // 当前的计算过程也要进行清空
                binding.tvCalculation.text = calculationValue

                // 可选：提示用户已转换
                Toast.makeText(this, "已为您切换状态", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("不用了", null)
            .show()
    }

    private fun showCameraBottomSheet() {
        // 1. 实例化 BottomSheetDialog，并传入我们写好的透明样式
        val bottomSheetDialog = BottomSheetDialog(this, R.style.TransparentBottomSheetStyle)

        // 2. 使用 ViewBinding 绑定弹窗布局 (假设布局文件名为 layout_dialog_camera.xml)
        val dialogBinding = LayoutDialogCameraBinding.inflate(layoutInflater)

        // 3. 设置内容视图
        bottomSheetDialog.setContentView(dialogBinding.root)

        // 在图库里面选择图片
        dialogBinding.llAlbum.setOnClickListener {
            PictureSelector.create(this)
                .openGallery(SelectMimeType.ofImage())
                .setImageEngine(GlideEngine.createGlideEngine())
                .setSelectionMode(SelectModeConfig.MULTIPLE)
                .setMinSelectNum(1)
                .setMaxSelectNum(9)
                .forResult(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: ArrayList<LocalMedia>) {
                        // 如果没有图片直接返回
                        if (result.isEmpty()) return

                        imageSelectList.clear()
                        imageSelectList.addAll(result)

                        // 拿第一张图的真实路径
                        val firstMedia = result[0]
                        val path = firstMedia.realPath ?: firstMedia.path

                        binding.llCameraDefault.visibility = View.GONE
                        binding.ivPreview.visibility = View.VISIBLE

                        // 使用glide加载缩略图
                        Glide.with(this@ManuallyActivity)
                            .load(path)
                            .transform(CenterCrop(), RoundedCorners(15))
                            .into(binding.ivPreview)

                    }

                    override fun onCancel() {
                        // 取消选择
                    }
                })
            bottomSheetDialog.dismiss()
        }

        // 拍照逻辑
        dialogBinding.llTakePhoto.setOnClickListener {
            PictureSelector.create(this)
                .openCamera(SelectMimeType.ofImage()) // 直接打开相机模式
                .setCameraImageFormat(PictureMimeType.JPEG) // 设置拍照图片格式
                .forResult(object : OnResultCallbackListener<LocalMedia> {
                    override fun onResult(result: ArrayList<LocalMedia>) {
                        // 下面的逻辑和图库选择图片则一致
                        if (result.isNotEmpty()) {
                            imageSelectList.clear()
                            imageSelectList.addAll(result)

                            val path = result[0].path

                            binding.llCameraDefault.visibility = View.GONE
                            binding.ivPreview.visibility = View.VISIBLE

                            // 用 Glide 加载高清原图
                            Glide.with(this@ManuallyActivity)
                                .load(path)
                                .transform(CenterCrop(), RoundedCorners(15))
                                .into(binding.ivPreview)
                        }
                    }

                    override fun onCancel() {}
                })
            bottomSheetDialog.dismiss()
        }

        // 5. 显示弹窗 (系统会自动执行从底部升起的动画，并让背景变灰)
        bottomSheetDialog.show()
    }

    /**
     * 将dp转换为px，方便在Kt里面直接转换
     */
    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

}