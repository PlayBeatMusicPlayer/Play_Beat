package com.knesarcreation.playbeat.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.knesarcreation.playbeat.R
import me.ibrahimsn.lib.SmoothBottomBar

class SavedAppTheme(
    private val mContext: Context,
    private var homeFragBackground: CoordinatorLayout?,
    private val homeFragTabLayout: TabLayout?,
    private var hostActivityBG: RelativeLayout?,
    private val isHomeFrag: Boolean,
    private val isHostActivity: Boolean,
    private val tagEditorsBG: CoordinatorLayout?,
    private val isTagEditor: Boolean,
    private val bottomBar: SmoothBottomBar?,
    private val rlMiniPlayerBottomSheet: RelativeLayout?,
    private val bottomShadowIVAlbumFrag: ImageView?,
    private val isAlbumFrag: Boolean,
    private val topViewIV: ImageView?,
    private val bottomShadowIVArtistFrag: ImageView?,
    private val isArtistFrag: Boolean,
    private val topViewIVArtistFrag: ImageView?,
    private val parentViewArtistAndAlbumFrag: RelativeLayout?,
    private val bottomShadowIVPlaylist: ImageView?,
    private val isPlaylistFragCategory: Boolean,
    private val topViewIVPlaylist: ImageView?,
    private val playlistBG: CoordinatorLayout?,
    private val isPlaylistFrag: Boolean,
    private val searchFragBg: RelativeLayout?,
    private val isSearchFrag: Boolean,
    private val settingFragBg: CoordinatorLayout?,
    private val isSettingFrag: Boolean
) {
    var sharedPrefs: SharedPreferences? = null

    init {
        sharedPrefs = mContext.getSharedPreferences("AppTheme", AppCompatActivity.MODE_PRIVATE)
    }

    fun settingSavedBackgroundTheme() {
//        when {
//            isHomeFrag -> homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//
//            isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//
//            isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//
//            isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//
//            isHostActivity -> {r
//                hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//                rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_default)
//                bottomBar!!.barBackgroundColor =
//                    ContextCompat.getColor(mContext, R.color.app_theme_color)
//            }
//            isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//            isArtistFrag -> {
//                bottomShadowIVArtistFrag!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//                parentViewArtistAndAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//                topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_default)
//            }
//            isAlbumFrag -> {
//                bottomShadowIVAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//                topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_default)
//            }
//            isPlaylistFragCategory -> {
//                bottomShadowIVPlaylist!!.setBackgroundResource(AppThemesList.backgroundsList[0])
//                topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_default)
//            }
//        }

        // when (sharedPrefs!!.getInt("background", 0)) {
        //     0 -> {

        //   }
        /* 1 -> {
             when {
                 isHomeFrag -> homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[1])

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[1])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[1])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[1])

                 isHostActivity -> {
                     hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[1])

                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_1)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_1)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[1])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundResource(AppThemesList.backgroundsList[1])
                     parentViewArtistAndAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[1])
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v1)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[1])
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v1)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundResource(AppThemesList.backgroundsList[1])
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v1)
                 }
             }
         }
         2 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[2])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[2])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[2])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[2])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[2])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_2
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_2)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_2)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[2])

                 isArtistFrag -> {
                     parentViewArtistAndAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[2])
                     bottomShadowIVArtistFrag!!.setBackgroundResource(AppThemesList.backgroundsList[2])
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v2)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[2])
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v2)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundResource(AppThemesList.backgroundsList[2])
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v2)
                 }
             }
         }
         3 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[3])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[3])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[3])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[3])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[3])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_3
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_3)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_3)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[3])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundResource(AppThemesList.backgroundsList[3])
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v3)
                     parentViewArtistAndAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[3])

                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundResource(AppThemesList.backgroundsList[3])
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v3)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundResource(AppThemesList.backgroundsList[3])
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v3)
                 }
             }
         }
         4 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[4])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[4])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[4])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[4])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[4])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_4
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_4)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_4)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[4])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_4
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v4)
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_4
                         )
                     )

                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_4
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v4)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_4
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v4)
                 }
             }
         }
         5 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[5])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[5])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[5])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[5])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[5])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_5
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_5)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_5)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[5])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_5
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v5)
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_5
                         )
                     )
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_5
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v5)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_5
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v5)
                 }
             }
         }
         6 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[6])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[6])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[6])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[6])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[6])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_6
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_6)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_6)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[6])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_6
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v6)
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_6
                         )
                     )
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_6
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v6)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_6
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v6)
                 }
             }
         }
         7 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[7])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[7])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[7])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[7])


                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[7])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_7
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_7)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_7)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[7])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_7
                         )
                     )
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_7
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v7)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_7
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v7)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.theme_start_color_7
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v7)
                 }
             }
         }
         8 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[8])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[8])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[8])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[8])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[8])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_8
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_8)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_8)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[8])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_8
                         )
                     )
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_8
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v8)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_8
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v8)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_8
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v8)
                 }
             }
         }
         9 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[9])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[9])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[9])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[9])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[9])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_9
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_9)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_9)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[9])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_9
                         )
                     )
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_9
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v9)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_9
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v9)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_9
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v9)
                 }
             }
         }
         10 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[10])
                 }


                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[10])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[10])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[10])


                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[10])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_10
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_10)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_10)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[10])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_10
                         )
                     )
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_10
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v10)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_10
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v10)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_10
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v10)
                 }
             }
         }
         11 -> {
             when {
                 isHomeFrag -> {
                     homeFragBackground!!.setBackgroundResource(AppThemesList.backgroundsList[11])
                 }

                 isSettingFrag -> settingFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[11])

                 isPlaylistFrag -> playlistBG!!.setBackgroundResource(AppThemesList.backgroundsList[11])

                 isSearchFrag -> searchFragBg!!.setBackgroundResource(AppThemesList.backgroundsList[11])

                 isHostActivity -> {
                     //hostActivityBG!!.setBackgroundResource(AppThemesList.backgroundsList[11])
                     hostActivityBG!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_11
                         )
                     )
                     rlMiniPlayerBottomSheet!!.setBackgroundResource(R.drawable.mini_player_bg_11)
                     bottomBar!!.barBackgroundColor =
                         ContextCompat.getColor(mContext, R.color.bottom_bar_color_11)
                 }

                 isTagEditor -> tagEditorsBG!!.setBackgroundResource(AppThemesList.backgroundsList[11])

                 isArtistFrag -> {
                     bottomShadowIVArtistFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_11
                         )
                     )
                     parentViewArtistAndAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_11
                         )
                     )
                     topViewIVArtistFrag!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v11)
                 }

                 isAlbumFrag -> {
                     bottomShadowIVAlbumFrag!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_11
                         )
                     )
                     topViewIV!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v11)
                 }

                 isPlaylistFragCategory -> {
                     bottomShadowIVPlaylist!!.setBackgroundColor(
                         ContextCompat.getColor(
                             mContext,
                             R.color.bottom_bar_color_11
                         )
                     )
                     topViewIVPlaylist!!.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_v11)
                 }
             }
         }*/
        // }
    }
}