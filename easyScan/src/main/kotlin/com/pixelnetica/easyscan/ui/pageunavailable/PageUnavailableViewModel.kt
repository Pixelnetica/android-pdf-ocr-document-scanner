package com.pixelnetica.easyscan.ui.pageunavailable

import androidx.lifecycle.ViewModel
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.ui.viewitem.PageViewId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PageUnavailableViewModel @Inject constructor(
    private val repository: EasyScanRepository,
    @Named("pageId")
    pageId: PageViewId,
) : ViewModel() {

    private val id = pageId.id

    fun deletePage() = repository.deletePages(id)
}
