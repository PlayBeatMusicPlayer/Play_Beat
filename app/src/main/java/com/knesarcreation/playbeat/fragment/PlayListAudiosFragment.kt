package com.knesarcreation.playbeat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.knesarcreation.playbeat.databinding.FragmentPlayListAudiosBinding

class PlayListAudiosFragment : Fragment() {

    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPlayListAudiosBinding.inflate(inflater, container, false)
        val view = binding?.root

        return view
    }

}