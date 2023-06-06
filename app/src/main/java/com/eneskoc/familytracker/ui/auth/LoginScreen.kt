package com.eneskoc.familytracker.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eneskoc.data.Resource
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.databinding.FragmentLoginScreenBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginScreen : Fragment() {

    private val viewModel by viewModels<AuthViewModel>()
    private var _binding: FragmentLoginScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLoginScreenBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            viewModel.login(email, password)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loginFlow.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            findNavController().navigate(R.id.action_loginScreen_to_homeScreen)
                        }
                        is Resource.Failure -> {
                            val exception = resource.exception
                            Snackbar.make(view, exception.message.toString(), Snackbar.LENGTH_LONG).show()
                        }
                        is Resource.Loading -> {

                        }
                        else -> {}
                    }
                }
            }
        }

        binding.tvSignupMessage.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_signupScreen)
        }
    }
}