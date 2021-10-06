package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.adapter.PlaylistNamesAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetChoosePlaylistBinding

class BottomSheetChoosePlaylist(var allSongsModel: AllSongsModel) : BottomSheetDialogFragment(),
    PlaylistNamesAdapter.OnSelectPlaylist {

    private var _binding: BottomSheetChoosePlaylistBinding? = null
    private val binding get() = _binding
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var playlistNamesAdapter: PlaylistNamesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetChoosePlaylistBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        setUpRecyclerView()
        observerPlayList()

        binding?.llAddNewPlaylist?.setOnClickListener {
            val audioList = ArrayList<Long>()
            audioList.add(allSongsModel.songId)
            val audioJson = convertListToString(audioList)
            val bottomSheetCreatePlaylist =
                BottomSheetCreatePlaylist(activity as Context, audioJson)
            bottomSheetCreatePlaylist.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetCreatePlaylist"
            )
            dismiss()
        }

        binding?.llToFav?.setOnClickListener {
            if (allSongsModel.favAudioAddedTime == 0L) {
                mViewModelClass.updateFavouriteAudio(
                    true,
                    allSongsModel.songId,
                    System.currentTimeMillis(),
                    lifecycleScope
                )
                Toast.makeText(
                    activity as Context,
                    "Song added to favourites",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            } else {
                Toast.makeText(
                    activity as Context,
                    "Already added in favourites",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return view
    }

    private fun observerPlayList() {
        mViewModelClass.getAllPlaylists().observe(viewLifecycleOwner) {
            if (it != null) {
                playlistNamesAdapter.submitList(it)
            }
        }
    }

    private fun setUpRecyclerView() {
        playlistNamesAdapter = PlaylistNamesAdapter(this)
        binding?.rvPlaylistNames?.adapter = playlistNamesAdapter
    }

    override fun selectPlaylist(playlistModel: PlaylistModel) {
        /*val audioJson = getAudioJson(playlistModel)*/
        var duplicateData = false
        val audioJson: String = if (playlistModel.songIds != "") {
            val audioListFromJson = convertStringToList(playlistModel.songIds)
            duplicateData = if (audioListFromJson.contains(allSongsModel.songId)) {
                Toast.makeText(activity as Context, "Duplicate songs found", Toast.LENGTH_SHORT).show()
                true
            } else {
                audioListFromJson.add(allSongsModel.songId)
                false
            }
            convertListToString(audioListFromJson)
        } else {
            val audioList = ArrayList<Long>()
            audioList.add(allSongsModel.songId)
            convertListToString(audioList)
        }

        if (!duplicateData) {
            mViewModelClass.updatePlaylist(audioJson, playlistModel.id, lifecycleScope)
            Toast.makeText(
                activity as Context,
                "Song added to ${playlistModel.playlistName}.",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()

        }
    }

    /* private fun getAudioJson(playlistModel: PlaylistModel): String {

         return audioJson
     }*/

    private fun convertListToString(audioListFromJson: ArrayList<Long>): String {
        val gson = Gson()
        return gson.toJson(audioListFromJson)
    }

    private fun convertStringToList(audioList: String): ArrayList<Long> {
        val gson = Gson()
        val type = object : TypeToken<ArrayList<Long>>() {}.type
        return gson.fromJson(audioList, type)
    }
}