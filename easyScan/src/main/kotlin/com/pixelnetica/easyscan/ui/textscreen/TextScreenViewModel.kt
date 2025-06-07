package com.pixelnetica.easyscan.ui.textscreen

import android.graphics.RectF
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.design.lang.LanguageManager
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.ui.viewitem.PageViewId
import com.pixelnetica.easyscan.ui.viewitem.providePageViewId
import com.pixelnetica.scanning.ScanPicture
import com.pixelnetica.scanning.ScanText
import com.pixelnetica.support.Tag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class TextScreenViewModel @Inject constructor(
    private val repository: EasyScanRepository,
    private val languageManager: LanguageManager,
    @Named("textPageId") pageViewId: PageViewId,
): ViewModel() {

    private val pageId = pageViewId.id

    // To show current languages
    val languagesTitle = languageManager.getDisplayString(2)

    // ReadHandler implementation
    val imagePicture: Flow<ScanPicture?> =
        repository.queryOriginalPicture(pageId)

    val lookupRect: Flow<RectF?> =
        repository
            .queryRecognitionLookup(pageId)

    val lookupProgress: Flow<Int> =
        repository
            .queryRecognitionProgress(pageId)

    val originalText: Flow<ScanText> =
        repository
            .queryOriginalText(pageId)
            .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    private val _modifiedText = MutableStateFlow(initialModifiedText).apply {
        repository
            .queryModifiedText(pageId)
            .distinctUntilChanged()
            .onEach { modifiedText ->
//                log.d("Update modifiedText from repository = ${modifiedText.toString().lines().firstOrNull()}")
                emit(modifiedText)
            }.launchIn(viewModelScope)
    }
    val modifiedText: Flow<ScanText> = _modifiedText

    val hasText: Flow<Boolean> =
        originalText.map {
            it.isReady
        }

    fun cancel() = refresh(false)

    fun refresh(force: Boolean) {
        repository.refreshRecognition(pageId, force)
    }

    fun onModifiedTextChanged(modifiedText: ScanText) {
        viewModelScope.launch {
            _modifiedText.emit(modifiedText)
        }
    }

    fun delete() {
        repository.deleteRecognition(pageId)
    }

    fun saveChanges() {
        // Save only modified text
        _modifiedText.value.also {
            if (it !== initialModifiedText) {
                repository.updateModifiedText(pageId, it)
            }
        }
    }

    init {
        // Start recognize when recognized text is missing
        viewModelScope.launch {
            if (repository.queryRecognitionTask(pageId).first().isNothing) {
                refresh(true)
            }
        }
    }

    data class InconsistentLanguages(val currentLanguages: String, val textLanguages: String)

    // Skip empty original text when compare languages
    private val consistentText = originalText.filter { it.isReady }

    val inconsistentLanguages get() =
        combine(languageManager.languageStore, consistentText) { langStore, text ->
            if (text.checkLanguages(langStore.languages)) {
                null
            } else {
                InconsistentLanguages(langStore.languages, text.languages)
            }
        }.shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    companion object: Tag by AppTagger("TextScreenViewModel") {
        private val initialModifiedText = ScanText()
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object TextPageViewModelModule {
    @Provides
    @ViewModelScoped
    @Named("textPageId")
    fun provideTextPageViewId(savedStateHandle: SavedStateHandle) =
        savedStateHandle.providePageViewId("textPageId")
}