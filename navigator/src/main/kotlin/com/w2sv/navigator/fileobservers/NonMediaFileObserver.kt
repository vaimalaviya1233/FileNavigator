package com.w2sv.navigator.fileobservers

import android.content.ContentResolver
import com.anggrayudi.storage.media.MediaType
import com.w2sv.data.model.FileType
import com.w2sv.navigator.model.MediaStoreFile
import com.w2sv.navigator.model.NavigatableFile
import slimber.log.i

internal class NonMediaFileObserver(
    private val fileTypes: List<FileType.NonMedia>,
    contentResolver: ContentResolver,
    onNewMoveFile: (NavigatableFile) -> Unit
) :
    FileObserver(
        MediaType.DOWNLOADS.readUri!!,
        contentResolver,
        onNewMoveFile
    ) {

    init {
        i { "Initialized NonMediaFileObserver with fileTypes: ${fileTypes.map { it.identifier }}" }
    }

    override fun getNavigatableFileIfMatching(
        mediaStoreFile: MediaStoreFile
    ): NavigatableFile? =
        fileTypes
            .firstOrNull { it.matchesFileExtension(mediaStoreFile.columnData.fileExtension) }
            ?.let { fileType ->
                NavigatableFile(
                    type = fileType,
                    sourceKind = FileType.Source.Kind.Download,
                    mediaStoreFile = mediaStoreFile
                )
            }
}