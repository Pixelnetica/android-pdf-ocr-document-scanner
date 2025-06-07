package com.pixelnetica.easyscan.data

import android.graphics.RectF
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Page::class,
            parentColumns = ["pageId"],
            childColumns = ["recognitionId"],
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class Recognition(
    @PrimaryKey
    @Embedded(prefix = "recognition")
    val id: Page.Id,

    /**
     * Recognition progress
     */
    @ColumnInfo(defaultValue = "-1")
    val progress: Int = -1,

    /**
     * Lookup item
     */
    val lookup: RectF? = null,
    )
