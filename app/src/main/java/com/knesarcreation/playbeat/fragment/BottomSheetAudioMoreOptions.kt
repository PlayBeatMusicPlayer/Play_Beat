package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activity.TagEditorActivity
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetAudioMoreOptionBinding
import com.knesarcreation.playbeat.utils.GetRealPathOfUri
import com.knesarcreation.playbeat.utils.PlaybackStatus
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.round

class BottomSheetAudioMoreOptions(
    var mContext: Context,
    var allSongsModel: AllSongsModel,
    var openFromExpandedPlayer: Boolean,
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAudioMoreOptionBinding? = null
    private val binding get() = _binding
    private lateinit var storage: StorageUtil
    private var audioIndexPos = 0
    private lateinit var mViewModelClass: ViewModelClass
    private var isTempFavAudio = false
    var listener: SingleSelectionMenuOption? = null

    interface SingleSelectionMenuOption {
        fun playNext()
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

        storage = StorageUtil(mContext)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        setUpViews()

        addToPlaylist()

        if (openFromExpandedPlayer) {
            binding?.songDetailsContainer?.visibility = View.GONE
            binding?.horizontalView?.visibility = View.GONE
            binding?.llPlayNext?.visibility = View.GONE
            binding?.llAddToQueue?.visibility = View.GONE
            binding?.llEqualizer?.visibility = View.VISIBLE
            binding?.llSavePlayingQueue?.visibility = View.VISIBLE
        }

        binding?.llEqualizer?.setOnClickListener {
            Snackbar.make(
                dialog!!.window!!.decorView,
                "Sorry for inconvenience, feature is under development", Snackbar.LENGTH_LONG
            ).show()
        }


        savePlayingQueue()

        lifecycleScope.launch(Dispatchers.IO) {
            val oneFavAudio = mViewModelClass.getOneFavAudio(allSongsModel.songId)
            if (oneFavAudio.isNotEmpty()) {
                isTempFavAudio = oneFavAudio[0].isFavourite
                if (oneFavAudio[0].isFavourite) {
                    binding!!.likedAudioIV.setImageResource(R.drawable.ic_filled_red_heart)
                } else {
                    binding!!.likedAudioIV.setImageResource(R.drawable.vd_trimclip_heart_empty)
                }
            }
        }

        likeOrUnLikeAudio()

        binding!!.llPlayNext.setOnClickListener {
            listener?.playNext()
        }

        addToPlayingQueue()

        editTagEditor()

        showDetails()

        deleteAudioFromDevice()

        shareAudio()

        return view
    }

    private fun shareAudio() {
        binding?.llShare?.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(allSongsModel.contentUri))
            startActivity(Intent.createChooser(shareIntent, "Share a audio file"))
            dismiss()
        }
    }

    private fun deleteAudioFromDevice() {
        binding?.llDeleteFromDevice?.setOnClickListener {
            val audioPath =
                GetRealPathOfUri().getUriRealPath(mContext, Uri.parse(allSongsModel.contentUri))
            if (audioPath != null) {
                try {
                    val audioFile = File(audioPath)
                    Log.d("SongThatWillBeDelete", "deleteAudioFromDevice: path: $audioFile ")
                    if (audioFile.exists()) {
                        val alertDialog =
                            AlertDialog.Builder(activity as Context, R.style.CustomAlertDialog)
                        val viewGroup: ViewGroup =
                            (activity as AppCompatActivity).findViewById(android.R.id.content)
                        val customView =
                            layoutInflater.inflate(R.layout.custom_alert_dialog, viewGroup, false)
                        val dialogTitleTV = customView.findViewById<TextView>(R.id.dialogTitleTV)
                        val dialogMessageTV =
                            customView.findViewById<TextView>(R.id.dialogMessageTV)
                        val cancelButton =
                            customView.findViewById<MaterialButton>(R.id.cancelButton)
                        val deleteBtn = customView.findViewById<MaterialButton>(R.id.positiveBtn)
                        alertDialog.setView(customView)
                        val dialog = alertDialog.create()

                        dialogTitleTV.text = "Delete song"
                        dialogMessageTV.text =
                            "Are you sure you want to delete '${allSongsModel.songName}' song"

                        deleteBtn.setOnClickListener {
                            val loadQueueAudio = storage.loadQueueAudio()
                            var currentPlayingAudioIndex = storage.loadAudioIndex()
                            val currentPlayingAudio = loadQueueAudio[currentPlayingAudioIndex]

                            Log.d("SongThatWillBeDelete", "deleteAudioFromDevice:$allSongsModel ")
                            loadQueueAudio.remove(allSongsModel)

                            mViewModelClass.deleteOneSong(allSongsModel.songId, lifecycleScope)
                            mViewModelClass.deleteOneQueueAudio(
                                allSongsModel.songId,
                                lifecycleScope
                            )

                            //delete file from storage
                            audioFile.delete()

                            MediaScannerConnection.scanFile(
                                mContext,
                                arrayOf(audioFile.path),
                                null,
                                null
                            )

                            Log.d(
                                "SongThatWillBeDelete",
                                "deleteAudioFromDevice: isAudioPlaying : ${allSongsModel.playingOrPause} "
                            )

                            if (allSongsModel.playingOrPause == 1 || allSongsModel.playingOrPause == 0) {
                                // if deleted audio was playing audio then play next audio
                                if (currentPlayingAudioIndex == loadQueueAudio.size /* there is no need to subtract size by 1 here, since one audio is already deleted */) {
                                    // last audio deleted which was playing
                                    // so play a prev audio, for that save a new index

                                    currentPlayingAudioIndex = --currentPlayingAudioIndex
                                    storage.storeAudioIndex(currentPlayingAudioIndex)
                                    Log.d(
                                        "SongThatWillBeDelete",
                                        "deleteAudioFromDevice: isAudioPlaying : $currentPlayingAudioIndex "
                                    )

                                }

                                if (AllSongFragment.musicService?.mediaPlayer != null) {
                                    storage.storeQueueAudio(loadQueueAudio)
                                    AllSongFragment.musicService?.pausedByManually = true
                                    val broadcastIntent =
                                        Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                                    (mContext as AppCompatActivity).sendBroadcast(
                                        broadcastIntent
                                    )
                                    Log.d(
                                        "SongThatWillBeDelete",
                                        "deleteAudioFromDevice: New audio played "
                                    )
                                }


                            } else {
                                // after deleting audio, current playing audio index might get changed
                                // so save a new index
                                val newIndex = loadQueueAudio.indexOf(currentPlayingAudio)
                                storage.storeAudioIndex(newIndex)
                                storage.storeQueueAudio(loadQueueAudio)
                                Log.d(
                                    "SongThatWillBeDelete",
                                    "deleteAudioFromDevice: audioIndexChanged: newIndex- $newIndex "
                                )
                            }
                           /* Snackbar.make(
                                it,
                                "${allSongsModel.songName} deleted", Snackbar.LENGTH_LONG
                            ).show()*/
                            Toast.makeText(mContext, "Song deleted", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        cancelButton.setOnClickListener {
                            dialog.dismiss()
                        }
                        dialog.show()

                        dismiss()//dismiss bottom sheet


                    } else {
                        Snackbar.make(
                            (activity as AppCompatActivity).window.decorView,
                            "File doesn't exists", Snackbar.LENGTH_LONG
                        ).show()

                    }

                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    Log.d("FileNotFoundException", "deleteAudioFromDevice:${e.message} ")
                }
            }

        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDetails() {
        binding?.llDetails?.setOnClickListener {
            val alertDialog = AlertDialog.Builder(activity as Context, R.style.CustomAlertDialog)
            val viewGroup: ViewGroup =
                (activity as AppCompatActivity).findViewById(android.R.id.content)
            val customView = layoutInflater.inflate(R.layout.dialog_audio_details, viewGroup, false)
            val fileNameTV = customView.findViewById<TextView>(R.id.fileNameTV)
            val pathNameTV = customView.findViewById<TextView>(R.id.pathNameTV)
            val fileSizeTV = customView.findViewById<TextView>(R.id.fileSizeTV)
            val formatTV = customView.findViewById<TextView>(R.id.formatTV)
            val lengthTV = customView.findViewById<TextView>(R.id.lengthTV)
            val yearTV = customView.findViewById<TextView>(R.id.yearTV)
            val artistTv = customView.findViewById<TextView>(R.id.artistTv)
            val albumTv = customView.findViewById<TextView>(R.id.albumTv)
            val closeBtn = customView.findViewById<MaterialButton>(R.id.closeBtn)
            alertDialog.setView(customView)
            val dialog = alertDialog.create()

            val audioPath =
                GetRealPathOfUri().getUriRealPath(mContext, Uri.parse(allSongsModel.contentUri))

            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(audioPath)
            val extractMetadata =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

            fileNameTV.text = allSongsModel.displayName
            pathNameTV.text = allSongsModel.data
            fileSizeTV.text = convertByteToMb(allSongsModel.size).toString() + " MB"
            formatTV.text =
                (round(extractMetadata!!.toDouble() / 1000).toInt()).toString() + " Kb/s"
            lengthTV.text = millisToMinutesAndSeconds(allSongsModel.duration)
            yearTV.text = allSongsModel.year.toString()
            artistTv.text = allSongsModel.artistsName
            albumTv.text = allSongsModel.albumName



            closeBtn.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            dismiss()
        }
    }

    private fun convertByteToMb(byte: Int): Float {
        return round((byte.toDouble() / 1000000)).toFloat()
    }

    private fun millisToMinutesAndSeconds(millis: Int): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
    }

    private fun editTagEditor() {
        binding!!.llTagEditor.setOnClickListener {
            val audioModel = Gson().toJson(allSongsModel)
            startActivity(
                Intent(
                    activity as Context,
                    TagEditorActivity::class.java
                ).putExtra("audioModel", audioModel)
            )
            dismiss()
        }
    }

    private fun addToPlayingQueue() {
        binding?.llAddToQueue?.setOnClickListener {
            var playingQueueAudioList = CopyOnWriteArrayList<AllSongsModel>()
            try {
                playingQueueAudioList = storage.loadQueueAudio()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (playingQueueAudioList.contains(allSongsModel)) {
                //remove duplicates
                playingQueueAudioList.remove(allSongsModel)
                mViewModelClass.deleteOneQueueAudio(allSongsModel.songId, lifecycleScope)
            }

            val audioIndex: Int = if (playingQueueAudioList.isEmpty()) {
                0
            } else {
                playingQueueAudioList.size
            }

            Handler(Looper.getMainLooper()).postDelayed({
                //adding to last index
                playingQueueAudioList.add(audioIndex, allSongsModel)
                val queueListModel = QueueListModel(
                    allSongsModel.songId,
                    allSongsModel.albumId,
                    allSongsModel.songName,
                    allSongsModel.artistsName,
                    allSongsModel.albumName,
                    allSongsModel.size,
                    allSongsModel.duration,
                    allSongsModel.data,
                    allSongsModel.contentUri,
                    allSongsModel.artUri,
                    allSongsModel.playingOrPause,
                    allSongsModel.dateAdded,
                    allSongsModel.isFavourite,
                    allSongsModel.favAudioAddedTime,
                    allSongsModel.mostPlayedCount,
                    allSongsModel.artistId
                )

                queueListModel.currentPlayedAudioTime = allSongsModel.currentPlayedAudioTime
                mViewModelClass.insertQueue(queueListModel, lifecycleScope)
                storage.storeQueueAudio(playingQueueAudioList)
                if (allSongsModel.playingOrPause == 1) {
                    storage.storeAudioIndex(audioIndex)
                }
            }, 1000)

            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "Added 1 song to playing queue", Snackbar.LENGTH_LONG
            ).show()

            dismiss()
        }
    }

    private fun addToPlaylist() {
        binding?.llAddToPlaylist?.setOnClickListener {
            val bottomSheetChooseToPlaylist = BottomSheetChoosePlaylist(allSongsModel, true, null)
            bottomSheetChooseToPlaylist.show(
                (mContext as AppCompatActivity).supportFragmentManager,
                "bottomSheetChooseToPlaylist"
            )
            dismiss()
        }
    }

    private fun savePlayingQueue() {
        binding?.llSavePlayingQueue?.setOnClickListener {
            // this option for expended player
            val playingQueueAudio = ArrayList<Long>()
            val loadQueueAudio = storage.loadQueueAudio()

            for (audio in loadQueueAudio) {
                playingQueueAudio.add(audio.songId)
            }

            val audioJson = convertListToString(playingQueueAudio)
            val bottomSheetCreatePlaylist =
                BottomSheetCreateOrRenamePlaylist(activity as Context, audioJson, true, null)
            bottomSheetCreatePlaylist.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetCreatePlaylist"
            )
            dismiss()
        }
    }

    private fun setUpViews() {
        binding?.songNameTV?.text = allSongsModel.songName
        binding?.artistNameTV?.text = allSongsModel.artistsName
        binding?.albumNameTv?.text = allSongsModel.albumName

        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        Glide.with(binding?.albumArtIv!!).load(allSongsModel.artUri)
            .transition(withCrossFade(factory)).apply(
                RequestOptions.placeholderOf(
                    R.drawable.music_note_icon
                )
            ).into(binding?.albumArtIv!!)
    }

    private fun likeOrUnLikeAudio() {
        binding!!.likedAudioIV.setOnClickListener {
            /* val queueList = storage.loadQueueAudio()
             val indexOf = queueList.indexOf(allSongsModel)
             val queueAudioModel = queueList[indexOf]*/

            audioIndexPos = storage.loadAudioIndex()
            val isFav = !isTempFavAudio // false -> true
            if (isFav) {
                // mark it fav
                isTempFavAudio = isFav
                binding!!.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_fill)
                showLikedAudioAnim()
            } else {
                isTempFavAudio = isFav
                // mark it un_fav
                binding!!.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_break)
                showLikedAudioAnim()
            }

            if (AllSongFragment.musicService?.mediaPlayer != null) {
                if (isFav) {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PLAYING,
                            PlaybackStatus.FAVOURITE,
                            1f
                        )
                    } else {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PAUSED,
                            PlaybackStatus.FAVOURITE,
                            0f
                        )
                    }
                } else {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PLAYING,
                            PlaybackStatus.UN_FAVOURITE,
                            1f
                        )
                    } else {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PAUSED,
                            PlaybackStatus.UN_FAVOURITE,
                            0f
                        )
                    }
                }
            }
            addAudioToFavourites(isFav)
        }
    }

    private fun addAudioToFavourites(isFav: Boolean) {
        val queueAudioList = storage.loadQueueAudio()
        val favAudioAddedTime = System.currentTimeMillis()
        mViewModelClass.updateFavouriteAudio(
            isFav,
            allSongsModel.songId,
            favAudioAddedTime,
            lifecycleScope,
        )

        val list = CopyOnWriteArrayList<AllSongsModel>()
        for (audio in queueAudioList) {
            if (audio.songId == allSongsModel.songId) {
                val allSongsModel = AllSongsModel(
                    audio.songId,
                    audio.albumId,
                    audio.songName,
                    audio.artistsName,
                    audio.albumName,
                    audio.size,
                    audio.duration,
                    audio.data,
                    audio.contentUri,
                    audio.artUri,
                    audio.dateAdded,
                    isFav,
                    favAudioAddedTime,
                    audio.artistId,
                    audio.displayName,
                    audio.contentType,
                    audio.year
                )
                allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
                allSongsModel.playingOrPause = audio.playingOrPause
                list.add(allSongsModel)
            } else {
                val allSongsModel = AllSongsModel(
                    audio.songId,
                    audio.albumId,
                    audio.songName,
                    audio.artistsName,
                    audio.albumName,
                    audio.size,
                    audio.duration,
                    audio.data,
                    audio.contentUri,
                    audio.artUri,
                    audio.dateAdded,
                    audio.isFavourite,
                    audio.favAudioAddedTime,
                    audio.artistId,
                    audio.displayName,
                    audio.contentType,
                    audio.year
                )
                allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
                allSongsModel.playingOrPause = audio.playingOrPause
                list.add(allSongsModel)
            }
        }
        storage.storeQueueAudio(list)
    }

    private fun convertListToString(audioListFromJson: ArrayList<Long>): String {
        val gson = Gson()
        return gson.toJson(audioListFromJson)
    }

    private fun showLikedAudioAnim() {
        val animatedVectorDrawable =
            binding!!.likedAudioIV.drawable as AnimatedVectorDrawable
        animatedVectorDrawable.start()
    }

    /*override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnAudioDeleteFromPlaylist
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/
}