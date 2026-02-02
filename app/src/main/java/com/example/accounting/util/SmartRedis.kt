package com.example.accounting.util

import android.util.LruCache

object SmartRedis {
    // 参数是缓存的最大容量。对于简单的 String，我们可以设定存储 100 条验证码
    private const val cacheSize = 10
    private val lruCache = LruCache<String, Pair<String, Long>>(cacheSize)

    /**
     * 存入验证码
     */
    fun set(key: String, value: String, expiryInSeconds: Long = 60) {
        val expiryTime = System.currentTimeMillis() + (expiryInSeconds * 1000)

        // 存入数据。如果超过 100 条，最老的那条会被自动踢出内存
        lruCache.put(key, value to expiryTime)
    }

    /**
     * 获取验证码
     */
    fun get(key: String): String? {
        val data = lruCache.get(key) ?: return null

        // 检查是否过期
        return if (System.currentTimeMillis() > data.second) {
            lruCache.remove(key) // 过期清理
            null
        } else {
            data.first
        }
    }
}