package com.knesarcreation.playbeat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.model.Contributor
import com.knesarcreation.playbeat.views.PlayBeatShapeableImageView

class ContributorAdapter(
    private var contributors: List<Contributor>
) : RecyclerView.Adapter<ContributorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_contributor_header,
                parent,
                false
            )
        )
    }

    /* companion object {
         const val HEADER: Int = 0
         const val ITEM: Int = 1
     }*/

    /*override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            HEADER
        } else {
            ITEM
        }
    }*/

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contributor = contributors[position]
        holder.bindData(contributor)
        /* holder.itemView.setOnClickListener {
             openUrl(it?.context as Activity, contributors[position].link)
         }*/
    }

    override fun getItemCount(): Int {
        return contributors.size
    }

    fun swapData(it: List<Contributor>) {
        contributors = it
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val text: TextView = itemView.findViewById(R.id.text)
        val image: PlayBeatShapeableImageView = itemView.findViewById(R.id.icon)

        internal fun bindData(contributor: Contributor) {
            title.text = contributor.name
            text.text = contributor.summary
            Glide.with(image.context)
                .load(contributor.image)
                .error(R.drawable.ic_account)
                .placeholder(R.drawable.ic_account)
                .dontAnimate()
                .into(image)
            //
        }
    }
}