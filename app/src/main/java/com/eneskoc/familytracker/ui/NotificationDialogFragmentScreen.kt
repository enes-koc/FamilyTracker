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
class NotificationDialogFragmentScreen : DialogFragment(), NotificationAdapterOnItemClickListener {

    private var _binding: NotificationDialogFragmentScreenBinding? = null
    private val binding get() = _binding!!

    private val authViewModel by viewModels<AuthViewModel>()
    private lateinit var adapter: NotificationDialogFragmentScreenAdapter

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

        adapter = NotificationDialogFragmentScreenAdapter(emptyList())
        adapter.setOnItemClickListener(this)
        binding.recyclerViewNotification.adapter = adapter
        listenFollowRequest()
    }

    override fun onAcceptButtonClicked(user: UserDataHolder) {

        authViewModel.acceptFollowRequest(user.uid)
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.acceptFollowRequest.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        listenFollowRequest()
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

    override fun onRejectButtonClicked(user: UserDataHolder) {
        authViewModel.rejectFollowRequest(user.uid)
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.rejectFollowRequest.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        listenFollowRequest()
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

    fun listenFollowRequest() {
        authViewModel.listenToFollowRequests()

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.listenToFollowRequestsFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        adapter.userDataList = resource.result
                        adapter.notifyDataSetChanged()

                        if (resource.result.isEmpty()) {
                            binding.tvResultMessage.visibility = View.VISIBLE
                            binding.recyclerViewNotification.visibility = View.GONE

                        } else {
                            binding.tvResultMessage.visibility = View.GONE
                            binding.recyclerViewNotification.visibility = View.VISIBLE
                        }
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