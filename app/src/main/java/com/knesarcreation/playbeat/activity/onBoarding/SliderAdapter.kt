package com.knesarcreation.playbeat.activity.onBoarding

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.PagerAdapter
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.OnboardSliderBinding

class SliderAdapter(var context: Context) : PagerAdapter() {

    private lateinit var layoutInflater: LayoutInflater

    private val slideImages =
        arrayOf(R.drawable.play_beat_logo, R.drawable.equalizer_img, R.drawable.trim_audio_img)

    private val slideDes = arrayOf(
        "An offline audio player build with a unique and simple design.",

        "App contains an IN-BUILT EQUALIZER. It includes Bass Booster, 3D Surrounded sound and 9 preset. Feel the real bass by customizing equalizer.",

        "Trim your favourite audio and set it as RINGTONE or ALARM or just simply save it to your device."
    )

    private val sliderHeadings = arrayOf("Welcome to Play Beat", "In-built Equalizer", "Trim Audio")

    private val sliderHeadingDes = arrayOf("Let the music speak!", "Feel the real bass.", "")

    override fun getCount(): Int {
        return sliderHeadings.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as RelativeLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var binding: OnboardSliderBinding? = null
        binding = OnboardSliderBinding.inflate(layoutInflater, container, false)

        binding.logoIV.setImageResource(slideImages[position])
        binding.headingTV.text = sliderHeadings[position]
        binding.headingDesTV.text = sliderHeadingDes[position]
        binding.desTV.text = slideDes[position]

        if (position == 1 || position == 2) {
            binding.rlBorder.background =
                ContextCompat.getDrawable(context, R.drawable.curved_all_edges_stroke_teal_color)
        } else {
            binding.rlBorder.background = null
        }
        val view = binding.root

        container.addView(view)

        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }
}