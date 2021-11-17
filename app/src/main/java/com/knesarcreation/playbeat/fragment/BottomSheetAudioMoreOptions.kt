package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.media.RingtoneManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
    private var requestIntent: ActivityResultLauncher<Intent>? = null
    private var reqWriteToSystemSetting: ActivityResultLauncher<Intent>? = null
    private lateinit var values: ContentValues

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

        requestIntent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                Log.d("Equalizer", "onCreateView: $it")
            }

        reqWriteToSystemSetting =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // writeAudioToSystemSetting()
            }

        openSystemEqualizer()

        setAsRingtone()

        return view
    }

    private fun setAsRingtone() {
        binding?.llSetAsRingtone?.setOnClickListener {
            values = ContentValues()
            values.put(MediaStore.MediaColumns.DATA, allSongsModel.data)
            values.put(MediaStore.MediaColumns.TITLE, allSongsModel.songName)
            values.put(MediaStore.MediaColumns.SIZE, allSongsModel.size)
            // values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg3")
            values.put(MediaStore.Audio.Media.ARTIST, allSongsModel.artistsName)
            values.put(MediaStore.Audio.Media.DURATION, allSongsModel.duration)
            values.put(MediaStore.Audio.Media.IS_RINGTONE, true)
            values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
            values.put(MediaStore.Audio.Media.IS_ALARM, false)
            values.put(MediaStore.Audio.Media.IS_MUSIC, false)

            checkSystemWritePermission()
        }
    }


    @SuppressLint("ObsoleteSdkInt")
    private fun checkSystemWritePermission(): Boolean {
        var permAllowed = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            permAllowed = Settings.System.canWrite(mContext)
            Log.d("WriteSettingPerm", "Can Write Settings: $permAllowed")
            if (permAllowed) {
                //Toast.makeText(mContext, "Write allowed :-)", Toast.LENGTH_LONG).show()
                writeAudioToSystemSetting()
            } else {
                //Toast.makeText(mContext, "Write not allowed :-(", Toast.LENGTH_LONG).show()
                val alertDialog = AlertDialog.Builder(mContext)
                alertDialog.setMessage("We required permission of WRITE SYSTEM SETTING to set your selected audio as default ringtone. Allow to proceed further.")
                alertDialog.setPositiveButton("Allow") { dialog, _ ->
                    permAllowed = true
                    openAndroidPermissionsMenu()
                    dialog.dismiss()
                }
                alertDialog.setNegativeButton("Cancel") { dialog, _ ->
                    permAllowed = true
                    dialog.dismiss()
                    dismiss()
                }
                alertDialog.show()

            }
        }
        return permAllowed
    }

    private fun openAndroidPermissionsMenu() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + (mContext as AppCompatActivity).packageName)
        reqWriteToSystemSetting!!.launch(intent)
    }

    private fun writeAudioToSystemSetting() {
        //Insert it into the database
        val uri = MediaStore.Audio.Media.getContentUriForPath(allSongsModel.data)
        if (uri != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RingtoneManager.setActualDefaultRingtoneUri(
                    mContext,
                    RingtoneManager.TYPE_RINGTONE,
                    Uri.parse(allSongsModel.contentUri)
                )
                Toast.makeText(mContext, "Ringtone set", Toast.LENGTH_LONG).show()
            } else {
                val filePathToDelete =
                    MediaStore.MediaColumns.DATA + "=\"" + allSongsModel.data + "\""
                (mContext as AppCompatActivity).contentResolver.delete(uri, filePathToDelete, null)

                val newUri: Uri? =
                    (mContext as AppCompatActivity).contentResolver.insert(uri, values)

                RingtoneManager.setActualDefaultRingtoneUri(
                    mContext,
                    RingtoneManager.TYPE_RINGTONE,
                    newUri
                )
                Toast.makeText(mContext, "Ringtone set", Toast.LENGTH_LONG).show()
            }

        } else {
            Toast.makeText(
                mContext,
                "Failed to set ringtone. Try with different audio.",
                Toast.LENGTH_SHORT
            ).show()
        }
        dismiss()
    }

    private fun openSystemEqualizer() {
        binding?.llEqualizer?.setOnClickListener {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            if (intent.resolveActivity((mContext as AppCompatActivity).packageManager) != null) {
                requestIntent!!.launch(intent)
            } else {
                Toast.makeText(mContext, "No Equalizer found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uriList = listOf<Uri>(Uri.parse(allSongsModel.contentUri))

                val pi: PendingIntent =
                    MediaStore.createDeleteRequest(
                        (activity as AppCompatActivity).contentResolver,
                        uriList
                    )

                try {
                    /*val registerForActivityResult =
                        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                            if (it.data != null) {
                                Log.d("deleteAudioFromDevice", "deleteAudioFromDevice: ${it.data}")
                            }
                        }

                    val isr = IntentSenderRequest.Builder(pi.intentSender).build()
                    registerForActivityResult.launch(isr)*/
                    startIntentSenderForResult(pi.intentSender, 1001, null, 0, 0, 0, null)
                    //deleteAudioReqAboveApi30()
                } catch (e: Exception) {
                    Log.d(
                        "startIntentSenderForResult",
                        "requestDeleteMultipleAudioPermission:${e.message} "
                    )
                    Toast.makeText(mContext, "Permission denied!", Toast.LENGTH_SHORT).show()
                }
                /* dismiss()*/
            } else {
                deleteAudioReqBelowApi30()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == -1) {
            // if  resultCode == -1 i.e., audio deleted
            // if resultCode == 0 i.e, permission denied
            deleteAudioReqAboveApi30()
            //Toast.makeText(mContext, "$data  ,$resultCode", Toast.LENGTH_SHORT).show()
        } else {
            // bottom sheet dismiss
            dismiss()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    // For api above 30
    private fun deleteAudioReqAboveApi30() {
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

        Toast.makeText(mContext, "Song deleted", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    // For api below 30
    private fun deleteAudioReqBelowApi30() {
        val audioPath =
            GetRealPathOfUri().getUriRealPath(mContext, Uri.parse(allSongsModel.contentUri))

        if (audioPath != null) {
            try {
                val audioFile = File(audioPath)
                Log.d("SongThatWillBeDelete", "deleteAudioFromDevice: path: $audioFile ")

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

                dialogTitleTV.text = getString(R.string.delete_song)
                dialogMessageTV.text =
                    "Are you sure you want to delete ${allSongsModel.songName} song"

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

                    if (audioFile.exists()) {
                        //delete file from storage
                        audioFile.delete()

                        MediaScannerConnection.scanFile(
                            mContext,
                            arrayOf(audioFile.path),
                            null,
                            null
                        )
                    } /*else {
                        Snackbar.make(
                            (activity as AppCompatActivity).window.decorView,
                            "File doesn't exists", Snackbar.LENGTH_LONG
                        ).show()
                    }*/

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

                    Toast.makeText(mContext, "Song deleted", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.show()

                dismiss()//dismiss bottom sheet


            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Log.d("FileNotFoundException", "deleteAudioFromDevice:${e.message} ")
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
                    audio.year,
                    audio.folderId,
                    audio.folderName,
                    audio.noOfSongs
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
                    audio.year,
                    audio.folderId,
                    audio.folderName,
                    audio.noOfSongs
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