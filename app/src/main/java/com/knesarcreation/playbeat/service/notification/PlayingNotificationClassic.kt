package com.knesarcreation.playbeat.service.notification

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle
import com.knesarcreation.playbeat.activities.MainActivity
import com.knesarcreation.playbeat.extensions.isColorLight
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.glide.palette.BitmapPaletteWrapper
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_QUIT
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_REWIND
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_SKIP
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_TOGGLE_PAUSE
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.knesarcreation.appthemehelper.util.ATHUtil.resolveColor
import com.knesarcreation.appthemehelper.util.ColorUtil
import com.knesarcreation.appthemehelper.util.MaterialValueHelper
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.extensions.isSystemDarkModeEnabled
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.util.ImageUtil.createBitmap
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor

/**
 * @author Hemanth S (h4h13).
 */
@SuppressLint("RestrictedApi")
class PlayingNotificationClassic(
    val context: Context
) : PlayingNotification(context) {
    private var primaryColor: Int = 0

    private fun getCombinedRemoteViews(collapsed: Boolean, song: Song): RemoteViews {
        val remoteViews = RemoteViews(
            context.packageName,
            if (collapsed) R.layout.layout_notification_collapsed else R.layout.layout_notification_expanded
        )
        remoteViews.setTextViewText(
            R.id.appName,
            context.getString(R.string.app_name) + " • " + song.albumName
        )
        remoteViews.setTextViewText(R.id.title, song.title)
        remoteViews.setTextViewText(R.id.subtitle, song.artistName)
        linkButtons(remoteViews)
        return remoteViews
    }

    override fun updateMetadata(song: Song, onUpdate: () -> Unit) {
        val notificationLayout = getCombinedRemoteViews(true, song)
        val notificationLayoutBig = getCombinedRemoteViews(false, song)

        val action = Intent(context, MainActivity::class.java)
        action.putExtra(MainActivity.EXPAND_PANEL, PreferenceUtil.isExpandPanel)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val clickIntent = PendingIntent
            .getActivity(
                context,
                0,
                action,
                PendingIntent.FLAG_UPDATE_CURRENT or if (VersionUtils.hasMarshmallow())
                    PendingIntent.FLAG_IMMUTABLE
                else 0
            )
        val deleteIntent = buildPendingIntent(context, ACTION_QUIT, null)

        setSmallIcon(R.drawable.ic_notification)
        setContentIntent(clickIntent)
        setDeleteIntent(deleteIntent)
        setCategory(NotificationCompat.CATEGORY_SERVICE)
        priority = NotificationCompat.PRIORITY_MAX
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setCustomContentView(notificationLayout)
        setCustomBigContentView(notificationLayoutBig)
        setStyle(DecoratedMediaCustomViewStyle())
        setOngoing(true)
        val bigNotificationImageSize = context.resources
            .getDimensionPixelSize(R.dimen.notification_big_image_size)
        GlideApp.with(context).asBitmapPalette().songCoverOptions(song)
            .load(PlayBeatGlideExtension.getSongModel(song))
            .centerCrop()
            .into(object : CustomTarget<BitmapPaletteWrapper>(
                bigNotificationImageSize,
                bigNotificationImageSize
            ) {
                override fun onResourceReady(
                    resource: BitmapPaletteWrapper,
                    transition: Transition<in BitmapPaletteWrapper>?
                ) {
                    val colors = MediaNotificationProcessor(context, resource.bitmap)
                    update(resource.bitmap, colors.backgroundColor)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    update(
                        null,
                        resolveColor(context, R.attr.colorSurface, Color.WHITE)
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    update(
                        null,
                        resolveColor(context, R.attr.colorSurface, Color.WHITE)
                    )
                }

                private fun update(bitmap: Bitmap?, bgColor: Int) {
                    var bgColorFinal = bgColor
                    if (bitmap != null) {
                        contentView.setImageViewBitmap(R.id.largeIcon, bitmap)
                        bigContentView.setImageViewBitmap(R.id.largeIcon, bitmap)
                    } else {
                        contentView.setImageViewResource(
                            R.id.largeIcon,
                            R.drawable.default_audio_art
                        )
                        bigContentView.setImageViewResource(
                            R.id.largeIcon,
                            R.drawable.default_audio_art
                        )
                    }

                    // Android 12 applies a standard Notification template to every notification
                    // which will in turn have a default background so setting a different background
                    // than that, looks weird
                    if (!VersionUtils.hasS()) {
                        if (!PreferenceUtil.isColoredNotification) {
                            bgColorFinal =
                                resolveColor(context, R.attr.colorSurface, Color.WHITE)
                        }
                        setBackgroundColor(bgColorFinal)
                        setNotificationContent(ColorUtil.isColorLight(bgColorFinal))
                    } else {
                        if (PreferenceUtil.isColoredNotification) {
                            setColorized(true)
                            color = bgColor
                            setNotificationContent(color.isColorLight)
                        } else {
                            setNotificationContent(!context.isSystemDarkModeEnabled())
                        }
                    }
                    onUpdate()
                }

                private fun setBackgroundColor(color: Int) {
                    contentView.setInt(R.id.image, "setBackgroundColor", color)
                    bigContentView.setInt(R.id.image, "setBackgroundColor", color)
                }

                private fun setNotificationContent(dark: Boolean) {
                    val primary = MaterialValueHelper.getPrimaryTextColor(context, dark)
                    val secondary = MaterialValueHelper.getSecondaryTextColor(context, dark)
                    primaryColor = primary

                    val close = createBitmap(
                        PlayBeatUtil.getTintedVectorDrawable(
                            context,
                            R.drawable.ic_close,
                            primary
                        ), NOTIFICATION_CONTROLS_SIZE_MULTIPLIER
                    )
                    val prev = createBitmap(
                        PlayBeatUtil.getTintedVectorDrawable(
                            context,
                            R.drawable.ic_skip_previous_round_white_32dp,
                            primary
                        ), NOTIFICATION_CONTROLS_SIZE_MULTIPLIER
                    )
                    val next = createBitmap(
                        PlayBeatUtil.getTintedVectorDrawable(
                            context,
                            R.drawable.ic_skip_next_round_white_32dp,
                            primary
                        ), NOTIFICATION_CONTROLS_SIZE_MULTIPLIER
                    )
                    val playPause = getPlayPauseBitmap(true)

                    contentView.setTextColor(R.id.title, primary)
                    contentView.setTextColor(R.id.subtitle, secondary)
                    contentView.setTextColor(R.id.appName, secondary)

                    contentView.setImageViewBitmap(R.id.action_prev, prev)
                    contentView.setImageViewBitmap(R.id.action_next, next)
                    contentView.setImageViewBitmap(R.id.action_play_pause, playPause)

                    bigContentView.setTextColor(R.id.title, primary)
                    bigContentView.setTextColor(R.id.subtitle, secondary)
                    bigContentView.setTextColor(R.id.appName, secondary)

                    bigContentView.setImageViewBitmap(R.id.action_quit, close)
                    bigContentView.setImageViewBitmap(R.id.action_prev, prev)
                    bigContentView.setImageViewBitmap(R.id.action_next, next)
                    bigContentView.setImageViewBitmap(R.id.action_play_pause, playPause)

                    contentView.setImageViewBitmap(
                        R.id.smallIcon,
                        createBitmap(
                            PlayBeatUtil.getTintedVectorDrawable(
                                context,
                                R.drawable.ic_notification,
                                secondary
                            ), 0.6f
                        )
                    )
                    bigContentView.setImageViewBitmap(
                        R.id.smallIcon,
                        createBitmap(
                            PlayBeatUtil.getTintedVectorDrawable(
                                context,
                                R.drawable.ic_notification,
                                secondary
                            ), 0.6f
                        )
                    )
                }
            })
    }

    private fun getPlayPauseBitmap(isPlaying: Boolean): Bitmap {
        return createBitmap(
            PlayBeatUtil.getTintedVectorDrawable(
                context,
                if (isPlaying)
                    R.drawable.ic_pause_white_48dp
                else
                    R.drawable.ic_play_arrow_white_48dp, primaryColor
            ), NOTIFICATION_CONTROLS_SIZE_MULTIPLIER
        )
    }

    override fun setPlaying(isPlaying: Boolean) {
        getPlayPauseBitmap(isPlaying).also {
            contentView.setImageViewBitmap(R.id.action_play_pause, it)
            bigContentView.setImageViewBitmap(R.id.action_play_pause, it)
        }
    }

    override fun updateFavorite(song: Song, onUpdate: () -> Unit) {
    }

    private fun buildPendingIntent(
        context: Context, action: String,
        serviceName: ComponentName?
    ): PendingIntent {
        val intent = Intent(action)
        intent.component = serviceName
        return PendingIntent.getService(
            context, 0, intent, if (VersionUtils.hasMarshmallow())
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )
    }


    private fun linkButtons(notificationLayout: RemoteViews) {
        var pendingIntent: PendingIntent

        val serviceName = ComponentName(context, MusicService::class.java)

        // Previous track
        pendingIntent = buildPendingIntent(context, ACTION_REWIND, serviceName)
        notificationLayout.setOnClickPendingIntent(R.id.action_prev, pendingIntent)

        // Play and pause
        pendingIntent = buildPendingIntent(context, ACTION_TOGGLE_PAUSE, serviceName)
        notificationLayout.setOnClickPendingIntent(R.id.action_play_pause, pendingIntent)

        // Next track
        pendingIntent = buildPendingIntent(context, ACTION_SKIP, serviceName)
        notificationLayout.setOnClickPendingIntent(R.id.action_next, pendingIntent)

        // Close
        pendingIntent = buildPendingIntent(context, ACTION_QUIT, serviceName)
        notificationLayout.setOnClickPendingIntent(R.id.action_quit, pendingIntent)
    }

    companion object {
        fun from(
            context: Context,
            notificationManager: NotificationManager
        ): PlayingNotification {
            if (VersionUtils.hasOreo()) {
                createNotificationChannel(context, notificationManager)
            }
            return PlayingNotificationClassic(context)
        }
    }
}
