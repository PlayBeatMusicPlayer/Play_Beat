package com.knesarcreation.playbeat.fragments.bottomSheets

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.AudioTrimmerActivity
import com.knesarcreation.playbeat.databinding.BottomSheetAudioMoreOptionBinding
import com.knesarcreation.playbeat.db.PlaylistEntity
import com.knesarcreation.playbeat.db.toSongEntity
import com.knesarcreation.playbeat.fragments.LibraryViewModel
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatColoredTarget
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class BottomSheetAudioMoreOptions(
    private var mode: Int,
    private var song: Song,
    private var mActivity: FragmentActivity
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioMoreOptionBinding? = null
    private val binding get() = _binding

    var listener: SingleSelectionMenuOption? = null
    val libraryViewModel: LibraryViewModel by sharedViewModel()


    interface SingleSelectionMenuOption {
        fun playNext()
        fun addToPlayingQueue()
        fun addToPlaylist()
        fun goToAlbum()
        fun goToArtist()
        fun share()
        fun tagEditor()
        fun details()
        fun setAsRingtone()
        fun addToBlackList()
        fun deleteFromDevice()
        fun removeFromPlayingQueue()
        fun removeFromPlaylist()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SingleSelectionMenuOption
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetAudioMoreOptionBinding.inflate(inflater, container, false)
        val view = binding?.root

        /*
         * mode 1 -> queue song menu
         * mode 2 -> playlist song menu
         * */

        setUpView()

        when (mode) {
            1 -> {
                binding?.llAddToQueue?.visibility = View.GONE
                binding?.llAddToBlacklist?.visibility = View.GONE
                binding?.llRemoveFromPlayingQueue?.visibility = View.VISIBLE
            }
            2 -> {
                binding?.llRemoveFromPlaylist?.visibility = View.VISIBLE
                binding?.llAddToBlacklist?.visibility = View.GONE
            }
            //4 -> {}
        }

        handleClickListeners()

        return view
    }

    private fun setUpView() {
        GlideApp.with(mActivity).asBitmapPalette().songCoverOptions(song)
            .load(PlayBeatGlideExtension.getSongModel(song))
            .into(object : PlayBeatColoredTarget(binding?.image!!) {
                override fun onColorReady(colors: MediaNotificationProcessor) {}
            })

        binding?.songNameTV?.text = song.title
        binding?.albumNameTv?.text = song.albumName
        binding?.artistNameTV?.text = song.artistName

        updateIsFavorite(true)

        binding?.likedAudioIV?.setOnClickListener {
            toggleFavorite(song)
        }
    }

    private fun handleClickListeners() {
        binding?.llPlayNext?.setOnClickListener {
            listener?.playNext()
        }

        binding?.llAddToQueue?.setOnClickListener {
            listener?.addToPlayingQueue()
        }
        binding?.llAddToPlaylist?.setOnClickListener {
            listener?.addToPlaylist()
        }
        binding?.llGoToAlbum?.setOnClickListener {
            listener?.goToAlbum()
        }
        binding?.llGoToArtist?.setOnClickListener {
            listener?.goToArtist()
        }
        binding?.llSharee?.setOnClickListener {
            listener?.share()
        }
        binding?.llTagEditor?.setOnClickListener {
            listener?.tagEditor()
        }
        binding?.llDetails?.setOnClickListener {
            listener?.details()
        }
        binding?.llSetAsRingtone?.setOnClickListener {
            listener?.setAsRingtone()
        }
        binding?.llAddToBlacklist?.setOnClickListener {
            listener?.addToBlackList()
        }

        binding?.llDeleteFromDevice?.setOnClickListener {
            listener?.deleteFromDevice()
        }

        binding?.llRemoveFromPlayingQueue?.setOnClickListener {
            listener?.removeFromPlayingQueue()
        }

        binding?.llRemoveFromPlaylist?.setOnClickListener {
            listener?.removeFromPlaylist()
        }

        binding?.llTrimAudio?.setOnClickListener {
            val intent = Intent(activity as Context, AudioTrimmerActivity::class.java)
            val gson = Gson()
            val audioData = gson.toJson(song)
            intent.putExtra("AudioData", audioData)
            startActivity(intent)
            dismiss()
        }
    }

    private fun updateIsFavorite(animate: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            val isFavorite: Boolean =
                libraryViewModel.isSongFavorite(song.id)
            withContext(Dispatchers.Main) {
                val icon: Int = if (animate && VersionUtils.hasMarshmallow()) {
                    if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
                } else {
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                }
                val drawable: Drawable = if(isFavorite) PlayBeatUtil.getTintedVectorDrawable(
                    requireContext(),
                    icon,
                    Color.RED
                ) else PlayBeatUtil.getTintedVectorDrawable(
                    requireContext(),
                    icon,
                    Color.GRAY
                )
                if (binding != null) {
                    binding?.likedAudioIV?.apply {
                        setImageDrawable(drawable)
                        if (drawable is AnimatedVectorDrawable) {
                            drawable.start()
                        }

                    }
                }
            }
        }
    }

    private
    fun toggleFavorite(song: Song) {
        lifecycleScope.launch(Dispatchers.IO) {
            val playlist: PlaylistEntity = libraryViewModel.favoritePlaylist()
            if (playlist != null) {
                val songEntity = song.toSongEntity(playlist.playListId)
                val isFavorite = libraryViewModel.isSongFavorite(song.id)
                if (isFavorite) {
                    libraryViewModel.removeSongFromPlaylist(songEntity)
                } else {
                    libraryViewModel.insertSongs(listOf(song.toSongEntity(playlist.playListId)))
                }
            }
            libraryViewModel.forceReload(ReloadType.Playlists)
            requireContext().sendBroadcast(Intent(MusicService.FAVORITE_STATE_CHANGED))
            updateIsFavorite(true)
        }
    }

}