package com.pixelnetica.easyscan.ui.sharedialog

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.data.EasyScanRepository
import com.pixelnetica.easyscan.data.ShareSession
import com.pixelnetica.easyscan.ui.viewitem.PageViewId
import com.pixelnetica.support.Tag
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ShareViewModel
@Inject constructor(
    private val repository: EasyScanRepository,
    @Named("pages") pages: List<PageViewId>,
    ) : ViewModel() {

    private val pageIds = pages.map { it.id }

    val previews: Flow<List<Bitmap?>> =
        repository
            .queryPagePreviews(pageIds)
            .map { pageBitmaps ->
                // Keep order!
                val associations = pageBitmaps.associateBy { it.pageId }
                pageIds.map {
                    associations[it]?.bitmap
                }
            }

    val hasText = repository
        .queryPagesHaveText(pageIds)

    /**
     * To show progress bar
     */
    val hasShareSessions = repository
        .queryHasShareSessions()

    val singlePage = pages.size < 2

    enum class ShareType {
        PNG,
        TIFF,
        PDF,
        Text,
    }

    fun sharePages(type: ShareType) {
        val shareType = ShareSession.Type.values()[type.ordinal]
        repository.createShareSession(shareType, pageIds)
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object ShareViewModelModule {
    @Provides
    @ViewModelScoped
    @Named("pages")
    fun provideSharePageViewIdList(savedStateHandle: SavedStateHandle): List<PageViewId> {
        val key = "pages"
        return savedStateHandle.get<String>(key)
            .orEmpty()
            .split(",")
            .map {
                PageViewId(it.trim().toLong())
            }
    }
}

internal object Logger: Tag by AppTagger("ShareViewModel")