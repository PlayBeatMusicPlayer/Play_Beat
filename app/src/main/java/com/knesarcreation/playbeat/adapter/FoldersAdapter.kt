package com.knesarcreation.playbeat.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.databinding.RecyclerFoldersItemsBinding
import com.knesarcreation.playbeat.model.FolderModel
import com.knesarcreation.playbeat.utils.AdBanner

class FoldersAdapter(private var listener: OnFolderClicked, var context: Context) :
    ListAdapter<FolderModel, FoldersAdapter.FolderViewHolder>(AllFoldersItemCallback()) {

    class FolderViewHolder(var binding: RecyclerFoldersItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var folderNameTV = binding.folderNameTV // same view as Artists name
        private val noOfSongs = binding.noOfSongs

        //val forwardIconIV = binding.forwardIconIV
        val rlFolderView = binding.rlFolderView

        private fun initializeAddMob(context: Context) {
            // initialize adMob inside the expandable Player
            val adBanner = AdBanner(context, binding.adViewContainer)
            adBanner.initializeAddMob()
        }

        @SuppressLint("SetTextI18n")
        fun bind(
            folderModel: FolderModel,
            position: Int,
            context: Context,
            currentList: MutableList<FolderModel>
        ) {
            if (currentList.size >= 5) {
                if (position == 5) {
                    initializeAddMob(context)
                    binding.adViewContainer.visibility = View.VISIBLE
                } else {
                    binding.adViewContainer.visibility = View.GONE
                }
            } else if (currentList.size < 5) {
                if (position == 3) {
                    initializeAddMob(context)
                    binding.adViewContainer.visibility = View.VISIBLE
                }
            } else if (currentList.size == 1) {
                initializeAddMob(context)
                binding.adViewContainer.visibility = View.VISIBLE
            }

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
        holder.bind(getItem(position), position, context, currentList)
        holder.rlFolderView.setOnClickListener {
            listener.clickOnFolder(getItem(position))
        }
    }

    interface OnFolderClicked {
        fun clickOnFolder(item: FolderModel)
    }
}