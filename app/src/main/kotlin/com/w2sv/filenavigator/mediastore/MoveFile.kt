package com.w2sv.filenavigator.mediastore

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.anggrayudi.storage.media.MediaFile
import com.anggrayudi.storage.media.MediaStoreCompat
import com.w2sv.androidutils.notifying.getNotificationManager
import com.w2sv.filenavigator.service.FileNavigatorService
import kotlinx.parcelize.Parcelize

/**
 * @param uri The MediaStore URI.
 */
@Parcelize
data class MoveFile(
    val uri: Uri,
    val type: FileType,
    val defaultTargetDir: FileType.Source.DefaultTargetDir,
    val data: MediaStoreFileData
) : Parcelable {

    fun getMediaFile(context: Context): MediaFile? =
        MediaStoreCompat.fromMediaId(
            context,
            type.simpleStorageType,
            data.id
        )

    @Parcelize
    data class NotificationParameters(
        val notificationId: Int,
        val requestCodes: ArrayList<Int>
    ) : Parcelable {

        companion object {
            const val EXTRA = "com.w2sv.filenavigator.extra.NOTIFICATION_PARAMETERS"
        }

        fun cancelUnderlyingNotification(context: Context){
            context.getNotificationManager().cancel(notificationId)

            FileNavigatorService.onNotificationCancelled(
                this,
                context
            )
        }
    }
}