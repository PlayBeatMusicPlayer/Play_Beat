package com.knesarcreation.playbeat.fragments.other

import androidx.navigation.fragment.navArgs
import com.knesarcreation.playbeat.fragments.albums.AlbumDetailsViewModel
import com.knesarcreation.playbeat.fragments.artists.ArtistDetailsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MoreAlbumSongsDetailsFragment : AbsMoreAlbumSongsFragment() {

    private val arguments by navArgs<MoreAlbumSongsDetailsFragmentArgs>()
    override val detailsViewModel: AlbumDetailsViewModel by viewModel {
        parametersOf(arguments.extraAlbumId, null)
    }

}
