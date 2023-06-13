package com.eneskoc.familytracker.ui.notification

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.eneskoc.familytracker.databinding.FragmentHomeScreenBinding
import com.eneskoc.familytracker.databinding.RecyclerViewFollowingItemBinding
import com.eneskoc.familytracker.databinding.RecyclerViewNotificationItemBinding

class HomeScreenFollowingAdapter(var userDataList: List<UserDataHolder>) :
    RecyclerView.Adapter<HomeScreenFollowingAdapter.FollowingUserDataHolder>() {

    private var itemClickListener: FollowingAdapterOnItemClickListener? = null

    fun setOnItemClickListener(listener: FollowingAdapterOnItemClickListener) {
        this.itemClickListener = listener
    }

    inner class FollowingUserDataHolder(val binding: RecyclerViewFollowingItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingUserDataHolder {
        val binding = RecyclerViewFollowingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FollowingUserDataHolder(binding)
    }


    override fun onBindViewHolder(holder: FollowingUserDataHolder, position: Int) {

        val user = userDataList[position]
        holder.binding.tvUserDisplayName.text = user.displayName
        holder.binding.tvUsername.text = user.username
        holder.binding.tvBatteryLevel.text = user.batteryLevel.toString()

        holder.itemView.setOnLongClickListener {
            holder.binding.layoutFollowing.visibility = View.GONE
            holder.binding.layoutDeleteItem.visibility = View.VISIBLE
            holder.binding.tvDeleteMessage.text= "Do you want to unfollow ${user.displayName}?"
            true
        }

        holder.binding.btnCancel.setOnClickListener {
            holder.binding.layoutFollowing.visibility = View.VISIBLE
            holder.binding.layoutDeleteItem.visibility = View.GONE
        }

//        holder.binding.btnReject.setOnClickListener {
//            itemClickListener?.onRejectButtonClicked(user)
//        }
    }

    override fun getItemCount(): Int {
        return userDataList.size
    }

}

interface FollowingAdapterOnItemClickListener {
//    fun onAcceptButtonClicked(user: UserDataHolder)
//    fun onRejectButtonClicked(user: UserDataHolder)
}