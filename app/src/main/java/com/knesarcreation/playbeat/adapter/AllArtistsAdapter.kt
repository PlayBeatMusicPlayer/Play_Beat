package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.RecyclerAllSongsItemBinding
import com.knesarcreation.playbeat.model.ArtistsModel

class AllArtistsAdapter(
    var context: Context,
    var artistsList: List<ArtistsModel>,
    var listener: OnArtistClicked
) :
    RecyclerView.Adapter<AllArtistsAdapter.ArtistsViewHolder>() {

    class ArtistsViewHolder(binding: RecyclerAllSongsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val artistName = binding.songNameTV // same view as Artists name
        val noOfTracks = binding.artistNameTV
        val noOfAlbums = binding.albumNameTv
        val forwardIconIV = binding.forwardIconIV
        val artistAvatar = binding.albumArtIv
        val rlParentView = binding.rlAudio
        //val sepratorView = binding.sepratorView
    }

    interface OnArtistClicked {
        fun getArtistData(artistsModel: ArtistsModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistsViewHolder {
        return ArtistsViewHolder(
            RecyclerAllSongsItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ArtistsViewHolder, position: Int) {
        val artistsModel = artistsList[position]
        holder.artistName.text = artistsModel.artistName
        holder.noOfTracks.text = "${artistsModel.noOfTracks} Tracks"
        holder.noOfAlbums.text = "${artistsModel.noOfAlbums} Albums"

        holder.artistAvatar.setImageResource(R.drawable.artists_avatar)
        holder.forwardIconIV.visibility = View.VISIBLE
        holder.rlParentView.setOnClickListener {
            listener.getArtistData(artistsModel)
        }

    }

    override fun getItemCount() = artistsList.size
}