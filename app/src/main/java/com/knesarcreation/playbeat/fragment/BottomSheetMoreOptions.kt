package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.databinding.BottomSheetAudioMoreOptionBinding

class BottomSheetMoreOptions(var mContext: Context, var allSongsModel: AllSongsModel) :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioMoreOptionBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetAudioMoreOptionBinding.inflate(inflater, container, false)
        val view = binding?.root

        setUpViews()

        binding?.llAddToPlaylist?.setOnClickListener {
            val bottomSheetChooseToPlaylist = BottomSheetChoosePlaylist(allSongsModel)
            bottomSheetChooseToPlaylist.show(
                (mContext as AppCompatActivity).supportFragmentManager,
                "bottomSheetChooseToPlaylist"
            )
            dismiss()
        }

        return view
    }

    private fun setUpViews() {
        binding?.songNameTV?.text = allSongsModel.songName
        binding?.artistNameTV?.text = allSongsModel.artistsName
        binding?.albumNameTv?.text = allSongsModel.albumName

        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        Glide.with(binding?.albumArtIv!!).load(allSongsModel.artUri)
            .transition(withCrossFade(factory)).apply(
                RequestOptions.placeholderOf(
                    R.drawable.music_note_icon
                )
            ).into(binding?.albumArtIv!!)
    }
}