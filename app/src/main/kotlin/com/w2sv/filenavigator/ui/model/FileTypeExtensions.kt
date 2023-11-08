package com.w2sv.filenavigator.ui.model

import androidx.compose.ui.graphics.Color
import com.w2sv.data.model.FileType

/**
 * Returns previously cached Color.
 */
val FileType.color: Color
    get() = fileTypeColors.getValue(this)

private val fileTypeColors =
    FileType
        .getValues()
        .associateWith { Color(it.colorInt) }
