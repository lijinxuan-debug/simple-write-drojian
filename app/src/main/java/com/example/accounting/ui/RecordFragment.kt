import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.example.accounting.data.model.AccountGroup
import com.example.accounting.data.model.AccountItem
import com.example.accounting.data.model.CategoryGroup
import com.example.accounting.data.model.CategoryItem
import com.example.accounting.databinding.FragmentRecordBinding
import com.example.accounting.databinding.LayoutAccountBottomSheetBinding
import com.example.accounting.databinding.LayoutCategoryBottomSheetBinding
import com.example.accounting.databinding.LayoutDialogCameraBinding
import com.example.accounting.engine.GlideEngine
import com.example.accounting.utils.CategoryData.accountGroups
import com.example.accounting.utils.CategoryData.expenseCategories
import com.example.accounting.utils.CategoryData.incomeCategories
import com.example.accounting.viewmodel.BillViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnExternalPreviewEventListener
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    // 区分当前是支出还是收入
    private var pageType: Int = 0

    private val viewModel : BillViewModel by activityViewModels()

    // 默认设置今天
    private var selectedDateMillis: Long = System.currentTimeMillis()

    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.getDefault())

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
            }
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

        // 2. 点击金额显示区域，再次弹出键盘
        binding.clAmountArea.setOnClickListener {
            viewModel.updateKeyboardVisible(true)
        }

        // 监听点击事件按钮逻辑
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
            val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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
            insets
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
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.TransparentBottomSheetStyle)

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
                        Glide.with(binding.ivPreview)
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
                            Glide.with(binding.ivPreview)
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
     * 初始化分类选项
     */
    private fun initCategorySelection(categoryGroups: List<CategoryGroup>) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetBinding = LayoutCategoryBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        val categoryAdapter = CategoryGroupAdapter(categoryGroups) { selectedCategoryItem ->
            // 更新主界面显示的分类名
            binding.tvCategoryName.text = "${selectedCategoryItem.groupName} -> ${selectedCategoryItem.name}"

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

        val accountAdapter = AccountGroupAdapter(accountGroups) { selectedAccount ->
            // 更新主界面显示的账户名
            binding.tvAccountName.text = "${selectedAccount.name}(CNY)"

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
        // 创建 MaterialDatePicker
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择账单日期")
            .setSelection(selectedDateMillis) // 默认选中上一次选中的日期
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDateMillis = selection
            // 使用我们之前写的友好日期逻辑函数
            binding.tvSelectedTime.text = getFriendlyDate(selection)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER_TAG")
    }

    fun getFriendlyDate(millis: Long): String {
        // 1. 将毫秒转为 LocalDate (强制使用 UTC，确保不受当地时区偏移干扰)
        val targetDate = Instant.ofEpochMilli(millis)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()

        // 2. 获取当地的纯日期 (LocalDate.now 本身不带时分秒，非常安全)
        val today = LocalDate.now()

        // 3. 计算天数差 (ChronoUnit 会处理闰年等逻辑)
        val diffDays = ChronoUnit.DAYS.between(today, targetDate).toInt()

        // 4. 格式化日期部分 (dateFormatter 和 fullDateFormatter 使用之前定义的)
        val datePart = targetDate.format(dateFormatter)

        return when (diffDays) {
            0 -> "今天 $datePart"
            -1 -> "昨天 $datePart"
            -2 -> "前天 $datePart"
            1 -> "明天 $datePart"
            2 -> "后天 $datePart"
            else -> {
                if (targetDate.year == today.year) {
                    datePart
                } else {
                    targetDate.format(fullDateFormatter)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}