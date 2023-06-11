package com.eneskoc.familytracker.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.data.Resource
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.eneskoc.familytracker.databinding.NotificationDialogFragmentScreenBinding
import com.eneskoc.familytracker.ui.auth.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class NotificationDialogFragmentScreen : DialogFragment() , NotificationAdapterOnItemClickListener{

    private var _binding: NotificationDialogFragmentScreenBinding? = null
    private val binding get() = _binding!!

    private val authViewModel by viewModels<AuthViewModel>()
    private lateinit var userDataList: List<UserDataHolder>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(R.layout.notification_dialog_fragment_screen)

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
        _binding = NotificationDialogFragmentScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.listenToFollowRequests()

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.listenToFollowRequestsFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        userDataList=resource.result
                        val notificationDialogFragmentAdapter = NotificationDialogFragmentScreenAdapter(userDataList)
                        notificationDialogFragmentAdapter.setOnItemClickListener(this@NotificationDialogFragmentScreen)
                        binding.recyclerViewNotification.adapter=notificationDialogFragmentAdapter
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

    override fun onAcceptButtonClicked(user: UserDataHolder) {
        println("Accept = ${user.displayName}")
    }

    override fun onRejectButtonClicked(user: UserDataHolder) {
        println("Reject = ${user.displayName}")
    }

}