package com.knesarcreation.playbeat.interfaces


import com.knesarcreation.playbeat.adapter.SliderAdapter

interface OnRecyclerItemClickListener {
    fun onBannerItemClick(sliderItem: SliderAdapter.Page){}
}