package com.knesarcreation.playbeat.fragment

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.adapter.ArtistsAlbumAdapter
import com.knesarcreation.playbeat.databinding.FragmentArtistsTracksAndAlbumBinding
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.model.ArtistsModel
import com.knesarcreation.playbeat.utils.AudioPlayingFromCategory
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class ArtistsTracksAndAlbumFragment : Fragment(), AllSongsAdapter.OnClickSongItem {

    private var _binding: FragmentArtistsTracksAndAlbumBinding? = null
    private val binding get() = _binding
    private lateinit var viewmodel: DataObservableClass
    private var artistsModel: ArtistsModel? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private val albumList = CopyOnWriteArrayList<AlbumModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentArtistsTracksAndAlbumBinding.inflate(inflater, container, false)
        val view = binding?.root
        binding?.artisNameTVToolbar?.isSelected = true
        binding?.artisNameTV?.isSelected = true
        viewmodel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        viewmodel.artistsData.observe(viewLifecycleOwner, {
            convertGsonToAlbumModel(it)

            binding?.artisNameTV?.text = artistsModel?.artistName
            binding?.artisNameTVToolbar?.text = artistsModel?.artistName
            if (artistsModel?.noOfTracks == 1 && artistsModel?.noOfAlbums == 1) {
                binding?.TracksAndAlbum?.text =
                    "${artistsModel?.noOfTracks} Track  |  ${artistsModel?.noOfAlbums} Album"
            } else {
                binding?.TracksAndAlbum?.text =
                    "${artistsModel?.noOfTracks} Tracks  |  ${artistsModel?.noOfAlbums} Albums"
            }

            loadArtistFromMedia()
            loadAlbumFromMedia()

        })

        return view
    }

    private fun loadAlbumFromMedia() {

        albumList.clear()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM_ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
        )

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Artists.ARTIST} = ?"
        val selectionArgs = arrayOf(artistsModel?.artistName)

        // Display audios in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

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
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                val albumModel =
                    AlbumModel(
                        albumId,
                        album,
                        artist,
                        artUri
                    )
                if (!albumList.contains(albumModel)) {
                    albumList.add(albumModel)
                }
            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
                binding?.rvAlbums?.adapter =
                    ArtistsAlbumAdapter(
                        activity as Context,
                        albumList
                        /*object : AllAlbumsAdapter.OnAlbumClicked {
                            override fun onClicked(albumModel: AlbumModel) {
                                val gson = Gson()
                                val album = gson.toJson(albumModel)
                                listener?.openAlbum(album)
                            }
                        }*/
                    )

                cursor.close()
            }
        }


    }

    private fun loadArtistFromMedia() {
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
        val selection =
            "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Artists.ARTIST} =?"
        val selectionArgs = arrayOf(
            TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString(),
            artistsModel?.artistName
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

                //getting audio uri
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
                    AllSongsAdapter(activity as Context, audioList, this)
                binding!!.rvTracks.adapter = albumAdapter
            }
            cursor.close()
        }
    }

    private fun convertGsonToAlbumModel(artistsDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<ArtistsModel>() {}.type
        artistsModel = gson.fromJson(artistsDataString, type)
    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        val storageUtil = StorageUtil(activity as Context)
        AudioPlayingFromCategory.audioPlayingFromAlbumORArtist = true

        storageUtil.storeAudio(audioList)
        storageUtil.storeAudioIndex(position)

        //Send a broadcast to the service -> PLAY_NEW_AUDIO
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)

        (activity as Context).sendBroadcast(broadcastIntent)
        Log.d(
            "isAudioListSaved",
            "onAudioPlayed: serviceBound is active: $//musicService "
        )
    }

}