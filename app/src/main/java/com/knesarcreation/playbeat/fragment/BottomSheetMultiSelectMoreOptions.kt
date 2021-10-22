package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.databinding.BottomSheetMultiSelectMoreOptionsBinding

class BottomSheetMultiSelectMoreOptions(var isOpenedFromCustomPlaylist: Boolean) :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetMultiSelectMoreOptionsBinding? = null
    private val binding get() = _binding
    lateinit var listener: MultiSelectAudioMenuOption

    interface MultiSelectAudioMenuOption {
        fun playNext()
        fun addToPlaylist()
        fun addToPlayingQueue()
        fun deleteFromDevice()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetMultiSelectMoreOptionsBinding.inflate(inflater, container, false)
        val view = binding?.root

        if (isOpenedFromCustomPlaylist) {
            binding?.deleteTV?.text = "Delete from playlist"
        }

        binding?.llPlayNext!!.setOnClickListener {
            listener.playNext()
        }

        binding?.llAddToPlaylist!!.setOnClickListener {
            listener.addToPlaylist()
        }

        binding?.llAddToQueue!!.setOnClickListener {
            listener.addToPlayingQueue()
        }

        binding?.llDelete!!.setOnClickListener {
            listener.deleteFromDevice()
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as MultiSelectAudioMenuOption
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}