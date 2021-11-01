package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetPlaylistOptionsMenuBinding
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class BottomSheetPlaylistMoreOptions(var playlistModel: PlaylistModel) :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaylistOptionsMenuBinding? = null
    private val binding get() = _binding
    private var songsIdList = ArrayList<Long>()
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var playlistAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private var currentPlayingAudioIndex = 0
    var listener: OnPlaylistMoreOptionsClicked? = null

    interface OnPlaylistMoreOptionsClicked {
        fun playNext(playlistAudioList: CopyOnWriteArrayList<AllSongsModel>)
        fun addToPlayingQueue(playlistAudioList: CopyOnWriteArrayList<AllSongsModel>)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnPlaylistMoreOptionsClicked
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetPlaylistOptionsMenuBinding.inflate(inflater, container, false)
        val view = binding?.root

        storage = StorageUtil(activity as Context)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        convertStringToList()

        setUpViews()

        getPlaylistAudios()

        playPlaylistAudio()

        deletePlaylist()

        renamePlaylist()

        addToPlaylist()

        binding!!.llAddToQueue.setOnClickListener {
            listener?.addToPlayingQueue(playlistAudioList)
        }

        binding!!.llPlayNext.setOnClickListener {
            listener!!.playNext(playlistAudioList)
        }

        return view
    }

    private fun addToPlaylist() {
        binding?.llAddToPlaylist?.setOnClickListener {
            val bottomSheetChooseToPlaylist =
                BottomSheetChoosePlaylist(null, false, songsIdList)
            bottomSheetChooseToPlaylist.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetChooseToPlaylist"
            )
            dismiss()
        }
    }

    private fun renamePlaylist() {
        binding!!.llRename.setOnClickListener {
            val bottomSheetCreateOrRenamePlaylist =
                BottomSheetCreateOrRenamePlaylist(activity as Context, null, false, playlistModel)
            bottomSheetCreateOrRenamePlaylist.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetCreateOrRenamePlaylist"
            )
            dismiss()
        }
    }

    private fun deletePlaylist() {
        binding!!.llDelete.setOnClickListener {
            val alertDialog = AlertDialog.Builder(activity as Context, R.style.CustomAlertDialog)
            val viewGroup: ViewGroup =
                (activity as AppCompatActivity).findViewById(android.R.id.content)
            val customView = layoutInflater.inflate(R.layout.custom_alert_dialog, viewGroup, false)
            val dialogTitleTV = customView.findViewById<TextView>(R.id.dialogTitleTV)
            val dialogMessageTV = customView.findViewById<TextView>(R.id.dialogMessageTV)
            val cancelButton = customView.findViewById<MaterialButton>(R.id.cancelButton)
            val deleteBtn = customView.findViewById<MaterialButton>(R.id.positiveBtn)
            alertDialog.setView(customView)
            val dialog = alertDialog.create()
            deleteBtn.setOnClickListener {
                mViewModelClass.deletePlaylist(playlistModel.id, lifecycleScope)
                dialog.dismiss()

            }
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            dialogTitleTV.text = "Delete playlist"
            dialogMessageTV.text =
                "Are you sure you want to delete '${playlistModel.playlistName}' playlist"
            dialog.show()
            dismiss()//dismiss bottom sheet
        }
    }

    private fun getPlaylistAudios() {
        /*audioList = storage.loadAudio()
        for (songId in songsIdList) {
            for (audio in audioList) {
                if (audio.songId == songId) {
                    playlistAudioList.add(audio)
                    break
                }
            }
        }*/

        lifecycleScope.launch {
            val audio = mViewModelClass.getRangeOfPlaylistAudio(songsIdList)
            playlistAudioList.addAll(audio)
            // audio is sorted then get the sorted audio
            if (playlistAudioList.isNotEmpty()) {
                val sortedAudio = getSortedAudio(playlistAudioList)
                playlistAudioList.clear()
                playlistAudioList.addAll(sortedAudio)
                Log.d("sortedAudioPlaylistAudioList", "getPlaylistAudios: $playlistAudioList ")
            }
        }
    }

    private fun playPlaylistAudio() {
        binding?.llPlayPlaylist?.setOnClickListener {
            if (playlistAudioList.isNotEmpty()) {
                onClickAudio(playlistAudioList[0])
            }
        }
    }

    private fun convertStringToList() {
        if (playlistModel.songIds.isNotEmpty()) {
            val gson = Gson()
            val type = object : TypeToken<ArrayList<Long>>() {}.type
            songsIdList = gson.fromJson(playlistModel.songIds, type)
        }
    }

    private fun setUpViews() {
        binding?.playlistNameTV?.text = playlistModel.playlistName

        if (songsIdList.size > 0)
            binding?.totalAudioTV?.text = "${songsIdList.size} Songs"
        else
            binding?.totalAudioTV?.text = "${songsIdList.size} Song"

        //val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        //Glide.with(binding?.playlistArtIV!!).load(allSongsModel.artUri)
        //   .transition(DrawableTransitionOptions.withCrossFade(factory)).apply(
        //      RequestOptions.placeholderOf(
        //         R.drawable.music_note_icon
        //    )
        // ).into(binding?.albumArtIv!!)
    }

    private fun onClickAudio(
        allSongModel: AllSongsModel,
    ) {
        storage.saveIsShuffled(false)
        val prevPlayingAudioIndex = storage.loadAudioIndex()
        val prevQueueList = storage.loadQueueAudio()
        val prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

        mViewModelClass.deleteQueue(lifecycleScope)

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

        playAudio()

        // adding queue list to DB and show highlight of current audio
        for (audio in this.playlistAudioList) {
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
            mViewModelClass.insertQueue(queueListModel, lifecycleScope)
        }

        mViewModelClass.updateQueueAudio(
            prevPlayingAudioModel.songId,
            prevPlayingAudioModel.songName,
            -1,
            lifecycleScope
        )

        mViewModelClass.updateQueueAudio(
            allSongModel.songId,
            allSongModel.songName,
            1,
            lifecycleScope
        )
    }

    private fun playAudio() {
        this.currentPlayingAudioIndex = 0
        //store audio to prefs

        //val playlist = CopyOnWriteArrayList<AllSongsModel>()
        //playlist.addAll(playlistAudioList)
        storage.storeQueueAudio(playlistAudioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(0)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        dismiss()

    }

    private fun getSortedAudio(
        audio: CopyOnWriteArrayList<AllSongsModel>
    ): ArrayList<AllSongsModel> {
        val sortedPlaylistAudio = ArrayList<AllSongsModel>()
        val sortedAudio: List<AllSongsModel>
        when (storage.getAudioSortedValue(playlistModel.playlistName)) {
            "Name" -> {
                sortedAudio = audio.sortedBy { allSongsModel -> allSongsModel.songName }
            }

            "DateAdded" -> {
                sortedAudio =
                    audio.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
            }

            "ArtistName" -> {
                sortedAudio = audio.sortedBy { allSongsModel -> allSongsModel.artistsName }
            }

            else -> {
                sortedAudio = audio.sortedBy { allSongsModel -> allSongsModel.songName }
            }
        }
        sortedPlaylistAudio.addAll(sortedAudio)
        return sortedPlaylistAudio
    }

}