package com.pixelnetica.easyscan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pending store last processed image (or original for first time)
 */
@Entity(
    indices = [Index("pendingId"), Index("pendingImageFileId"), Index("pendingPreviewFileId")],
    foreignKeys = [
        ForeignKey(entity = Page::class, parentColumns = ["pageId"], childColumns = ["pendingId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["pendingImageFileId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["pendingPreviewFileId"], onDelete = ForeignKey.RESTRICT),
    ]
)
data class Pending(
    @PrimaryKey
    @Embedded(prefix = "pending")
    val id: Page.Id,

    @Embedded(prefix = "pendingImageFile")
    val pendingImageFileId: DataFile.Id,

    @Embedded(prefix = "pendingPreviewFile")
    val pendingPreviewFileId: DataFile.Id,
)
