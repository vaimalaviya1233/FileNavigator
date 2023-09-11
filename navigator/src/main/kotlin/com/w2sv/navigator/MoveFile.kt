package com.w2sv.navigator

import android.content.ContentResolver
import android.content.Context
import android.database.CursorIndexOutOfBoundsException
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import com.anggrayudi.storage.media.MediaFile
import com.anggrayudi.storage.media.MediaStoreCompat
import com.w2sv.common.utils.parseBoolean
import com.w2sv.common.utils.queryNonNullMediaStoreData
import com.w2sv.data.model.FileType
import com.w2sv.kotlinutils.dateFromUnixTimestamp
import com.w2sv.kotlinutils.timeDelta
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import slimber.log.i
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * @param uri The MediaStore URI.
 */
@Parcelize
data class MoveFile(
    val uri: Uri,
    val type: FileType,
    val sourceKind: FileType.SourceKind,
    val data: MediaStoreData
) : Parcelable {

    @IgnoredOnParcel
    val source: FileType.Source by lazy {
        FileType.Source(type, sourceKind)
    }

    fun getMediaFile(context: Context): MediaFile? =
        MediaStoreCompat.fromMediaId(
            context,
            type.mediaType,
            data.id
        )

    /**
     * @param relativePath Relative path from the storage volume, e.g. "Documents/", "DCIM/Camera/".
     */
    @Parcelize
    data class MediaStoreData(
        val id: String,
        val absPath: String,
        val relativePath: String,
        val name: String,
        val dateAdded: Date,
        val size: Long,
        val isDownload: Boolean,
        val isPendingFlag: Boolean
    ) : Parcelable {

        val isNewlyAdded: Boolean
            get() = (timeDelta(
                dateAdded, Date(System.currentTimeMillis()), TimeUnit.SECONDS
            ) < 10).also {
                i { "isNewlyAdded: $it" }
            }

        val isPending: Boolean
            get() = (isPendingFlag || size == 0L).also {
                i { "isPending: $it" }
            }

        fun pointsToSameContentAs(other: MediaStoreData): Boolean =
            id == other.id || (size == other.size && nonIncrementedNameWOExtension == other.nonIncrementedNameWOExtension)    // TODO

        @IgnoredOnParcel
        val fileExtension: String by lazy {
            name.substringAfterLast(".")
        }

        @IgnoredOnParcel
        val nonIncrementedNameWOExtension: String by lazy {
            name.substringBeforeLast(".")  // remove file extension
                .replace(
                    Regex("\\(\\d+\\)$"),
                    ""
                )  // remove trailing file incrementation parentheses
        }

        @IgnoredOnParcel
        val dirName: String by lazy {
            relativePath
                .removeSuffix(File.separator)
                .substringAfterLast(File.separator)
        }

        fun getSourceKind(): FileType.SourceKind = when {
            isDownload -> FileType.SourceKind.Download
            // NOTE: Don't change the order of the Screenshot and Camera branches, as the actual screenshot dir
            // may be a child dir of the camera directory
            relativePath.contains(Environment.DIRECTORY_SCREENSHOTS) -> FileType.SourceKind.Screenshot
            relativePath.contains(Environment.DIRECTORY_DCIM) -> FileType.SourceKind.Camera
            else -> FileType.SourceKind.OtherApp
        }
            .also {
                i { "Determined SourceKind: ${it.name}" }
            }

        companion object {

            fun fetch(
                uri: Uri, contentResolver: ContentResolver
            ): MediaStoreData? = try {
                contentResolver.queryNonNullMediaStoreData(
                    uri,
                    arrayOf(
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DATA,
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        MediaStore.MediaColumns.DATE_ADDED,
                        MediaStore.MediaColumns.SIZE,
                        MediaStore.MediaColumns.IS_DOWNLOAD,
                        MediaStore.MediaColumns.IS_PENDING
                    )
                )?.run {
                    MediaStoreData(
                        id = get(0),
                        absPath = get(1),
                        relativePath = get(2),
                        name = get(3),
                        dateAdded = dateFromUnixTimestamp(get(4)),
                        size = get(5).toLong(),
                        isDownload = parseBoolean(get(6)),
                        isPendingFlag = parseBoolean(get(7))
                    )
                        .also {
                            i { it.toString() }
                        }
                }
            } catch (e: CursorIndexOutOfBoundsException) {
                i { e.toString() }
                null
            }
        }
    }
}