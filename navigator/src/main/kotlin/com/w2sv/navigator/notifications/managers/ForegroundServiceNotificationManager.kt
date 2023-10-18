package com.w2sv.navigator.notifications.managers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.w2sv.navigator.FileNavigator
import com.w2sv.navigator.R
import com.w2sv.navigator.notifications.getNotificationChannel
import com.w2sv.navigator.notifications.managers.abstrct.AppNotificationManager

class ForegroundServiceNotificationManager(
    context: Context,
    notificationManager: NotificationManager
) : AppNotificationManager<AppNotificationManager.Args.Empty>(
    notificationChannel = getNotificationChannel(
        id = "FILE_NAVIGATOR",
        name = context.getString(R.string.file_navigator_is_running)
    ),
    notificationManager = notificationManager,
    context = context
) {
    override fun getBuilder(args: Args.Empty): Builder =
        object : Builder() {
            override fun build(): Notification {
                setSmallIcon(R.drawable.ic_app_logo_24)
                setContentTitle(context.getString(R.string.file_navigator_is_running))

                setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            "You will receive a notification when a new file pertaining to your selected file types gets registered."
                        )
                )

                setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        1,
                        Intent.makeRestartActivityTask(
                            ComponentName(context, "com.w2sv.filenavigator.MainActivity")
                        ),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )

                // add stop action
                addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_cancel_24,
                        context.getString(R.string.stop),
                        PendingIntent.getService(
                            context,
                            0,
                            FileNavigator.getStopIntent(context),
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                        ),
                    )
                )

                return super.build()
            }
        }
}