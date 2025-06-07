package com.pixelnetica.easyscan.data

import android.graphics.RectF
import android.net.Uri
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pixelnetica.scanning.RefineFeature
import com.pixelnetica.scanning.ScanCutout
import com.pixelnetica.scanning.ScanText
import java.util.*

object EasyScanConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?) = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?) = date?.time

    @TypeConverter
    fun uriFromString(value: String?) = value?.let {
        Uri.parse(it)
    }

    @TypeConverter
    fun uriToString(uri: Uri?) = uri?.toString()

    @TypeConverter
    fun dataFileIdToLong(fileId: DataFile.Id?): Long? = fileId?.id

    @TypeConverter
    fun dataFileIdFromLong(value: Long?): DataFile.Id? = value?.let {
        DataFile.Id(it)
    }

    @TypeConverter
    fun pageIdToLong(pageId: Page.Id): Long = pageId.id

    @TypeConverter
    fun pageIdFromLong(value: Long): Page.Id = Page.Id(value)

    @TypeConverter
    fun profileTypeToLong(type: RefineFeature.Profile.Type): Long = type.ordinal.toLong()

    @TypeConverter
    fun profileTypeFromLong(value: Long): RefineFeature.Profile.Type =
        RefineFeature.Profile.Type.entries[value.toInt()]

    @TypeConverter
    fun cutoutFromByteArray(value: ByteArray?) = value?.let { ScanCutout(it) }

    @TypeConverter
    fun cutoutToByteArray(cutout: ScanCutout?) = cutout?.writeBytes(true)

    @TypeConverter
    fun fRectToJsonString(rect: RectF?): String? = rect?.let {
        val type = object : TypeToken<List<Float>>() {}.type
        Gson().toJson(listOf(it.left, it.top, it.right, it.bottom), type)
    }

    @TypeConverter
    fun fRectFromJSonString(value: String?) = value?.let {
        val type = object : TypeToken<List<Float>>() {}.type
        val items = Gson().fromJson<List<Float>>(it, type)
        check(items.size == 4) {
            "Illegal array size for RectF ${items.size}"
        }
        RectF(items[0], items[1], items[2], items[3])
    }

    @TypeConverter
    fun scanTextFromByteArray(value: ByteArray?): ScanText? = value?.let {
        ScanText(it)
    }

    @TypeConverter
    fun scanTextToByteArray(scanText: ScanText?): ByteArray? = scanText?.writeBytes(true)

    @TypeConverter
    fun shareSessionIdToLong(value: ShareSession.Id?) = value?.id

    @TypeConverter
    fun shareSessionIdFromLong(value: Long?) = value?.let {
        ShareSession.Id(it)
    }

    @TypeConverter
    fun paperSizeToLong(paper: Page.Paper?): Long? =
        paper?.toLong()

    @TypeConverter
    fun paperSizeFromLong(value: Long?): Page.Paper? = value?.let {
        Page.Paper.fromLong(value)
    }
}