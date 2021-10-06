package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.knesarcreation.playbeat.adapter.AllAlbumsAdapter
import com.knesarcreation.playbeat.databinding.FragmentAllAlbumsBinding
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllAlbumsFragment : Fragment() {

    private var _binding: FragmentAllAlbumsBinding? = null
    private val binding get() = _binding
    private val albumList = ArrayList<AlbumModel>()
    var listener: OnAlbumItemClicked? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAllAlbumsBinding.inflate(inflater, container, false)
        val view = binding!!.root

        lifecycleScope.launch(Dispatchers.IO) {
            loadAlbum()
        }

        sortAudios()

        return view
    }

    private fun sortAudios() {
        when (StorageUtil(activity as Context).getAudioSortedValue(StorageUtil.ALBUM_AUDIO_KEY)) {
            "year" -> {
                binding?.sortAudioTV?.text = "Year"
            }
            "AlbumName" -> {
                binding?.sortAudioTV?.text = "Album Name"
            }
            "SongCount" -> {
                binding?.sortAudioTV?.text = "Song Count"
            }
            "AlbumArtistName" -> {
                binding?.sortAudioTV?.text = "Album Artist Name"
            }
            else -> {
                binding?.sortAudioTV?.text = "Album Name"
            }
        }

        val storageUtil = StorageUtil(activity as Context)
        binding?.sortAudios?.setOnClickListener {
            val bottomSheetSortByOptions = BottomSheetSortBy(activity as Context, "album","")
            bottomSheetSortByOptions.show(
                (context as AppCompatActivity).supportFragmentManager,
                "bottomSheetSortByOptions"
            )
            bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
                override fun year() {
                    storageUtil.saveAudioSortingMethod(StorageUtil.ALBUM_AUDIO_KEY, "year")
                    val sortedByYear =
                        albumList.sortedByDescending { albumModel -> albumModel.lastYear }

                    albumList.clear()
                    albumList.addAll(sortedByYear)
                    setUpRecyclerView()

                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Year"
                }

                override fun byName() {
                    storageUtil.saveAudioSortingMethod(StorageUtil.ALBUM_AUDIO_KEY, "AlbumName")
                    val sortedByAlbumName =
                        albumList.sortedBy { albumModel -> albumModel.albumName }
                    albumList.clear()
                    albumList.addAll(sortedByAlbumName)
                    setUpRecyclerView()

                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Album Name"
                }

                override fun count() {
                    storageUtil.saveAudioSortingMethod(StorageUtil.ALBUM_AUDIO_KEY, "SongCount")
                    val sortedByCount =
                        albumList.sortedBy { albumModel -> albumModel.songCount }

                    albumList.clear()
                    albumList.addAll(sortedByCount)
                    setUpRecyclerView()

                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Song Count"
                }

                override fun byArtistName() {
                    storageUtil.saveAudioSortingMethod(
                        StorageUtil.ALBUM_AUDIO_KEY,
                        "AlbumArtistName"
                    )
                    val sortedByAlbumArtistName =
                        albumList.sortedBy { albumModel -> albumModel.artistName }
                    albumList.clear()
                    albumList.addAll(sortedByAlbumArtistName)
                    setUpRecyclerView()
                    binding?.sortAudioTV?.text = "Album Artist Name"
                    bottomSheetSortByOptions.dismiss()
                }
            }
        }
    }

    private fun loadAlbum() {
        albumList.clear()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            /*MediaStore.Audio.Albums.ALBUM_ID,*/
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
        )

        // Show only audios that are at least 1 minutes in duration.
        //val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        // val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display audios in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        val query =
            (activity as AppCompatActivity).contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            //val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR)
            val noOfSongsColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST)


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                //val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val lastYear = cursor.getInt(lastYearColumn)
                val noOfSongs = cursor.getInt(noOfSongsColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                val artUri = Uri.withAppendedPath(sArtworkUri, id.toString()).toString()

                val albumModel =
                    AlbumModel(
                        id,
                        album,
                        artist,
                        artUri,
                        null,
                        lastYear,
                        noOfSongs
                    )
                if (!albumList.contains(albumModel)) {
                    albumList.add(albumModel)
                }
            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {

                setUpRecyclerView()
                cursor.close()
            }
        }

    }

    private fun setUpRecyclerView() {
        binding?.rvAlbums?.adapter =
            AllAlbumsAdapter(
                activity as Context,
                albumList,
                object : AllAlbumsAdapter.OnAlbumClicked {
                    override fun onClicked(albumModel: AlbumModel) {
                        val gson = Gson()
                        val album = gson.toJson(albumModel)
                        listener?.openAlbum(album)
                    }
                })
        Log.d("albumListQuery", "loadAlbum: $albumList")

    }

    interface OnAlbumItemClicked {
        fun openAlbum(album: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnAlbumItemClicked
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}