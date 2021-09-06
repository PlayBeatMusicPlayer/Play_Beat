package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.knesarcreation.playbeat.R

class AlbumFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_album, container, false)
//        Toast.makeText(activity as Context, "Album", Toast.LENGTH_SHORT).show()
        return  view
    }

    override fun onResume() {
        super.onResume()
//        Toast.makeText(activity as Context, "Album Resumed", Toast.LENGTH_SHORT).show()
    }
}