package com.knesarcreation.playbeat.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.databinding.RecyclerFoldersItemsBinding
import com.knesarcreation.playbeat.model.FolderModel

class FoldersAdapter(private var listener: OnFolderClicked) :
    ListAdapter<FolderModel, FoldersAdapter.FolderViewHolder>(AllFoldersItemCallback()) {

    class FolderViewHolder(binding: RecyclerFoldersItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var folderNameTV = binding.folderNameTV // same view as Artists name
        private val noOfSongs = binding.noOfSongs

        //val forwardIconIV = binding.forwardIconIV
        val rlFolderView = binding.rlFolderView

        @SuppressLint("SetTextI18n")
        fun bind(folderModel: FolderModel) {
            folderNameTV.text = folderModel.folderName
             if (folderModel.noOfSongs > 1) {
                 noOfSongs.text = "${folderModel.noOfSongs} Songs"
             } else {
                 noOfSongs.text = "${folderModel.noOfSongs} Song"
             }
        }

    }

    class AllFoldersItemCallback : DiffUtil.ItemCallback<FolderModel>() {
        override fun areItemsTheSame(oldItem: FolderModel, newItem: FolderModel) =
            oldItem.folderId == newItem.folderId

        override fun areContentsTheSame(
            oldItem: FolderModel,
            newItem: FolderModel
        ) = oldItem.folderName == newItem.folderName

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        return FolderViewHolder(
            RecyclerFoldersItemsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.rlFolderView.setOnClickListener {
            listener.clickOnFolder(getItem(position))
        }
    }

    interface OnFolderClicked {
        fun clickOnFolder(item: FolderModel)
    }
}