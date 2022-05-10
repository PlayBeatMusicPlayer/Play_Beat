package com.knesarcreation.playbeat.equailizer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.equailizer.AnalogController.onProgressChangedListener
import com.knesarcreation.playbeat.extensions.accentColor
import com.knesarcreation.playbeat.extensions.applyToolbar
import com.knesarcreation.playbeat.extensions.generalThemeValue
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.musicService
import com.knesarcreation.playbeat.util.theme.ThemeMode

class EqualizerFragment : Fragment() {
    // var mEqualizer: Equalizer? = null
    private var equalizerSwitch: SwitchCompat? = null
    private val equalizerPresetNames = ArrayList<String>()

    //var bassBoost: BassBoost? = null
    //var chart: LineChartView? = null

    // var presetReverb: PresetReverb? = null
    private var y = 0

    //var spinnerDropDownIcon: ImageView? = null
    private var mLinearLayout: LinearLayout? = null
    private var seekBarFinal = arrayOfNulls<SeekBar>(5)
    private var bassController: AnalogController? = null
    private var reverbController: AnalogController? = null
    private var presetSpinner: AutoCompleteTextView? = null
    private var chooseBranchMenu: TextInputLayout? = null
    private var equalizerBlocker: FrameLayout? = null
    private var ctx: Context? = null
    private val equalizerSetting = Settings()

    //var dataset: LineSet? = null
    private var paint: Paint? = null

    // lateinit var points: FloatArray
    private var numberOfFrequencyBands: Short = 0
    private var audioSesionId = 0

    override fun onResume() {
        super.onResume()
        if (musicService == null) {
            (activity as AppCompatActivity).onBackPressed()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.isEditing = true
        if (arguments != null && requireArguments().containsKey(ARG_AUDIO_SESSIOIN_ID)) {
            audioSesionId = requireArguments().getInt(ARG_AUDIO_SESSIOIN_ID)
        }
        if (Settings.equalizerModel == null) {
            Settings.equalizerModel = EqualizerModel()
            Settings.equalizerModel!!.reverbPreset = PresetReverb.PRESET_NONE
            Settings.equalizerModel!!.bassStrength = (1000 / 19).toShort()
        }
//        if (AllSongFragment.musicService != null) {
//            if (AllSongFragment.musicService!!.mEqualizer != null)
//                AllSongFragment.musicService!!.mEqualizer!!.release()
//            if (AllSongFragment.musicService!!.bassBoost != null)
//                AllSongFragment.musicService!!.bassBoost!!.release()
//            if (AllSongFragment.musicService!!.presetReverb != null)
//                AllSongFragment.musicService!!.presetReverb!!.release()
//
        if (musicService != null) {
            if (musicService!!.playback != null) {
                try {
                    musicService!!.mEqualizer = Equalizer(1000, audioSesionId)
                    musicService!!.bassBoost = BassBoost(1000, audioSesionId)
                    musicService!!.presetReverb = PresetReverb(1000, audioSesionId)

                } catch (e: java.lang.Exception) {
                }
            }
        }


//            AllSongFragment.musicService!!.bassBoost!!.enabled = Settings.isEqualizerEnabled
//            // val bassBoostSettingTemp = AllSongFragment.musicService!!.bassBoost!!.properties
//            // val bassBoostSetting = BassBoost.Settings(bassBoostSettingTemp.toString())
//            //bassBoostSetting.strength = Settings.equalizerModel.bassStrength
//            AllSongFragment.musicService!!.bassBoost!!.setStrength(Settings.equalizerModel!!.bassStrength)
//
//            AllSongFragment.musicService!!.presetReverb!!.preset =
//                Settings.equalizerModel!!.reverbPreset
//            AllSongFragment.musicService!!.presetReverb!!.enabled = Settings.isEqualizerEnabled
//            AllSongFragment.musicService!!.mEqualizer!!.enabled = Settings.isEqualizerEnabled
//            if (Settings.presetPos == 0) {
//                for (bandIdx in 0 until AllSongFragment.musicService!!.mEqualizer!!.numberOfBands) {
//                    AllSongFragment.musicService!!.mEqualizer!!.setBandLevel(
//                        bandIdx.toShort(),
//                        Settings.seekbarpos[bandIdx].toShort()
//                    )
//                }
//            } else {
//                AllSongFragment.musicService!!.mEqualizer!!.usePreset(Settings.presetPos.toShort())
//            }
//        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_equalizer, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar: MaterialToolbar = view.findViewById(R.id.playerToolbar)
        applyToolbar(toolbar)
        toolbar.setNavigationOnClickListener {
            (context as AppCompatActivity).onBackPressed()
        }

        equalizerSwitch = view.findViewById(R.id.equalizer_switch)
        equalizerSwitch!!.isChecked = Settings.isEqualizerEnabled
        equalizerSwitch!!.setOnCheckedChangeListener { _, isChecked ->
            if (musicService != null) {
                musicService!!.mEqualizer!!.enabled = isChecked
                musicService!!.bassBoost!!.enabled = isChecked
                musicService!!.presetReverb!!.enabled = isChecked

                updateUI(isChecked)

            }

            Settings.isEqualizerEnabled = isChecked
            Settings.equalizerModel!!.isEqualizerEnabled = isChecked

            //saving changes in equalizer
            equalizerSetting.saveEqualizerSettings(ctx!!)
        }

        //spinnerDropDownIcon = view.findViewById(R.id.spinner_dropdown_icon)
        //spinnerDropDownIcon!!.setOnClickListener {  }
        presetSpinner = view.findViewById(R.id.equalizer_preset_spinner)
        chooseBranchMenu = view.findViewById(R.id.chooseBranchMenu)
        presetSpinner!!.performClick()
        equalizerBlocker = view.findViewById(R.id.equalizerBlocker)
        //chart = view.findViewById(R.id.lineChart)
        paint = Paint()
        //dataset = LineSet()
        bassController = view.findViewById(R.id.controllerBass)
        reverbController = view.findViewById(R.id.controller3D)
        bassController!!.label = ""
        reverbController!!.label = ""
        bassController!!.circlePaint2!!.color = themeColor
        bassController!!.linePaint!!.color = themeColor
        bassController!!.invalidate()
        reverbController!!.circlePaint2!!.color = themeColor
        reverbController!!.linePaint!!.color = themeColor
        reverbController!!.invalidate()


        if (musicService != null) {
            if (!Settings.isEqualizerReloaded) {
                var x = 0
                if (musicService!!.bassBoost != null) {
                    try {
                        x = musicService!!.bassBoost!!.roundedStrength * 19 / 1000
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (musicService!!.presetReverb != null) {
                    try {
                        y = musicService!!.presetReverb!!.preset * 19 / 6
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (x == 0) {
                    bassController!!.progress = 1
                } else {
                    bassController!!.progress = x
                }
                if (y == 0) {
                    reverbController!!.progress = 1
                } else {
                    reverbController!!.progress = y
                }
            } else {
                val x = Settings.bassStrength * 19 / 1000
                y = Settings.reverbPreset * 19 / 6
                if (x == 0) {
                    bassController!!.progress = 1
                } else {
                    bassController!!.progress = x
                }
                if (y == 0) {
                    reverbController!!.progress = 1
                } else {
                    reverbController!!.progress = y
                }
            }


            bassController!!.setOnProgressChangedListener(object : onProgressChangedListener {
                override fun onProgressChanged(progress: Int) {
                    Settings.bassStrength = (1000.toFloat() / 19 * progress).toInt().toShort()
                    Log.d("bassController...", "onProgressChanged: $progress")
                    try {
                        if (musicService != null) {
                            musicService!!.bassBoost!!.setStrength(Settings.bassStrength)
                            Settings.equalizerModel!!.bassStrength = Settings.bassStrength

                            //saving changes in equalizer
                            equalizerSetting.saveEqualizerSettings(ctx!!)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })

            reverbController!!.setOnProgressChangedListener(object : onProgressChangedListener {
                override fun onProgressChanged(progress: Int) {
                    Settings.reverbPreset = (progress * 6 / 19).toShort()
                    Settings.equalizerModel!!.reverbPreset = Settings.reverbPreset
                    try {
                        if (musicService != null) {
                            musicService!!.presetReverb!!.preset = Settings.reverbPreset
                            //saving changes in equalizer
                            equalizerSetting.saveEqualizerSettings(ctx!!)

                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    y = progress
                }
            })

            mLinearLayout = view.findViewById(R.id.equalizerContainer)

            val equalizerHeading = TextView(context)
            equalizerHeading.setText(R.string.equalizer)
            equalizerHeading.textSize = 20f
            equalizerHeading.isAllCaps = false
            equalizerHeading.gravity = Gravity.CENTER_HORIZONTAL
            numberOfFrequencyBands = 5
            // points = FloatArray(numberOfFrequencyBands.toInt())

            val lowerEqualizerBandLevel = musicService!!.mEqualizer!!.bandLevelRange[0]
            val upperEqualizerBandLevel = musicService!!.mEqualizer!!.bandLevelRange[1]

            //frequency bands
            for (i in 0 until numberOfFrequencyBands) {
                val equalizerBandIndex = i.toShort()
                val frequencyHeaderTextView = TextView(context)
                frequencyHeaderTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                frequencyHeaderTextView.gravity = Gravity.CENTER_HORIZONTAL
                frequencyHeaderTextView.setTextColor(Color.parseColor("#FFFFFF"))

                val centralFreq =
                    musicService!!.mEqualizer!!.getCenterFreq(equalizerBandIndex) / 1000
                if ((centralFreq) >= 1000) {
                    frequencyHeaderTextView.text =
                        "%.0f".format(((centralFreq).toDouble() / 1000)) + "kHz"
                } else
                    frequencyHeaderTextView.text =
                        (musicService!!.mEqualizer!!.getCenterFreq(equalizerBandIndex) / 1000).toString() + "Hz"

                val seekBarRowLayout = LinearLayout(context)
                seekBarRowLayout.orientation = LinearLayout.VERTICAL

                val lowerAndUpperEqualizerBandLevelTextView = TextView(context)
                /*lowerAndUpperEqualizerBandLevelTextView.gravity =
                    Gravity.CENTER*/
                lowerAndUpperEqualizerBandLevelTextView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                //lowerEqualizerBandLevelTextView.text = (lowerEqualizerBandLevel / 100).toString() + "dB"

                // val upperEqualizerBandLevelTextView = TextView(context)
                // upperEqualizerBandLevelTextView.gravity = Gravity.CENTER_HORIZONTAL
                // upperEqualizerBandLevelTextView.layoutParams = ViewGroup.LayoutParams(
                //     ViewGroup.LayoutParams.WRAP_CONTENT,
                //      ViewGroup.LayoutParams.WRAP_CONTENT
                //)
                //upperEqualizerBandLevelTextView.text = (upperEqualizerBandLevel / 100).toString() + "dB"

                val layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                layoutParams.weight = 1f
                var seekBar = SeekBar(context)
                var frequencyHeaderTV = TextView(context)
                var upperrAndLowerEQBandLevel = TextView(context)
                when (i) {
                    0 -> {
                        seekBar = view.findViewById(R.id.seekBar1)
                        frequencyHeaderTV = view.findViewById(R.id.textView1)
                        upperrAndLowerEQBandLevel = view.findViewById(R.id.textView01)
                    }
                    1 -> {
                        seekBar = view.findViewById(R.id.seekBar2)
                        frequencyHeaderTV = view.findViewById(R.id.textView2)
                        upperrAndLowerEQBandLevel = view.findViewById(R.id.textView02)
                    }
                    2 -> {
                        seekBar = view.findViewById(R.id.seekBar3)
                        frequencyHeaderTV = view.findViewById(R.id.textView3)
                        upperrAndLowerEQBandLevel = view.findViewById(R.id.textView03)
                    }
                    3 -> {
                        seekBar = view.findViewById(R.id.seekBar4)
                        frequencyHeaderTV = view.findViewById(R.id.textView4)
                        upperrAndLowerEQBandLevel = view.findViewById(R.id.textView04)
                    }
                    4 -> {
                        seekBar = view.findViewById(R.id.seekBar5)
                        frequencyHeaderTV = view.findViewById(R.id.textView5)
                        upperrAndLowerEQBandLevel = view.findViewById(R.id.textView05)
                    }
                }
                seekBarFinal[i] = seekBar
                // seekBar.progressDrawable.colorFilter =
                //   PorterDuffColorFilter(seekDrawableColor, PorterDuff.Mode.SRC_IN)
                //seekBar.thumb.colorFilter = PorterDuffColorFilter(themeColor, PorterDuff.Mode.SRC_IN)
                seekBar.id = i
                //            seekBar.setLayoutParams(layoutParams);
                seekBar.max = upperEqualizerBandLevel - lowerEqualizerBandLevel
                frequencyHeaderTV.text = frequencyHeaderTextView.text
                upperrAndLowerEQBandLevel.text = lowerAndUpperEqualizerBandLevelTextView.text
                //frequencyHeaderTV.setTextColor(Color.WHITE)
                // upperrAndLowerEQBandLevel.setTextColor(Color.WHITE)
                frequencyHeaderTV.textAlignment = View.TEXT_ALIGNMENT_CENTER
                upperrAndLowerEQBandLevel.textAlignment = View.TEXT_ALIGNMENT_CENTER
                upperrAndLowerEQBandLevel.text = "${(Settings.seekbarpos[i]) / 100} db"

                // Log.d(
                //     "getCenterFreq",
                //     "equalizeSound:${(AllSongFragment.musicService!!.mEqualizer!!.getCenterFreq(equalizerBandIndex) / 1000)} "
                //)

                if (Settings.isEqualizerReloaded) {
                    // points[i] = (Settings.seekbarpos[i] - lowerEqualizerBandLevel).toFloat()
                    // dataset!!.addPoint(frequencyHeaderTextView.text.toString(), points[i])
                    seekBar.progress = Settings.seekbarpos[i] - lowerEqualizerBandLevel
                } else {
                    // points[i] =
                    //(AllSongFragment.musicService!!.mEqualizer!!.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()
                    // dataset!!.addPoint(frequencyHeaderTextView.text.toString(), points[i])
                    seekBar.progress =
                        musicService!!.mEqualizer!!.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel
                    //Settings.seekbarpos[i] =
                    //    AllSongFragment.musicService!!.mEqualizer!!.getBandLevel(equalizerBandIndex)
                    //        .toInt()
                }
                seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        musicService!!.mEqualizer!!.setBandLevel(
                            equalizerBandIndex,
                            (progress + lowerEqualizerBandLevel).toShort()
                        )

                        upperrAndLowerEQBandLevel.text =
                            "${(progress + lowerEqualizerBandLevel) / 100} db"

                        Log.d(
                            "ProgressBand",
                            "onProgressChanged:${progress + lowerEqualizerBandLevel} "
                        )
                        // points[seekBar.id] =
                        //   (AllSongFragment.musicService!!.mEqualizer!!.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel).toFloat()
                        if (Settings.presetPos == 0) {
                            Settings.isEqualizerReloaded = true

                            Settings.seekbarpos[seekBar.id] = progress + lowerEqualizerBandLevel
                            Settings.equalizerModel!!.seekbarpos[seekBar.id] =
                                progress + lowerEqualizerBandLevel
                        }
                        //dataset!!.updateValues(points)
                        //chart!!.notifyDataUpdate()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {
                        presetSpinner!!.setText(equalizerPresetNames[0], false)

                        Settings.presetPos = 0
                        Settings.equalizerModel!!.presetPos = 0
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        //saving changes in equalizer
                        equalizerSetting.saveEqualizerSettings(ctx!!)
                    }
                })
            }

            equalizeSound()

            updateUI(Settings.isEqualizerEnabled)

            paint!!.color = Color.parseColor("#FF03DAC5")
            paint!!.strokeWidth = (1.10 * Settings.ratio).toFloat()
            val mEndButton = Button(context)
            mEndButton.setBackgroundColor(themeColor)
            mEndButton.setTextColor(Color.WHITE)

            // dataset!!.color = themeColor
            //dataset!!.isSmooth = true
            //dataset!!.thickness = 5F
            //chart!!.setXAxis(false)
            //chart!!.setYAxis(false)
            //chart!!.setYLabels(AxisController.LabelPosition.NONE)
            //chart!!.setXLabels(AxisController.LabelPosition.NONE)
            //chart!!.setGrid(ChartView.GridType.NONE, 7, 10, paint)
            //chart!!.setAxisBorderValues(-300, 3300)
            //chart!!.addData(dataset)
            //chart!!.show()
        }
    }

    private fun updateUI(isChecked: Boolean) {
        presetSpinner!!.isEnabled = isChecked
        chooseBranchMenu!!.isEnabled = isChecked
        for (i in 0 until musicService!!.mEqualizer!!.numberOfBands) {
            seekBarFinal[i]!!.isEnabled = isChecked

            if (isChecked)
                seekBarFinal[i]!!.thumb =
                    ContextCompat.getDrawable(ctx!!, R.drawable.custom_equalizer_thumb)
            else
                seekBarFinal[i]!!.thumb =
                    ContextCompat.getDrawable(ctx!!, R.drawable.custom_equalizer_thumb_disabled)

        }
        bassController!!.isEnabled = isChecked
        reverbController!!.isEnabled = isChecked

        var circlePaintColor: Int = ContextCompat.getColor(ctx!!, R.color.md_grey_800)
        var linePaint: Int = ContextCompat.getColor(ctx!!, R.color.md_grey_600)
        when ((activity as AppCompatActivity).generalThemeValue) {
            ThemeMode.LIGHT -> {
                circlePaintColor =
                    ContextCompat.getColor(ctx!!, R.color.twenty_percent_black_overlay)
                linePaint = ContextCompat.getColor(ctx!!, R.color.md_grey_600)
            }
            ThemeMode.DARK -> {
                circlePaintColor = ContextCompat.getColor(ctx!!, R.color.md_grey_700)
                linePaint = ContextCompat.getColor(ctx!!, R.color.md_grey_600)
            }
            ThemeMode.AUTO -> {}
        }

        if (!isChecked) {
            //controller is diabled , change color to grey
            bassController!!.circlePaint2!!.color = circlePaintColor
            bassController!!.linePaint!!.color = linePaint
            reverbController!!.circlePaint2!!.color = circlePaintColor
            reverbController!!.linePaint!!.color = linePaint
        } else {
            bassController!!.circlePaint2!!.color = accentColor()
            bassController!!.linePaint!!.color = accentColor()
            reverbController!!.circlePaint2!!.color = accentColor()
            reverbController!!.linePaint!!.color = accentColor()
        }
    }

    private fun equalizeSound() {
        equalizerPresetNames.clear()
        val equalizerPresetSpinnerAdapter = ArrayAdapter(
            ctx!!,
            R.layout.spinner_item,
            equalizerPresetNames
        )
        equalizerPresetSpinnerAdapter.setDropDownViewResource(R.layout.expanded_menu_item)
        equalizerPresetNames.add("Custom")
        for (i in 0 until musicService!!.mEqualizer!!.numberOfPresets) {
            equalizerPresetNames.add(musicService!!.mEqualizer!!.getPresetName(i.toShort()))
        }
        presetSpinner!!.setAdapter(equalizerPresetSpinnerAdapter)
        //presetSpinner.setDropDownWidth((Settings.screen_width * 3) / 4);
        if (!Settings.isEqualizerReloaded && Settings.presetPos != 0) {
            //other preset except custom
            presetSpinner!!.setText(equalizerPresetNames[Settings.presetPos], false)
        }
        presetSpinner!!.onItemClickListener =
            AdapterView.OnItemClickListener { p0, p1, position, p2 ->

                val numberOfFreqBands: Short = 5

                try {
                    if (position != 0) {
                        Settings.isEqualizerReloaded = false
                        musicService!!.mEqualizer!!.usePreset((position - 1).toShort())
                        Settings.presetPos = position
                        val lowerEqualizerBandLevel =
                            musicService!!.mEqualizer!!.bandLevelRange[0]

                        for (i in 0 until numberOfFreqBands) {
                            seekBarFinal[i]!!.progress =
                                musicService!!.mEqualizer!!.getBandLevel(i.toShort()) - lowerEqualizerBandLevel
                            //points[i] =
                            //  (AllSongFragment.musicService!!.mEqualizer!!.getBandLevel(i.toShort()) - lowerEqualizerBandLevel).toFloat()
                            // Settings.seekbarpos[i] =
                            //   AllSongFragment.musicService!!.mEqualizer!!.getBandLevel(
                            //     i.toShort()
                            // ).toInt()
                            //Settings.equalizerModel!!.seekbarpos[i] =
                            //    AllSongFragment.musicService!!.mEqualizer!!.getBandLevel(
                            //      i.toShort()
                            //  ).toInt()
                        }
                        //dataset!!.updateValues(points)
                        //chart!!.notifyDataUpdate()
                    } else {
                        // if custom preset is selected
                        Settings.isEqualizerReloaded = true

                        val lowerEqualizerBandLevel =
                            musicService!!.mEqualizer!!.bandLevelRange[0]

                        for (bandIdx in 0 until numberOfFreqBands) {
                            Log.d(
                                "seekProgress..",
                                "equalizeSound:${Settings.equalizerModel!!.seekbarpos[bandIdx] - lowerEqualizerBandLevel} "
                            )

                            seekBarFinal[bandIdx]!!.progress =
                                Settings.equalizerModel!!.seekbarpos[bandIdx] - lowerEqualizerBandLevel
                            musicService!!.mEqualizer!!.setBandLevel(
                                bandIdx.toShort(),
                                Settings.equalizerModel!!.seekbarpos[bandIdx].toShort()
                            )

                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Error while updating Equalizer", Toast.LENGTH_SHORT).show()
                }
                Settings.equalizerModel!!.presetPos = position

                //saving changes in equalizer
                equalizerSetting.saveEqualizerSettings(ctx!!)


            }
    }

    class Builder {
        private var id = -1
        fun setAudioSessionId(id: Int): Builder {
            this.id = id
            return this
        }

        fun setAccentColor(color: Int): Builder {
            themeColor = color
            return this
        }

        fun setShowBackButton(show: Boolean): Builder {
            showBackButton = show
            return this
        }

        fun build(): EqualizerFragment {
            return newInstance(id)
        }
    }

    companion object {
        const val ARG_AUDIO_SESSIOIN_ID = "audio_session_id"
        var themeColor = Color.parseColor("#FF03DAC5")
        var showBackButton = true
        fun newInstance(audioSessionId: Int): EqualizerFragment {
            val args = Bundle()
            args.putInt(ARG_AUDIO_SESSIOIN_ID, audioSessionId)
            val fragment = EqualizerFragment()
            fragment.arguments = args
            return fragment
        }

        fun newBuilder(): Builder {
            return Builder()
        }
    }
}