package com.pixelnetica.easyscan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.pixelnetica.scanning.ScanText


@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Page::class,
            parentColumns = ["pageId"],
            childColumns = ["textId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class Text(
    @PrimaryKey
    @Embedded(prefix = "text")
    val id: Page.Id,

    /**
     * Recognition result
     */
    val original: ScanText,

    /**
     * Changed by user
     */
    val modified: ScanText,
)
