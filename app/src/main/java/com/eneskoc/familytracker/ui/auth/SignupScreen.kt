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
import com.eneskoc.familytracker.databinding.FragmentSignupScreenBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignupScreen : Fragment() {

    private val viewModel by viewModels<AuthViewModel>()
    private var _binding: FragmentSignupScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignupScreenBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.btnSignup.setOnClickListener {
            println("Deneme")
            val name=binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            viewModel.signup(name,email, password)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.signupFlow.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            // Başarı durumu, kullanıcı oturum açmıştır
                            println("Account Created")
                            findNavController().navigate(R.id.action_signupScreen_to_loginScreen)
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

        return view
    }
}