package com.pixelnetica.easyscan.data

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.io.File

@Entity
data class DataFile(
    @Embedded(prefix = "file")
    @PrimaryKey(autoGenerate = true)
    val id: Id,

    /**
     * main value: relative path to file
     */
    val path: String
) {
    @Parcelize
    data class Id(
        @ColumnInfo(name = "Id")
        val id: Long
    ) : Parcelable {
        companion object {
            val Initial = Id(0)
        }
    }

    fun buildFile(root: File) = File(root, path)

    companion object {
        val Empty = DataFile(Id.Initial, "")
    }
}