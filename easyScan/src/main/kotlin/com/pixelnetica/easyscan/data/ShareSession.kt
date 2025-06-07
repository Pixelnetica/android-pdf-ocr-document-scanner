package com.pixelnetica.easyscan.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ShareSession(
    @Embedded(prefix = "shareSession")
    @PrimaryKey(autoGenerate = true)
    val id: Id,
    val type: Type,

    val progress: Int = -1,
) {
    enum class Type {
        PNG,
        TIFF,
        PDF,
        Text,
    }

    data class Id(
        @ColumnInfo(name = "Id")
        val id: Long
    ) {
        companion object {
            val Initial = Id(0L)
        }
    }
}
