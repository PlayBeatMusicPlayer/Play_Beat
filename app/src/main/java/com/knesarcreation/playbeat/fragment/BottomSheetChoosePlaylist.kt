package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.adapter.PlaylistNamesAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetChoosePlaylistBinding

class BottomSheetChoosePlaylist(
    private var allSongsModel: AllSongsModel?,
    private var singleSelectionAudio: Boolean,
    private var songIdsList: ArrayList<Long>?
) : BottomSheetDialogFragment(),
    PlaylistNamesAdapter.OnSelectPlaylist {

    private var _binding: BottomSheetChoosePlaylistBinding? = null
    private val binding get() = _binding
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var playlistNamesAdapter: PlaylistNamesAdapter
    private var audioIdsList = ArrayList<Long>()
    var listener: PlaylistSelected? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetChoosePlaylistBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        audioIdsList.addAll(audioIdsList)

        setUpRecyclerView()
        observerPlayList()

        Log.d("BottomSheetChoosePlaylist", "onCreateView:$songIdsList ")

        binding?.llAddNewPlaylist?.setOnClickListener {
            val audioList = ArrayList<Long>()
            if (singleSelectionAudio) {
                audioList.add(allSongsModel!!.songId)
            } else {
                if (songIdsList != null) {
                    for (songId in songIdsList!!) {
                        audioList.add(songId)
                    }
                    if (listener != null)
                        listener!!.onSelected()
                }
            }
            if (audioList.isNotEmpty()) {
                val audioJson = convertListToString(audioList)

                val bottomSheetCreatePlaylist =
                    BottomSheetCreateOrRenamePlaylist(activity as Context, audioJson, true, null)
                bottomSheetCreatePlaylist.show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    "bottomSheetCreatePlaylist"
                )
            } else {
                Snackbar.make(
                    (activity as AppCompatActivity).window.decorView,
                    "No audio found in this playlist", Snackbar.LENGTH_LONG
                ).show()

            }
            dismiss()
        }

        binding?.llToFav?.setOnClickListener {
            if (singleSelectionAudio) {
                if (allSongsModel!!.favAudioAddedTime == 0L) {
                    mViewModelClass.updateFavouriteAudio(
                        true,
                        allSongsModel!!.songId,
                        System.currentTimeMillis(),
                        lifecycleScope
                    )
                    Snackbar.make(
                        (activity as AppCompatActivity).window.decorView,
                        "Song added to favourites", Snackbar.LENGTH_LONG
                    ).show()
                    dismiss()
                } else {
                    Snackbar.make(
                        (activity as AppCompatActivity).window.decorView,
                        "Already added in favourites", Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                if (songIdsList != null) {
                    for (songId in songIdsList!!) {
                        mViewModelClass.updateFavouriteAudio(
                            true,
                            songId,
                            System.currentTimeMillis(),
                            lifecycleScope
                        )
                        Snackbar.make(
                            (activity as AppCompatActivity).window.decorView,
                            "Song added to favourites", Snackbar.LENGTH_LONG
                        ).show()

                        dismiss()
                    }
                    if (listener != null)
                        listener!!.onSelected()
                }
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
        if (singleSelectionAudio) {
            var duplicateData = false
            val audioJson: String = if (playlistModel.songIds != "") {
                val audioListFromJson = convertStringToList(playlistModel.songIds)
                duplicateData = if (audioListFromJson.contains(allSongsModel!!.songId)) {
                    Snackbar.make(
                        (activity as AppCompatActivity).window.decorView,
                        "Duplicate songs found", Snackbar.LENGTH_LONG
                    ).show()

                    true
                } else {
                    audioListFromJson.add(allSongsModel!!.songId)
                    false
                }
                convertListToString(audioListFromJson)
            } else {
                val audioList = ArrayList<Long>()
                audioList.add(allSongsModel!!.songId)
                convertListToString(audioList)
            }

            if (!duplicateData) {
                mViewModelClass.updatePlaylist(audioJson, playlistModel.id, lifecycleScope)
                Snackbar.make(
                    (activity as AppCompatActivity).window.decorView,
                    "Song added to ${playlistModel.playlistName}.", Snackbar.LENGTH_LONG
                ).show()
                dismiss()

            }
        } else {
            val audioJson: String = if (playlistModel.songIds != "") {
                val audioListFromJson = convertStringToList(playlistModel.songIds)

                if (songIdsList != null) {
                    for (ids in songIdsList!!) {
                        if (!audioListFromJson.contains(ids)) {
                            audioListFromJson.add(ids)
                        }
                    }
                }

                convertListToString(audioListFromJson)
            } else {
                val audioList = ArrayList<Long>()
                if (songIdsList != null) {
                    for (ids in songIdsList!!) {
                        audioList.add(ids)
                    }
                }
                convertListToString(audioList)
            }

            mViewModelClass.updatePlaylist(audioJson, playlistModel.id, lifecycleScope)
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "Song added to ${playlistModel.playlistName}.", Snackbar.LENGTH_LONG
            ).show()

            if (listener != null)
                listener!!.onSelected()

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

    interface PlaylistSelected {
        fun onSelected()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as PlaylistSelected
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }

    }
}