package com.pixelnetica.easyscan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Size
import com.pixelnetica.design.lang.LanguageManager
import com.pixelnetica.easyscan.AppSettings
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.BuildConfig
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.EasyScanSettings
import com.pixelnetica.easyscan.PaperProperties
import com.pixelnetica.easyscan.UserProcessing
import com.pixelnetica.easyscan.appSettingsDataStore
import com.pixelnetica.easyscan.paperPropertiesDataStore
import com.pixelnetica.easyscan.userProcessingDataStore
import com.pixelnetica.scanning.ImageWriter
import com.pixelnetica.scanning.ImageWriterPdf
import com.pixelnetica.scanning.ImageWriterPng
import com.pixelnetica.scanning.ImageWriterTiff
import com.pixelnetica.scanning.RefineFeature
import com.pixelnetica.scanning.ScanCutout
import com.pixelnetica.scanning.ScanDetector
import com.pixelnetica.scanning.ScanOrientation
import com.pixelnetica.scanning.ScanPicture
import com.pixelnetica.scanning.ScanReader
import com.pixelnetica.scanning.ScanText
import com.pixelnetica.scanning.rectify
import com.pixelnetica.scanning.resize
import com.pixelnetica.scanning.withOrientation
import com.pixelnetica.support.Tag
import com.pixelnetica.support.cache
import com.pixelnetica.support.insert
import com.pixelnetica.support.distinctList
import com.pixelnetica.support.untwistList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

data class ShareResult(
    val mimeType: String,
    val shareFiles: List<File>,
) {
    companion object {
        val Empty = ShareResult("", emptyList())
    }
}

interface EasyScanRepository {

    val pageIds: Flow<List<Page.Id>>

    fun queryPageViewports(representative: String, preview: Boolean): Flow<List<PageViewport>>

    fun queryPagePictureState(
        pageId: Page.Id,
        preview: Boolean,
        requiredStatus: Page.Status?,
        ): Flow<PageState>

    fun insertPages(
        uriList: List<Uri>,
        startPageId: Page.Id = Page.Id.Undefined,
        insertAfter: Boolean = true,
        callback: suspend EasyScanRepository.(List<Page.Id>) -> Unit = { }
    )

    fun checkPage(pageId: Page.Id, checked: Boolean, representative: String)

    fun checkAllPages(checked: Boolean, representative: String)

    fun deleteCheckedPages(representative: String)

    fun movePage(idFrom: Page.Id, idTo: Page.Id)

    fun reorderPages(pages: List<Page.Id>)

    fun deletePages(vararg pageId: Page.Id)

    fun rotatePage(pageId: Page.Id, clockwise: Boolean)

    fun queryPageProfile(pageId: Page.Id): Flow<PageProfile?>

    fun setPageShadows(pageId: Page.Id, shadows: Boolean)

    fun setPageProfile(pageId: Page.Id, profile: RefineFeature.Profile.Type)

    fun setPageOrientation (pageId: Page.Id, orientation: ScanOrientation)

    fun setPageCutout(pageId: Page.Id, cutout: ScanCutout, orientation: ScanOrientation)

    fun queryOriginalPicture(pageId: Page.Id): Flow<ScanPicture?>

    fun queryRecognitionLookup(pageId: Page.Id): Flow<RectF?>

    fun queryRecognitionProgress(pageId: Page.Id): Flow<Int>

    fun queryOriginalText(pageId: Page.Id): Flow<ScanText>

    fun queryModifiedText(pageId: Page.Id): Flow<ScanText>

    fun queryRecognitionTask(pageId: Page.Id): Flow<Page.RecognitionTask>

    fun refreshRecognition(pageId: Page.Id, force: Boolean)

    fun deleteRecognition(pageId: Page.Id)

    fun updateModifiedText(pageId: Page.Id, text: ScanText)

    fun queryPagesHaveText(pages: List<Page.Id>): Flow<Boolean>

    /** Whether any of these pages has lost the image it would be exported from. */
    fun queryPagesUnavailable(pages: List<Page.Id>): Flow<Boolean>

    fun createShareSession(type: ShareSession.Type, pages: List<Page.Id>)

    fun queryHasShareSessions(): Flow<Boolean>

    fun queryShareResult(): Flow<ShareResult>

    fun queryPagePaperSize(pageId: Page.Id): Flow<Page.Paper.Predefined>

    fun queryPageOrientation(pageId: Page.Id): Flow<Page.Paper.Orientation>

    fun setPagePaper(pageId: Page.Id, paper: Page.Paper, paperOrientation: Page.Paper.Orientation)

    fun ensurePageText(pageId: Page.Id, hasText: Boolean)

    class PageBitmap(val pageId: Page.Id, val bitmap: Bitmap?)
    fun queryPagePreviews(pageIds: List<Page.Id>): Flow<List<PageBitmap>>
}

class DefaultEasyScanRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val easyScanDao: EasyScanDao,
    private val languageManager: LanguageManager,
) : EasyScanRepository, CoroutineScope {

    init {
        instances++

        check(instances == 1) {
            "Repository must me a singleton"
        }
    }

    private val errorHandler = CoroutineExceptionHandler { _, error ->
        log.e("Repository error ${error.message.orEmpty()}")
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob() + errorHandler

    private val pagesDir: File by lazy {
        File(context.filesDir, PAGES_DIR).apply {
            mkdirs()
        }
    }

    private val shareDir: File by lazy {
        File(context.cacheDir, SHARE_DIR).apply {
            mkdirs()
        }
    }

    private fun makeShareFile(name: String) = File(shareDir, name).apply {
        deleteOnExit()
    }


    private fun makeTmpImageFile(block: File.() -> Unit) =
        File.createTempFile("scpx-", ".png").apply {
            deleteOnExit()
            block()
        }

    private fun DataFile.withdraw(file: File) = apply {
        val target = buildFile(pagesDir).apply {
            parentFile?.mkdirs()
        }

        check(file.renameTo(target)) {
            "Cannot move from $file to $target"
        }
    }

    /**
     * Page files are moved into place by [withdraw] *before* the database
     * transaction that records them commits, so for a moment a file exists on
     * disk that the store does not yet admit to owning. File housekeeping and
     * page writes take turns through this lock, so housekeeping never inspects
     * a page that is only half written.
     *
     * Always taken **outside** a database transaction, never inside one: a
     * page write holding a transaction while waiting for this lock, against
     * housekeeping holding the lock while it queries, is a deadlock.
     */
    private val pageFileLock = Mutex()

    internal suspend fun <T> withPageFiles(block: suspend () -> T): T =
        pageFileLock.withLock { block() }

    /**
     * Run a page transaction that also moves files into the page store.
     *
     * The lock is taken here, around the transaction rather than inside it,
     * which is the order that keeps [pageFileLock] free of deadlock. Stages
     * that only rewrite rows do not need this and call the dao directly.
     */
    private suspend fun withPageFileTransaction(
        pageIds: Set<Page.Id>,
        requiredStatus: Page.Status,
        block: suspend EasyScanDao.(Page) -> Unit,
    ) = withPageFiles {
        easyScanDao.withPages(pageIds, requiredStatus, block)
    }

    // Store existing picture
    private val pictureCache = WeakHashMap<DataFile, ScanPicture>()

    private fun DataFile.withdrawPictureFile(pictureFile: PictureFile) =
        withdraw(pictureFile.file).apply {
            pictureCache.insert(this) {
                pictureFile.picture
            }
        }.id

    /**
     * Load a page's picture, or report that its file is no longer there.
     *
     * A page's row and its image file are separate stores and they do drift
     * apart on real devices - a storage cleaner, a restore, or a page saved
     * while file housekeeping was running. That is a page-level failure, not
     * an I/O error worth propagating, so it comes back as no picture. A file
     * that is present but unreadable is a different problem and still raises.
     */
    private suspend fun loadPicture(dataFile: DataFile): ScanPicture? {
        val picture = try {
            pictureCache.cache(dataFile) {
                withContext(Dispatchers.IO) {
                    FileInputStream(dataFile.buildFile(pagesDir)).use { stream ->
                        ScanPicture.load(stream)
                    }
                }
            }
        } catch (missingFile: FileNotFoundException) {
            log.e("Page file ${dataFile.path} is gone: ${missingFile.message.orEmpty()}")
            return null
        }

        // Return a copy to process
        return ScanPicture(picture)
    }

    /** The message a page carries once its image file has gone missing. */
    private val missingFileMessage: String
        get() = context.getString(R.string.page_error_image_file_missing)

    /**
     * The one way into a page's stored picture.
     *
     * Every screen resolves a page through here, so the handling of a page
     * whose file has gone missing lives in one place rather than in each
     * caller.
     */
    private suspend fun pagePictureState(
        pageId: Page.Id,
        preview: Boolean,
        requiredStatus: Page.Status? = null,
    ): PageState? =
        easyScanDao.getPagePictureState(
            pageId,
            preview,
            requiredStatus,
            ::loadPicture,
            missingFileMessage,
        )

    /**
     * The page's file turned out to be gone while exporting it. Record that
     * against the page and stop the export: a document quietly missing a page
     * is worse than none, and the record is what lets the next attempt explain
     * itself instead of failing the same silent way again.
     */
    private suspend fun abortExportForMissingFile(pageId: Page.Id): Nothing {
        failMissingFile(pageId)
        error("The image for page ${pageId.id} is no longer on the device")
    }

    /** Record that a page's image file is gone, so it stays failed after a restart. */
    private suspend fun failMissingFile(pageId: Page.Id) {
        easyScanDao.getPage(pageId, Page.Status.entries.toSet())?.let { page ->
            easyScanDao.failedPage(PageFailedStatus(page, missingFileMessage))
        }
    }

    private suspend fun getDataFile(fileId: DataFile.Id) =
        checkNotNull(easyScanDao.getDataFile(fileId)) {
            "Data file $fileId is missed."
        }

    private fun Long.formatHex() = toString(16).padStart(8, '0')
    private suspend fun createPageFile(
        page: Page,
        prefix: String,
        suffix: String = ".png",
    ): DataFile =
        easyScanDao.createDataFile { fileId ->
            DataFile(fileId, "page-${page.id.id.formatHex()}${File.separatorChar}$prefix$suffix")
        }

    /**
     * Typed pair for ScanPicture and File
     */
    private class PictureFile(val picture: ScanPicture, val file: File)

    // Save picture to given file
    private fun ScanPicture.writeTmpFile() = PictureFile(
        this,
        makeTmpImageFile {
            FileOutputStream(this).use {
                this@writeTmpFile.write(it, false)
            }
        })

    private fun ScanPicture.makePreview() = ScanPicture(this).also { preview ->
        preview.resize(previewSize, true)
    }

    // Skip "Cancellation" exception
    private inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> =
        if (exceptionOrNull() is CancellationException) {
            this
        } else {
            onFailure(action)
        }

    private class InputData(
        val image: PictureFile,
        val preview: PictureFile,
        val initialOrientation: ScanOrientation,
        val detectedCutout: ScanCutout,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun lookupInitialPages() = easyScanDao
        .queryInitialPages()
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()
        .mapLatest { pages ->
            pages.associate { page: Page ->
                page.id to runCatching {
                    // Load picture from specified URI. Do not close this
                    // picture or its preview: writeTmpFile hands both wrappers
                    // to the page cache together with the saved files, so they
                    // stay owned by the cache after this block.
                    val picture = ScanPicture(context, checkNotNull(page.initialUri))

                    // Reset orientation for data file
                    val initialOrientation = picture.orientation
                    picture.orientation = ScanOrientation.Undefined

                    // Save images outside transaction
                    val image = picture.writeTmpFile()
                    val preview = picture.makePreview().writeTmpFile()

                    val cutout = picture.detectCutout()

                    InputData(image, preview, initialOrientation, cutout)
                }
            }
        }.onEach { data ->
            withPageFileTransaction(data.keys, Page.Status.Initial) { page ->
                runCatching {
                    with(data.getValue(page.id).getOrThrow()) {
                        insertInput(
                            Input(
                                page.id,
                                createPageFile(page, "input").withdrawPictureFile(image),
                                createPageFile(page, "input-preview").withdrawPictureFile(preview),
                                initialOrientation,
                                detectedCutout,
                            )
                        )

                        updatePageInput(
                            PageInputStatus(
                                page = page,
                                orientation = initialOrientation
                            )
                        )
                    }
                }.onError {
                    easyScanDao.failedPage(PageFailedStatus(page, it.message.orEmpty()))
                }
            }
        }

    private class OriginalData(
        val image: PictureFile,
        val preview: PictureFile,
        val cutout: ScanCutout,
        val undefinedCutout: Boolean,
        val detectedOrientation: ScanOrientation,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun lookupPageInputs() = easyScanDao
        .queryPageInputs()
        .filter { list ->
            list.isNotEmpty()
        }
        .distinctUntilChanged()
        .map { list ->
            list.map { pageInput ->
                val state = pagePictureState(pageInput.page.id, false)

                pageInput.copy(picture = state?.picture)
            }.filter {
                it.picture != null
            }
        }
        .mapLatest { items ->
            // Prepare ScanDetector for each set of images. The detector holds
            // native memory, so use{} releases it as soon as this batch is
            // processed - including when a newer emission cancels this one.
            val languageManager = LanguageManager.getInstance(context = context)
            languageManager.detectorPath.first()?.let {
                ScanDetector(it)
            }.use { orientationDetector ->
                items.associate { (page, input, inputPicture) ->
                    page.id to runCatching {
                        // Setup cutout
                        val (cutout, undefined) = when (page.resetCutout) {
                            Page.ResetCutout.Reset -> Pair(
                                input.inputCutout,
                                !input.inputCutout.isDefined
                            )

                            Page.ResetCutout.Setup -> Pair(checkNotNull(page.cutout) {
                                "User cutout is not defined!"
                            }, false)

                            Page.ResetCutout.Expand -> Pair(ScanCutout(input.inputCutout).apply {
                                expand()
                            }, false)
                        }

                        val picture = checkNotNull(inputPicture) {
                            "Cannot load the input picture!"
                        }

                        picture.rectify(cutout)

                        // Try to detect orientation of text AFTER rectify to cutout was found
                        val detectedOrientation =
                            if (page.autoDetectOrientation && orientationDetector != null && picture.detectOrientation(orientationDetector)) {
                                picture.orientation
                        } else {
                            ScanOrientation.Undefined
                        }

                        OriginalData(
                            picture.writeTmpFile(),
                            picture.makePreview().writeTmpFile(),
                            cutout,
                            undefined,
                            detectedOrientation,
                        )
                    }
                }
            }
        }
        .onEach { data ->
            // Simple setup cutout
            // There isn't any processing
            withPageFileTransaction(data.keys, Page.Status.Input) { page ->
                runCatching {
                    with(data.getValue(page.id).getOrThrow()) {
                        insertOriginal(
                            Original(
                                page.id,
                                createPageFile(page, "original").withdrawPictureFile(image),
                                createPageFile(
                                    page,
                                    "original-preview"
                                ).withdrawPictureFile(preview),
                            )
                        )

                        updatePageOriginal(
                            PageOriginalStatus(
                                page = page,
                                cutout = cutout,
                                undefinedCutout = undefinedCutout,
                            )
                        )

                        if (detectedOrientation.isDefined()) {
                            updatePageDetectedOrientation(DetectedOrientation(page.id, detectedOrientation))
                        }
                    }
                }.onError {
                    easyScanDao.failedPage(PageFailedStatus(page, it.message.orEmpty()))
                }
            }
        }

    private fun lookupPageOriginals() = easyScanDao
        .queryPageOriginals()
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()
        .onEach { items ->
            items.associateBy { item -> item.page.id }.apply {
                easyScanDao.withPages(keys, Page.Status.Original) { page ->
                    runCatching {
                        with (getValue(page.id)) {
                            // Simple create Pending without any processing
                            insertPending(
                                Pending(
                                    page.id,
                                    original.originalImageFileId,
                                    original.originalPreviewFileId,
                                    )
                            )
                            updatePageStatus(PageStatus(page, Page.Status.Pending))
                        }
                    }.onFailure {
                        easyScanDao.failedPage(PageFailedStatus(page, it.message.orEmpty()))
                    }
                }
            }
        }
    private class CompleteData(
        val image: PictureFile,
        val preview: PictureFile,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun lookupPendingPages() = easyScanDao
        .queryPendingPages()
        .filter { it.isNotEmpty() }
        .map { list ->
            list.map { pagePending ->
                // NOTE: Here we get original picture
                val state = pagePictureState(pagePending.page.id, false, Page.Status.Original)

                pagePending.copy(picture = state?.picture)
            }.filter {
                it.picture != null
            }
        }
        .distinctUntilChanged()
        .mapLatest { items ->
            items.associate { (page, _, _, originalPicture) ->
                page.id to runCatching {
                    val picture = checkNotNull(originalPicture) {
                        "Cannot load the original picture"
                    }

                    picture.refine(
                        listOf(
                            RefineFeature.Shadows(page.strongShadows),
                            RefineFeature.Profile(page.profileType),
                        )
                    )

                    CompleteData(
                        picture.writeTmpFile(),
                        picture.makePreview().writeTmpFile(),
                    )
                }

            }
        }.onEach { data ->
            withPageFileTransaction(data.keys, Page.Status.Pending) { page ->
                runCatching {
                    with(data.getValue(page.id).getOrThrow()) {
                        val completeImage = createPageFile(page, "complete").withdrawPictureFile(image)
                        val completePreview = createPageFile(
                            page,
                            "complete-preview"
                        ).withdrawPictureFile(preview)
                        insertComplete(
                            Complete(
                                page.id,
                                completeImage,
                                completePreview,
                            )
                        )
                        // Update Pending
                        insertPending(
                            Pending(
                                page.id,
                                completeImage,
                                completePreview,
                            )
                        )

                        updatePageComplete(PageCompleteStatus(page))
                    }
                }.onError {
                    easyScanDao.failedPage(PageFailedStatus(page, it.message.orEmpty()))
                }
            }
        }

    private class OutputData(
        val image: PictureFile,
        val orientation: ScanOrientation,
        val estimatedSize: Long
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun lookupPageCompletes() = easyScanDao
        .queryPageCompletes()
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()
        .map { list ->
            list.map { pageComplete: PageComplete ->
                val state = pagePictureState(pageComplete.page.id, false)

                pageComplete.copy(picture = state?.picture)
            }.filter {
                it.picture != null
            }
        }
        .mapLatest { items ->
            items.associate { (page, _, completePicture) ->
                page.id to runCatching {
                    val picture = checkNotNull(completePicture) {
                        "Cannot load the pending picture."
                    }

                    val outputFile = PictureFile(picture, makeTmpImageFile {
                        ImageWriterPng(this).use { writer ->
                            writer.setupPaperSize(page)

                            picture.orientation = page.orientation
                            writer.write(picture)
                        }
                    })

                    OutputData(outputFile, page.orientation, outputFile.file.length())
                }
            }
        }.onEach { data ->
            withPageFileTransaction(data.keys, Page.Status.Complete) { page ->
                runCatching {
                    with(data.getValue(page.id).getOrThrow()) {
                        insertOutput(
                            Output(
                                page.id,
                                createPageFile(page, "output").withdrawPictureFile(image),
                                orientation,
                                estimatedSize
                            )
                        )

                        updatePageStatus(PageStatus(page, Page.Status.Output))
                    }
                }.onError {
                    easyScanDao.failedPage(PageFailedStatus(page, it.message.orEmpty()))
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun lookupRecognitionTasks() = easyScanDao
        .queryRecognitionTasks()
        .distinctUntilChanged { oldList, newList ->
            // Mean status Complete and Output as the same
            fun PageComplete.distinctStatus() =
                copy(page = page.copy(status = page.status.distinctAtLeast(Page.Status.Complete)))

            oldList.map {
                it.distinctStatus()
            } == newList.map {
                it.distinctStatus()
            }
        }
        .filter {
            // Don't cancel recognized task
            it.isNotEmpty()
        }
        .map { list ->
            list.map { pageComplete ->
                val state = pagePictureState(pageComplete.page.id, false, Page.Status.Complete)

                pageComplete.copy(picture = state?.picture)
            }.filter {
                it.picture != null
            }
        }
        .mapLatest { completeList ->
            coroutineScope {
                completeList.map { (page, _, completePicture) ->
                    val pageId = page.id
                    when (page.recognitionTask.job) {
                        // Simple cancel recognition
                        Page.RecognitionTask.Job.Cancel ->
                            RecognizedText.Cancelled(pageId)

                        // Clear recognition results
                        Page.RecognitionTask.Job.Clear ->
                            RecognizedText.Cleared(pageId)

                        // Change modified text
                        Page.RecognitionTask.Job.Modify ->
                            RecognizedText.Modified(
                                pageId,
                                checkNotNull(page.recognitionTask.modifiedText)
                            )

                        // Main recognition routine
                        Page.RecognitionTask.Job.Recognize -> {
                            val langStore = languageManager.languageStore.first()
                            val languages = page.recognitionTask.languages ?: langStore.languages

                            // Don't start recognition if no languages specified
                            if (languages.isEmpty()) {
                                RecognizedText.Cancelled(pageId)
                            } else {
                                val picture = checkNotNull(completePicture) {
                                    "Cannot load the complete picture to recognize it!"
                                }.withOrientation(page.orientation)

                                // Clear recognition state
                                easyScanDao.resetPageText(pageId)

                                // Recognize page. The reader wraps a full recognition
                                // engine in native memory - use{} releases it as soon
                                // as the page is read.
                                ScanReader(
                                    langStore.directory,
                                    languages,
                                    object : ScanReader.ProgressCallback {
                                        override fun onCancel(page: Int, words: Int) = !isActive

                                        override fun onProgress(
                                            page: Int,
                                            progress: Int,
                                            box: RectF?
                                        ) {
                                            runBlocking(coroutineContext) {
                                                easyScanDao.updateRecognition(
                                                    Recognition(pageId, progress, box)
                                                )
                                            }
                                        }
                                    },
                                ).use { reader ->
                                    picture.read(reader)
                                }
                                yield()

                                RecognizedText.Recognized(page.id, picture.scanText)
                            }
                        }
                    }
                }
            }

        }
        .onEach { recognizedTexts ->
            easyScanDao.updateTexts(recognizedTexts)
        }


    private fun lookupShareSessions() = easyScanDao
        .queryShareSessionItems()
        .distinctList()
        .untwistList()
        .map { sessionItems ->
            val title = "easyScan-${
                System.currentTimeMillis()
                    .toString(16)
                    .padStart(12, '0')
                    .uppercase()
            }"

            runCatching {
                if (sessionItems.items.size != easyScanDao.getShareItemCount(sessionItems.session.id)) {
                    // A page asked for is not among the ones that can be
                    // written - its image went missing, most likely between
                    // this export being asked for and it running. Exporting
                    // the rest would hand back a document quietly missing a
                    // page, so nothing is produced. The session still ends,
                    // which is what stops the dialog waiting.
                    return@runCatching ShareResult.Empty
                }

                when (sessionItems.session.type) {
                    ShareSession.Type.PNG -> {
                        val total = sessionItems.items.size
                        // Compute page count digits
                        val pageSuffixDigits = numDigits(total)
                        val outputs = sessionItems.items.map { state ->
                            // Append index for multiple files
                            val pageSuffix = if (total > 1)
                                "-${
                                    (state.item.shareOrder + 1).toString()
                                        .padStart(pageSuffixDigits, '0')
                                }"
                            else
                                ""

                            // Simple copy output file!
                            // No re-compression
                            val shareFile = makeShareFile("$title$pageSuffix.png")
                            val outputFile = getDataFile(state.output.outputFileId).buildFile(pagesDir)

                            try {
                                outputFile.copyTo(shareFile, true)
                            } catch (missingFile: FileNotFoundException) {
                                // Asking first and copying second would leave a
                                // gap for the file to vanish in between, so the
                                // copy itself is what reports it.
                                abortExportForMissingFile(state.page.id)
                            }

                            shareFile
                        }

                        ShareResult("image/png", outputs)
                    }

                    ShareSession.Type.TIFF -> {
                        makeShareFile("$title.tif").let { shareFile ->
                            ImageWriterTiff(shareFile).use { writer ->
                                sessionItems.items.forEach { state ->
                                    // Load picture from Complete; use{} releases its
                                    // native pixels as soon as the page is written
                                    (loadPicture(state.complete.completeImageFileId)
                                        ?: abortExportForMissingFile(state.page.id)).use { picture ->
                                        picture.orientation = state.page.orientation

                                        writer.setupPaperSize(state.page)
                                        writer.write(picture)
                                    }
                                }
                            }

                            ShareResult("image/tiff", listOf(shareFile))
                        }
                    }

                    ShareSession.Type.PDF -> {
                        // Get application settings
                        val settings = context
                            .appSettingsDataStore
                            .data.catch { emit(EasyScanSettings.AppSettingsSerializer.defaultValue) }
                            .first()

                        makeShareFile("$title.pdf").let { shareFile ->
                            ImageWriterPdf(shareFile).use { writer ->

                                // Setup global PDF options:
                                // Setup fonts by extracting its from assets
                                val fonts = setOf(
                                    "fonts/NotoSans-Regular.ttf",
                                    "fonts/NotoSansArabic-Regular.ttf",
                                    "fonts/NotoSansJP-Regular.ttf",
                                    "fonts/NotoSansKR-Regular.ttf",
                                    "fonts/NotoSansHebrew-Regular.ttf",
                                    "fonts/NotoSansDevanagari-Regular.ttf",
                                    "fonts/NotoSansSC-Regular.ttf",
                                    "fonts/NotoSansTC-Regular.ttf",
                                    "fonts/NotoSansMyanmar-Regular.ttf",
                                ).map { assetPath ->
                                    val name =
                                        if (BuildConfig.DEBUG) {
                                            // Extract file name
                                            val pattern = Regex("(?<=/)(.*?)(?=\\.)")
                                            pattern.findAll(assetPath).lastOrNull()?.let { result ->
                                                result.groupValues[1]
                                            }?.let {
                                                "$it-"
                                            }.orEmpty()
                                        }
                                        else ""

                                    File.createTempFile("px-${name}", ".ttf").apply {
                                        deleteOnExit()

                                        context.assets.open(assetPath).use { input ->
                                            outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                    }
                                }

                                writer.setFontFiles(fonts)

                                // Compression level for colored images
                                val imageCompression: ImageWriterPdf.ImageCompression = when (settings.imageCompression) {
                                    AppSettings.ImageCompression.LOSSLESS -> ImageWriterPdf.ImageCompression.Lossless
                                    AppSettings.ImageCompression.LOW -> ImageWriterPdf.ImageCompression.Low
                                    AppSettings.ImageCompression.MEDIUM -> ImageWriterPdf.ImageCompression.Medium
                                    AppSettings.ImageCompression.HIGH -> ImageWriterPdf.ImageCompression.High
                                    AppSettings.ImageCompression.EXTREME -> ImageWriterPdf.ImageCompression.Extreme
                                    else -> throw IllegalArgumentException("Unknown image compression ${settings.imageCompression}")
                                }
                                // The writer copies the compression setting, so the
                                // native compression object can be released right away
                                imageCompression.use { writer.setImageCompression(it) }

                                // Debug options to show hidden text only for DEBUG
                                writer.showHiddenText(settings.showPdfHiddenText and BuildConfig.DEBUG)

                                // Per-page processing
                                sessionItems.items.forEach { state ->

                                    // Setup per-page PDF options
                                    writer.setupPaperSize(state.page)

                                    val modifiedText =
                                        if (state.page.recognitionTask.isReady) {
                                            state.text?.modified
                                        } else {
                                            null
                                        }

                                    // Load a picture from Complete; use{} releases its
                                    // native pixels as soon as the page is written
                                    (loadPicture(state.complete.completeImageFileId)
                                        ?: abortExportForMissingFile(state.page.id)).use { picture ->
                                        // Setup the picture. The picture copies the
                                        // assigned text, so a freshly created empty
                                        // fallback is released right away; a recognized
                                        // text stays owned by its page state.
                                        picture.orientation = state.page.orientation
                                        if (modifiedText != null) {
                                            picture.scanText = modifiedText
                                        } else {
                                            ScanText().use { picture.scanText = it }
                                        }

                                        // Write the picture
                                        writer.write(picture)
                                    }
                                }
                            }

                            ShareResult("application/pdf", listOf(shareFile))
                        }
                    }

                    ShareSession.Type.Text -> {
                        makeShareFile("$title.txt").let { shareFile ->
                            shareFile.bufferedWriter().use { writer ->
                                sessionItems.items.forEach {state ->
                                    if (state.page.recognitionTask.isReady) {
                                        val text = state.text?.modified?.text.orEmpty()
                                        if (text.isNotEmpty()) {
                                            writer.write(text)

                                            // TODO: May be write page number???
                                            writer.newLine()
                                        }
                                    }
                                }
                            }
                            ShareResult("text/plaint", listOf(shareFile))
                        }
                    }
                }

            }.onSuccess { share ->
                if (share != ShareResult.Empty) {
                    shareResult.emit(share)
                }
            }
            easyScanDao.deleteShareSession(sessionItems.session.id)
        }

    private fun lookupOrphanShareSessions() = easyScanDao
        .queryOrphanShareSessions()
        .distinctUntilChanged()
        .filter { it / 10 > 0 }
        .onEach {
            easyScanDao.deleteOrphanShareSessions()
        }

    private val shareResult = MutableSharedFlow<ShareResult>()
    override fun queryShareResult(): Flow<ShareResult> =
        shareResult.asSharedFlow()


    private fun lookupUselessFiles() = easyScanDao
        .queryDataFiles()
        .map { dataFiles ->
            // Transform to path
            dataFiles.map { dataFile->
                dataFile.buildFile(pagesDir)
            }.toSet()
        }
        .onEach { usedFiles ->
            sweepUselessFiles(usedFiles)
        }

    /**
     * Delete every page file the store no longer references, then drop the
     * data-file rows that went with them.
     *
     * [usedFiles] is the set of referenced files carried by the database
     * emission that triggered this sweep.
     */
    internal suspend fun sweepUselessFiles(usedFiles: Set<File>) = withPageFiles {
        // [usedFiles] is a first filter, not the verdict. It was read when the
        // emission was produced, so a page saved since then is missing from it
        // through no fault of its own - and deleting a file a page still
        // points at is what leaves that page permanently unopenable. Anything
        // this set condemns is therefore confirmed against the store as it
        // stands now, read once and only if there is something to confirm.
        var referenced: Set<File>? = null

        pagesDir.walk().forEach { file ->
            when {
                file.isFile ->
                    if (!usedFiles.contains(file)) {
                        val current = referenced ?: easyScanDao.getDataFilePaths()
                            .mapTo(mutableSetOf()) { path -> File(pagesDir, path) }
                            .also { referenced = it }

                        if (!current.contains(file)) {
                            file.delete()
                        }
                    }

                file.isDirectory ->
                    if (file.listFiles().isNullOrEmpty()) {
                        file.delete()
                    }
            }
        }

        // Cleanup table
        easyScanDao.clearUselessFiles()
    }
    init {
        launch {
            coroutineScope {
                languageManager.requestServer("https://www.pixelnetica.com/products/OCR/dst/", 1)
                languageManager.ensureArchives(1)    // Delete old archives with other codes
            }

            // Start DB-routines after initialization of languages
            lookupInitialPages().launchIn(this)
            lookupPageInputs().launchIn(this)
            lookupPageOriginals().launchIn(this)
            lookupPendingPages().launchIn(this)
            lookupPageCompletes().launchIn(this)
            lookupRecognitionTasks().launchIn(this)
            lookupShareSessions().launchIn(this)
            lookupOrphanShareSessions().launchIn(this)
            lookupUselessFiles().launchIn(this)
        }
    }

    override val pageIds: Flow<List<Page.Id>>
        get() = easyScanDao.queryPageIds().distinctUntilChanged()

//    override fun queryPageStatus(pageId: Page.Id): Flow<PageStatus> =
//        easyScanDao.queryPageStatus(pageId).filterNotNull().distinctUntilChanged()

    override fun queryPageViewports(representative: String, preview: Boolean): Flow<List<PageViewport>> =
        easyScanDao
            .queryPageViewports(representative)
            .distinctUntilChanged()
            .map { list ->
                list.mapNotNull { (pageState, representation) ->
                    pagePictureState(pageState.page.id, preview)?.let { actualState ->
                        PageViewport(actualState, representation)
                    }
                }
            }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun queryPagePictureState(
        pageId: Page.Id,
        preview: Boolean,
        requiredStatus: Page.Status?
    ): Flow<PageState> = easyScanDao
        .queryPage(pageId)
        .distinctUntilChanged()
        .mapLatest {
            pagePictureState(pageId, preview, requiredStatus)
        }
        .filterNotNull()

    override fun insertPages(
        uriList: List<Uri>,
        startPageId: Page.Id,
        insertAfter: Boolean,
        callback: suspend EasyScanRepository.(List<Page.Id>) -> Unit
    ) {
        launch {
            // Load page settings
            val processing = context
                .userProcessingDataStore
                .data
                .catch { emit(EasyScanSettings.ProfileSerializer.defaultValue) }
                .first()

            val strongShadows = processing.strongShadows
            val profileType = when (processing.profile) {
                UserProcessing.ColorProfile.BITONAL -> RefineFeature.Profile.Type.Bitonal
                UserProcessing.ColorProfile.MONOCHROME -> RefineFeature.Profile.Type.Monochrome
                UserProcessing.ColorProfile.COLORED -> RefineFeature.Profile.Type.Colored
                else -> RefineFeature.Profile.Type.Original
            }

            // Read app settings
            val settings = context
                .appSettingsDataStore
                .data.catch { emit(EasyScanSettings.AppSettingsSerializer.defaultValue) }
                .first()

            // Load default paper size
            val paperProps = context
                .paperPropertiesDataStore
                .data
                .catch { emit(EasyScanSettings.PaperPropertiesSerializer.defaultValue) }
                .first()

            this@DefaultEasyScanRepository.callback(
                easyScanDao.insertPages(
                uriList,
                startPageId,
                true,
                ) { initialPage ->
                    // Setup defaults: Color processing, paper size, etc
                    initialPage.copy(
                        strongShadows = strongShadows,
                        profileType = profileType,
                        autoDetectOrientation = settings.autoDetectOrientation,
                        paper = Page.Paper.fromLong(paperProps.id),
                        paperOrientation = Page.Paper.Orientation.entries[paperProps.orientationValue]
                    )
                }
            )
        }
    }

    private suspend fun loadPicture(imageFileId: DataFile.Id) =
        loadPicture(checkNotNull(easyScanDao.getDataFile(imageFileId)) {
            "Cannot get data file for id $imageFileId"
        })

    override fun checkPage(pageId: Page.Id, checked: Boolean, representative: String) {
        launch {
            easyScanDao.checkPages(RepresentationChecked(pageId, representative, checked))
        }
    }

    override fun checkAllPages(checked: Boolean, representative: String) {
        launch {
            easyScanDao.checkAllPages(checked, representative)
        }
    }

    override fun deleteCheckedPages(representative: String) {
        launch {
            easyScanDao.deleteCheckedPages(representative)
        }
    }

    override fun movePage(idFrom: Page.Id, idTo: Page.Id) {
        launch {
            easyScanDao.movePage(idFrom, idTo)
        }
    }

    override fun reorderPages(pages: List<Page.Id>) {
        launch {
            easyScanDao.reorderPages(pages)
        }
    }

    override fun deletePages(vararg pageId: Page.Id) {
        launch {
            easyScanDao.deletePages(*pageId)
        }
    }

    override fun rotatePage(pageId: Page.Id, clockwise: Boolean) {
        launch {
            easyScanDao.withPages(setOf(pageId), Page.Status.Initial) { page ->
                if (page.orientation.isDefined()) {
                    val newOrientation =
                        if (clockwise) {
                            page.orientation.rotateCW()
                        } else {
                            page.orientation.rotateCCW()
                        }
                    setPageOrientation(page.id, newOrientation)
                }
            }
        }
    }

    override fun queryPageProfile(pageId: Page.Id): Flow<PageProfile?> {
        return easyScanDao.queryPageProfile(pageId).distinctUntilChanged()
    }

    override fun setPageShadows(pageId: Page.Id, shadows: Boolean) {
        launch {
            easyScanDao.setPageShadows(pageId, shadows)

            // Save to settings
            context.userProcessingDataStore.updateData {
                it.toBuilder().setStrongShadows(shadows).build()
            }
        }
    }

    override fun setPageProfile(pageId: Page.Id, profile: RefineFeature.Profile.Type) {
        launch {
            easyScanDao.setPageProfile(pageId, profile)

            // Save to settings
            context.userProcessingDataStore.updateData {
                it.toBuilder().setProfile(
                    when (profile) {
                        RefineFeature.Profile.Type.Original -> UserProcessing.ColorProfile.ORIGINAL
                        RefineFeature.Profile.Type.Bitonal -> UserProcessing.ColorProfile.BITONAL
                        RefineFeature.Profile.Type.Monochrome -> UserProcessing.ColorProfile.MONOCHROME
                        RefineFeature.Profile.Type.Colored -> UserProcessing.ColorProfile.COLORED
                    }
                ).build()
            }
        }
    }

    override fun setPageOrientation(pageId: Page.Id, orientation: ScanOrientation) {
        launch {
            easyScanDao.setPageOrientation(pageId, orientation)
        }
    }

    override fun setPageCutout(pageId: Page.Id, cutout: ScanCutout, orientation: ScanOrientation) {
        launch {
            easyScanDao.setPageCutout(pageId, cutout, orientation)
        }
    }

    override fun queryOriginalPicture(pageId: Page.Id): Flow<ScanPicture?> =
        easyScanDao
            .queryOriginalImage(pageId)
            .distinctUntilChanged()
            .map { imageSource ->
                imageSource?.let {
                    // This route reads the page's file directly rather than
                    // through the shared page-state resolver, so it has to
                    // recognise a missing file itself - otherwise the text
                    // screen would still die on a page every other screen
                    // already reports as failed.
                    val loaded = loadPicture(it.fileId)
                    if (loaded == null) {
                        failMissingFile(pageId)
                        return@let null
                    }

                    val oriented = loaded.withOrientation(it.orientation)
                    // withOrientation returns a new picture when the orientation
                    // differs; release the intermediate copy in that case
                    if (oriented !== loaded) {
                        loaded.close()
                    }
                    oriented
                }
            }

    override fun queryRecognitionLookup(pageId: Page.Id): Flow<RectF?> =
        easyScanDao.queryRecognitionLookup(pageId).distinctUntilChanged()

    override fun queryRecognitionProgress(pageId: Page.Id): Flow<Int> =
        easyScanDao.queryRecognitionProgress(pageId).map {
            // Mean null as -1
            it ?: -1
        }

    override fun queryOriginalText(pageId: Page.Id): Flow<ScanText> =
        easyScanDao
            .queryOriginalText(pageId)
            .map {
                it ?: ScanText()
            }

    override fun queryModifiedText(pageId: Page.Id): Flow<ScanText> =
        easyScanDao
            .queryModifiedText(pageId)
            .map {
                it ?: ScanText()
            }

    override fun queryRecognitionTask(pageId: Page.Id): Flow<Page.RecognitionTask> =
        easyScanDao
            .queryRecognitionTask(pageId)
            .mapNotNull {
                it?.task
            }

    override fun refreshRecognition(pageId: Page.Id, force: Boolean) {
        launch {
            easyScanDao.startRecognition(
                pageId,
                if (force)
                    Page.RecognitionTask.Job.Recognize
                else
                    Page.RecognitionTask.Job.Cancel,
            null,
            null,
            )
        }
    }

    override fun deleteRecognition(pageId: Page.Id) {
        launch {
            easyScanDao.startRecognition(
                pageId,
                Page.RecognitionTask.Job.Clear,
                null,
                null,
            )
        }
    }

    override fun updateModifiedText(pageId: Page.Id, text: ScanText) {
       launch {
           easyScanDao.startRecognition(
               pageId,
               Page.RecognitionTask.Job.Modify,
               null,
               text,
           )
       }
    }

    override fun queryPagesHaveText(pages: List<Page.Id>): Flow<Boolean> =
        easyScanDao.queryPagesHaveText(pages).distinctUntilChanged()

    override fun queryPagesUnavailable(pages: List<Page.Id>): Flow<Boolean> =
        easyScanDao.queryPagesUnavailable(pages).distinctUntilChanged()

    override fun createShareSession(type: ShareSession.Type, pages: List<Page.Id>) {
        launch {
            easyScanDao.createNewShareSession(type, *pages.toTypedArray())
        }
    }

    override fun queryHasShareSessions(): Flow<Boolean> =
        easyScanDao.queryHasShareSessions().map { (it ?: 0) > 0 }

    override fun queryPagePaperSize(pageId: Page.Id): Flow<Page.Paper.Predefined> =
            easyScanDao
                .queryPagePaperSize(pageId)
                .distinctUntilChanged()
                .filterIsInstance()

    override fun queryPageOrientation(pageId: Page.Id): Flow<Page.Paper.Orientation> =
        easyScanDao
            .queryPagePaperOrientation(pageId)
            .filterNotNull()
            .distinctUntilChanged()

    override fun setPagePaper(
        pageId: Page.Id,
        paper: Page.Paper,
        paperOrientation: Page.Paper.Orientation
    ) {
        launch {
            easyScanDao.updatePagePaper(
                PagePaper(
                    PageId(pageId),
                    paper,
                    paperOrientation,
                )
            )

            // Save defaults
            context.paperPropertiesDataStore.updateData {
                it.toBuilder()
                    .setId(paper.toLong())
                    .setOrientation(PaperProperties.Orientation.forNumber(paperOrientation.ordinal))
                    .build()
            }
        }
    }

    override fun ensurePageText(pageId: Page.Id, hasText: Boolean) {
        launch {
            easyScanDao.ensureRecognition(pageId, hasText)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun queryPagePreviews(pageIds: List<Page.Id>): Flow<List<EasyScanRepository.PageBitmap>> =
        easyScanDao
            .queryPages(pageIds)
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .mapLatest { list ->
                list.map { page ->
                    val pageStatus = pagePictureState(page.id, true)

                    if (pageStatus?.picture != null) {
                        val orientation = pageStatus.page.orientation
                        val bitmap = withContext(Dispatchers.IO) {
                            pageStatus.picture.createBitmap(
                                ScanPicture.DISPLAY_BITMAP or ScanPicture.FIT_TO_HARDWARE or ScanPicture.FIT_TO_TEXTURE,
                                orientation)
                        }

                        EasyScanRepository.PageBitmap(page.id, bitmap)
                    } else {
                        EasyScanRepository.PageBitmap(page.id, null)
                    }
                }

            }

    /**
     * Put paper configuration from [Page] to [ImageWriter]
     */
    private fun ImageWriter.setupPaperSize(page: Page) {
        when (page.paper) {
            is Page.Paper.Id ->
                throw IllegalStateException("Custom paper is not implemented yet")
            is Page.Paper.Predefined -> {
                // NOTE: We hope than Page.PaperSize.Predefined.Size
                // is equivalent with ImageWriter.Paper.Size!
                val paperSize = ImageWriter.Paper.Size.entries[page.paper.size.ordinal]

                // NOTE: We hope than Page.PaperSize.Orientation
                // is equivalent ImageWriter.Paper.Orientation!
                val paperOrientation = ImageWriter.Paper.Orientation.entries[page.paperOrientation.ordinal]

                setupPaperSize(paperSize, paperOrientation)
            }
        }
    }

    companion object: Tag by AppTagger("DefaultEasyScanRepository") {
        private var instances = 0

        private const val PAGES_DIR = "easyscan/pages"
        private const val SHARE_DIR = "easyscan/share"

        // TODO: get it from settings
        private val previewSize = Size(640, 640)

        /**
         * Compute number of integer digits
         * e.g.
         * @return 4 for 1234
         */
        private fun numDigits(number: Int): Int {
            var length = 0
            var temp = 1
            while (temp <= number) {
                length++
                temp *= 10
            }

            return length
        }
    }
}
