package com.pixelnetica.easyscan.ui.pagescreen

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.ui.viewitem.PageViewId
import com.pixelnetica.easyscan.ui.viewitem.PageViewPicture
import com.pixelnetica.easyscan.ui.viewitem.PageViewProfile
import com.pixelnetica.easyscan.ui.viewitem.providePageViewId
import com.pixelnetica.scanning.RefineFeature
import com.pixelnetica.support.Tag
import com.pixelnetica.support.cache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PageSliderViewModel @Inject constructor(
    private val repository: EasyScanRepository,
    @Named("startPageId")
    startPageId: PageViewId,
    ) : ViewModel() {

    // Return the same PageContent instance for the same Page.Id
    private val pageContentCache = WeakHashMap<Page.Id, PageContent>()

    val pageContent  = repository.pageIds.map { list ->
        list.map { pageId ->
            pageContentCache.cache(pageId) {
                PageContent(pageId)
            }
        }
    }

    // Page to scroll after operations
    private val _showPageId = MutableStateFlow(startPageId)
    val showPageId = _showPageId.asStateFlow()

    fun resetShowPage() {
        _showPageId.value = PageViewId.Undefined
    }

    inner class PageContent(private val pageId: Page.Id) {

        fun asKey(): Long = pageId.id

        fun asArg(): String = pageId.id.toString()

        fun asViewId(): PageViewId = PageViewId(pageId)

        /**
         * Shared deliberately, not collected afresh per reader.
         *
         * The image and the controls that act on it are drawn by different
         * parts of the screen. Reading this separately in each would let them
         * hold different answers for the same page for a moment - long enough
         * to offer an action on an image the screen has already given up on -
         * and would load the same picture twice besides.
         */
        val pagePicture: StateFlow<PageViewPicture> =
            repository
                .queryPagePictureState(pageId, false, null)
                .map { pageState ->
                    val orientation = pageState.page.orientation
                    val isComplete = pageState.page.status.isAtLeast(Page.Status.Complete)
                    PageViewPicture(
                        pageState.picture,
                        orientation,
                        isComplete,
                        isUnavailable = pageState.page.status == Page.Status.Invalid,
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    // Stop the moment nobody is looking, and drop the value
                    // with it. A picture holds native memory, so keeping one
                    // warm for a page that is no longer on screen would hold
                    // that memory for as long as this screen lives - and
                    // handing a returning reader the old picture of a page
                    // that has since become unusable is exactly the stale
                    // answer this sharing exists to prevent.
                    started = SharingStarted.WhileSubscribed(
                        stopTimeoutMillis = 0,
                        replayExpirationMillis = 0,
                    ),
                    initialValue = PageViewPicture.Empty,
                )

        val profile  =
            repository
                .queryPageProfile(pageId)
                .filterNotNull()
                .map { pageProfile ->
                    PageViewProfile(pageProfile)
                }

        fun deletePage() {
            repository.deletePages(pageId)
        }

        fun rotatePage() {
            repository.rotatePage(pageId, false)
        }

        fun setShadows(shadows: Boolean) {
            repository.setPageShadows(pageId, shadows)
        }

        fun setProfile(profile: PageViewProfile.Type) {
            repository.setPageProfile(
                pageId,
                when (profile) {
                    PageViewProfile.Type.Original -> RefineFeature.Profile.Type.Original
                    PageViewProfile.Type.Bitonal -> RefineFeature.Profile.Type.Bitonal
                    PageViewProfile.Type.Monochrome -> RefineFeature.Profile.Type.Monochrome
                    PageViewProfile.Type.Colored -> RefineFeature.Profile.Type.Colored
                }
            )
        }

        override fun toString(): String =
            "PageSliderViewModel {pageId=${pageId.id}}"
    }

    fun insertNewPages(insertAfter: PageContent?, uriList: List<Uri>) {
        repository.insertPages(
            uriList,
            insertAfter?.asViewId()?.id ?: Page.Id.Undefined,
        ) { pageIds ->
            _showPageId.value =
                pageIds.firstOrNull()
                    ?.let { PageViewId(it) }
                    ?: PageViewId.Undefined
        }
    }

    companion object: Tag by AppTagger("PageSliderViewModel") {
        const val REPRESENTATIVE = "PageSlider"
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object PageSliderViewModelModule {
    @Provides
    @ViewModelScoped
    @Named("startPageId")
    fun provideStartPageId(savedStateHandle: SavedStateHandle): PageViewId {
        return savedStateHandle.providePageViewId("startPageId")
    }
}