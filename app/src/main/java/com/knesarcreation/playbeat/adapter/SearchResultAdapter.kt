/*
package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.RecyclerAllSearchResultsBinding
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.model.SearchResultModel
import com.knesarcreation.playbeat.utils.StorageUtil
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class SearchResultAdapter(
    var context: Context,
    private var searchResultList: CopyOnWriteArrayList<SearchResultModel>,
    var userInput: String
) :
    RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {


    class SearchResultViewHolder(
        var binding: RecyclerAllSearchResultsBinding,
        var context: Context
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private var storageUtil = StorageUtil(context)
        private val mViewModelClass =
            ViewModelProvider(context as AppCompatActivity)[ViewModelClass::class.java]

        fun bind(
            searchResultModel: SearchResultModel,
            context: Context,
            pos: Int,
            userInput: String
        ) {
            when (pos) {
                0 -> {
                    binding.titleTV.text = "${searchResultModel.songList.size} Songs"
                    binding.iconIV.setImageResource(R.drawable.music_note_icon_teal)
                    val allSongsAdapter = AllSongsAdapter(
                        context,
                        AllSongsAdapter.OnClickListener { allSongModel, position ->
                           // onClickAudio(allSongModel, position, searchResultModel)
                        },
                        AllSongsAdapter.OnLongClickListener { _, _ ->

                        },
                        false
                    )

                    allSongsAdapter.isSearching = true
                    allSongsAdapter.queryText = userInput
                    binding.rvSearchResult.adapter = allSongsAdapter
                    allSongsAdapter.submitList(searchResultModel.songList.sortedBy { allSongsModel -> allSongsModel.songName })
                }
                1 -> {
                    binding.titleTV.text = "Albums"
                    binding.iconIV.setImageResource(R.drawable.ic_album_teal)

                }
                2 -> {
                    binding.titleTV.text = "Artist"
                    binding.iconIV.setImageResource(R.drawable.ic_artist_teal)

                }
            }


        }

        private fun onClickAudio(
            allSongModel: AllSongsModel,
            position: Int,
            searchResultModel: SearchResultModel
        ) {
            // Toast.makeText(activity as Context, "$position", Toast.LENGTH_SHORT).show()
            if (File(Uri.parse(allSongModel.data).path!!).exists()) {
                storageUtil.saveIsShuffled(false)
                val prevPlayingAudioIndex = storageUtil.loadAudioIndex()
                val prevQueueList = storageUtil.loadQueueAudio()
                val prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

                // restricting to update if clicked audio is same

                mViewModelClass.deleteQueue((context as AppCompatActivity).lifecycleScope)

                mViewModelClass.updateSong(
                    prevPlayingAudioModel.songId,
                    prevPlayingAudioModel.songName,
                    -1,
                    (context as AppCompatActivity).lifecycleScope
                )

                mViewModelClass.updateSong(
                    allSongModel.songId,
                    allSongModel.songName,
                    1,
                    (context as AppCompatActivity).lifecycleScope
                )

                playAudio(position, searchResultModel.songList)

                // adding queue list to DB and show highlight of current audio
                for (audio in searchResultModel.songList) {
                    val queueListModel = QueueListModel(
                        audio.songId,
                        audio.albumId,
                        audio.songName,
                        audio.artistsName,
                        audio.albumName,
                        audio.size,
                        audio.duration,
                        audio.data,
                        audio.contentUri,
                        audio.artUri,
                        -1,
                        audio.dateAdded,
                        audio.isFavourite,
                        audio.favAudioAddedTime,
                        audio.mostPlayedCount,
                        audio.artistId
                    )
                    queueListModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
                    mViewModelClass.insertQueue(
                        queueListModel,
                        (context as AppCompatActivity).lifecycleScope
                    )
                }

                mViewModelClass.updateQueueAudio(
                    prevPlayingAudioModel.songId,
                    prevPlayingAudioModel.songName,
                    -1,
                    (context as AppCompatActivity).lifecycleScope
                )

                mViewModelClass.updateQueueAudio(
                    allSongModel.songId,
                    allSongModel.songName,
                    1,
                    (context as AppCompatActivity).lifecycleScope
                )
            } else {
                Snackbar.make(
                    (context as AppCompatActivity).window.decorView,
                    "File doesn't exists", Snackbar.LENGTH_LONG
                ).show()
            }

        }

        private fun playAudio(audioIndex: Int, audioSearchList: ArrayList<AllSongsModel>) {
            //this.currentPlayingAudioIndex = audioIndex
            //store audio to prefs
            val searchList = CopyOnWriteArrayList<AllSongsModel>()
            searchList.addAll(audioSearchList)
            storageUtil.storeQueueAudio(searchList)
            //Store the new audioIndex to SharedPreferences
            storageUtil.storeAudioIndex(audioIndex)

            //Service is active send broadcast
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            (context as AppCompatActivity).sendBroadcast(broadcastIntent)

            */
/*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
            (activity as Context).sendBroadcast(updatePlayer)*//*

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder(
            RecyclerAllSearchResultsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            parent.context
        )
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(searchResultList[position], context, position, userInput)
    }

    override fun getItemCount() = searchResultList.size
}*/
