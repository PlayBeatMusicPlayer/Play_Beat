package com.knesarcreation.playbeat.fragment

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.knesarcreation.playbeat.adapter.AllAlbumsAdapter
import com.knesarcreation.playbeat.databinding.FragmentAlbumBinding
import com.knesarcreation.playbeat.model.AlbumModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllAlbumsFragment : Fragment() {

    private var _binding: FragmentAlbumBinding? = null
    private val binding get() = _binding
    private val albumList = ArrayList<AlbumModel>()
    var listener: OnAlbumItemClicked? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
        val view = binding!!.root

        lifecycleScope.launch(Dispatchers.IO) {
            loadAlbum()
        }

        return view
    }

    private fun loadAlbum() {
        albumList.clear()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            //MediaStore.Audio.Albums.ALBUM_ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
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


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                //val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)

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
                        null
                    )
                if (!albumList.contains(albumModel)) {
                    albumList.add(albumModel)
                }
            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
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

                cursor.close()
            }
        }

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