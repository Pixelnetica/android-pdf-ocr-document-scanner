package com.pixelnetica.easyscan.ui.pagelist

import android.net.Uri
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.ui.viewitem.*
import com.pixelnetica.scanning.ScanOrientation
import com.pixelnetica.scanning.ScanPicture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
open class PageListViewModel @Inject constructor(private val repository: EasyScanRepository) :
    ViewModel() {

    fun createNewPages(list: List<Uri>) {
        repository.insertPages(list)
    }

    private suspend fun ScanPicture?.loadBitmap(orientation: ScanOrientation): Bitmap =
        checkNotNull(this) {
            "ScanPicture is null for PageListViewModel::loadBitmap"
        }.let {
            withContext(Dispatchers.IO) {
                createBitmap(ScanPicture.DISPLAY_BITMAP or ScanPicture.FIT_TO_TEXTURE or ScanPicture.FIT_TO_HARDWARE, orientation)
            }
        }

    val viewItems = repository
        .queryPageViewports(REPRESENTATIVE, preview = true)
        .map { viewports ->
            viewports.map { (state, representation) ->
                with(state) {
                    when (page.status) {
                        Page.Status.Invalid -> InvalidViewItem(page, representation)

                        Page.Status.Initial -> InitialViewItem(page, representation)

                        Page.Status.Input -> InputViewItem(
                            page, representation,
                            state.picture.loadBitmap(state.page.orientation)
                        )

                        Page.Status.Original -> OriginalViewItem(
                            page, representation,
                            state.picture.loadBitmap(state.page.orientation)
                        )

                        Page.Status.Pending -> PendingViewItem(
                            page, representation,
                            state.picture.loadBitmap(state.page.orientation)
                        )

                        Page.Status.Complete -> CompleteViewItem(
                            page, representation,
                            state.picture.loadBitmap(state.page.orientation),
                            checkNotNull(complete),
                            recognition,
                        )

                        Page.Status.Output -> OutputViewItem(
                            page, representation,
                            state.picture.loadBitmap(state.page.orientation),
                            checkNotNull(complete),
                            recognition,
                            checkNotNull(output)
                        )
                    }
                }
            }
        }
        .distinctUntilChanged()
        .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    val totalPages: Flow<List<PageViewId>> = viewItems
        .map { list ->
            list.map {
                it.id
            }
        }

    val checkedPages: Flow<List<PageViewId>> = viewItems
        .map { list ->
            list.filter {
                it.isChecked
            }.map {
                it.id
            }
        }
    fun checkPage(viewItem: PageViewItem, checked: Boolean) {
        repository.checkPage(viewItem.id.id, checked, REPRESENTATIVE)
    }

    fun checkAllPages(checked: Boolean) {
        repository.checkAllPages(checked, REPRESENTATIVE)
    }

    fun deleteCheckedPages() {
        repository.deleteCheckedPages(REPRESENTATIVE)
    }

    fun movePage(itemFrom: PageViewItem, itemTo: PageViewItem) {
        repository.movePage(itemFrom.id.id, itemTo.id.id)
    }

    fun reorderPages(items: List<PageViewItem>) {
        repository.reorderPages(items.map { it.id.id })
    }

    companion object {
        const val REPRESENTATIVE = "PageList"
    }
}