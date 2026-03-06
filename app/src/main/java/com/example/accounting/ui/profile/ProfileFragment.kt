package com.example.accounting.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.accounting.MainActivity
import com.example.accounting.R
import com.example.accounting.data.model.Record
import com.example.accounting.databinding.DialogLogoutBinding
import com.example.accounting.databinding.FragmentProfileBinding
import com.example.accounting.engine.GlideEngine
import com.example.accounting.ui.LoginActivity
import com.example.accounting.utils.FileUtil
import com.example.accounting.utils.SpUtil
import com.example.accounting.utils.ToastUtils
import com.example.accounting.viewmodel.BillViewModel
import com.example.accounting.viewmodel.UserViewModel
import com.luck.picture.lib.basic.PictureSelector
import com.luck.picture.lib.config.SelectMimeType
import com.luck.picture.lib.config.SelectModeConfig
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.interfaces.OnResultCallbackListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.lang.Exception
import java.util.ArrayList

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by viewModels()

    private val billViewMOdel: BillViewModel by viewModels()

    // 权限请求的 Launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 检查所有权限是否都被授予
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            // 权限授予成功，打开相册
            openGallery()
        } else {
            // 权限被拒绝，检查是否彻底禁止
            val isPermanentlyDenied = permissions.entries.any { (permission, _) ->
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permission
                )
            }

            if (isPermanentlyDenied) {
                // 彻底禁止，显示引导弹窗
                showPermissionDeniedDialog()
            } else {
                // 普通拒绝，提示用户
                Toast.makeText(requireContext(), "需要权限才能更换头像", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                if (user.avatar == "default_avatar") {
                    GlideEngine.createGlideEngine()
                        .loadImage(requireContext(), R.drawable.img, binding.ivAvatar)
                } else {
                    // 那就是已经存储了图片，直接获取即可
                    val avatarFile = FileUtil.getAvatarFile(requireContext(), user.avatar)
                    // 使用glide加载图片
                    GlideEngine.createGlideEngine()
                        .loadImage(requireContext(), avatarFile, binding.ivAvatar)
                }
                // 头像处理之后，用户的账号也需要显现
                binding.tvUsername.text = user.email
            } else {
                Log.e("不存在对象", "可能发生错误")
            }
        }

        binding.itemExport.apply {
            itemTitle.text = "导出账单"
            GlideEngine.createGlideEngine()
                .loadImage(requireContext(), R.drawable.ic_export_link, itemIcon)
            root.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        (activity as? MainActivity)?.showGlobalLoading(true)
                        root.isEnabled = false

                        ToastUtils.show(requireContext(), "正在导出，请稍等")
                        val allRecord =
                            billViewMOdel.getAllRecord(SpUtil.getUserId(requireContext()))
                        // 账单为空
                        if (allRecord.isEmpty()) {
                            ToastUtils.show(requireContext(), "账单为空，无法导出")
                            return@launch
                        }
                        val excelFile = generateExcelFile(allRecord)

                        if (excelFile != null && excelFile.exists()) {
                            shareFile(excelFile)
                        } else {
                            ToastUtils.show(requireContext(), "导出数据失败")
                        }
                    } catch (e: Exception) {
                        Log.e("ExportError", "导出失败：${e.message}")
                        ToastUtils.show(requireContext(), "导出出现问题")
                    } finally {
                        (activity as? MainActivity)?.showGlobalLoading(false)
                        root.isEnabled = true
                    }
                }
            }
        }

        binding.itemLogout.apply {
            itemTitle.text = "退出"
            GlideEngine.createGlideEngine()
                .loadImage(requireContext(), R.drawable.ic_logout, itemIcon)
            root.setOnClickListener {
                logout()
            }
        }

        // 获取相关用户信息
        userViewModel.getUserInfo()

        // 监听退出登录
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // 更换头像
        binding.ivAvatar.setOnClickListener {
            // 先检查权限
            val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 及以上使用 READ_MEDIA_IMAGES
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                // Android 12 及以下使用 READ_EXTERNAL_STORAGE
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            // 检查是否已经有权限
            val allGranted = permissionsToCheck.all {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (allGranted) {
                // 已经有权限，直接打开相册
                openGallery()
            } else {
                // 没有权限，请求权限
                permissionLauncher.launch(permissionsToCheck)
            }
        }
    }

    private fun openGallery() {
        PictureSelector.create(this)
            .openGallery(SelectMimeType.TYPE_IMAGE)
            .setImageEngine(GlideEngine.createGlideEngine())
            .setSelectionMode(SelectModeConfig.SINGLE)
            .isDisplayCamera(true)
            .forResult(object : OnResultCallbackListener<LocalMedia> {
                override fun onResult(result: ArrayList<LocalMedia?>?) {
                    val media = result?.firstOrNull() ?: return

                    // 获取路径并转为 Uri
                    val path = media.availablePath
                    val uri =
                        if (path.startsWith("content://")) Uri.parse(path) else Uri.fromFile(
                            File(path)
                        )

                    // 调用更新逻辑
                    updateAvatar(uri)
                }

                override fun onCancel() {
                    // 取消选择，不做任何操作
                }
            })
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("功能受限")
            .setMessage("由于你已彻底禁用权限，请前往设置手动开启。")
            .setPositiveButton("去设置") { _, _ ->
                // 跳转到系统设置页面
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("取消") { _, _ ->
                // 取消操作，不做任何操作
            }
            .show()
    }

    private suspend fun generateExcelFile(records: List<Record>): File? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("我的账单")

                // 创建表头
                val headerRow = sheet.createRow(0)
                val titles = listOf("日期", "类型", "分类", "账户", "金额", "备注")
                titles.forEachIndexed { i, t -> headerRow.createCell(i).setCellValue(t) }

                // 填充数据
                records.forEachIndexed { index, record ->
                    val row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue("${record.dateStr} ${record.timeStr}")
                    row.createCell(1).setCellValue(if (record.type == 0) "支出" else "收入")
                    row.createCell(2).setCellValue(record.categoryName)
                    row.createCell(3).setCellValue(record.paymentMethod)
                    row.createCell(4).setCellValue(record.amount.toDoubleOrNull() ?: 0.0)
                    row.createCell(5).setCellValue(record.remark)
                }

                // 保存到缓存目录
                val file =
                    File(requireContext().cacheDir, "账单导出_${System.currentTimeMillis()}.xlsx")
                file.outputStream().use { workbook.write(it) }
                workbook.close()
                file
            } catch (e: Exception) {
                null
            }
        }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "发送账单到..."))
    }

    private fun updateAvatar(uri: Uri) {
        val userAvatar = FileUtil.saveUserAvatar(requireContext(), uri)

        if (userAvatar != null) {
            lifecycleScope.launch {
                val isSuccess = userViewModel.saveUserAvatar(userAvatar)
                if (isSuccess) {
                    GlideEngine.createGlideEngine()
                        .loadImage(requireContext(), uri, binding.ivAvatar)
                }
            }
        } else {
            Toast.makeText(requireContext(), "更换头像失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        val dialogViewBinding = DialogLogoutBinding.inflate(layoutInflater)

        val dialog = AlertDialog
            .Builder(requireContext())
            .setView(dialogViewBinding.root)
            .create()

        // 去除直角背景，利用已有的圆角背景
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        with(dialogViewBinding) {
            tvCancel.setOnClickListener {
                dialog.dismiss()
            }
            tvConfirmLogout.setOnClickListener {
                // 清空当前用户的登录状态
                SpUtil.clear(requireContext())
                // 页面进行跳转
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    // 清空栈
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setDimAmount(0.5f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}