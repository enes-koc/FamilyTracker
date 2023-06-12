package com.eneskoc.familytracker.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.eneskoc.familytracker.databinding.RecyclerViewNotificationItemBinding

class NotificationDialogFragmentScreenAdapter(var userDataList: List<UserDataHolder>) :
    RecyclerView.Adapter<NotificationDialogFragmentScreenAdapter.UserNotificationDataHolder>() {

    private var itemClickListener: NotificationAdapterOnItemClickListener? = null

    fun setOnItemClickListener(listener: NotificationAdapterOnItemClickListener) {
        this.itemClickListener = listener
    }

    inner class UserNotificationDataHolder(val binding: RecyclerViewNotificationItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserNotificationDataHolder {
        val binding = RecyclerViewNotificationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserNotificationDataHolder(binding)
    }

    override fun onBindViewHolder(holder: UserNotificationDataHolder, position: Int) {

        val user = userDataList[position]
        holder.binding.tvUserDisplayName.text = user.displayName
        holder.binding.tvUsername.text = user.username

        holder.binding.btnAccept.setOnClickListener {
            itemClickListener?.onAcceptButtonClicked(user)
        }

        holder.binding.btnReject.setOnClickListener {
            itemClickListener?.onRejectButtonClicked(user)
        }
    }

    override fun getItemCount(): Int {
        return userDataList.size
    }
}

interface NotificationAdapterOnItemClickListener {
    fun onAcceptButtonClicked(user: UserDataHolder)
    fun onRejectButtonClicked(user: UserDataHolder)
}