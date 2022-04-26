package com.knesarcreation.playbeat.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.FragmentMainSettingsBinding

class MainSettingsFragment : Fragment() {
    private var _binding: FragmentMainSettingsBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMainSettingsBinding.inflate(inflater, container, false)

        findNavController().navigate(R.id.action_mainSettingsFragment_to_themeSettingsFragment2)

        return binding.root
    }

}