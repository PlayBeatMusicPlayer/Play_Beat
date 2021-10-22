package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.knesarcreation.playbeat.adapter.AllArtistsAdapter
import com.knesarcreation.playbeat.database.ArtistsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentArtistsBinding
import java.util.concurrent.CopyOnWriteArrayList

class AllArtistsFragment : Fragment() {

    private var _binding: FragmentArtistsBinding? = null
    private val binding get() = _binding
    private var artistsList = CopyOnWriteArrayList<ArtistsModel>()
    private var listener: OpenArtisFragment? = null
    private lateinit var allArtistsAdapter: AllArtistsAdapter
    private lateinit var mViewModelClass: ViewModelClass

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentArtistsBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass =
            ViewModelProvider(this)[ViewModelClass::class.java]

        //val storage = StorageUtil(activity as AppCompatActivity)
        //val loadAudio = storage.loadAudio()
        //loadArtists()
        setUpRecyclerAdapter()
        observeAudioArtistData()

        return view
    }

    private fun observeAudioArtistData() {
        mViewModelClass.getAllArtists().observe(viewLifecycleOwner, {
            if (it != null) {
                if (it.isEmpty()) {
                    binding?.rvArtists!!.visibility = View.GONE
                    binding?.rlNoArtistPresent!!.visibility = View.VISIBLE
                } else {
                    binding?.rvArtists!!.visibility = View.VISIBLE
                    binding?.rlNoArtistPresent!!.visibility = View.GONE
                    artistsList.clear()
                    artistsList.addAll(it.sortedBy { artistsModel -> artistsModel.artistName })
                    allArtistsAdapter.submitList(it.sortedBy { artistsModel -> artistsModel.artistName })
                }
            } else {
                binding?.rvArtists!!.visibility = View.GONE
                binding?.rlNoArtistPresent!!.visibility = View.VISIBLE
            }
        })
    }

    private fun setUpRecyclerAdapter() {
        allArtistsAdapter = AllArtistsAdapter(
            activity as Context,
            object : AllArtistsAdapter.OnArtistClicked {
                override fun getArtistData(artistsModel: ArtistsModel) {
                    val gson = Gson()
                    val artistsData = gson.toJson(artistsModel)
                    listener?.onOpenArtistTrackAndAlbumFragment(artistsData)
                }
            }
        )
        binding?.rvArtists?.adapter = allArtistsAdapter
    }

    interface OpenArtisFragment {
        fun onOpenArtistTrackAndAlbumFragment(artistsData: String)
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