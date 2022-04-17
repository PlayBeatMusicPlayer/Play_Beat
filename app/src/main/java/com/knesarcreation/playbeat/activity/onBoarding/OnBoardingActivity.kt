package com.knesarcreation.playbeat.activity.onBoarding

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.knesarcreation.playbeat.databinding.ActivityOnBoardingBinding
import com.knesarcreation.playbeat.utils.LoadAllAudios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

class OnBoardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnBoardingBinding
    private lateinit var sliderAdapter: SliderAdapter
    private var mCurrentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sliderAdapter = SliderAdapter(this)
        binding.sliderPager.adapter = sliderAdapter
        binding.dotsIndicator.setViewPager(binding.sliderPager)

        binding.sliderPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

            }

            override fun onPageSelected(position: Int) {
                mCurrentPage = position
                binding.skipBtn.visibility = View.VISIBLE
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        binding.nextButton.setOnClickListener {
            if (mCurrentPage == 2) {
                loadAllAudios()
            }

            binding.sliderPager.setCurrentItem(mCurrentPage + 1, true)


        }

        binding.skipBtn.setOnClickListener {
            loadAllAudios()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadAllAudios() {
        binding.lottieLoadAudio.playAnimation()
        binding.rlOnboard.animate()
            .translationY(10f)
            //translationYBy(30f)
            .alpha(0.0f)
            .setListener(null)

        binding.rlOnboard.visibility = View.GONE
        binding.rlLoadAudioScreen.visibility = View.VISIBLE

        binding.rlLoadAudioScreen.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)

        Handler(Looper.myLooper()!!).postDelayed({
            lifecycleScope.launch(Dispatchers.IO) {
                LoadAllAudios(this@OnBoardingActivity, true).loadAudio(false)
            }
        }, 2000)
    }


}