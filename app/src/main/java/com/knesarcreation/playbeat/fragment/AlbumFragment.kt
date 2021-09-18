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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AlbumAdapter
import com.knesarcreation.playbeat.databinding.FragmentAlbumSongBinding
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.utils.*
import java.util.concurrent.CopyOnWriteArrayList

class AlbumFragment : Fragment(), AlbumAdapter.OnAlbumSongClicked/*, ServiceConnection*/ {

    private lateinit var viewModel: DataObservableClass
    private var _binding: FragmentAlbumSongBinding? = null
    private val binding get() = _binding
    private var albumData: AlbumModel? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var isAudioListSaved = false
    private lateinit var storageUtil: StorageUtil
    /*companion object {
        var isAlbumSongPlaying = false
    }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAlbumSongBinding.inflate(inflater, container, false)
        val view = binding!!.root

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

            loadAlbumSongs()
        })

        playAllAudioInAlbum()
        return view
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
            MediaStore.Audio.Media.DISPLAY_NAME,
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
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
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
                audioList.add(allSongsModel)

            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
                val albumAdapter =
                    AlbumAdapter(activity as Context, audioList, this)
                binding!!.rvAlbumAudio.adapter = albumAdapter


            }
            cursor.close()
        }

    }

    private fun convertGsonToAlbumModel(albumDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<AlbumModel>() {}.type
        albumData = gson.fromJson(albumDataString, type)
    }

    override fun onAudioPlayed(audioModel: AllSongsModel, position: Int) {
        storageUtil.saveIsShuffled(false)
        playAudio(position)
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
            "onAudioPlayed:$isAudioListSaved ,serviceBound is active: $//musicService "
        )

    }
}