package com.knesarcreation.playbeat.adapter


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.interfaces.OnRecyclerItemClickListener
import io.github.vejei.carouselview.CarouselAdapter

class SliderAdapter(var listener: OnRecyclerItemClickListener) :
    CarouselAdapter<SliderAdapter.ViewHolder>() {
    private var data = mutableListOf<Page>()

    fun setData(data: MutableList<Page>) {
        this.data.clear()
        this.data.addAll(data)
        notifyDataSetChanged()
    }

    data class Page(val imageRes: Int, val url: String)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val backgroundImageView = itemView.findViewById<ImageView>(R.id.image_slider)
        //private val contentTextView = itemView.findViewById<TextView>(R.id.text_view_content)

        fun bind(page: Page) {
            backgroundImageView.clipToOutline = true
            backgroundImageView.setImageResource(page.imageRes)
            //contentTextView.text = page.content
            backgroundImageView.setOnClickListener {
                listener.onBannerItemClick(page)
            }
        }
    }

    override fun onCreatePageViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.slide_item_container, parent, false)
        )
    }

    override fun onBindPageViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getPageCount(): Int {
        return data.size
    }
}