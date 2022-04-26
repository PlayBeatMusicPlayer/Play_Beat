package com.knesarcreation.playbeat.fragments.bottomSheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.checkbox.MaterialCheckBox
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.BottomSheetSortBinding
import com.knesarcreation.playbeat.helper.SortOrder
import com.knesarcreation.playbeat.util.PreferenceUtil

class BottomSheetSort(var sortOrder: String?, var mode: Int) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSortBinding? = null
    private val binding get() = _binding
    var listenerAudio: OnAudioSortOptionListener? = null
    var listenerAlbum: OnAlbumSortOptionListener? = null
    var listenerArtist: OnArtistSortOptionListener? = null
    var listenerPlaylist: OnPlaylistSortOptionListener? = null

    interface OnAudioSortOptionListener {
        fun sortByName()
        fun sortByAlbumName()
        fun sortByArtistName()
        fun sortByDateAdded()
        fun sortByDateModified()
        fun sortByComposer()
        fun sortByYear()
    }

    interface OnAlbumSortOptionListener {
        fun sortByAlbumName()
        fun sortByAlbumArtist()
        fun sortByAlbumNoOfSongs()
        fun sortByAlbumYear()
    }

    interface OnArtistSortOptionListener {
        fun sortByArtistNameASC()
        fun sortByArtistNameDESC()
        fun showAlbumArtist(cbShowAlbumArtist: MaterialCheckBox)
        /* fun sortByArtistNoOfSongs()
         fun sortByArtistNoOfAlbums()*/
    }

    interface OnPlaylistSortOptionListener {
        fun sortByPlaylistName()
        fun sortByPlaylistSongCount()
        fun sortByPlaylistSongCountDesc()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listenerAudio = context as OnAudioSortOptionListener
            listenerAlbum = context as OnAlbumSortOptionListener
            listenerArtist = context as OnArtistSortOptionListener
            listenerPlaylist = context as OnPlaylistSortOptionListener
        } catch (e: ClassCastException) {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSortBinding.inflate(inflater, container, false)
        val view = binding!!.root

        /*
        * mode 1 -> sort songs
        * mode 2 -> sort album
        * mode 3 -> sort artist
        * mode 4 -> sort playlist
        * */
        when (mode) {
            1 -> {
                binding!!.llSongsSort.visibility = View.VISIBLE
                binding!!.llAlbumsSort.visibility = View.GONE
                binding!!.llArtistsSort.visibility = View.GONE
                binding!!.llPlaylistSort.visibility = View.GONE
                binding!!.cbShowAlbumArtist.isChecked = false
            }
            2 -> {
                binding!!.llSongsSort.visibility = View.GONE
                binding!!.llAlbumsSort.visibility = View.VISIBLE
                binding!!.llArtistsSort.visibility = View.GONE
                binding!!.llPlaylistSort.visibility = View.GONE
                binding!!.cbShowAlbumArtist.isChecked = false
            }

            3 -> {
                binding!!.llSongsSort.visibility = View.GONE
                binding!!.llAlbumsSort.visibility = View.GONE
                binding!!.llArtistsSort.visibility = View.VISIBLE
                binding!!.llPlaylistSort.visibility = View.GONE
                binding!!.cbShowAlbumArtist.isChecked = PreferenceUtil.albumArtistsOnly
            }
            4 -> {
                binding!!.llSongsSort.visibility = View.GONE
                binding!!.llAlbumsSort.visibility = View.GONE
                binding!!.llArtistsSort.visibility = View.GONE
                binding!!.llPlaylistSort.visibility = View.VISIBLE
                binding!!.cbShowAlbumArtist.isChecked = false
            }
        }

        when (sortOrder) {
            SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> {
                binding!!.llSortByName.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)
                if (SortOrder.SongSortOrder.SONG_A_Z == sortOrder)
                    binding!!.nameSortIV.setImageResource(R.drawable.ic_sort_asc)
                else
                    binding!!.nameSortIV.setImageResource(R.drawable.ic_sort_desc)
            }

            SortOrder.SongSortOrder.SONG_ALBUM_ASC, SortOrder.SongSortOrder.SONG_ALBUM_DESC -> {
                if (binding!!.llSongsSort.visibility == View.VISIBLE) {
                    binding!!.llSortByAlbumName.background =
                        ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                    if (SortOrder.SongSortOrder.SONG_ALBUM_ASC == sortOrder)
                        binding!!.albumNameSortIV.setImageResource(R.drawable.ic_sort_asc)
                    else
                        binding!!.albumNameSortIV.setImageResource(R.drawable.ic_sort_desc)
                } else {
                    binding!!.llSortByAlbum.background =
                        ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                    if (SortOrder.AlbumSortOrder.ALBUM_A_Z == sortOrder)
                        binding!!.albumSortByIV.setImageResource(R.drawable.ic_sort_asc)
                    else
                        binding!!.albumSortByIV.setImageResource(R.drawable.ic_sort_desc)
                }
            }

            SortOrder.SongSortOrder.SONG_ARTIST_ASC, SortOrder.SongSortOrder.SONG_ARTIST_DESC -> {
                if (binding!!.llSongsSort.visibility == View.VISIBLE) {
                    binding!!.llSortByArtistName.background =
                        ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                    if (SortOrder.SongSortOrder.SONG_ARTIST_ASC == sortOrder)
                        binding!!.artistNameSortIV.setImageResource(R.drawable.ic_sort_asc)
                    else
                        binding!!.artistNameSortIV.setImageResource(R.drawable.ic_sort_desc)
                } else {
                    if (SortOrder.ArtistSortOrder.ARTIST_A_Z == sortOrder) {
                        binding!!.llSortByArtistNameASC.background =
                            ContextCompat.getDrawable(
                                activity as Context,
                                R.drawable.curved_at_edge
                            )
                        binding!!.artistSortASCByIV.setImageResource(R.drawable.ic_sort_asc)
                    } else {
                        binding!!.llSortByArtistNameDESC.background =
                            ContextCompat.getDrawable(
                                activity as Context,
                                R.drawable.curved_at_edge
                            )
                        binding!!.artistSortDESCByIV.setImageResource(R.drawable.ic_sort_asc)
                    }
                }

            }

            SortOrder.SongSortOrder.SONG_YEAR_DESC, SortOrder.SongSortOrder.SONG_YEAR_ASC -> {
                if (binding!!.llSongsSort.visibility == View.VISIBLE) {
                    binding!!.llSortByYear.background =
                        ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                    if (SortOrder.SongSortOrder.SONG_YEAR_DESC == sortOrder)
                        binding!!.yearSortIV.setImageResource(R.drawable.ic_sort_asc)
                    else
                        binding!!.yearSortIV.setImageResource(R.drawable.ic_sort_desc)
                } else {
                    binding!!.llSortByAlbumYear.background =
                        ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)
                    binding!!.albumYearSortIV.setImageResource(R.drawable.ic_sort_desc)
                }
            }

            SortOrder.SongSortOrder.SONG_DATE_DESC, SortOrder.SongSortOrder.SONG_DATE_ASC -> {
                binding!!.llSortByDateAdded.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                if (SortOrder.SongSortOrder.SONG_DATE_DESC == sortOrder)
                    binding!!.dateAddedSortIV.setImageResource(R.drawable.ic_sort_asc)
                else
                    binding!!.dateAddedSortIV.setImageResource(R.drawable.ic_sort_desc)
            }

            SortOrder.SongSortOrder.SONG_DATE_MODIFIED_DESC, SortOrder.SongSortOrder.SONG_DATE_MODIFIED_ASC -> {
                binding!!.llSortByDateModified.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                if (SortOrder.SongSortOrder.SONG_DATE_MODIFIED_DESC == sortOrder)
                    binding!!.dateModifiedIV.setImageResource(R.drawable.ic_sort_asc)
                else
                    binding!!.dateModifiedIV.setImageResource(R.drawable.ic_sort_desc)
            }

            SortOrder.SongSortOrder.COMPOSER_ACS, SortOrder.SongSortOrder.COMPOSER_DESC -> {
                binding!!.llSortByComposer.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                if (SortOrder.SongSortOrder.COMPOSER_ACS == sortOrder)
                    binding!!.composerSortIV.setImageResource(R.drawable.ic_sort_asc)
                else
                    binding!!.composerSortIV.setImageResource(R.drawable.ic_sort_desc)
            }

            // album sorting
            SortOrder.AlbumSortOrder.ALBUM_ARTIST -> {
                binding!!.llSortByAlbumArtist.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)
                binding!!.albumArtistSortIV.setImageResource(R.drawable.ic_sort_asc)

            }

            SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS -> {
                binding!!.llSortByAlbumNoOfSongs.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)

                binding!!.albumNoOfSongsSortIV.setImageResource(R.drawable.ic_sort_asc)

            }

            //Playlist sort
            SortOrder.PlaylistSortOrder.PLAYLIST_A_Z, SortOrder.PlaylistSortOrder.PLAYLIST_Z_A -> {
                binding!!.llSortByPlaylistName.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)
                if (SortOrder.PlaylistSortOrder.PLAYLIST_A_Z == sortOrder)
                    binding!!.playlistNameSortByIV.setImageResource(R.drawable.ic_sort_asc)
                else
                    binding!!.playlistNameSortByIV.setImageResource(R.drawable.ic_sort_desc)

            }
            SortOrder.PlaylistSortOrder.PLAYLIST_SONG_COUNT -> {
                binding!!.llSortByPlaylistSongsCount.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)
                binding!!.playlistSongsCountSortIV.setImageResource(R.drawable.ic_sort_asc)

            }
            SortOrder.PlaylistSortOrder.PLAYLIST_SONG_COUNT_DESC -> {
                binding!!.llSortByPlaylistSongCountDes.background =
                    ContextCompat.getDrawable(activity as Context, R.drawable.curved_at_edge)
                binding!!.playlistSongCountDesSortIV.setImageResource(R.drawable.ic_sort_asc)

            }

        }

        handleClickListenerOfSongSorting()

        handleClickListenerOfAlbumSorting()

        handleClickListenerOfArtistSorting()

        handleClickListenerOfPlaylistSorting()

        return view
    }

    private fun handleClickListenerOfPlaylistSorting() {
        binding!!.llSortByPlaylistName.setOnClickListener {
            listenerPlaylist!!.sortByPlaylistName()
        }
        binding!!.llSortByPlaylistSongsCount.setOnClickListener {
            listenerPlaylist!!.sortByPlaylistSongCount()
        }
        binding!!.llSortByPlaylistSongCountDes.setOnClickListener {
            listenerPlaylist!!.sortByPlaylistSongCountDesc()
        }
    }

    private fun handleClickListenerOfArtistSorting() {
        binding!!.llSortByArtistNameASC.setOnClickListener {
            listenerArtist!!.sortByArtistNameASC()
        }
        binding!!.llSortByArtistNameDESC.setOnClickListener {
            listenerArtist!!.sortByArtistNameDESC()
        }
        binding!!.llShowAlbumArtist.setOnClickListener {
            binding!!.cbShowAlbumArtist.isChecked = !binding!!.cbShowAlbumArtist.isChecked
            listenerArtist!!.showAlbumArtist(binding!!.cbShowAlbumArtist)
        }

        binding!!.cbShowAlbumArtist.setOnCheckedChangeListener { _, _ ->
            listenerArtist!!.showAlbumArtist(binding!!.cbShowAlbumArtist)
        }
    }

    private fun handleClickListenerOfAlbumSorting() {
        binding!!.llSortByAlbum.setOnClickListener {
            listenerAlbum!!.sortByAlbumName()
        }
        binding!!.llSortByAlbumArtist.setOnClickListener {
            listenerAlbum!!.sortByAlbumArtist()
        }
        binding!!.llSortByAlbumNoOfSongs.setOnClickListener {
            listenerAlbum!!.sortByAlbumNoOfSongs()
        }
        binding!!.llSortByAlbumYear.setOnClickListener {
            listenerAlbum!!.sortByAlbumYear()
        }

    }

    private fun handleClickListenerOfSongSorting() {
        /*sort audio*/
        binding!!.llSortByName.setOnClickListener {
            listenerAudio!!.sortByName()
        }
        binding!!.llSortByAlbumName.setOnClickListener {
            listenerAudio!!.sortByAlbumName()
        }
        binding!!.llSortByArtistName.setOnClickListener {
            listenerAudio!!.sortByArtistName()
        }
        binding!!.llSortByYear.setOnClickListener {
            listenerAudio!!.sortByYear()
        }
        binding!!.llSortByDateAdded.setOnClickListener {
            listenerAudio!!.sortByDateAdded()
        }
        binding!!.llSortByDateModified.setOnClickListener {
            listenerAudio!!.sortByDateModified()
        }
        binding!!.llSortByComposer.setOnClickListener {
            listenerAudio!!.sortByComposer()
        }
    }

}