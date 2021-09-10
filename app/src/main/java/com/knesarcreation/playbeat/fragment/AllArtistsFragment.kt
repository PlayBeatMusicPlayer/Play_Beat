package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.knesarcreation.playbeat.adapter.AllArtistsAdapter
import com.knesarcreation.playbeat.databinding.FragmentArtistsBinding
import com.knesarcreation.playbeat.model.ArtistsModel
import java.util.concurrent.CopyOnWriteArrayList

class AllArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding
    private var artistsList = CopyOnWriteArrayList<ArtistsModel>()
    private var listener: OpenArtisFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        val view = binding?.root

//        val storage = StorageUtil(activity as AppCompatActivity)
//        val loadAudio = storage.loadAudio()
        loadArtists()

        return view
    }

    interface OpenArtisFragment {
        fun onOpenArtistFragment(artistsData: String)
    }

    private fun loadArtists() {
        artistsList.clear()
        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        // Show only audios that are at least 1 minutes in duration.
        //val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        // val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display audios in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        val query =
            (activity as Context).contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )


        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistsIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val noOfAlbumsColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val noOfTracksColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val artists = cursor.getString(artistsIdColumn)
                val noOfAlbum = cursor.getInt(noOfAlbumsColumn)
                val noOfTracks = cursor.getInt(noOfTracksColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                //val sArtworkUri = Uri
                //   .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                // val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                val allSongsModel =
                    ArtistsModel(
                        id,
                        artists,
                        noOfAlbum,
                        noOfTracks,

                    )
                artistsList.add(allSongsModel)
            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
                binding?.rvArtists?.adapter =
                    AllArtistsAdapter(
                        activity as Context,
                        artistsList,
                        object : AllArtistsAdapter.OnArtistClicked {
                            override fun getArtistData(artistsModel: ArtistsModel) {
                                val gson = Gson()
                                val artistsData = gson.toJson(artistsModel)
                                listener?.onOpenArtistFragment(artistsData)
                            }
                        }
                    )

                cursor.close()
            }
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as OpenArtisFragment
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}