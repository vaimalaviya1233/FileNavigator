package com.w2sv.navigator.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.callback.FileCallback
import com.anggrayudi.storage.file.getSimplePath
import com.anggrayudi.storage.media.MediaFile
import com.w2sv.androidutils.coroutines.getValueSynchronously
import com.w2sv.androidutils.notifying.showToast
import com.w2sv.data.storage.repositories.FileTypeRepository
import com.w2sv.navigator.FileNavigator
import com.w2sv.navigator.R
import com.w2sv.navigator.model.MoveFile
import com.w2sv.navigator.notifications.AppNotificationsManager
import com.w2sv.navigator.notifications.NotificationResources
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import slimber.log.i
import javax.inject.Inject

@AndroidEntryPoint
class FileMoveActivity : ComponentActivity() {

    @HiltViewModel
    class ViewModel @Inject constructor(
        savedStateHandle: SavedStateHandle,
        private val fileTypeRepository: FileTypeRepository,
        @ApplicationContext context: Context
    ) :
        androidx.lifecycle.ViewModel() {

        // ===============
        // Intent Extras
        // ===============

        private val moveFile: MoveFile =
            savedStateHandle[FileNavigator.EXTRA_MOVE_FILE]!!

        val moveMediaFile: MediaFile? = moveFile.getMediaFile(context)

        val notificationResources: NotificationResources =
            savedStateHandle[NotificationResources.EXTRA]!!

        // ===============
        // DataStore Attributes
        // ===============

        val defaultTargetDirDocumentUri: Uri? =
            fileTypeRepository.getUriFlow(moveFile.source.defaultDestination)
                .getValueSynchronously()
                .also {
                    i { "Retrieved ${moveFile.source.defaultDestination.preferencesKey} = $it" }
                }

        fun saveFileSourceDefaultDestination(defaultDestination: Uri): Job =
            viewModelScope.launch {
                fileTypeRepository.saveDefaultDestination(
                    moveFile.source,
                    defaultDestination
                )
            }
    }

    @Inject
    lateinit var appNotificationsManager: AppNotificationsManager

    private val viewModel by viewModels<ViewModel>()

    private val destinationSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            i { "DocumentTree Uri: $treeUri" }

            // Exit on null treeUri (received on exiting folder picker via back press)
            treeUri ?: run {
                finish()
                return@registerForActivityResult
            }

            // Exit on unsuccessful conversion to SimpleStorage objects
            val targetDirectoryDocumentFile =
                DocumentFile.fromTreeUri(this, treeUri)

            if (targetDirectoryDocumentFile == null || viewModel.moveMediaFile == null) {
                finish()
                return@registerForActivityResult
            }

            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            appNotificationsManager.newMoveFileNotificationManager.cancelNotification(viewModel.notificationResources)

            // Move file
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.moveMediaFile!!.moveTo(
                    targetDirectoryDocumentFile,
                    callback = object : FileCallback() {
                        override fun onCompleted(result: Any) {
                            showToast(
                                getString(
                                    R.string.moved_file_to,
                                    targetDirectoryDocumentFile.getSimplePath(this@FileMoveActivity)
                                )
                            )
                        }

                        // TODO: refined errorCode handling
                        override fun onFailed(errorCode: ErrorCode) {
                            showToast(R.string.couldn_t_move_file)
                        }
                    }
                )
            }

            if (targetDirectoryDocumentFile != viewModel.defaultTargetDirDocumentUri) {
                // Save targetDirectoryDocumentFile to DataStore and finish activity on saving completed
                viewModel
                    .saveFileSourceDefaultDestination(targetDirectoryDocumentFile.uri)
                    .invokeOnCompletion {
                        finish()
                    }
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        destinationSelectionLauncher.launch(viewModel.defaultTargetDirDocumentUri)
    }
}