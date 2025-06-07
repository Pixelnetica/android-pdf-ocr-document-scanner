package com.pixelnetica.easyscan.ui.viewitem

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.pixelnetica.easyscan.data.Complete
import com.pixelnetica.easyscan.data.Output
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.data.PageProfile
import com.pixelnetica.easyscan.data.Recognition
import com.pixelnetica.easyscan.data.Representation
import com.pixelnetica.scanning.RefineFeature
import com.pixelnetica.scanning.ScanOrientation
import com.pixelnetica.scanning.ScanPicture
import kotlinx.parcelize.Parcelize

@Parcelize
data class PageViewId(val id: Page.Id): Parcelable {

    // TODO: Remove when Compose Navigation will support parcelable types
    constructor(arg: Long): this(Page.Id(arg))
    fun asArg() = id.id.toString()
    fun asKey() = id.id

    companion object {
        val Undefined = PageViewId(Page.Id.Undefined)
    }
}

// Shortcut for dependency providers
fun SavedStateHandle.providePageViewId(key: String): PageViewId {
    val id = checkNotNull(get<Long>(key)) {
        "Invalid PageViewId key $key."
    }
    return PageViewId(id)
}

data class PageViewPicture(
    val picture: ScanPicture?,
    val orientation: ScanOrientation,
    val isComplete: Boolean,
) {
    companion object {
        val Empty = PageViewPicture(null, ScanOrientation.Undefined, false)
    }
}

data class PageViewProfile(
    val shadows: Boolean,
    val profile: Type) {
    enum class Type {
        Original,
        Bitonal,
        Monochrome,
        Colored,
    }

    constructor(pageProfile: PageProfile) :
            this(
                pageProfile.strongShadows,
                when (pageProfile.profileType) {
                    RefineFeature.Profile.Type.Original -> Type.Original
                    RefineFeature.Profile.Type.Bitonal -> Type.Bitonal
                    RefineFeature.Profile.Type.Monochrome -> Type.Monochrome
                    RefineFeature.Profile.Type.Colored -> Type.Colored
                }
            )
}

sealed class PageViewItem(
    protected open val page: Page,
    protected open val representation: Representation?,
) {
    val id get() = PageViewId(page.id)

    val order get() = page.orderIndex + 1

    val isChecked get() = representation?.checked ?: false

    val isCutoutUndefined get() = page.undefinedCutout

    val isRecognitionReady get() = page.recognitionTask.isReady
    val isRecognitionPending get() = page.recognitionTask.isPending

    val orientation get() = page.orientation
}

// Preview-less items
data class InvalidViewItem(
    override val page: Page,
    override val representation: Representation?,
): PageViewItem(page, representation) {
    val errorMessage = page.errorMessage.orEmpty()
}
data class InitialViewItem(
    override val page: Page,
    override val representation: Representation?,
): PageViewItem(page, representation)

// Loaded page always has a preview
sealed class PreviewViewItem(
    override val page: Page,
    override val representation: Representation?,
    open val preview: Bitmap,
): PageViewItem(page, representation) {
    val isProcessing get() = !page.status.isAtLeast(Page.Status.Complete)
}

data class InputViewItem(
    override val page: Page,
    override val representation: Representation?,
    override val preview: Bitmap,
): PreviewViewItem(page, representation, preview)

data class OriginalViewItem(
    override val page: Page,
    override val representation: Representation?,
    override val preview: Bitmap,
): PreviewViewItem(page, representation, preview)

data class PendingViewItem(
    override val page: Page,
    override val representation: Representation?,
    override val preview: Bitmap,
): PreviewViewItem(page, representation, preview)

sealed class ProcessedViewItem(
    override val page: Page,
    override val representation: Representation?,
    override val preview: Bitmap,
    open val complete: Complete,
    open val recognition: Recognition?,
) : PreviewViewItem(page, representation, preview) {
    val dateModified get()  = complete.dateModified
    val hasRecognitionLookup get() = recognition?.lookup != null
    val recognitionProgress get(): Int {
        // Tesseract perform main recognition from -1 to 70% and at last set to 100%
        val percent = ((recognition?.progress ?: -1) + 1) * 100 / 71
        return kotlin.math.min(100, percent)
    }
}

data class CompleteViewItem(
    override val page: Page,
    override val representation: Representation?,
    override val preview: Bitmap,
    override val complete: Complete,
    override val recognition: Recognition?,
): ProcessedViewItem(page, representation, preview, complete, recognition)

data class OutputViewItem(
    override val page: Page,
    override val representation: Representation?,
    override val preview: Bitmap,
    override val complete: Complete,
    override val recognition: Recognition?,
    val output: Output,
): ProcessedViewItem(page, representation, preview, complete, recognition) {
    val estimatedSize = output.estimatedSize
}


