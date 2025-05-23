package com.knesarcreation.playbeat.adapter.song

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil
import java.util.logging.Handler

class SimpleSongAdapter(
    context: FragmentActivity,
    songs: ArrayList<Song>,
    layoutRes: Int,
    ICabHolder: ICabHolder?
) : SongAdapter(context, songs, layoutRes, ICabHolder) {

    override fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = dataSet.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val fixedTrackNumber = MusicUtil.getFixedTrackNumber(dataSet[position].trackNumber)

        holder.imageText?.text = if (fixedTrackNumber > 0) fixedTrackNumber.toString() else "-"
        holder.time?.text = MusicUtil.getReadableDurationString(dataSet[position].duration)
    }

    override fun getItemCount(): Int {
        return if (dataSet.size > 5) 5 else dataSet.size
    }
}
