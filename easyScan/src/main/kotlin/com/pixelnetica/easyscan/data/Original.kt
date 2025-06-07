package com.pixelnetica.easyscan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index("originalId"), Index("originalImageFileId"), Index("originalPreviewFileId")],
    foreignKeys = [
        ForeignKey(entity = Page::class, parentColumns = ["pageId"], childColumns = ["originalId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["originalImageFileId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["originalPreviewFileId"], onDelete = ForeignKey.RESTRICT),
    ]
)
data class Original(
    @PrimaryKey
    @Embedded(prefix = "original")
    val id: Page.Id,

    @Embedded(prefix = "originalImageFile")
    val originalImageFileId: DataFile.Id,

    @Embedded(prefix = "originalPreviewFile")
    val originalPreviewFileId: DataFile.Id,
)
