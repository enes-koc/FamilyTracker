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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSignup.setOnClickListener {
            println("Buton çalıştı")
            val name=binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            viewModel.signup(name,email, password)

            viewLifecycleOwner.lifecycleScope.launch {
                println("Launch çalıştı")
                viewModel.signupFlow.collectLatest { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            Snackbar.make(view, "Your account has been successfully created", Snackbar.LENGTH_LONG).show()
                            findNavController().navigate(R.id.action_signupScreen_to_loginScreen)
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

        binding.tvLoginMessage.setOnClickListener {
            findNavController().navigate(R.id.action_signupScreen_to_loginScreen)
        }
    }
}