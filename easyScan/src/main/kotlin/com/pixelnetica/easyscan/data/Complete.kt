package com.pixelnetica.easyscan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    indices = [Index("completeId"), Index("completeImageFileId"), Index("completePreviewFileId")],
    foreignKeys = [
        ForeignKey(entity = Page::class, parentColumns = ["pageId"], childColumns = ["completeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["completeImageFileId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DataFile::class, parentColumns = ["fileId"], childColumns = ["completePreviewFileId"], onDelete = ForeignKey.RESTRICT),
    ]
)
data class Complete(
    @PrimaryKey
    @Embedded(prefix = "complete")
    val id: Page.Id,

    @Embedded(prefix = "completeImageFile")
    val completeImageFileId: DataFile.Id,

    @Embedded(prefix = "completePreviewFile")
    val completePreviewFileId: DataFile.Id,

    /**
     * Last modified data
     */
    val dateModified: Date = Date(),
    )
