package com.pixelnetica.easyscan.ui.cropscreen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.design.crop.CropHandler
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.data.EasyScanRepository
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class CropScreenViewModel @Inject constructor(
    private val repository: EasyScanRepository,
    @Named("cropPageId") private val pageViewId: PageViewId
): ViewModel() {

    private val pageId = pageViewId.id

    private data class Content(
        val picture: ScanPicture?,
        val orientation: ScanOrientation?,
        val imageCutout: ScanCutout?,
        val inputCutout: ScanCutout?,
    )

    private val pageContent = repository.queryPageState(pageId).map { state ->
        val picture = state.input?.inputImageFileId?.let { dataFileId ->
            repository.loadPictureAsync(dataFileId).await()
        }

        Content(picture, state.page.orientation, state.page.cutout, state.input?.inputCutout)
    }.distinctUntilChanged().shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    val imagePicture =
        pageContent
            .map {
                it.picture
            }

    val imageOrientation =
        pageContent
            .map {
                it.orientation
            }

    val imageCutout =
        MutableStateFlow<ScanCutout?>(null)
            .apply {
                pageContent
                    .map {
                        it.imageCutout
                    }
                    .onEach {
                        emit(it)
                    }
                    .launchIn(viewModelScope)
            }

    val canExpandCutout = imageCutout.map {
        it?.isDefined == true
    }

    val canRevertCutout = pageContent.map {
        it.inputCutout?.isDefined == true
    }

    fun rotatePage(clockwise: Boolean) {
        repository.rotatePage(pageId, clockwise)
    }

    fun revertCutout(expand: Boolean) {
        viewModelScope.launch {
            repository.getInputCutoutAsync(pageId, expand).await()?.also { cutout ->
                imageCutout.emit(cutout)
            }
        }
    }

    fun changeCutout(cutout: ScanCutout?) {
        viewModelScope.launch {
            imageCutout.emit(cutout)
        }
    }

    fun acceptCutout() {
        // Null-safe
        imageCutout.value?.let { cutout ->
            viewModelScope.launch {
                withContext(NonCancellable) {
                    repository.setPageCutout(pageId, cutout)
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

