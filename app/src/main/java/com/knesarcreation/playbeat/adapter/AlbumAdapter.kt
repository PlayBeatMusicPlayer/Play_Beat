package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.databinding.RecyclerAlbumItemsBinding
import com.knesarcreation.playbeat.model.AllSongsModel
import java.util.concurrent.CopyOnWriteArrayList

class AlbumAdapter(
    var context: Context,
    var audioList: CopyOnWriteArrayList<AllSongsModel>,
    var listener: OnAlbumSongClicked
) :
    RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(binding: RecyclerAlbumItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val audioNameTv = binding.audioName
        val artistNameTV = binding.artistNameTV
        val rlAudio = binding.rlAudio
    }

    interface OnAlbumSongClicked {
        fun onAudioPlayed(audioModel: AllSongsModel, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        return AlbumViewHolder(
            RecyclerAlbumItemsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val audioModel = audioList[position]
        holder.audioNameTv.text = audioModel.songName
        holder.artistNameTV.text = audioModel.artistsName
        holder.rlAudio.setOnClickListener {
           listener.onAudioPlayed(audioModel,position)
        }


    }

    override fun getItemCount() = audioList.size
}