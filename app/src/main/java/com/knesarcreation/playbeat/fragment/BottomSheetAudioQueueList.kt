package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.databinding.QueueListBotomSheetBinding
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList


class BottomSheetAudioQueueList(var mContext: Context) : BottomSheetDialogFragment(),
    AllSongsAdapter.OnClickSongItem {
    private var _binding: QueueListBotomSheetBinding? = null
    private val binding get() = _binding
    private var storageUtil: StorageUtil? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var audioIndex = -1
    private var adapter: AllSongsAdapter? = null
    var listener: OnRepeatAudioListener? = null


    interface OnRepeatAudioListener {
        fun onRepeatIconClicked()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = QueueListBotomSheetBinding.inflate(inflater, container, false)
        val view = binding?.root

        storageUtil = StorageUtil(mContext)
        audioList = storageUtil?.loadAudio()!!
        audioIndex = storageUtil?.loadAudioIndex()!!

        setupUpNextAdapter()
        updateCurrentPlayingAudio()

        manageRepeatAudio()

        return view
    }

    private fun manageRepeatAudio() {
        if (storageUtil?.getIsRepeatAudio()!!) {
            binding?.repeatQueueIV?.setImageResource(R.drawable.repeat_one_on_24)
        } else {
            binding?.repeatQueueIV?.setImageResource(R.drawable.ic_repeat_24)
        }
        binding?.repeatQueueIV?.setOnClickListener {
            if (!storageUtil?.getIsRepeatAudio()!!) {
                //repeat current audio
                Toast.makeText(mContext, "Repeat current audio", Toast.LENGTH_SHORT).show()
                binding?.repeatQueueIV?.setImageResource(R.drawable.repeat_one_on_24)
                storageUtil?.saveIsRepeatAudio(true)
                listener?.onRepeatIconClicked()
            } else {
                //repeat audio list
                Toast.makeText(mContext, "Repeat audio list", Toast.LENGTH_SHORT).show()
                binding?.repeatQueueIV?.setImageResource(R.drawable.ic_repeat_24)
                storageUtil?.saveIsRepeatAudio(false)
                listener?.onRepeatIconClicked()
            }
        }
    }

    private fun setupUpNextAdapter() {
        binding?.rvUpNext?.setHasFixedSize(true)
        adapter = AllSongsAdapter(mContext, audioList, this)
        binding?.rvUpNext?.adapter = adapter
        Log.d("updateCurrentPlayingAudioList", "updateCurrentPlayingAudio: $audioList")

    }

    private fun updateCurrentPlayingAudio() {
        Log.d("updateCurrentPlayingAudioIndex", "updateCurrentPlayingAudio: $audioIndex")
        if (audioIndex == -1) audioIndex = 0 else audioIndex
        binding?.songNameTV!!.text = audioList[audioIndex].songName
        binding?.artistNameTV!!.text = audioList[audioIndex].artistsName
        binding?.albumNameTv!!.text = audioList[audioIndex].albumName

        Glide.with(mContext).load(audioList[audioIndex].artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder))
            .into(binding?.albumArtIv!!)
    }

    private fun playAudio(audioIndex: Int) {
        //store audio to prefs
        //storageUtil!!.storeAudio(audioList)
        //Store the new audioIndex to SharedPreferences
        storageUtil!!.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        val updatePlayer = Intent(AllSongFragment.Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)
    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        audioIndex = position
        playAudio(position)
        updateCurrentPlayingAudio()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnRepeatAudioListener
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}