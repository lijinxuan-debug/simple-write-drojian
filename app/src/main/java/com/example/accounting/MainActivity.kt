package com.example.accounting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.accounting.databinding.ActivityMainBinding
import com.example.accounting.ui.discovery.DiscoveryFragment
import com.example.accounting.ui.home.HomeFragment
import com.example.accounting.ui.profile.ProfileFragment
import com.example.accounting.ui.statistics.StatisticsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 用 Tag 来标识 Fragment
    private val tagHome = "home"
    private val tagStatistics = "statistics"
    private val tagDiscovery = "discovery"
    private val tagProfile = "profile"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 默认显示首页
        if (savedInstanceState == null) {
            switchFragment(tagHome)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(tagHome)
                R.id.nav_statistics -> switchFragment(tagStatistics)
                R.id.nav_discovery -> switchFragment(tagDiscovery)
                R.id.nav_profile -> switchFragment(tagProfile)
            }
            true
        }
    }

    private fun switchFragment(tag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        // 寻找是否已经创建过这个 Fragment
        var fragment = supportFragmentManager.findFragmentByTag(tag)

        // 隐藏当前正在显示的 Fragment
        val currentFragment = supportFragmentManager.fragments.find { it.isVisible }
        currentFragment?.let { transaction.hide(it) }

        if (fragment == null) {
            // 如果没创建过，才根据 tag 创建实例
            fragment = when (tag) {
                tagHome -> HomeFragment()
                tagStatistics -> StatisticsFragment()
                tagDiscovery -> DiscoveryFragment()
                tagProfile -> ProfileFragment()
                else -> HomeFragment()
            }
            transaction.add(R.id.fragment_container, fragment, tag)
        } else {
            // 4. 如果创建过了，直接显示
            transaction.show(fragment)
        }

        transaction.commit()
    }
}