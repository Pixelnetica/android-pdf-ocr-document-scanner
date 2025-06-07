package com.pixelnetica.easyscan.ui.pageprops

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.ui.viewitem.PageViewId
import com.pixelnetica.easyscan.ui.viewitem.providePageViewId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PagePropertiesViewModel @Inject constructor(
        private val repository: EasyScanRepository,
        @Named("pageId")
        _pageId: PageViewId,
    ) : ViewModel() {
    val pageId = _pageId.id

    val preview: Flow<Bitmap?> =
        repository
            .queryPagePreviews(listOf(pageId))
            .map { pageBitmaps ->
                pageBitmaps.firstOrNull()?.bitmap
            }

    val paper: Flow<Page.Paper.Predefined>
        get() =
            repository.queryPagePaperSize(pageId)

    val paperOrientation: Flow<Page.Paper.Orientation> get() =
        repository.queryPageOrientation(pageId)

    val hasText: Flow<Boolean> get() =
        repository.queryPagesHaveText(listOf(pageId))

    fun savePaper(paper: Page.Paper.Predefined, paperOrientation: Page.Paper.Orientation) {
        repository.setPagePaper(pageId, paper, paperOrientation)
    }

    fun ensurePageText(hasText: Boolean) {
        repository.ensurePageText(pageId, hasText)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object PageSliderViewModelModule {
    @Provides
    @ViewModelScoped
    @Named("pageId")
    fun provideStartPageId(savedStateHandle: SavedStateHandle): PageViewId {
        return savedStateHandle.providePageViewId("pageId")
    }
}