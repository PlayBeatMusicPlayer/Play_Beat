package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.PlaylistModel

class PlaylistAdapter(
    val context: Context,
    var listener: OnPlaylistClicked,
    var dataset: ArrayList<PlaylistModel>
) :
    DragDropSwipeAdapter<PlaylistModel, PlaylistAdapter.PlayListViewHolder>(dataset) {

    interface OnPlaylistClicked {
        fun onClicked(playlistModel: PlaylistModel)
    }

    inner class PlayListViewHolder(view: View) :
        DragDropSwipeAdapter.ViewHolder(view) {
        private val playListIV: ImageView = view.findViewById(R.id.playListIV)
         val forwardIconIV: ImageView = view.findViewById(R.id.forwardIconIV)
        private val playLisName: TextView = view.findViewById(R.id.playlistName)
        private val songCountTV: TextView = view.findViewById(R.id.songCountTV)
        val rlPlaylistContainer: RelativeLayout = view.findViewById(R.id.rlPlaylistItemContainer)

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
            val type = object : TypeToken<ArrayList<Long>>() {}.type
            return gson.fromJson(songId, type)
        }
    }

     class PlayListItemCallback : DiffUtil.ItemCallback<PlaylistModel>() {
         override fun areItemsTheSame(oldItem: PlaylistModel, newItem: PlaylistModel) =
             oldItem.id == newItem.id

         override fun areContentsTheSame(
             oldItem: PlaylistModel,
             newItem: PlaylistModel
         ) = oldItem.playlistName == newItem.playlistName

     }

    /*override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        return PlayListViewHolder(
            RecyclerPlaylistItemsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }*/

    override fun getViewHolder(itemView: View) = PlayListViewHolder(itemView)

    override fun getViewToTouchToStartDraggingItem(
        item: PlaylistModel,
        viewHolder: PlayListViewHolder,
        position: Int
    ): View? {
        return null
    }

    override fun onBindViewHolder(
        item: PlaylistModel,
        viewHolder: PlayListViewHolder,
        position: Int
    ) {
        viewHolder.bind(item)
        viewHolder.rlPlaylistContainer.setOnClickListener {
            listener.onClicked(item)
        }
    }

    /* override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
         holder.bind(getItem(position))
         holder.rlPlaylistContainer.setOnClickListener {
             listener.onClicked(getItem(position))
         }
     }*/
}