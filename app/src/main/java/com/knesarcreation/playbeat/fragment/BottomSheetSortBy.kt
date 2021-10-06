package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.BottomSheetSortingOptionsBinding
import com.knesarcreation.playbeat.utils.StorageUtil

class BottomSheetSortBy(
    var mContext: Context,
    var sortFrom: String,
    var playlistSortAudioKey: String
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortingOptionsBinding? = null
    private val binding get() = _binding
    var listener: OnSortingAudio? = null

    interface OnSortingAudio {
        fun byDate() {}
        fun byName() {}
        fun byDuration() {}
        fun byArtistName() {}
        fun year() {}
        fun count() {}
        fun defaultOrder() {}
        fun albumName() {}
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetSortingOptionsBinding.inflate(inflater, container, false)
        val view = binding?.root

        val storage = StorageUtil(mContext)

        when (sortFrom) {
            "audio" -> {
                when (storage.getAudioSortedValue(StorageUtil.AUDIO_KEY)) {
                    "Name" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }
                    "Duration" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.durationBtn)
                    }
                    "DateAdded" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.dateAddedBtn)
                    }
                    "ArtistName" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.artistNameBtn)
                    }
                    else -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }
                }
                binding!!.sortByAlbumToggleGroup.visibility = View.GONE
            }

            "album" -> {
                binding!!.sortByAudioToggleGroup.visibility = View.GONE
                when (storage.getAudioSortedValue(StorageUtil.ALBUM_AUDIO_KEY)) {
                    "year" -> {
                        binding!!.sortByAlbumToggleGroup.check(R.id.yearBtn)
                    }
                    "AlbumName" -> {
                        binding!!.sortByAlbumToggleGroup.check(R.id.albumNameBtn)
                    }
                    "SongCount" -> {
                        binding!!.sortByAlbumToggleGroup.check(R.id.songCountBtn)
                    }
                    "AlbumArtistName" -> {
                        binding!!.sortByAlbumToggleGroup.check(R.id.albumArtistNameBtn)
                    }
                    else -> {
                        binding!!.sortByAlbumToggleGroup.check(R.id.albumNameBtn)
                    }
                }

            }

            "favourites" -> {
                binding!!.defaultOrderBtn.visibility = View.VISIBLE
                binding!!.sortByAlbumToggleGroup.visibility = View.GONE
                binding!!.durationBtn.visibility = View.GONE

                when (storage.getAudioSortedValue(StorageUtil.FAV_AUDIO_KEY)) {
                    "Name" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }
                    "defaultOrder" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.defaultOrderBtn)
                    }
                    "DateAdded" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.dateAddedBtn)
                    }
                    "ArtistName" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.artistNameBtn)
                    }
                    else -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.defaultOrderBtn)
                    }
                }
            }

            "playlist" -> {
                binding!!.defaultOrderBtn.visibility = View.GONE
                binding!!.sortByAlbumToggleGroup.visibility = View.GONE
                binding!!.durationBtn.visibility = View.GONE
                binding!!.artistNameBtn.visibility = View.GONE

                when (storage.getAudioSortedValue(StorageUtil.PLAYLIST_KEY)) {
                    "Name" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }

                    "DateAdded" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.dateAddedBtn)
                    }

                    else -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }
                }
            }

            "playlistAudios" -> {
                binding!!.defaultOrderBtn.visibility = View.GONE
                binding!!.sortByAlbumToggleGroup.visibility = View.GONE
                binding!!.durationBtn.visibility = View.GONE

                when (storage.getAudioSortedValue(playlistSortAudioKey)) {
                    "Name" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }

                    "DateAdded" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.dateAddedBtn)
                    }

                    "ArtistName" -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.artistNameBtn)
                    }

                    else -> {
                        binding!!.sortByAudioToggleGroup.check(R.id.songNameBtn)
                    }
                }
            }
        }

        binding!!.sortByAudioToggleGroup.addOnButtonCheckedListener { /*group*/_, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.dateAddedBtn -> listener?.byDate()
                    R.id.songNameBtn -> listener?.byName()
                    R.id.durationBtn -> listener?.byDuration()
                    R.id.artistNameBtn -> listener?.byArtistName()
                    R.id.defaultOrderBtn -> listener?.defaultOrder()
                }
            }
        }

        binding!!.sortByAlbumToggleGroup.addOnButtonCheckedListener { /*group*/_, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.yearBtn -> listener?.year()
                    R.id.albumNameBtn -> listener?.byName()
                    R.id.songCountBtn -> listener?.count()
                    R.id.albumArtistNameBtn -> listener?.byArtistName()
                }
            }
        }

        /*binding?.dateAddedBtn?.setOnClickListener {
            listener?.byDate()
        }
        binding?.songNameBtn?.setOnClickListener {
            listener?.bySongName()
        }
        binding?.durationBtn?.setOnClickListener {
            listener?.byDuration()
        }
        binding?.artistNameBtn?.setOnClickListener {
            listener?.byArtistName()
        }*/

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnSortingAudio
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}