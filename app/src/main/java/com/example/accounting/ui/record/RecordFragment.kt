package com.example.accounting.ui.record

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.accounting.R
import com.example.accounting.adapter.AccountGroupAdapter
import com.example.accounting.adapter.CategoryGroupAdapter
import com.example.accounting.data.model.CategoryGroup
import com.example.accounting.databinding.FragmentRecordBinding
import com.example.accounting.databinding.LayoutAccountBottomSheetBinding
import com.example.accounting.databinding.LayoutCategoryBottomSheetBinding
import com.example.accounting.databinding.LayoutDialogCameraBinding
import com.example.accounting.engine.GlideEngine
import com.example.accounting.utils.CategoryAndAccountData
import com.example.accounting.utils.CategoryAndAccountData.expenseCategories
import com.example.accounting.utils.CategoryAndAccountData.incomeCategories
import com.example.accounting.viewmodel.BillViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnExternalPreviewEventListener
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    // 区分当前是支出还是收入
    private var pageType: Int = 0

    private val viewModel : BillViewModel by activityViewModels()

    // 默认设置今天
    private var selectedDateMillis: Long = System.currentTimeMillis()

    private var imageSelectList = ArrayList<LocalMedia>()

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): RecordFragment {
            val fragment = RecordFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        pageType = arguments?.getInt(ARG_POSITION) ?: 0
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 观察金额变化
        viewModel.amount.observe(viewLifecycleOwner) { newAmount ->
            binding.tvAmount.text = newAmount
        }

        // 观察计算过程
        viewModel.calculationExpression.observe(viewLifecycleOwner) { expression ->
            binding.tvCalculation.text = expression
            // 同时需要进行水平滚动
            scrollToBottom()
        }

        // 观察单位提示
        viewModel.unitHint.observe(viewLifecycleOwner) { hint ->
            binding.tvUnitHint.text = hint
        }

        // 观察单位状态
        viewModel.unitHintVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                binding.tvUnitHint.visibility = View.VISIBLE
            } else {
                binding.tvUnitHint.visibility = View.INVISIBLE
            }
        }

        // 观察计算键盘显示状态
        viewModel.isKeyboardVisible.observe(viewLifecycleOwner) { isVisible ->
            if (isVisible) {
                // 同时使得输入横线变粗
                binding.lineAmount.layoutParams.height = 4.dpToPx()
                binding.lineAmount.requestLayout()
            } else {
                // 同时输入横线变细
                binding.lineAmount.layoutParams.height = 1.5f.dpToPx()
                binding.lineAmount.requestLayout()
                // 计算过程需要先隐藏才可以
                binding.hsvCalculation.visibility = View.INVISIBLE
            }
        }

        // 观察保存结果
        viewModel.billSaveResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 1. 根据当前页面身份，锁定对应的 LiveData 源
        val sourceDate = if (pageType == 0) viewModel.expenseDate else viewModel.incomeDate
        val sourceCategory = if (pageType == 0) viewModel.expenseCategoryItem else viewModel.incomeCategoryItem
        val sourceAccount = if (pageType == 0) viewModel.expenseAccount else viewModel.incomeAccount
        val sourceRemark = if (pageType == 0) viewModel.expenseRemark else viewModel.incomeRemark
        val sourceImage = if (pageType == 0) viewModel.expenseImage else viewModel.incomeImage

        // 统一观察日期
        sourceDate.observe(viewLifecycleOwner) { millis ->
            binding.tvSelectedTime.text = getFriendlyDateTime(millis)
            selectedDateMillis = millis // 同步给 Fragment 内部变量
        }

        // 统一观察分类
        sourceCategory.observe(viewLifecycleOwner) { item ->
            binding.tvCategoryName.text = "${item.groupName} -> ${item.name}"
        }

        // 统一观察账户
        sourceAccount.observe(viewLifecycleOwner) { account ->
            binding.tvAccountName.text = "${account.name}(CNY)"
        }

        // 统一观察备注
        sourceRemark.observe(viewLifecycleOwner) { remark ->
            // 只有当输入框内容不一致时才更新，防止死循环
            if (binding.etRemark.text.toString() != remark) {
                binding.etRemark.setText(remark)
            }
        }

        // 统一观察图片
        sourceImage.observe(viewLifecycleOwner) { paths ->
            handleImageMapping(paths)
        }

        // 1. 拿到身份 (0是支出，1是收入)
        pageType = arguments?.getInt("position") ?: 0

        val themeColor = if (pageType == 0) {
            ContextCompat.getColor(requireContext(), R.color.revenue_green) // 支出青
        } else {
            ContextCompat.getColor(requireContext(), R.color.expenditure_red) // 收入红
        }

        binding.tvAmount.setTextColor(themeColor) // 金额数字变色
        binding.lineAmount.setBackgroundColor(themeColor) // 下划线变色

        // 监听照相按钮
        binding.llCamera.setOnClickListener {
            showCameraBottomSheet()
        }

        // 当选择图片之后显示这个
        binding.ivPreview.setOnClickListener {
            // 只有当列表里有图片时才进入预览
            if (imageSelectList.isNotEmpty()) {
                selectPicture()
            }
        }

        // 监听点击分类按钮
        binding.llCategoryRow.setOnClickListener {
            if (pageType == 0) {
                // 支出
                initCategorySelection(expenseCategories)
            } else {
                // 收入
                initCategorySelection(incomeCategories)
            }
        }

        // 监听点击账户按钮
        binding.llAccountRow.setOnClickListener {
            initAccountSelection()
        }

        // 点击金额显示区域，再次弹出键盘
        binding.clAmountArea.setOnClickListener {
            viewModel.updateKeyboardVisible(true)
            // 同时计算过程也要显现
            binding.hsvCalculation.visibility = View.VISIBLE
        }

        // 监听点击时间按钮逻辑
        binding.llTimeRow.setOnClickListener {
            showDatePicker()
        }

        // 监听备注输入
        binding.llRemarkRow.setOnClickListener {
            // 先隐藏计算器
            viewModel.updateKeyboardVisible(false)
            // 聚焦
            binding.etRemark.requestFocus()
            // 将光标移到末尾
            binding.etRemark.setSelection(binding.etRemark.text.length)
            // 强制弹出软键盘
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etRemark, InputMethodManager.SHOW_IMPLICIT)
        }

        // 监听键盘关闭
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // 检查键盘（IME）是否可见
            val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            if (!isKeyboardVisible) {
                // 关键逻辑：如果键盘关了，且当前焦点确实在备注框上，就清除它
                if (binding.etRemark.isFocused) {
                    binding.etRemark.clearFocus()
                }
            }

            // 同时将备注保存
            if (pageType == 0) {
                viewModel.expenseRemark.value = binding.etRemark.text.toString()
            } else {
                viewModel.incomeRemark.value = binding.etRemark.text.toString()
            }

            insets
        }

        // 初始化支出和收入分类初始值
        initDefaultCategory()
    }

    private fun handleImageMapping(pathList: List<String>) {
        // 同步给预览列表
        imageSelectList = convertPathsToLocalMedia(pathList)

        if (pathList.isNotEmpty()) {
            // 隐藏加号，显示预览图
            binding.llCameraDefault.visibility = View.GONE
            binding.ivPreview.visibility = View.VISIBLE
            // 映射第一张图到封面
            updateThumbnail(pathList[0])
        } else {
            // 没图就切回加号
            binding.llCameraDefault.visibility = View.VISIBLE
            binding.ivPreview.visibility = View.GONE
        }
    }

    // 将 String 路径列表转换为 LocalMedia 列表
    private fun convertPathsToLocalMedia(paths: List<String>): ArrayList<LocalMedia> {
        val result = ArrayList<LocalMedia>()
        for (path in paths) {
            val media = LocalMedia.create()
            media.path = path        // 文件的路径
            media.realPath = path    // 真实路径（在私有目录里，path 和 realPath 通常一致）
            media.mimeType = "image/*" // 设置类型
            media.chooseModel = 1    // 这是图片模式

            result.add(media)
        }
        return result
    }

    private fun selectPicture(){
        PictureSelector.create(this)
            .openPreview() // 打开预览模式
            .setImageEngine(GlideEngine.createGlideEngine()) // 必须设置图片引擎
            .setExternalPreviewEventListener(object : OnExternalPreviewEventListener {
                // 【核心：监听预览界面的删除动作】
                override fun onPreviewDelete(position: Int) {
                    // 当用户在预览界面点击“删除”时，同步更新我们本地的全局列表
                    imageSelectList.removeAt(position)

                    val pathList = imageSelectList.map { result ->
                        result.realPath ?: result.path
                    }

                    // 同时更新viewModel选中的图片数据
                    if (pageType == 0) {
                        viewModel.expenseImage.value = pathList
                    } else {
                        viewModel.incomeImage.value = pathList
                    }

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

    private fun initDefaultCategory() {
        // 根据当前是支出还是收入，拿到对应的数据源
        val defaultList = if (pageType == 0) expenseCategories else incomeCategories

        // 确保列表不为空，防止崩溃
        if (defaultList.isNotEmpty()) {
            val firstGroup = defaultList[0]
            if (firstGroup.items.isNotEmpty()) {
                val firstItem = firstGroup.items[0]
                binding.tvCategoryName.text = "${firstGroup.groupName} -> ${firstItem.name}"

                if (pageType == 0) {
                    viewModel.expenseCategoryItem.value = firstItem
                } else {
                    viewModel.incomeCategoryItem.value = firstItem
                }
            }
        }
    }

    // 计算过程需要进行滚动
    private fun scrollToBottom() {
        // 需要使用 post，因为文字改变后布局刷新需要时间，直接滚动会滚不到位
        binding.hsvCalculation.post {
            binding.hsvCalculation.fullScroll(View.FOCUS_RIGHT)
        }
    }

    private fun showCameraBottomSheet() {
        // 1. 实例化 BottomSheetDialog，并传入我们写好的透明样式
        val bottomSheetDialog =
            BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetStyle)

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

                        val pathList = result.map { media ->
                            // 优先真实路径，找不到则普通路径即可
                            media.realPath ?: media.path
                        }

                        if (pageType == 0) {
                            viewModel.expenseImage.value = pathList
                        } else {
                            viewModel.incomeImage.value = pathList
                        }

                        // 拿第一张图的真实路径
                        val firstPath = pathList[0]

                        binding.llCameraDefault.visibility = View.GONE
                        binding.ivPreview.visibility = View.VISIBLE

                        // 使用glide加载缩略图
                        Glide.with(binding.ivPreview)
                            .load(firstPath)
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

                            val pathList = result.map { media ->
                                // 优先真实路径，找不到则普通路径即可
                                media.realPath ?: media.path
                            }

                            val firstPath = pathList[0]

                            if (pageType == 0) {
                                viewModel.expenseImage.value = pathList
                            } else {
                                viewModel.incomeImage.value = pathList
                            }

                            binding.llCameraDefault.visibility = View.GONE
                            binding.ivPreview.visibility = View.VISIBLE

                            // 用 Glide 加载高清原图
                            Glide.with(binding.ivPreview)
                                .load(firstPath)
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
     * 初始化分类选项
     */
    private fun initCategorySelection(categoryGroups: List<CategoryGroup>) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetBinding = LayoutCategoryBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        val categoryAdapter = CategoryGroupAdapter(categoryGroups) { selectedCategoryItem ->
            // 更新主界面显示的分类名
            binding.tvCategoryName.text =
                "${selectedCategoryItem.groupName} -> ${selectedCategoryItem.name}"

            if (pageType == 0) {
                viewModel.expenseCategoryItem.value = selectedCategoryItem
            } else {
                viewModel.incomeCategoryItem.value = selectedCategoryItem
            }

            // 选完关掉弹窗
            bottomSheetDialog.dismiss()
        }

        // 配置弹窗里的 RecyclerView
        sheetBinding.rvCategoryGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }

        bottomSheetDialog.show()

    }

    /**
     * 初始化账户选项
     */
    private fun initAccountSelection() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetBinding = LayoutAccountBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        val accountAdapter = AccountGroupAdapter(CategoryAndAccountData.accountGroups) { selectedAccount ->
            if (pageType == 0) {
                viewModel.expenseAccount.value = selectedAccount
            } else {
                viewModel.incomeAccount.value = selectedAccount
            }

            // 选完关掉弹窗
            bottomSheetDialog.dismiss()
        }

        // 配置弹窗里的 RecyclerView
        sheetBinding.rvAccountGroups.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountAdapter
        }

        bottomSheetDialog.show()
    }

    // 辅助方法：更新主界面缩略图
    private fun updateThumbnail(path: String) {
        Glide.with(this)
            .load(path)
            .transform(CenterCrop(), RoundedCorners(15))
            .into(binding.ivPreview)
    }

    private fun showDatePicker() {
        // 1. 第一步：日期选择
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择账单日期")
            .setSelection(selectedDateMillis)
            .build()

        datePicker.addOnPositiveButtonClickListener { dateSelection ->
            // 2. 第二步：当日期选好后，立即弹出时间选择器
            showTimePicker(dateSelection)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showTimePicker(dateMillis: Long) {
        val now = Calendar.getInstance()

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H) // 24小时制
            .setHour(now.get(Calendar.HOUR_OF_DAY)) // 默认当前时间
            .setMinute(now.get(Calendar.MINUTE))
            .setTitleText("选择具体时间")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            // 3. 第三步：将日期和时间合并
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dateMillis
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)

            val finalDateTime = calendar.timeInMillis
            selectedDateMillis = finalDateTime

            // 更新 ViewModel
            if (pageType == 0) {
                viewModel.expenseDate.value = finalDateTime
            } else {
                viewModel.incomeDate.value = finalDateTime
            }
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    fun getFriendlyDateTime(millis: Long): String {
        // 1. 获取本地时区和时间对象
        val zoneId = ZoneId.systemDefault()
        val targetDateTime = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDateTime()
        val targetDate = targetDateTime.toLocalDate()

        val today = LocalDate.now()
        val diffDays = ChronoUnit.DAYS.between(today, targetDate).toInt()

        // 2. 准备各种格式化工具
        val timePart = targetDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        val datePartShort = targetDate.format(DateTimeFormatter.ofPattern("M月d日"))
        val datePartFull = targetDate.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))

        // 3. 核心修改：将描述词和日期拼接
        val dateDescription = when (diffDays) {
            0 -> "今天 $datePartShort"
            -1 -> "昨天 $datePartShort"
            -2 -> "前天 $datePartShort"
            1 -> "明天 $datePartShort"
            2 -> "后天 $datePartShort"
            else -> {
                if (targetDate.year == today.year) {
                    datePartShort
                } else {
                    datePartFull
                }
            }
        }

        // 4. 最终组合结果
        return "$dateDescription $timePart"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}