package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetCreatePlayListBinding

class BottomSheetCreateOrRenamePlaylist(
    var mContext: Context,
    var audioJson: String?,
    var isCreate: Boolean,
    var playlistModel: PlaylistModel?,
) :
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

        if (!isCreate) {
            binding?.titleNameTV!!.text = "Rename playlist"
            binding?.createOrRenamePlayListBtn?.text = "Rename"
            binding?.etPlaylist!!.setText(playlistModel!!.playlistName)
        }

        binding?.createOrRenamePlayListBtn?.setOnClickListener {
            if (isCreate) {
                if (binding?.etPlaylist?.text?.toString() != "") {
                    /*val gson = Gson()
                    val audioJson: String = gson.toJson(audioList)*/
                    mViewModelClass.insertPlaylist(
                        PlaylistModel(
                            binding?.etPlaylist?.text?.toString()!!,
                            audioJson!!,
                            System.currentTimeMillis()
                        ),
                        lifecycleScope
                    )
                    Snackbar.make(
                        (activity as AppCompatActivity).window.decorView,
                        "Playlist created successfully.", Snackbar.LENGTH_LONG
                    ).show()

                    dismiss()
                } else {
                    binding?.etPlaylist?.error = "Playlist name can't be empty."
                    Snackbar.make(
                        dialog!!.window!!.decorView,
                        "Playlist name can't be empty.", Snackbar.LENGTH_LONG
                    ).show()
                }
            } else {
                // rename playlist
                val renamedPlaylistName = binding?.etPlaylist?.text!!.toString()
                if (renamedPlaylistName.isNotEmpty()) {
                    binding?.etPlaylist?.error = null
                    mViewModelClass.renamePlaylist(
                        renamedPlaylistName,
                        playlistModel!!.id,
                        lifecycleScope
                    )
                    dismiss()
                } else {
                    binding?.etPlaylist?.error = "Playlist name can't be empty."
                    Snackbar.make(
                        dialog!!.window!!.decorView,
                        "Playlist name can't be empty.", Snackbar.LENGTH_LONG
                    ).show()
                   /* Toast.makeText(
                        activity as Context,
                        "Playlist name can't be empty.",
                        Toast.LENGTH_SHORT
                    ).show()*/
                }
            }

        }

        binding?.cancelDialog?.setOnClickListener {
            dismiss()
        }

        return view
    }
}