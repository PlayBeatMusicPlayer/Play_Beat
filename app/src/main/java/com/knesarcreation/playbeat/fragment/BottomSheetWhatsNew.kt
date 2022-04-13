package com.knesarcreation.playbeat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.BuildConfig
import com.knesarcreation.playbeat.databinding.BottomSheetWhatsNewBinding


class BottomSheetWhatsNew : BottomSheetDialogFragment() {

    private var _binding: BottomSheetWhatsNewBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetWhatsNewBinding.inflate(inflater, container, false)
        val view = binding!!.root

        binding?.versionNameTV?.text = BuildConfig.VERSION_NAME

        return view

    }
}