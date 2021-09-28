package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.knesarcreation.playbeat.databinding.FragmentPlaylistsBinding

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding
    private var listener: OnPlayListCategoryClicked? = null

    interface OnPlayListCategoryClicked {
        fun playlistCategory(category: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        val view = binding?.root

        binding?.favButton?.setOnClickListener {
            listener?.playlistCategory("fav")
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnPlayListCategoryClicked
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

}