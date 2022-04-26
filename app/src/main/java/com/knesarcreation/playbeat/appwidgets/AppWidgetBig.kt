package com.knesarcreation.playbeat.appwidgets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.knesarcreation.appthemehelper.util.MaterialValueHelper
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.MainActivity
import com.knesarcreation.playbeat.appwidgets.base.BaseAppWidget
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_REWIND
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_SKIP
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_TOGGLE_PAUSE
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil

class AppWidgetBig : BaseAppWidget() {
    private var target: Target<Bitmap>? = null // for cancellation

    /**
     * Initialize given widgets to default state, where we launch Music on default click and hide
     * actions if service not running.
     */
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(
            context.packageName, R.layout.app_widget_big
        )

        appWidgetView.setViewVisibility(
            R.id.media_titles,
            View.INVISIBLE
        )
        appWidgetView.setImageViewResource(R.id.image, R.drawable.default_audio_art)
        appWidgetView.setImageViewBitmap(
            R.id.button_next, createBitmap(
                PlayBeatUtil.getTintedVectorDrawable(
                    context,
                    R.drawable.ic_skip_next,
                    MaterialValueHelper.getPrimaryTextColor(context, false)
                ), 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev, createBitmap(
                PlayBeatUtil.getTintedVectorDrawable(
                    context,
                    R.drawable.ic_skip_previous,
                    MaterialValueHelper.getPrimaryTextColor(context, false)
                ), 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause, createBitmap(
                PlayBeatUtil.getTintedVectorDrawable(
                    context,
                    R.drawable.ic_play_arrow_white_32dp,
                    MaterialValueHelper.getPrimaryTextColor(context, false)
                ), 1f
            )
        )

        linkButtons(context, appWidgetView)
        pushUpdate(context, appWidgetIds, appWidgetView)
    }

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(
            service.packageName, R.layout.app_widget_big
        )

        val isPlaying = service.isPlaying
        val song = service.currentSong

        // Set the titles and artwork
        if (song.title.isEmpty() && song.artistName.isEmpty()) {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.INVISIBLE
            )
        } else {
            appWidgetView.setViewVisibility(
                R.id.media_titles,
                View.VISIBLE
            )
            appWidgetView.setTextViewText(R.id.title, song.title)
            appWidgetView.setTextViewText(
                R.id.text,
                getSongArtistAndAlbum(song)
            )
        }

        val primaryColor = MaterialValueHelper.getPrimaryTextColor(service, false)
        // Set correct drawable for pause state
        val playPauseRes =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow_white_32dp
        appWidgetView.setImageViewBitmap(
            R.id.button_toggle_play_pause, createBitmap(
                PlayBeatUtil.getTintedVectorDrawable(
                    service,
                    playPauseRes,
                    primaryColor
                ), 1f
            )
        )

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(
            R.id.button_next, createBitmap(
                PlayBeatUtil.getTintedVectorDrawable(
                    service,
                    R.drawable.ic_skip_next,
                    primaryColor
                ), 1f
            )
        )
        appWidgetView.setImageViewBitmap(
            R.id.button_prev, createBitmap(
                PlayBeatUtil.getTintedVectorDrawable(
                    service,
                    R.drawable.ic_skip_previous,
                    primaryColor
                ), 1f
            )
        )

        // Link actions buttons to intents
        linkButtons(service, appWidgetView)

        // Load the album cover async and push the update on completion
        val p = PlayBeatUtil.getScreenSize(service)
        val widgetImageSize = p.x.coerceAtMost(p.y)
        val appContext = service.applicationContext
        service.runOnUiThread {
            if (target != null) {
                Glide.with(service).clear(target)
            }
            target = GlideApp.with(appContext)
                .asBitmap()
                //.checkIgnoreMediaStore()
                .load(PlayBeatGlideExtension.getSongModel(song))
                .into(object : SimpleTarget<Bitmap>(widgetImageSize, widgetImageSize) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        update(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        update(null)
                    }

                    private fun update(bitmap: Bitmap?) {
                        if (bitmap == null) {
                            appWidgetView.setImageViewResource(
                                R.id.image,
                                R.drawable.default_audio_art
                            )
                        } else {
                            appWidgetView.setImageViewBitmap(R.id.image, bitmap)
                        }
                        pushUpdate(appContext, appWidgetIds, appWidgetView)
                    }
                })
        }
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(context, MainActivity::class.java)
            .putExtra(
                MainActivity.EXPAND_PANEL,
                PreferenceUtil.isExpandPanel
            )

        val serviceName = ComponentName(context, MusicService::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent =
            PendingIntent.getActivity(
                context, 0, action, if (VersionUtils.hasMarshmallow())
                    PendingIntent.FLAG_IMMUTABLE
                else 0
            )
        views.setOnClickPendingIntent(R.id.clickable_area, pendingIntent)

        // Previous track
        pendingIntent = buildPendingIntent(context, ACTION_REWIND, serviceName)
        views.setOnClickPendingIntent(R.id.button_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, ACTION_TOGGLE_PAUSE, serviceName)
        views.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, ACTION_SKIP, serviceName)
        views.setOnClickPendingIntent(R.id.button_next, pendingIntent)
    }

    companion object {

        const val NAME: String = "app_widget_big"
        private var mInstance: AppWidgetBig? = null

        val instance: AppWidgetBig
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetBig()
                }
                return mInstance!!
            }
    }
}
