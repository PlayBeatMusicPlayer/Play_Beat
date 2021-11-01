package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.ArtistsModel
import com.knesarcreation.playbeat.databinding.RecyclerAllSongsItemBinding

class AllArtistsAdapter(
    var context: Context,
    //var artistsList: List<ArtistsModel>,
    var listener: OnArtistClicked
) : ListAdapter<ArtistsModel, AllArtistsAdapter.ArtistsViewHolder>(DiffUtilArtistCallback())
/*RecyclerView.Adapter<AllArtistsAdapter.ArtistsViewHolder>() */ {

    class ArtistsViewHolder(binding: RecyclerAllSongsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val artistName = binding.songNameTV // same view as Artists name
        val noOfTracks = binding.artistNameTV
        val noOfAlbums = binding.albumNameTv
        val forwardIconIV = binding.forwardIconIV
        val artistAvatar = binding.albumArtIv
        val rlParentView = binding.rlAudio
        val sepratorView = binding.sepratorView
        val llArtistNameOrAlbumName = binding.llArtistNameOrAlbumName
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
        val artistsModel = getItem(position)
        holder.artistName.text = artistsModel.artistName
        //holder.noOfTracks.text = "${artistsModel.noOfTracks} Tracks"
        // holder.noOfAlbums.text = "${artistsModel.noOfAlbums} Albums"
        holder.llArtistNameOrAlbumName.visibility = View.GONE
        holder.noOfAlbums.visibility = View.GONE
        holder.sepratorView.visibility = View.GONE

        holder.artistAvatar.setImageResource(R.drawable.ic_artist)
        holder.forwardIconIV.visibility = View.VISIBLE
        holder.rlParentView.setOnClickListener {
            listener.getArtistData(artistsModel)
        }

    }

    class DiffUtilArtistCallback : DiffUtil.ItemCallback<ArtistsModel>() {
        override fun areItemsTheSame(oldItem: ArtistsModel, newItem: ArtistsModel) =
            oldItem.artistId == newItem.artistId

        override fun areContentsTheSame(oldItem: ArtistsModel, newItem: ArtistsModel) =
            oldItem == newItem
    }

    //override fun getItemCount() = artistsList.size
}