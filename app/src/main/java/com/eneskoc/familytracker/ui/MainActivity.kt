package com.eneskoc.familytracker.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.databinding.ActivityMainBinding
import com.eneskoc.familytracker.other.Constants.ACTION_SHOW_HOME_FRAGMENT
import com.eneskoc.familytracker.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        navigateToHomeFragmentIfNeeded(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToHomeFragmentIfNeeded(intent)
    }
    private fun navigateToHomeFragmentIfNeeded(intent: Intent?){
        if (intent?.action == ACTION_SHOW_HOME_FRAGMENT) {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.action_global_trackingHomeFragment)
        }
    }
}