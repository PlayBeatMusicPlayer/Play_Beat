package com.knesarcreation.playbeat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.databinding.RecyclerPlaylistItemsNamesBinding

class PlaylistNamesAdapter(var listener: OnSelectPlaylist) :
    ListAdapter<PlaylistModel, PlaylistNamesAdapter.PlaylistNamesViewHolder>(PlaylistAdapter.PlayListItemCallback()) {

    interface OnSelectPlaylist {
        fun selectPlaylist(playlistModel: PlaylistModel)
    }

    class PlaylistNamesViewHolder(binding: RecyclerPlaylistItemsNamesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val playlistName = binding.playlistNameTV
        val llAddNewPlaylist = binding.llAddNewPlaylist

        fun bind(playlistModel: PlaylistModel) {
            playlistName.text = playlistModel.playlistName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistNamesViewHolder {
        return PlaylistNamesViewHolder(
            RecyclerPlaylistItemsNamesBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PlaylistNamesViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.llAddNewPlaylist.setOnClickListener {
            listener.selectPlaylist(getItem(position))
        }

    }

}