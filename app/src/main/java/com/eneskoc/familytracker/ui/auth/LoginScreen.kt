package com.eneskoc.familytracker.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eneskoc.data.Resource
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.databinding.FragmentLoginScreenBinding
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
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

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            viewModel.login(email, password)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loginFlow.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            // Başarı durumu, kullanıcı oturum açmıştır
                            findNavController().navigate(R.id.action_loginScreen_to_homeScreen)
                        }
                        is Resource.Failure -> {
                            // Hata durumu, kullanıcı oturum açarken bir hata oluştu
                            val exception = resource.exception
                            // Hata işleme
                            println(exception)
                        }
                        is Resource.Loading -> {
                            // Yüklenme durumu, oturum açma işlemi devam ediyor
                        }
                        else -> {}
                    }
                }
            }
        }

        binding.textView.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_signupScreen)
        }

        return view
    }
}