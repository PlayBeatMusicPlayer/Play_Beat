package com.knesarcreation.playbeat.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.*
import com.knesarcreation.playbeat.databinding.ActivityTagEditorBinding
import com.knesarcreation.playbeat.utils.GetRealPathOfUri
import com.knesarcreation.playbeat.utils.MakeStatusBarTransparent
import com.knesarcreation.playbeat.utils.SavedAppTheme
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.id3.ID3v23Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File


class TagEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTagEditorBinding
    private var registerPickImageRequest: ActivityResultLauncher<Intent>? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var audioModel = ""
    private var artist = ""
    private var album = ""
    private var audioTitle = ""
    private var tempArtist = ""
    private var tempAlbum = ""
    private var tempAudioTitle = ""
    private var newAlbumArtUri: Uri? = null
    private lateinit var allSongsModel: AllSongsModel
    private var albumData: AlbumModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MakeStatusBarTransparent().transparent(this)

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]
        audioModel = intent?.getStringExtra("audioModel")!!

        binding.arrowBackIV.setOnClickListener {
            onBackPressed()
        }

        allSongsModel = convertStringToAudioModel(audioModel)
        binding.audioTitleTV.text = allSongsModel.songName
        binding.artisNameTV.text = allSongsModel.artistsName
        binding.etTitle.setText(allSongsModel.songName)
        binding.etAlbum.setText(allSongsModel.albumName)
        binding.etArtist.setText(allSongsModel.artistsName)

        tempAudioTitle = allSongsModel.songName
        tempAlbum = allSongsModel.albumName
        tempArtist = allSongsModel.artistsName

        val factory = DrawableCrossFadeFactory.Builder(200)

        Glide.with(binding.albumArtIV).load(allSongsModel.artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.music_note_icon))
            .transition(withCrossFade(factory))
            .into(binding.albumArtIV)

        saveEditedTag()

        pickImageRequest()

        registerActivityPickImageRequest()
    }


    private fun saveEditedTag() {
        binding.saveTag.setOnClickListener {
            if (binding.etTitle.text.toString().isNotEmpty() /*&& binding.etArtist.text.toString()
                    .isNotEmpty() && binding.etAlbum.text.toString().isNotEmpty()*/
            ) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val albumList: List<AlbumModel> =
                        mViewModelClass.getOnAlbum(allSongsModel.albumName)
                    Log.d("albumListabcdefgh", "saveEditedTag: $albumList ")
                    if (albumList.isNotEmpty()) {
                        albumData = albumList[0]
                    }
                    audioTitle = binding.etTitle.text.toString().trim()
                    artist = binding.etArtist.text.toString().trim()
                    album = binding.etAlbum.text.toString().trim()

                    if (audioTitle == "")
                        audioTitle = tempAudioTitle

                    if (artist == "")
                        artist = tempArtist

                    if (album == "")
                        album = tempAlbum

                    /*  runOnUiThread {
                          Toast.makeText(
                              this@TagEditorActivity,
                              "title: $audioTitle , artist: $artist , album: $album",
                              Toast.LENGTH_SHORT
                          ).show()
                      }*/

                    updateAudioTagsToDatabase()

                }
            } else {
                Toast.makeText(this, "Title should not empty.", Toast.LENGTH_SHORT).show()
            }

            // updateAlbumArtMediaStore(this, allSongsModel.albumId, uriRealPath)
        }
    }

    private fun registerActivityPickImageRequest() {
        registerPickImageRequest =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it != null) {
                    if (it.data != null) {
                        newAlbumArtUri = it.data?.data!!
                        //val uriRealPath = GetRealPathOfUri().getUriRealPath(this, newAlbumArtUri!!)
                        //Toast.makeText(this, "$newAlbumArtUri", Toast.LENGTH_SHORT).show()
                        Log.d(
                            "newAlbumArtUriTagEditor",
                            "registerActivityPickImageRequest:${newAlbumArtUri} "
                        )

                        val factory = DrawableCrossFadeFactory.Builder(200)
                        Glide.with(binding.albumArtIV).load(newAlbumArtUri)
                            .apply(RequestOptions.placeholderOf(R.drawable.music_note_icon))
                            .transition(withCrossFade(factory))
                            .into(binding.albumArtIV)
                    }

                }
            }
    }

    private fun pickImageRequest() {
        binding.editAudioAlbumArt.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_PICK
            intent.type = "image/*"
            registerPickImageRequest?.launch(intent)
        }
    }

    private fun updateMetadata() {
        // Apply a grayscale filter to the image at the given content URI.
        var uriRealPath: String? = null
        if (newAlbumArtUri != null) {
            uriRealPath = GetRealPathOfUri().getUriRealPath(this, newAlbumArtUri!!)
        } /*else {
            GetRealPathOfUri().getUriRealPath(this, Uri.parse(allSongsModel.artUri))
        }*/

        Log.d(
            "uriRealPathTagEditor",
            "saveEditedTag: dataPath : $uriRealPath"
        )

        val src = File(allSongsModel.data)
        var albumArtFile: File? = null
        try {
            albumArtFile = File(uriRealPath!!)
            Log.d("SuccessTagEditor", "updateMetadata: $albumArtFile ")
        } catch (e: Exception) {
            Log.d("FileNoFoundTagEditor", "updateMetadata: ${e.message} ")
        }

        try {
            TagOptionSingleton.getInstance().isAndroid = true
            ///TagOptionSingleton.getInstance()
            val musicFile = AudioFileIO.read(src)
            musicFile.tag = ID3v23Tag()
            val tag = musicFile.tag

            tag.setField(FieldKey.TITLE, binding.etTitle.text.toString())
            tag.setField(FieldKey.ARTIST, binding.etArtist.text.toString())
            tag.setField(FieldKey.ALBUM, binding.etAlbum.text.toString())

            if (albumArtFile != null) {
                val cover = ArtworkFactory.createArtworkFromFile(albumArtFile)
                //Toast.makeText(applicationContext, "$cover", Toast.LENGTH_SHORT).show()

                cover.setFromFile(albumArtFile)
                tag.deleteArtworkField()
                tag.createField(cover)
                tag.addField(cover)
                tag.setField(cover)
            } else {
                val firstArtwork = ArtworkFactory.createArtworkFromFile(src)
                firstArtwork.setFromFile(src)
                tag.deleteArtworkField()
                tag.createField(firstArtwork)
                tag.addField(firstArtwork)
                tag.setField(firstArtwork)
                // Toast.makeText(this, "$firstArtwork", Toast.LENGTH_SHORT).show()
            }

            musicFile.tag = tag
            musicFile.commit()


            /* deprecated method*/
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(src)
            sendBroadcast(intent)

            // update audio tag to database
            updateAudioTagsToDatabase()

            finish()

        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Failed  , ${e.message}", Toast.LENGTH_SHORT)
                .show()
            Log.d("fieldsTagEditorCover", "updateMetadata:${e.message} ")
            e.printStackTrace()
        }
        //Toast.makeText(this, "name: $title , $src", Toast.LENGTH_SHORT).show()

    }

    private fun updateAudioTagsToDatabase() {
        //val bitmap = BitmapFactory.decodeFile(albumArtFile!!.path)
        //val stream = ByteArrayOutputStream()
        //bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        //val byteArray = stream.toByteArray()
        //bitmap.recycle()

        val storage = StorageUtil(this)
        val loadQueueAudio = storage.loadQueueAudio()
        val audioList = storage.loadAudio()
        Log.d("loadQueueAudio", "updateAudioTagsToDatabase:$loadQueueAudio ")
        var indexOfSelectedAudio = 0
        if (loadQueueAudio.isNotEmpty())
            indexOfSelectedAudio = loadQueueAudio.indexOf(allSongsModel)

        if (newAlbumArtUri != null) {
            if (loadQueueAudio.isNotEmpty() && indexOfSelectedAudio != -1) {
                mViewModelClass.deleteQueue(lifecycleScope)
                //update queue audio
                loadQueueAudio[indexOfSelectedAudio].songName = audioTitle.trim()
                loadQueueAudio[indexOfSelectedAudio].albumName = album.trim()
                loadQueueAudio[indexOfSelectedAudio].artistsName = artist.trim()
                loadQueueAudio[indexOfSelectedAudio].artUri = newAlbumArtUri!!.toString()

                //update audio list
                val audioModel =
                    audioList.find { allSongsModel -> allSongsModel.songId == loadQueueAudio[indexOfSelectedAudio].songId }
                audioModel!!.songName = audioTitle.trim()
                audioModel.albumName = album.trim()
                audioModel.artistsName = artist.trim()
                audioModel.artUri = newAlbumArtUri!!.toString()
            }

            mViewModelClass.updateAudioTagsInAllSongsModel(
                allSongsModel.songId,
                audioTitle,
                album,
                artist,
                newAlbumArtUri!!.toString(),
                lifecycleScope
            )

            var lastYear = 0
            var songCount = 0

            if (albumData != null) {
                lastYear = albumData!!.lastYear
                songCount = albumData!!.songCount
            }

            val albumModel = AlbumModel(
                allSongsModel.albumId,
                album,
                artist,
                newAlbumArtUri!!.toString(),
                lastYear,
                songCount
            )
            mViewModelClass.insertAlbum(albumModel, lifecycleScope)

            val artistModel = ArtistsModel(
                allSongsModel.artistId,
                artist
            )
            mViewModelClass.insertArtist(artistModel, lifecycleScope)

            if (indexOfSelectedAudio != -1 && loadQueueAudio.isNotEmpty()) {
                storage.storeQueueAudio(loadQueueAudio)
                storage.storeAudio(audioList)
                for (audio in loadQueueAudio) {
                    val queueListModel = QueueListModel(
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
                        audio.playingOrPause,
                        audio.dateAdded,
                        audio.isFavourite,
                        audio.favAudioAddedTime,
                        audio.mostPlayedCount,
                        audio.artistId
                    )
                    mViewModelClass.insertQueue(queueListModel, lifecycleScope)
                }
            }

            Log.d(
                "NewQueueAudios",
                "updateAudioTagsToDatabase:${loadQueueAudio[indexOfSelectedAudio]} "
            )
        } else {
            if (loadQueueAudio.isNotEmpty() && indexOfSelectedAudio != -1) {
                mViewModelClass.deleteQueue(lifecycleScope)
                loadQueueAudio[indexOfSelectedAudio].songName = audioTitle
                loadQueueAudio[indexOfSelectedAudio].albumName = album
                loadQueueAudio[indexOfSelectedAudio].artistsName = artist

                //update audio list
                val audioModel =
                    audioList.find { allSongsModel -> allSongsModel.songId == loadQueueAudio[indexOfSelectedAudio].songId }
                audioModel!!.songName = audioTitle.trim()
                audioModel.albumName = album.trim()
                audioModel.artistsName = artist.trim()
                audioModel.artUri = allSongsModel.artUri
            }

            mViewModelClass.updateAudioTagsInAllSongsModel(
                allSongsModel.songId,
                audioTitle,
                album,
                artist,
                allSongsModel.artUri,
                lifecycleScope
            )

            var lastYear = 0
            var songCount = 0

            if (albumData != null) {
                lastYear = albumData!!.lastYear
                songCount = albumData!!.songCount
            }

            val albumModel = AlbumModel(
                allSongsModel.albumId,
                album,
                artist,
                allSongsModel.artUri,
                lastYear,
                songCount
            )
            mViewModelClass.insertAlbum(albumModel, lifecycleScope)

            val artistModel = ArtistsModel(
                allSongsModel.artistId,
                artist
            )
            mViewModelClass.insertArtist(artistModel, lifecycleScope)

            if (indexOfSelectedAudio != -1 && loadQueueAudio.isNotEmpty()) {
                storage.storeQueueAudio(loadQueueAudio)
                storage.storeAudio(audioList)
                for (audio in loadQueueAudio) {
                    val queueListModel = QueueListModel(
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
                        audio.playingOrPause,
                        audio.dateAdded,
                        audio.isFavourite,
                        audio.favAudioAddedTime,
                        audio.mostPlayedCount,
                        audio.artistId
                    )
                    mViewModelClass.insertQueue(queueListModel, lifecycleScope)
                }
            }
            /* Log.d(
                 "NewQueueAudios",
                 "updateAudioTagsToDatabase:${loadQueueAudio[indexOfSelectedAudio]} "
             )*/
        }
        finish()
    }

    private fun convertStringToAudioModel(audioModel: String): AllSongsModel {
        val gson = Gson()
        val type = object : TypeToken<AllSongsModel>() {}.type
        return gson.fromJson(audioModel, type)
    }

    private fun getByteArray(): ByteArray {
        val uriRealPath = GetRealPathOfUri().getUriRealPath(this, newAlbumArtUri!!)
        val src = BitmapFactory.decodeFile(uriRealPath)
        val baos = ByteArrayOutputStream()
        src.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    override fun onResume() {
        super.onResume()
        SavedAppTheme(
            this,
            null,
            null,
            null,
            isHomeFrag = false,
            isHostActivity = false,
            tagEditorsBG = binding.tagEditorsBG,
            isTagEditor = true,
            bottomBar = null,
            rlMiniPlayerBottomSheet = null,
            bottomShadowIVAlbumFrag = null,
            isAlbumFrag = false,
            topViewIV = null,
            bottomShadowIVArtistFrag = null,
            isArtistFrag = false,
            topViewIVArtistFrag = null,
            parentViewArtistAndAlbumFrag = null,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
            searchFragBg = null,
            isSearchFrag = false,
            settingFragBg = null,
            isSettingFrag = false
        ).settingSavedBackgroundTheme()
    }

    /*private fun updateAlbumArtMediaStore(context: Context, id: Long, art: String?) {
        val uri =
            ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
       // context.contentResolver.delete(uri, null, null)
        val values = ContentValues()
        values.put("album_id", id)
        values.put("_data", art)
        val newuri: Uri? = context.contentResolver
            .insert(
                Uri.parse("content://media/external/audio/albumart"),
                values
            )
        if (newuri != null) {
            Toast.makeText(this, "UPDATED", Toast.LENGTH_LONG).show()
            context.contentResolver.notifyChange(uri, null)
            val src = File(allSongsModel.data)
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(src)
            sendBroadcast(intent)
        } else {
            Toast.makeText(this, "FAILED", Toast.LENGTH_LONG).show()
        }
    }*/


    /*@RequiresApi(Build.VERSION_CODES.Q)
    private fun updateData() {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            allSongsModel.songId
        )
        val values = ContentValues()
        values.put(MediaStore.Audio.Media.IS_PENDING, 1)
        contentResolver.update(
            uri, values, MediaStore.Audio.Media._ID + " = ? ",
            arrayOf(allSongsModel.songId.toString())
        )

        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        values.put(MediaStore.Audio.Media.TITLE, binding.etTitle.text.toString())
        values.put(MediaStore.Audio.Media.ARTIST, binding.etArtist.text.toString())
        contentResolver.update(
            uri, values, MediaStore.Audio.Media._ID + " = ? ",
            arrayOf(allSongsModel.songId.toString())
        )

        *//*val src = File(allSongsModel.data)
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(src)
        sendBroadcast(intent)*//*
    }*/

    /*
    *     private fun setUpViews() {
        allSongsModel = convertStringToAudioModel(audioModel)

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
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media._ID} = ?"
        val selectionArgs = arrayOf(allSongsModel.songId.toString())

        val query = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )
        query.use { cursor ->
            // Cache column indices.
            val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

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
                val dateAdded = cursor.getString(dateAddedColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )


            }

            cursor.close()
        }

    }
*/
}