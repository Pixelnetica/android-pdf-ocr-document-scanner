package com.pixelnetica.easyscan.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = [
        "shareItemId",
        "shareOrder",
    ],

    indices = [
        Index("shareItemId"),
        Index("sharePageId")
              ],

    foreignKeys = [
        // Delete item when session will be deleted
        ForeignKey(entity = ShareSession::class, parentColumns = ["shareSessionId"], childColumns = ["shareItemId"], onDelete = ForeignKey.CASCADE),
        // Delete item when page will be delete
        ForeignKey(entity = Page::class, parentColumns = ["pageId"], childColumns = ["sharePageId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ShareItem(
    @Embedded("shareItem")
    val shareItemId: ShareSession.Id,
    // page number
    val shareOrder: Int,

    @Embedded(prefix = "sharePage")
    val pageId: Page.Id,
)
