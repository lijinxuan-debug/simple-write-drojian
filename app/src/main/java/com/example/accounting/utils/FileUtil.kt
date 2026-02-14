package com.example.accounting.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtil {

    private const val TAG = "FileUtils"
    private const val AVATAR_DIR = "avatars"

    // 保存用户图片
    fun saveUserAvatar(context: Context, sourcePath: String?): String? {
        if (sourcePath.isNullOrBlank()) return null

        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return null

            // 获取私有目录
            val folder = File(context.filesDir, AVATAR_DIR)
            if (!folder.exists()) {
                folder.mkdirs() // 如果文件夹不存在则创建
            }

            // 2. 生成唯一文件名：以时间戳命名，防止覆盖
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            val destFile = File(folder, fileName)

            // 3. 拷贝文件 (使用 Kotlin 扩展函数 copyTo)
            sourceFile.inputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            return fileName // 存入数据库的是这个名字

        } catch (e: Exception) {
            Log.e(TAG, "保存头像异常", e)
            return null
        }
    }

    /**
     * 获取头像的完整 File 对象，供 Glide 使用
     * @param fileName 数据库中存储的文件名
     */
    fun getAvatarFile(context: Context, fileName: String?): File? {
        if (fileName.isNullOrBlank()) return null
        val file = File(File(context.filesDir, AVATAR_DIR), fileName)
        return if (file.exists()) file else null
    }

    /**
     * 将图片复制到私有目录
     * @param originPaths 原始路径列表（相册路径）
     * @return 存储在私有目录后的新路径列表
     */
    fun copyImagesToPrivateStorage(context: Context, originPaths: List<String>): List<String> {
        val newPathList = mutableListOf<String>()

        // 1. 获取私有目录下的 images 文件夹 (如果没有则创建)
        val projectDir = File(context.filesDir, "bill_images")
        if (!projectDir.exists()) projectDir.mkdirs()

        originPaths.forEach { originPath ->
            val originFile = File(originPath)
            if (originFile.exists()) {
                // 2. 生成新的文件名（防止重名，建议用时间戳或原名）
                val newFileName = "IMG_${System.currentTimeMillis()}_${originFile.name}"
                val destFile = File(projectDir, newFileName)

                try {
                    // 3. 利用 Kotlin 的 File 扩展函数直接复制（非常简单）
                    originFile.copyTo(destFile, true)

                    // 4. 将新的私有目录路径存入列表
                    newPathList.add(destFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return newPathList
    }

}