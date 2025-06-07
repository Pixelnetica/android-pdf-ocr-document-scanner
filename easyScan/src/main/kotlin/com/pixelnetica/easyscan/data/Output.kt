package com.pixelnetica.easyscan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pixelnetica.scanning.ScanOrientation

@Entity(
    indices = [
        Index("outputId"),
        Index("outputFileId"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = Page::class,
            parentColumns = ["pageId"],
            childColumns = ["outputId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DataFile::class,
            parentColumns = ["fileId"],
            childColumns = ["outputFileId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Output(
    @PrimaryKey
    @Embedded(prefix = "output")
    val id: Page.Id,

    /**
     * Reference to output file
     */
    @Embedded(prefix = "outputFile")
    val outputFileId: DataFile.Id,

    /**
     * Orientation output file was written
     */
    val outputOrientation: ScanOrientation,

    /**
     * Estimated output size
     */
    val estimatedSize: Long,
)
