package com.w2sv.navigator.model

import android.net.Uri
import com.w2sv.data.model.MoveEntry
import java.time.LocalDateTime

fun getMoveEntry(moveFile: MoveFile, destinationDocumentUri: Uri, dateTime: LocalDateTime): MoveEntry =
    MoveEntry(
        fileName = moveFile.mediaStoreFile.columnData.name,
        originalLocation = moveFile.mediaStoreFile.columnData.volumeRelativeDirPath,
        fileType = moveFile.source.fileType,
        fileSourceKind = moveFile.source.kind,
        destinationDocumentUri = destinationDocumentUri,
        dateTime = dateTime
    )