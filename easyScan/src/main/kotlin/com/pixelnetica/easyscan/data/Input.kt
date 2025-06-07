package com.pixelnetica.easyscan.data

import androidx.room.*
import com.pixelnetica.scanning.ScanCutout
import com.pixelnetica.scanning.ScanOrientation

@Entity(
    indices = [Index("inputId"), Index("inputImageFileId"), Index("inputPreviewFileId")],
    foreignKeys = [
        ForeignKey(entity = Page::class, parentColumns = ["pageId"], childColumns = ["inputId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["inputImageFileId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["inputPreviewFileId"], onDelete = ForeignKey.RESTRICT),
    ],
)
data class Input(
    @PrimaryKey
    @Embedded(prefix = "input")
    val id: Page.Id,

    /**
     * reference to source file
     */
    @Embedded(prefix = "inputImageFile")
    val inputImageFileId: DataFile.Id,

    /**
     * reference to preview file
     */
    @Embedded(prefix = "inputPreviewFile")
    val inputPreviewFileId: DataFile.Id,

    /**
     * Initial image orientation
     */
    val inputOrientation: ScanOrientation,

    /**
     * Detected cutout
     */
    val inputCutout: ScanCutout,
)
