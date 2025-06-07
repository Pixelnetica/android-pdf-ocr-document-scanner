package com.pixelnetica.easyscan.ui.pageprops

import android.content.Context
import androidx.annotation.ArrayRes
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.data.Page

/**
 * Helper class to manage sparse paper size lizt
 */
class PaperMapper(context: Context, @ArrayRes displayId: Int ) {
    private val paperList = context.resources.getStringArray(R.array.paper_sizes)
    private val paperIdList = context.resources.getStringArray(R.array.paper_size_id_list)

    init {
        check(paperList.size == paperIdList.size)
    }

    private val displayIdList: Array<String> = context.resources.getStringArray(displayId)
    val displayList = displayIdList.map { id ->
        val index = paperIdList.indexOf(id)
        check(index >= 0)
        paperList[index]
    }

    fun displayIndexOf(paper: Page.Paper.Predefined): Int {
        val index = paper.size.ordinal
        require(index >= 0 && index < paperIdList.size) {
            "Invalid paper index $index"
        }

        val id = paperIdList[index]
        return displayIdList.indexOf(id)
    }

    fun getPaperForDisplayIndex(displayIndex: Int): Page.Paper.Predefined {
        require(displayIndex >= 0 && displayIndex < displayIdList.size) {
            "Invalid display index $displayIndex"
        }
        val id = displayIdList[displayIndex]
        val index = paperIdList.indexOf(id)
        check(index >= 0) {
            "Cannot find paper index for id $id"
        }

        return Page.Paper.Predefined(Page.Paper.Predefined.Size.values()[index])
    }
}