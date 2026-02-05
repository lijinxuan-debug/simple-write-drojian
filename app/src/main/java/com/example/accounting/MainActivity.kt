package com.example.accounting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.accounting.databinding.ActivityMainBinding
import com.example.accounting.ui.discovery.DiscoveryFragment
import com.example.accounting.ui.home.HomeFragment
import com.example.accounting.ui.profile.ProfileFragment
import com.example.accounting.ui.statistics.StatisticsFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container,fragment)
            .commit()
    }
}