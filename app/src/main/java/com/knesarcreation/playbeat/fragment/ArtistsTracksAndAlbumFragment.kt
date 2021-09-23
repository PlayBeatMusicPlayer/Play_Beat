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
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.adapter.AllAlbumsAdapter
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.adapter.ArtistsAlbumAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.databinding.FragmentArtistsTracksAndAlbumBinding
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.model.ArtistsModel
import com.knesarcreation.playbeat.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class ArtistsTracksAndAlbumFragment : Fragment()/*, AllSongsAdapter.OnClickSongItem*/ {

    private var _binding: FragmentArtistsTracksAndAlbumBinding? = null
    private val binding get() = _binding
    private lateinit var viewmodel: DataObservableClass
    private var artistsModel: ArtistsModel? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private val albumList = CopyOnWriteArrayList<AlbumModel>()
    private var progressBar: CustomProgressDialog? = null
    private var listener: OnArtistAlbumItemClicked? = null
    private lateinit var storageUtil: StorageUtil

    interface OnArtistAlbumItemClicked {
        fun openArtistAlbum(album: String)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentArtistsTracksAndAlbumBinding.inflate(inflater, container, false)
        val view = binding?.root
        binding?.artisNameTVToolbar?.isSelected = true
        binding?.artisNameTV?.isSelected = true

        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        storageUtil = StorageUtil(activity as Context)

        viewmodel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        progressBar = CustomProgressDialog(requireContext())
        progressBar!!.setMessage("Please wait...")
        progressBar!!.setIsCancelable(true)
        progressBar!!.setCanceledOnOutsideTouch(true)

        viewmodel.artistsData.observe(viewLifecycleOwner, {
            convertGsonToAlbumModel(it)

            binding?.rvAlbums?.setHasFixedSize(true)
            binding?.rvAlbums?.setItemViewCacheSize(20)
            binding?.rvTracks?.setHasFixedSize(true)
            binding?.rvTracks?.setItemViewCacheSize(20)

            binding?.artisNameTV?.text = artistsModel?.artistName
            binding?.artisNameTVToolbar?.text = artistsModel?.artistName
            if (artistsModel?.noOfTracks == 1 && artistsModel?.noOfAlbums == 1) {
                binding?.TracksAndAlbum?.text =
                    "${artistsModel?.noOfTracks} Track  |  ${artistsModel?.noOfAlbums} Album"
            } else {
                binding?.TracksAndAlbum?.text =
                    "${artistsModel?.noOfTracks} Tracks  |  ${artistsModel?.noOfAlbums} Albums"
            }

            progressBar!!.show()
            lifecycleScope.launch(Dispatchers.IO) {
                loadAlbumFromMedia()
            }
            loadArtistFromMedia()


        })

        binding?.playAll?.setOnClickListener { /*starting from 0*/
            storageUtil.saveIsShuffled(false)
            playAudio(0)
        }

        binding?.playAllToolbar?.setOnClickListener {
            storageUtil.saveIsShuffled(false)
            playAudio(0)
        }

        return view
    }

    private fun loadAlbumFromMedia() {
        albumList.clear()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            //MediaStore.Audio.Albums.ALBUM_ID,
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


                val bitmap =
                    UriToBitmapConverter.getBitmap(context?.contentResolver!!, artUri.toUri())

                //val albumBitmap = if (bitmap != null) {
                //    UriToBitmapConverter.getBitmap(context?.contentResolver!!, artUri.toUri())
                //} else {
                //   BitmapFactory.decodeResource(resources, R.drawable.album_png)
                //}

                val albumModel =
                    AlbumModel(
                        id,
                        album,
                        artist,
                        artUri,
                        bitmap
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
                        albumList,
                        object : AllAlbumsAdapter.OnAlbumClicked {
                            override fun onClicked(albumModel: AlbumModel) {
                                val gson = Gson()
                                val album = gson.toJson(albumModel)
                                listener?.openArtistAlbum(album)
                            }
                        }
                    )
                progressBar!!.dismiss()
                cursor.close()
            }
        }
    }

    private fun loadArtistFromMedia() {
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
        // val selection =
        //   "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Artists.ARTIST} =?"
        val selection =
            "${MediaStore.Audio.Artists.ARTIST} =?"
        val selectionArgs = arrayOf(
            //TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString(),
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

                //getting audio uri
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
                val albumAdapter =
                    AllSongsAdapter(activity as Context, AllSongsAdapter.OnClickListener{allSongModel, position ->
                        onClickAudio(allSongModel, position)
                    })
                binding!!.rvTracks.adapter = albumAdapter
                albumAdapter.submitList(audioList)
            }
            cursor.close()
        }
    }

    private fun onClickAudio(allSongModel: AllSongsModel, position: Int) {
        storageUtil.saveIsShuffled(false)
        playAudio(position)
    }

    private fun convertGsonToAlbumModel(artistsDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<ArtistsModel>() {}.type
        artistsModel = gson.fromJson(artistsDataString, type)
    }

   /* override fun onClick(allSongModel: AllSongsModel, position: Int) {
        storageUtil.saveIsShuffled(false)
        playAudio(position)
    }*/

    private fun playAudio(position: Int) {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as OnArtistAlbumItemClicked
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}