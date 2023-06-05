package com.knesarcreation.playbeat.fragments.other

import androidx.navigation.fragment.navArgs
import com.knesarcreation.playbeat.fragments.artists.ArtistDetailsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MoreArtistSongsDetailsFragment : AbsMoreArtistSongsFragment() {

    private val arguments by navArgs<MoreArtistSongsDetailsFragmentArgs>()
    override val detailsViewModel: ArtistDetailsViewModel by viewModel {
        parametersOf(arguments.extraArtistId, null)
    }

}
