package com.pixelnetica.easyscan.ui.cropscreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.data.PageState
import com.pixelnetica.easyscan.ui.viewitem.PageViewId
import com.pixelnetica.easyscan.ui.viewitem.providePageViewId
import com.pixelnetica.scanning.ScanCutout
import com.pixelnetica.scanning.ScanOrientation
import com.pixelnetica.scanning.ScanPicture
import com.pixelnetica.support.Tag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class CropScreenViewModel @Inject constructor(
    private val repository: EasyScanRepository,
    @Named("cropPageId") private val pageViewId: PageViewId
): ViewModel() {

    private val pageId = pageViewId.id

    private data class PageContent(
        val picture: ScanPicture?,
        val orientation: ScanOrientation?,
        val imageCutout: ScanCutout?,
        val inputCutout: ScanCutout?,
        val originCutout: ScanCutout?,
        val originOrientation: ScanOrientation?,
    ) {
        constructor(
            picture: ScanPicture?,
            orientation: ScanOrientation?,
            imageCutout: ScanCutout?,
            inputCutout: ScanCutout?,
            ) : this(picture, orientation, imageCutout, inputCutout, imageCutout, orientation)
        fun rotate(clockwise: Boolean): PageContent =
            copy(
                orientation =
                    if (clockwise) {
                        orientation?.rotateCW()
                    } else {
                        orientation?.rotateCCW()
                    }
            )

        fun revertCutout(expand: Boolean): PageContent =
            copy(
                imageCutout = inputCutout?.let {
                    // Make a copy of input cutout
                    ScanCutout(it).apply {
                        if (expand) {
                            // Expand is specified
                            expand()
                        }
                    }
                }
            )

        fun changeCutout(cutout: ScanCutout?): PageContent =
            copy(imageCutout = cutout)
    }

    private val pageContent = MutableStateFlow<PageContent?>(null).apply {
        // Query repository updates
        repository.queryPagePictureState(pageId, false, Page.Status.Input).map { state: PageState ->
            PageContent(state.picture, state.page.orientation, state.page.cutout, state.input?.inputCutout)
        }.onEach { pageContent: PageContent ->
            emit(pageContent)
        }.launchIn(viewModelScope)
    }

    val imagePicture = pageContent
        .filterNotNull()
        .map {
            it.picture
        }.distinctUntilChanged()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    val imageOrientation = pageContent
        .filterNotNull()
        .map {
            it.orientation
        }.distinctUntilChanged()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)


    val imageCutout = pageContent
        .filterNotNull()
        .map {
            it.imageCutout
        }.distinctUntilChanged()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)


    val canExpandCutout = imageCutout.map {
        it?.isDefined == true
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = false)


    val canRevertCutout = pageContent
        .filterNotNull()
        .map {
            it.inputCutout?.isDefined == true
        }.distinctUntilChanged()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = false)


    fun rotatePage(clockwise: Boolean) {
        viewModelScope.launch {
            pageContent.emit(
                pageContent.value?.rotate(clockwise)
            )
        }
    }


    fun revertCutout(expand: Boolean) {
        viewModelScope.launch {
            pageContent.emit(
                pageContent.value?.revertCutout(expand)
            )
        }
    }


    fun changeCutout(cutout: ScanCutout?) {
        viewModelScope.launch {
            pageContent.emit(
                pageContent.value?.changeCutout(cutout)
            )
        }
    }


    fun acceptChanges() {
        // Null-safe
        pageContent.value?.let { pageContent: PageContent ->
            if (pageContent.imageCutout != null && pageContent.orientation != null) {
                viewModelScope.launch(NonCancellable) {
                    if (pageContent.imageCutout != pageContent.originCutout) {
                        repository.setPageCutout(
                            pageId,
                            pageContent.imageCutout,
                            pageContent.orientation
                        )
                    } else if (pageContent.orientation != pageContent.originOrientation) {
                        repository.setPageOrientation(pageId, pageContent.orientation)
                    }
                }
            }
        }
    }


    companion object : Tag by AppTagger("CutoutScreenViewModel")
}

@Module
@InstallIn(ViewModelComponent::class)
object CropPageViewModelModule {
    @Provides
    @ViewModelScoped
    @Named("cropPageId")
    fun provideCropPageViewId(savedStateHandle: SavedStateHandle) =
        savedStateHandle.providePageViewId("cropPageId")
}

