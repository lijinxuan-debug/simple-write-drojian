package com.example.accounting.engine

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.luck.picture.lib.engine.ImageEngine
import com.luck.picture.lib.utils.ActivityCompatHelper
import java.io.File

/**
 * PictureSelector 的图片加载引擎适配器
 * 重点：它是连接 图片选择器 和 Glide 的桥梁
 */
class GlideEngine private constructor() : ImageEngine {

    // 加载普通图片（预览时使用）
    override fun loadImage(context: Context, url: String, imageView: ImageView) {
        // 先检查context是否有效，即Activity是否被销毁。
        if (!ActivityCompatHelper.assertValidRequest(context)) return
        // 这里利用glide异步加载图片
        Glide.with(context).load(url).into(imageView)
    }

    override fun loadImage(
        context: Context?,
        imageView: ImageView?,
        url: String?,
        maxWidth: Int,
        maxHeight: Int
    ) {
        // 这里做个判空处理，防止 context 为空导致崩溃
        if (context == null || imageView == null || url == null) return
        // 跟上面一样先检查context是否有效
        if (!ActivityCompatHelper.assertValidRequest(context)) return

        //
        Glide.with(context)
            .load(url)
            .override(maxWidth, maxHeight)
            .into(imageView)
    }

    // 加载相册封面
    override fun loadAlbumCover(context: Context, url: String, imageView: ImageView) {
        // 先校验activity是否销毁
        if (!ActivityCompatHelper.assertValidRequest(context)) return

        Glide.with(context)
            .asBitmap()
            .load(url)
            .placeholder(android.R.color.darker_gray) // 加载占位图
            .transform(CenterCrop()) // 封面给个微圆角
            .into(imageView)
    }

    // 加载列表里的方格图片
    override fun loadGridImage(context: Context, url: String, imageView: ImageView) {
        if (!ActivityCompatHelper.assertValidRequest(context)) return

        Glide.with(context)
            .load(url)
            .override(200, 200) // 限制加载尺寸，极致省内存
            .centerCrop()
            .placeholder(android.R.color.darker_gray)
            .into(imageView)
    }

    // 暂停/恢复加载（优化滑动体验）
    override fun pauseRequests(context: Context) {
        Glide.with(context).pauseRequests()
    }

    // 滑动停止后开始加载
    override fun resumeRequests(context: Context) {
        Glide.with(context).resumeRequests()
    }

    fun loadImage(context: Context, file: File?, imageView: ImageView) {
        Glide.with(context)
            .load(file)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }

    fun loadImage(context: Context, file: Int, imageView: ImageView) {
        Glide.with(context)
            .load(file)
            .circleCrop()
            .into(imageView)
    }

    companion object {
        private var instance: GlideEngine? = null
        fun createGlideEngine(): GlideEngine {
            if (instance == null) instance = GlideEngine()
            return instance!!
        }
    }
}