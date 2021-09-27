package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.BottomSheetSleepTimerBinding
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList

class BottomSheetSleepTimer(
    private var mContext: Context,
    private var sleepTimerTV: TextView,
    private var sleepTimeIV: ImageView
) :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetSleepTimerBinding? = null
    private val binding get() = _binding
    private var sleepTimeInMillis: Long = 0L
    private var storageUtil: StorageUtil? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var audioIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetSleepTimerBinding.inflate(inflater, container, false)
        val view = binding?.root

        storageUtil = StorageUtil(mContext)

        if (AllSongFragment.musicService != null) {
            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                val sleepTime: Long? = storageUtil?.getSleepTime()
                binding?.cancelOrStopTimerBtn?.text = "Stop"
                binding?.startTimerBtn?.text = "Done"
                binding?.rlEndSleepTimer?.visibility = View.VISIBLE
                binding?.endOfAudioTimeBtn?.visibility = View.GONE
                AllSongFragment.musicService?.startSleepTimeCountDown(
                    binding?.countingTimeTV!!,
                    binding?.rlEndSleepTimer,
                    binding?.endOfAudioTimeBtn,
                    sleepTime!!,
                    sleepTimeIV,
                    sleepTimerTV
                )

            } else {
                binding?.cancelOrStopTimerBtn?.text = "Cancel"
                binding?.rlEndSleepTimer?.visibility = View.GONE
                binding?.endOfAudioTimeBtn?.visibility = View.VISIBLE
            }
        }

        binding!!.sleepTimerToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.fiveMinBtn -> {
                        sleepTimeInMillis = 5L * 60000
                        if (AllSongFragment.musicService != null) {
                            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                                AllSongFragment.musicService?.startSleepTimeCountDown(
                                    binding?.countingTimeTV!!,
                                    binding?.rlEndSleepTimer,
                                    binding?.endOfAudioTimeBtn,
                                    sleepTimeInMillis,
                                    sleepTimeIV,
                                    sleepTimerTV
                                )
                            }
                        }
                    }
                    R.id.tenMinBtn -> {
                        sleepTimeInMillis = 10L * 60000
                        if (AllSongFragment.musicService != null) {
                            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                                AllSongFragment.musicService?.startSleepTimeCountDown(
                                    binding?.countingTimeTV!!,
                                    binding?.rlEndSleepTimer,
                                    binding?.endOfAudioTimeBtn,
                                    sleepTimeInMillis,
                                    sleepTimeIV,
                                    sleepTimerTV
                                )
                            }
                        }
                    }
                    R.id.fifteenMinBtn -> {
                        sleepTimeInMillis = 15L * 60000
                        if (AllSongFragment.musicService != null) {
                            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                                AllSongFragment.musicService?.startSleepTimeCountDown(
                                    binding?.countingTimeTV!!,
                                    binding?.rlEndSleepTimer,
                                    binding?.endOfAudioTimeBtn,
                                    sleepTimeInMillis,
                                    sleepTimeIV,
                                    sleepTimerTV
                                )
                            }
                        }
                    }

                    R.id.thirtyMinBtn -> {
                        sleepTimeInMillis = 30L * 60000
                        if (AllSongFragment.musicService != null) {
                            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                                AllSongFragment.musicService?.startSleepTimeCountDown(
                                    binding?.countingTimeTV!!,
                                    binding?.rlEndSleepTimer,
                                    binding?.endOfAudioTimeBtn,
                                    sleepTimeInMillis,
                                    sleepTimeIV,
                                    sleepTimerTV
                                )
                            }
                        }
                    }
                    R.id.fourtyMinBtn -> {
                        sleepTimeInMillis = 40L * 60000
                        if (AllSongFragment.musicService != null) {
                            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                                AllSongFragment.musicService?.startSleepTimeCountDown(
                                    binding?.countingTimeTV!!,
                                    binding?.rlEndSleepTimer,
                                    binding?.endOfAudioTimeBtn,
                                    sleepTimeInMillis,
                                    sleepTimeIV,
                                    sleepTimerTV
                                )
                            }
                        }
                    }
                    R.id.sixtyMinBtn -> {
                        sleepTimeInMillis = 60L * 60000
                        if (AllSongFragment.musicService != null) {
                            if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                                AllSongFragment.musicService?.startSleepTimeCountDown(
                                    binding?.countingTimeTV!!,
                                    binding?.rlEndSleepTimer,
                                    binding?.endOfAudioTimeBtn,
                                    sleepTimeInMillis,
                                    sleepTimeIV,
                                    sleepTimerTV
                                )
                            }
                        }
                    }
                }
            }
        }

        binding!!.increaseSleepTime.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                    val sleepTime: Long? = storageUtil?.getSleepTime()
                    val increasedSleepTime = sleepTime!! + (5 * 60000)
                    if (increasedSleepTime <= 60 * 60000) {
                        AllSongFragment.musicService?.startSleepTimeCountDown(
                            binding?.countingTimeTV!!,
                            binding?.rlEndSleepTimer,
                            binding?.endOfAudioTimeBtn,
                            sleepTime + (5 * 60000),
                            sleepTimeIV,
                            sleepTimerTV
                        )
                    }
                }
            }
        }

        binding!!.decreaseSleepTime.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                    val sleepTime: Long? = storageUtil?.getSleepTime()
                    if (sleepTime!! > (5 * 60000)) {
                        AllSongFragment.musicService?.startSleepTimeCountDown(
                            binding?.countingTimeTV!!,
                            binding?.rlEndSleepTimer,
                            binding?.endOfAudioTimeBtn,
                            sleepTime - (5 * 60000),
                            sleepTimeIV,
                            sleepTimerTV
                        )
                    }
                }
            }
        }

        binding!!.cancelOrStopTimerBtn.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                if (AllSongFragment.musicService?.isSleepTimeRunning!!) {
                    AllSongFragment.musicService?.stopSleepTimer(sleepTimerTV, sleepTimeIV)
                }
            }
            dismiss()
        }

        binding!!.startTimerBtn.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                if (!AllSongFragment.musicService?.isSleepTimeRunning!!) {
                    // if sleep timer is not running then run it
                    if (sleepTimeInMillis == 0L) {
                        sleepTimeInMillis = 5L * 60000
                    }
                    AllSongFragment.musicService?.startSleepTimeCountDown(
                        binding?.countingTimeTV!!,
                        binding?.rlEndSleepTimer,
                        binding?.endOfAudioTimeBtn,
                        sleepTimeInMillis,
                        sleepTimeIV,
                        sleepTimerTV
                    )
                }
            }
            dismiss()
        }

        binding!!.endOfAudioTimeBtn.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                //if (AllSongFragment.musicService?.mediaPlayer != null) {
                audioList = storageUtil?.loadAudio()!!
                audioIndex = storageUtil?.loadAudioIndex()!!
                    sleepTimeInMillis = audioList[audioIndex].duration.toLong()
                        /*AllSongFragment.musicService?.mediaPlayer?.duration?.toLong()!!*/

                    if (!AllSongFragment.musicService?.isSleepTimeRunning!!) {
                        AllSongFragment.musicService?.startSleepTimeCountDown(
                            binding?.countingTimeTV!!,
                            binding?.rlEndSleepTimer,
                            binding?.endOfAudioTimeBtn,
                            sleepTimeInMillis,
                            sleepTimeIV,
                            sleepTimerTV
                        )
                    }
               // }
            }
            dismiss()
        }

        return view
    }

    private fun millisToMinutesAndSeconds(millis: Long): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds.toInt() == 60) "${(minutes.toInt() + 1)} : 00" else "${minutes.toInt()} : ${if (seconds < 10) "0" else ""}$seconds "
    }
}