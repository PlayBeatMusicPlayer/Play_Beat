package com.knesarcreation.playbeat.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.knesarcreation.playbeat.databinding.ActivityAppThemesBinding
import com.knesarcreation.playbeat.utils.MakeStatusBarTransparent

class AppThemesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppThemesBinding
    var sharedPrefs: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppThemesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.arrowBackIV.setOnClickListener {
            onBackPressed()
        }
        sharedPrefs = getSharedPreferences("AppTheme", MODE_PRIVATE)

        MakeStatusBarTransparent().transparent(this)

        handleVisibility()

        gettingSavedBackgroundList()

        manageClickListener()

    }

    private fun gettingSavedBackgroundList() {
        handleCheckedIVVisibility(sharedPrefs!!.getInt("background", 0))
    }

    private fun handleCheckedIVVisibility(value: Int) {
        when (value) {
            0 -> {
                binding.checkIV1.visibility = View.VISIBLE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            1 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.VISIBLE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            2 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.VISIBLE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            3 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.VISIBLE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            4 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.VISIBLE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            5 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.VISIBLE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            6 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.VISIBLE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            7 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.VISIBLE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            8 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.VISIBLE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            9 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.VISIBLE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.GONE
            }
            10 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.VISIBLE
                binding.checkIV12.visibility = View.GONE
            }
            11 -> {
                binding.checkIV1.visibility = View.GONE
                binding.checkIV2.visibility = View.GONE
                binding.checkIV3.visibility = View.GONE
                binding.checkIV4.visibility = View.GONE
                binding.checkIV5.visibility = View.GONE
                binding.checkIV6.visibility = View.GONE
                binding.checkIV7.visibility = View.GONE
                binding.checkIV8.visibility = View.GONE
                binding.checkIV9.visibility = View.GONE
                binding.checkIV10.visibility = View.GONE
                binding.checkIV11.visibility = View.GONE
                binding.checkIV12.visibility = View.VISIBLE
            }
        }

    }

    private fun handleVisibility() {
        binding.checkIV1.visibility = View.GONE
        binding.checkIV2.visibility = View.GONE
        binding.checkIV3.visibility = View.GONE
        binding.checkIV4.visibility = View.GONE
        binding.checkIV5.visibility = View.GONE
        binding.checkIV6.visibility = View.GONE
        binding.checkIV7.visibility = View.GONE
        binding.checkIV8.visibility = View.GONE
        binding.checkIV9.visibility = View.GONE
        binding.checkIV10.visibility = View.GONE
        binding.checkIV11.visibility = View.GONE
        binding.checkIV12.visibility = View.GONE
    }

    private fun manageClickListener() {
        binding.cvDefault.setOnClickListener {
            handleCheckedIVVisibility(0)
            sharedPrefs!!.edit().putInt("background", 0).apply()
        }

        binding.cvSecondTheme.setOnClickListener {
            handleCheckedIVVisibility(1)
            sharedPrefs!!.edit().putInt("background", 1).apply()
        }

        binding.cvThirdTheme.setOnClickListener {
            handleCheckedIVVisibility(2)
            sharedPrefs!!.edit().putInt("background", 2).apply()
        }

        binding.cvFourthTheme.setOnClickListener {
            handleCheckedIVVisibility(3)
            sharedPrefs!!.edit().putInt("background", 3).apply()
        }

        binding.cvFifthTheme.setOnClickListener {
            handleCheckedIVVisibility(4)
            sharedPrefs!!.edit().putInt("background", 4).apply()
        }

        binding.cvSixthTheme.setOnClickListener {
            handleCheckedIVVisibility(5)
            sharedPrefs!!.edit().putInt("background", 5).apply()
        }

        binding.cvSeventhTheme.setOnClickListener {
            handleCheckedIVVisibility(6)
            sharedPrefs!!.edit().putInt("background", 6).apply()
        }

        binding.cvEightTheme.setOnClickListener {
            handleCheckedIVVisibility(7)
            sharedPrefs!!.edit().putInt("background", 7).apply()
        }

        binding.cvNinthTheme.setOnClickListener {
            handleCheckedIVVisibility(8)
            sharedPrefs!!.edit().putInt("background", 8).apply()
        }

        binding.cvTenthTheme.setOnClickListener {
            handleCheckedIVVisibility(9)
            sharedPrefs!!.edit().putInt("background", 9).apply()
        }

        binding.cvElevenTheme.setOnClickListener {
            handleCheckedIVVisibility(10)
            sharedPrefs!!.edit().putInt("background", 10).apply()
        }

        binding.cvTwelveTheme.setOnClickListener {
            handleCheckedIVVisibility(11)
            sharedPrefs!!.edit().putInt("background", 11).apply()
        }


    }

}