package com.knesarcreation.playbeat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.databinding.RecyclerPlaylistItemsBinding

class PlaylistAdapter(var listener: OnPlaylistClicked) :
    ListAdapter<PlaylistModel, PlaylistAdapter.PlayListViewHolder>(PlayListItemCallback()) {

    interface OnPlaylistClicked {
        fun onClicked(playlistModel: PlaylistModel)
    }

    class PlayListViewHolder(binding: RecyclerPlaylistItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val playListIV = binding.playListIV
        private val playLisName = binding.playlistName
        private val songCountTV = binding.songCountTV
        val rlPlaylistContainer = binding.rlPlaylistContainer

        fun bind(playlistModel: PlaylistModel) {
            playLisName.text = playlistModel.playlistName
            var songIdsList = ArrayList<Long>()
            if (playlistModel.songIds != "") {
                songIdsList = convertStringToList(playlistModel.songIds)
            }
            songCountTV.text = "${songIdsList.count()} Songs"
        }

        private fun convertStringToList(songId: String): ArrayList<Long> {
            val gson = Gson()
            val type = object : TypeToken<ArrayList<Long>>(){}.type
            return gson.fromJson(songId, type)
        }
    }

    class PlayListItemCallback : DiffUtil.ItemCallback<PlaylistModel>() {
        override fun areItemsTheSame(oldItem: PlaylistModel, newItem: PlaylistModel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PlaylistModel,
            newItem: PlaylistModel
        ) = oldItem == newItem

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        return PlayListViewHolder(
            RecyclerPlaylistItemsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.rlPlaylistContainer.setOnClickListener {
            listener.onClicked(getItem(position))
        }
    }
}