package com.pixelnetica.easyscan.ui.pagelist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.ui.viewitem.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

@HiltViewModel
open class PageListViewModel @Inject constructor(private val repository: EasyScanRepository) :
    ViewModel() {

    fun createNewPages(list: List<Uri>) {
        repository.insertPages(list)
    }

    val viewItems = repository
        .queryPageViewports(REPRESENTATIVE)
        .map { viewports ->
            viewports.map { (state, representation) ->
                with(state) {
                    when (page.status) {
                        Page.Status.Invalid -> InvalidViewItem(page, representation)

                        Page.Status.Initial -> InitialViewItem(page, representation)

                        Page.Status.Input -> InputViewItem(
                            page, representation,
                            repository.loadBitmapAsync(
                                checkNotNull(state.input) {
                                    "No input page"
                                }.inputPreviewFileId,
                                state.page.orientation
                            ).await()
                        )

                        Page.Status.Original -> OriginalViewItem(
                            page, representation,
                            repository.loadBitmapAsync(
                                checkNotNull(state.original) {
                                    "No original page"
                                }.originalPreviewFileId,
                                page.orientation
                            ).await()
                        )

                        Page.Status.Pending -> PendingViewItem(
                            page, representation,
                            repository.loadBitmapAsync(
                                // NOTE: Compatibility with previous database
                                state.pending?.pendingPreviewFileId
                                    ?: checkNotNull(state.original) {
                                        "No original page for pending"
                                    }.originalPreviewFileId,
                                page.orientation
                            ).await()
                        )

                        Page.Status.Complete -> CompleteViewItem(
                            page, representation,
                            repository.loadBitmapAsync(
                                checkNotNull(complete) {
                                    "No complete page"
                                }.completePreviewFileId,
                                page.orientation
                            ).await(),
                            complete,
                            recognition,
                        )

                        Page.Status.Output -> OutputViewItem(
                            page, representation,
                            repository.loadBitmapAsync(
                                checkNotNull(complete) {
                                    "No complete page"
                                }.completePreviewFileId,
                                page.orientation
                            ).await(),
                            complete,
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