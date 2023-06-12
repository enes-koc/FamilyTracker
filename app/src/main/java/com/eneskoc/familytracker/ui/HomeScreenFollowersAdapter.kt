package com.eneskoc.familytracker.ui.notification

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.eneskoc.familytracker.databinding.FragmentHomeScreenBinding
import com.eneskoc.familytracker.databinding.RecyclerViewFollowersItemBinding
import com.eneskoc.familytracker.databinding.RecyclerViewFollowingItemBinding
import com.eneskoc.familytracker.databinding.RecyclerViewNotificationItemBinding

class HomeScreenFollowersAdapter(var userDataList: List<UserDataHolder>) :
    RecyclerView.Adapter<HomeScreenFollowersAdapter.FollowersUserDataHolder>() {

    private var itemClickListener: FollowersAdapterOnItemClickListener? = null

    fun setOnItemClickListener(listener: FollowersAdapterOnItemClickListener) {
        this.itemClickListener = listener
    }

    inner class FollowersUserDataHolder(val binding: RecyclerViewFollowersItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowersUserDataHolder {
        val binding = RecyclerViewFollowersItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FollowersUserDataHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowersUserDataHolder, position: Int) {

        val user = userDataList[position]
        holder.binding.tvUserDisplayName.text = user.displayName
        holder.binding.tvUsername.text = user.username

//        holder.binding.btnReject.setOnClickListener {
//            itemClickListener?.onRejectButtonClicked(user)
//        }
    }

    override fun getItemCount(): Int {
        return userDataList.size
    }

}

interface FollowersAdapterOnItemClickListener {
//    fun onAcceptButtonClicked(user: UserDataHolder)
//    fun onRejectButtonClicked(user: UserDataHolder)
}