package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.knesarcreation.playbeat.adapter.FoldersAdapter
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentFoldersBinding
import com.knesarcreation.playbeat.model.FolderModel

class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding
    private var foldersAdapter: FoldersAdapter? = null
    var listener: OnFolderItemOpened? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var folderList = ArrayList<FolderModel>()

    interface OnFolderItemOpened {
        fun folderOpen(audioFiles: String)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        val view = binding!!.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        getAllFolders()
        return view

    }

    private fun getAllFolders() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (it != null) {
                folderList.clear()
                for (audioData in it) {
                    val folderModel =
                        FolderModel(audioData.folderId, audioData.folderName, audioData.noOfSongs)
                    if (!folderList.contains(folderModel)) {
                        folderList.add(folderModel)
                    }
                }

                foldersAdapter = FoldersAdapter(object : FoldersAdapter.OnFolderClicked {
                    override fun clickOnFolder(item: FolderModel) {
                        val gson = Gson()
                        val audioFiles = gson.toJson(item)
                        listener?.folderOpen(audioFiles)
                    }
                }, activity as Context)
                binding!!.rvAllFolders.adapter = foldersAdapter

                foldersAdapter!!.submitList(folderList)
                if (folderList.isEmpty()) {
                    binding?.rlNoFolder?.visibility = View.VISIBLE
                    binding?.rvAllFolders?.visibility = View.GONE
                } else {
                    binding?.rlNoFolder?.visibility = View.GONE
                    binding?.rvAllFolders?.visibility = View.VISIBLE
                }

            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnFolderItemOpened
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}