package com.example.accounting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.accounting.databinding.ActivityMainBinding
import com.example.accounting.ui.discovery.DiscoveryFragment
import com.example.accounting.ui.home.HomeFragment
import com.example.accounting.ui.profile.ProfileFragment
import com.example.accounting.ui.statistics.StatisticsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 当前屏幕显现的fragment
    private var activityFragment : Fragment ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            switchFragment(HomeFragment())
        }

        binding.bottomNavigation.setOnItemSelectedListener{ item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_statistics -> switchFragment(StatisticsFragment())
                R.id.nav_discovery -> switchFragment(DiscoveryFragment())
                R.id.nav_profile -> switchFragment(ProfileFragment())
            }
            true
        }
    }

    private fun switchFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()

        if (fragment.isAdded) {
            // 如果添加过了，直接隐藏旧的显示新fragment即可
            activityFragment?.let { transaction.hide(it) }
            transaction.show(fragment)
        } else {
            // 不管有无添加，都需先隐藏
            activityFragment?.let { transaction.hide(it) }
            // 没添加过则先添加
            transaction.add(R.id.fragment_container, fragment).show(fragment)
        }

        // 切换为当前显示的fragment
        activityFragment = fragment
        transaction.commit()
    }
}