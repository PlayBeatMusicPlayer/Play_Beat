package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AlbumModel
import com.knesarcreation.playbeat.databinding.RecyclerAllSongsItemBinding

class AllAlbumsAdapter(
    var context: Context,
    //var albumList: List<AlbumModel>,
    var listener: OnAlbumClicked
) : ListAdapter<AlbumModel, AllAlbumsAdapter.AlbumViewHolder>(DiffUtilAlbumDataCallback())
/*RecyclerView.Adapter<AllAlbumsAdapter.AlbumViewHolder>()*/ {

    var queryText = ""
    var isSearching = false

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
        val albumModel = getItem(position)

        if (isSearching) {
            //highlight and show the album name
            highlightSearchedAlbumText(queryText, albumModel, holder)
        } else {
            holder.albumName.text = albumModel.albumName
        }

        holder.artistName.text = albumModel.artistName

        holder.forwardIconIV.visibility = View.VISIBLE
        holder.songName.visibility = View.GONE
        holder.sepratorView.visibility = View.GONE

        val artUri = albumModel.artUri

        Glide.with(context).load(artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.album_png).centerCrop())
            .into(holder.albumIV)

        holder.rlParentView.setOnClickListener {
            Log.d("AlbumAdapterAlbumId", "onBindViewHolder: albumId: ${albumModel.albumId} ")
            listener.onClicked(albumModel)
        }
    }

    private fun highlightSearchedAlbumText(
        queryText: String,
        albumModel: AlbumModel,
        holder: AlbumViewHolder
    ) {
        if (queryText.isNotEmpty()) {
            val startPos = albumModel.albumName.lowercase().indexOf(queryText)
            val endPos = startPos + queryText.length

            if (startPos != -1) {
                val spannable = SpannableStringBuilder(albumModel.albumName)
                spannable.setSpan(
                    ForegroundColorSpan(Color.CYAN),
                    startPos,
                    endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.albumName.text = spannable
            } /*else {
                holder.albumName.text = albumModel.albumName
            }*/
        } /*else {
            holder.albumName.text = albumModel.albumName
        }*/
    }

    //override fun getItemCount() = albumList.size

    class DiffUtilAlbumDataCallback : DiffUtil.ItemCallback<AlbumModel>() {
        override fun areItemsTheSame(oldItem: AlbumModel, newItem: AlbumModel) =
            oldItem.albumId == newItem.albumId


        override fun areContentsTheSame(oldItem: AlbumModel, newItem: AlbumModel) =
            oldItem == newItem
    }
}