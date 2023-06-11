package com.eneskoc.familytracker.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.data.Resource
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.eneskoc.familytracker.databinding.UserSearchScreenBinding
import com.eneskoc.familytracker.ui.auth.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserSearchScreen : DialogFragment() {

    private var _binding: UserSearchScreenBinding? = null
    private val binding get() = _binding!!

    private val authViewModel by viewModels<AuthViewModel>()

    private lateinit var userData : UserDataHolder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(R.layout.user_search_screen)

        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams?.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = UserSearchScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.findUser(binding.tvSearch.text.toString())

        binding.tvSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Değişiklik öncesi yapılan işlemler
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.layoutSearchUser.visibility=View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
                val searchText = s?.toString()
                if(searchText?.length!! >=3){
                    authViewModel.findUser(searchText!!)
                    viewLifecycleOwner.lifecycleScope.launch {
                        authViewModel.findUserFlow.collect { resource ->
                            when (resource) {
                                is Resource.Success -> {
                                    userData= UserDataHolder(resource.result.uid,resource.result.displayName,resource.result.username)

                                    binding.layoutSearchUser.visibility=View.VISIBLE
                                    binding.tvResultMessage.visibility=View.GONE

                                    binding.tvUserDisplayName.text = userData.displayName
                                    binding.tvUserUsername.text = userData.username
                                }
                                is Resource.Failure -> {
                                    val exception = resource.exception
                                    binding.layoutSearchUser.visibility=View.GONE
                                    binding.tvResultMessage.visibility=View.VISIBLE

                                    binding.tvResultMessage.text = exception.message
                                }
                                is Resource.Loading -> {}
                                else -> {}
                            }
                        }
                    }
                }
            }
        })


        binding.btnRequest.setOnClickListener {
            authViewModel.sendFollowRequest(userData.uid)

            viewLifecycleOwner.lifecycleScope.launch {
                authViewModel.sendFollowRequestFlow.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            Snackbar.make(
                                requireView(),
                                "You send follow request",
                                Snackbar.LENGTH_LONG
                            ).show()
                            //dismiss()
                        }
                        is Resource.Failure -> {
                            val exception = resource.exception
                            Snackbar.make(
                                requireView(),
                                exception.message.toString(),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        is Resource.Loading -> {}
                        else -> {}
                    }
                }
            }
        }

    }
}