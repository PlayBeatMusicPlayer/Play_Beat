package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetCreatePlayListBinding

class BottomSheetCreatePlaylist(var mContext: Context, var audioJson: String) :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreatePlayListBinding? = null
    private val binding get() = _binding
    private lateinit var mViewModelClass: ViewModelClass

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = BottomSheetCreatePlayListBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        binding?.createPlayListBtn?.setOnClickListener {
            if (binding?.etPlaylist?.text?.toString() != "") {
                /*val gson = Gson()
                val audioJson: String = gson.toJson(audioList)*/
                mViewModelClass.insertPlaylist(
                    PlaylistModel(
                        binding?.etPlaylist?.text?.toString()!!,
                        audioJson,
                        System.currentTimeMillis()
                    ),
                    lifecycleScope
                )
                Toast.makeText(
                    activity as Context,
                    "Playlist created successfully.",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            } else {
                binding?.etPlaylist?.error = "Playlist name can't be empty."
                Toast.makeText(mContext, "Playlist name can't be empty.", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.cancelDialog?.setOnClickListener {
            dismiss()
        }

        return view
    }
}