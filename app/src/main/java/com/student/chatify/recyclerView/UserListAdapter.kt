package com.student.chatify.recyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.student.chatify.R
import com.student.chatify.model.User

class UserListAdapter(
    private val onItemClick: (User) -> Unit
) : ListAdapter<User, UserListAdapter.UserViewHolder>(DiffCallback()) {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.profileImageView)
        val name: TextView = view.findViewById(R.id.displayName)
        val status: TextView = view.findViewById(R.id.statusMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_row, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.name.text = user.displayName
        holder.status.text = user.statusMessage

        Glide.with(holder.avatar.context)
            .load(user.profileImage)
            .placeholder(R.drawable.ic_default_profile)
            .circleCrop()
            .into(holder.avatar)

        holder.itemView.setOnClickListener { onItemClick(user) }
    }

    class DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(old: User, new: User) = old.uid == new.uid
        override fun areContentsTheSame(old: User, new: User) = old == new
    }
}
