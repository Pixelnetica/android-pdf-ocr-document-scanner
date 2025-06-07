package com.pixelnetica.easyscan.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    primaryKeys = ["representationId", "representative"],
    foreignKeys = [
        ForeignKey(entity = Page::class, parentColumns = ["pageId"], childColumns = ["representationId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Representation(
    @Embedded(prefix = "representation")
    val id: Page.Id,

    val representative: String,

    @ColumnInfo(defaultValue = "0")
    val checked: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val selected: Int = 0,

    @ColumnInfo(defaultValue = Long.MAX_VALUE.toString())
    val deleteAfter: Date = Date(Long.MAX_VALUE),

)
