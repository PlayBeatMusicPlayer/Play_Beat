package com.knesarcreation.playbeat.fragment

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentAlbumSongBinding
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class AlbumFragment : Fragment()/*, AlbumAdapter.OnAlbumSongClicked*//*, ServiceConnection*/ {

    private lateinit var viewModel: DataObservableClass
    private var _binding: FragmentAlbumSongBinding? = null
    private val binding get() = _binding
    private var albumData: AlbumModel? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var isAudioListSaved = false
    private lateinit var storageUtil: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var allSongsAdapter: AllSongsAdapter
    private var launchAlumData: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAlbumSongBinding.inflate(inflater, container, false)
        val view = binding!!.root

        mViewModelClass =
            ViewModelProvider(this)[ViewModelClass::class.java]


        binding?.arrowBackIV!!.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }
        binding?.albumNameTV?.isSelected = true

        storageUtil = StorageUtil(activity as Context)

        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.albumData.observe(viewLifecycleOwner, {
            convertGsonToAlbumModel(it)
            binding?.albumNameTV!!.text = albumData?.albumName
            //setting album art to IV
            Glide.with(activity as Context).load(albumData?.artUri).apply(
                RequestOptions.placeholderOf(
                    R.drawable.album_png
                )
            ).into(binding?.albumArtIV!!)

            //setting blurred image to Cover view
            if (albumData?.artUri != null) {
                val bitmap = UriToBitmapConverter.getBitmap(
                    (activity as Context).contentResolver,
                    albumData?.artUri!!.toUri()
                )

                val blurredImg: Bitmap = if (bitmap != null) {
                    BlurBuilder().blur(activity as Context, bitmap, 25f)
                } else {
                    //placeholder album art
                    val placeHolderBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.album_png)
                    BlurBuilder().blur(activity as Context, placeHolderBitmap, 25f)

                }

                Glide.with(activity as Context).asBitmap().load(blurredImg)
                    .into(binding?.blurredCoverIV!!)
            }

            // loadAlbumSongs()
            getAudioAccordingAlbum()

        })


        playAllAudioInAlbum()
        return view
    }

    private fun getAudioAccordingAlbum() {
        allSongsAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)
                })
        binding!!.rvAlbumAudio.adapter = allSongsAdapter
        binding!!.rvAlbumAudio.itemAnimator = null
        binding!!.rvAlbumAudio.scrollToPosition(0)

        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (launchAlumData != null && launchAlumData?.isActive!!) {
                launchAlumData?.cancel()
            }
            launchAlumData = lifecycleScope.launch(Dispatchers.IO) {
                val audioAlbum: List<AllSongsModel> =
                    mViewModelClass.getAudioAccordingAlbum(albumData?.albumName!!)
                audioList.clear()
                val sortedList = audioAlbum.sortedBy { allSongsModel -> allSongsModel.songName }
                audioList.addAll(sortedList)

                (activity as AppCompatActivity).runOnUiThread {
                    allSongsAdapter.submitList(audioAlbum.sortedBy { allSongsModel -> allSongsModel.songName })
                }

            }
        }
    }


    private fun playAllAudioInAlbum() {
        binding?.playAlbum?.setOnClickListener {
            storageUtil.saveIsShuffled(false)
            playAudio(0)
        }
    }

    private fun loadAlbumSongs() {
        audioList.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // path
        )

        // Show only audios that are at least 1 minutes in duration.
//        val selection =
//            "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Albums.ALBUM} =?"
        val selection = "${MediaStore.Audio.Albums.ALBUM} =?"
        val selectionArgs = arrayOf(
            /*TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString(),*/
            albumData?.albumName
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val query =
            (activity as Context).contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val data = cursor.getString(dataColumn)

                Log.d("SongDetails", "loadAlbumSongs: $name, $artist")

                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                val albumArtUri =
                    Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val allSongsModel =
                    AllSongsModel(
                        id,
                        albumId,
                        name,
                        artist,
                        album,
                        size,
                        duration,
                        data,
                        contentUri.toString(),
                        albumArtUri
                    )
                allSongsModel.playingOrPause = -1
                audioList.add(allSongsModel)

            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
                /*val albumAdapter =
                    AlbumAdapter(activity as Context, audioList, this)
                binding!!.rvAlbumAudio.adapter = albumAdapter*/


            }
            cursor.close()
        }

    }

    private fun convertGsonToAlbumModel(albumDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<AlbumModel>() {}.type
        albumData = gson.fromJson(albumDataString, type)
    }


    private fun onClickAudio(allSongModel: AllSongsModel, position: Int) {
        storageUtil.saveIsShuffled(false)
        val prevPlayingAudioIndex = storageUtil.loadAudioIndex()
        val audioList = storageUtil.loadAudio()
        val prevPlayingAudioModel = audioList[prevPlayingAudioIndex]

        // restricting to update if clicked audio is same
        if (allSongModel.songId != prevPlayingAudioModel.songId) {
            mViewModelClass.deleteQueue(lifecycleScope)


            mViewModelClass.updateSong(
                prevPlayingAudioModel.songId,
                prevPlayingAudioModel.songName,
                -1,
                (context as AppCompatActivity).lifecycleScope
            )

            mViewModelClass.updateSong(
                allSongModel.songId,
                allSongModel.songName,
                1,
                (context as AppCompatActivity).lifecycleScope
            )

        }

        playAudio(position)

        // restricting to update if clicked audio is same
        if (allSongModel.songId != prevPlayingAudioModel.songId) {
            // adding queue list to DB and show highlight of current audio
            for (audio in this.audioList) {
                val queueListModel = QueueListModel(
                    audio.songId,
                    audio.albumId,
                    audio.songName,
                    audio.artistsName,
                    audio.albumName,
                    audio.size,
                    audio.duration,
                    audio.data,
                    audio.audioUri,
                    audio.artUri,
                    -1
                )
                mViewModelClass.insertQueue(queueListModel, lifecycleScope)
            }

            mViewModelClass.updateQueueAudio(
                prevPlayingAudioModel.songId,
                prevPlayingAudioModel.songName,
                -1,
                (context as AppCompatActivity).lifecycleScope
            )

            mViewModelClass.updateQueueAudio(
                allSongModel.songId,
                allSongModel.songName,
                1,
                (context as AppCompatActivity).lifecycleScope
            )
        }
    }

    private fun playAudio(position: Int) {
        AudioPlayingFromCategory.audioPlayingFromAlbumORArtist = true

        storageUtil.storeAudio(audioList)
        storageUtil.storeAudioIndex(position)

        //Send a broadcast to the service -> PLAY_NEW_AUDIO
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)

        (activity as Context).sendBroadcast(broadcastIntent)
        Log.d(
            "isAudioListSaved",
            "onAudioPlayed:$isAudioListSaved ,serviceBound is active:  "
        )

    }

}