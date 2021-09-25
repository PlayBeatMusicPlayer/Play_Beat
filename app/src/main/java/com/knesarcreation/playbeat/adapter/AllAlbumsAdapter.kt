package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.RecyclerAllSongsItemBinding
import com.knesarcreation.playbeat.model.AlbumModel

class AllAlbumsAdapter(
    var context: Context,
    var albumList: List<AlbumModel>,
    var listener: OnAlbumClicked
) :
    RecyclerView.Adapter<AllAlbumsAdapter.AlbumViewHolder>() {

    class AlbumViewHolder(binding: RecyclerAllSongsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val albumName = binding.songNameTV // same view as album name
        val artistName = binding.artistNameTV
        val forwardIconIV = binding.forwardIconIV
        val albumIV = binding.albumArtIv
        val sepratorView = binding.sepratorView
        val rlParentView = binding.rlAudio
        val songName = binding.albumNameTv // same view as song name
    }

    interface OnAlbumClicked {
        fun onClicked(albumModel: AlbumModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        return AlbumViewHolder(
            RecyclerAllSongsItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val albumModel = albumList[position]
        holder.albumName.text = albumModel.albumName
        holder.artistName.text = albumModel.artistName

        holder.forwardIconIV.visibility = View.VISIBLE
        holder.songName.visibility = View.GONE
        holder.sepratorView.visibility = View.GONE

        val artUri = albumModel.artUri

        Glide.with(context).load(artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.album_png).centerCrop())
            .into(holder.albumIV)

        holder.rlParentView.setOnClickListener {
            Log.d("AlbumAdapterAlbumId", "onBindViewHolder: albumId: ${albumModel.id} ")
            listener.onClicked(albumModel)
        }
    }

    override fun getItemCount() = albumList.size
}