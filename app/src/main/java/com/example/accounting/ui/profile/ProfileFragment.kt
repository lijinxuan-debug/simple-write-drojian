package com.example.accounting.ui.profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.accounting.R
import com.example.accounting.databinding.FragmentProfileBinding
import com.example.accounting.engine.GlideEngine
import com.example.accounting.ui.LoginActivity
import com.example.accounting.utils.FileUtil
import com.example.accounting.utils.SpUtil
import com.example.accounting.viewmodel.UserViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding ?= null

    private val binding get() = _binding!!

    private lateinit var userViewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // 检查用户头像是否存在，不存在则直接调用默认头像
        val userId = SpUtil.getUserId(requireContext())

        userViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                if (user.avatar == "default_avatar") {
                    GlideEngine.createGlideEngine().loadImage(requireContext(),R.drawable.img,binding.ivAvatar)
                } else {
                    // 那就是已经存储了图片，直接获取即可
                    val avatarFile = FileUtil.getAvatarFile(requireContext(), user.avatar)
                    // 使用glide加载图片
                    GlideEngine.createGlideEngine().loadImage(requireContext(),avatarFile,binding.ivAvatar)
                }
                // 头像处理之后，用户的账号也需要显现
                binding.tvUsername.text = user.email
            } else {
                Log.e("不存在对象","可能发生错误")
            }
        }

        // 获取相关用户信息
        userViewModel.getUserInfo(userId)

        // 监听退出登录
        binding.btnLogout.setOnClickListener {
            logout()
        }

    }

    private fun logout() {
        // 清空当前用户的登录状态
        SpUtil.clear(requireContext())
        // 页面进行跳转
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            // 清空栈
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroy()
        _binding = null
    }
}