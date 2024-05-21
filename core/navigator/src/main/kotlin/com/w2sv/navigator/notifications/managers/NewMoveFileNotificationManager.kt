package com.w2sv.navigator.notifications.managers

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannedString
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.w2sv.androidutils.coroutines.stateInWithSynchronousInitial
import com.w2sv.common.di.AppDispatcher
import com.w2sv.common.di.GlobalScope
import com.w2sv.common.utils.getDocumentUriFileName
import com.w2sv.common.utils.lineBreakSuffixed
import com.w2sv.common.utils.removeSlashSuffix
import com.w2sv.common.utils.slashPrefixed
import com.w2sv.core.navigator.R
import com.w2sv.domain.model.FileType
import com.w2sv.domain.repository.NavigatorRepository
import com.w2sv.navigator.moving.FileMoveActivity
import com.w2sv.navigator.moving.MoveBroadcastReceiver
import com.w2sv.navigator.moving.MoveFile
import com.w2sv.navigator.notifications.AppNotificationChannel
import com.w2sv.navigator.notifications.NotificationResources
import com.w2sv.navigator.notifications.NotificationResourcesCleanupBroadcastReceiver
import com.w2sv.navigator.notifications.getNotificationChannel
import com.w2sv.navigator.notifications.managers.abstrct.MultiInstanceAppNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import slimber.log.i
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NewMoveFileNotificationManager @Inject constructor(
    @ApplicationContext context: Context,
    notificationManager: NotificationManager,
    private val navigatorRepository: NavigatorRepository,
    @GlobalScope(AppDispatcher.Default) private val scope: CoroutineScope
) : MultiInstanceAppNotificationManager<NewMoveFileNotificationManager.BuilderArgs>(
    notificationChannel = AppNotificationChannel.NewNavigatableFile.getNotificationChannel(context),
    notificationManager = notificationManager,
    context = context,
    resourcesBaseSeed = 1,
    summaryId = 999,
) {
    inner class BuilderArgs(
        val moveFile: MoveFile,
    ) : MultiInstanceAppNotificationManager.BuilderArgs(
        resources = getNotificationResources(
            nPendingRequestCodes = 4
        )
    )

    private val sourceToLastMoveDestinationStateFlow =
        mutableMapOf<FileType.Source, StateFlow<Uri?>>()

    private fun getLastMoveDestination(source: FileType.Source): Uri? =
        sourceToLastMoveDestinationStateFlow.getOrPut(
            key = source,
            defaultValue = {
                navigatorRepository
                    .getLastMoveDestinationFlow(source)
                    .stateInWithSynchronousInitial(scope)
            }
        )
            .value

    override fun getBuilder(args: BuilderArgs): Builder =
        object : Builder() {

            override fun build(): Notification {
                setContentTitle(
                    "${context.getString(R.string.new_)} ${getMoveFileTitle()}"
                )
                // Set icons
                setLargeIcon(
                    AppCompatResources.getDrawable(context, args.moveFile.source.getIconRes())
                        ?.apply { setTint(args.moveFile.source.fileType.colorInt) }
                        ?.toBitmap()
                )
                // Set content
                setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(getContentText())
                )

                // Set actions & intents
                val requestCodeIterator = args.resources.actionRequestCodes.iterator()

                addAction(getMoveFileAction(requestCodeIterator.next()))

                // Add quickMoveAction if lastMoveDestination present.
                getLastMoveDestination(args.moveFile.source)?.let { lastMoveDestination ->
                    // Don't add action if folder doesn't exist anymore, which results in getDocumentUriFileName returning null.
                    getDocumentUriFileName(
                        documentUri = lastMoveDestination,
                        context = context
                    )
                        ?.let { fileName ->
                            addAction(
                                getQuickMoveAction(
                                    requestCode = requestCodeIterator.next(),
                                    lastMoveDestination = lastMoveDestination,
                                    lastMoveDestinationFileName = fileName
                                )
                            )
                        }
                }

                setContentIntent(getViewFilePendingIntent(requestCodeIterator.next()))

                setDeleteIntent(
                    getCleanupNotificationResourcesPendingIntent(
                        requestCode = requestCodeIterator.next(),
                    )
                )

                return super.build()
            }

            private fun getMoveFileTitle(): String =
                when (val fileType = args.moveFile.source.fileType) {
                    is FileType.Media -> {
                        when (val sourceKind =
                            args.moveFile.mediaStoreFile.columnData.getSourceKind()) {
                            FileType.Source.Kind.Recording, FileType.Source.Kind.Screenshot -> context.getString(
                                sourceKind.labelRes
                            )

                            FileType.Source.Kind.Camera -> context.getString(
                                when (args.moveFile.source.fileType) {
                                    FileType.Image -> com.w2sv.core.domain.R.string.photo
                                    FileType.Video -> com.w2sv.core.domain.R.string.video
                                    else -> throw Error()
                                }
                            )

                            FileType.Source.Kind.Download -> "${context.getString(fileType.titleRes)} ${
                                context.getString(R.string.download)
                            }"

                            FileType.Source.Kind.OtherApp -> "${args.moveFile.mediaStoreFile.columnData.dirName} ${
                                context.getString(
                                    fileType.titleRes
                                )
                            }"
                                .slashPrefixed()
                        }
                    }

                    is FileType.NonMedia -> {
                        context.getString(args.moveFile.source.fileType.titleRes)
                    }
                }

            private fun getContentText(): SpannedString =
                buildSpannedString {
                    append(args.moveFile.mediaStoreFile.columnData.name.lineBreakSuffixed())
                    bold { append(context.getString(R.string.found_at).lineBreakSuffixed()) }
                    append(
                        args.moveFile.mediaStoreFile.columnData.volumeRelativeDirPath.removeSlashSuffix()
                            .slashPrefixed()
                    )
                }

            private fun getViewFilePendingIntent(requestCode: Int)
                    : PendingIntent =
                PendingIntent.getActivity(
                    context,
                    requestCode,
                    Intent()
                        .setAction(Intent.ACTION_VIEW)
                        .setDataAndType(
                            args.moveFile.mediaStoreFile.uri,
                            args.moveFile.source.fileType.simpleStorageMediaType.mimeType
                        ),
                    PendingIntent.FLAG_IMMUTABLE
                )

            private fun getMoveFileAction(
                requestCode: Int,
            ): NotificationCompat.Action =
                NotificationCompat.Action(
                    R.drawable.ic_app_logo_24,
                    context.getString(R.string.move),
                    PendingIntent.getActivity(
                        context,
                        requestCode,
                        FileMoveActivity.makeRestartActivityIntent(
                            args.moveFile,
                            args.resources,
                            context
                        ),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )

            private fun getQuickMoveAction(
                requestCode: Int,
                lastMoveDestination: Uri,
                lastMoveDestinationFileName: String
            ): NotificationCompat.Action =
                NotificationCompat.Action(
                    R.drawable.ic_app_logo_24,
                    context.getString(R.string.to, lastMoveDestinationFileName),
                    PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        MoveBroadcastReceiver.getIntent(
                            moveFile = args.moveFile,
                            moveDestinationDocumentUri = lastMoveDestination.also {
                                i { "MoveDestination Extra: $it" }
                            },
                            notificationResources = args.resources,
                            context = context
                        ),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )

            private fun getCleanupNotificationResourcesPendingIntent(
                requestCode: Int
            ): PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    ResourcesCleanupBroadcastReceiver.getIntent(
                        context,
                        args.resources
                    ),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )
        }

    override fun buildSummaryNotification(): Notification =
        Builder()
            .setContentTitle(
                context.getString(
                    if (nActiveNotifications >= 2) R.string.navigatable_files else R.string.navigatable_file,
                    nActiveNotifications
                )
            )
            .setOnlyAlertOnce(true)
            .setGroupSummary(true)
            .build()

    @AndroidEntryPoint
    class ResourcesCleanupBroadcastReceiver : NotificationResourcesCleanupBroadcastReceiver() {

        @Inject
        lateinit var newMoveFileNotificationManager: NewMoveFileNotificationManager

        override val multiInstanceAppNotificationManager: MultiInstanceAppNotificationManager<*>
            get() = newMoveFileNotificationManager

        companion object {
            fun getIntent(
                context: Context,
                notificationResources: NotificationResources
            ): Intent =
                getIntent<ResourcesCleanupBroadcastReceiver>(
                    context,
                    notificationResources
                )

            fun startFromResourcesComprisingIntent(
                context: Context,
                intent: Intent
            ) {
                startFromResourcesComprisingIntent<ResourcesCleanupBroadcastReceiver>(
                    context,
                    intent
                )
            }
        }
    }
}