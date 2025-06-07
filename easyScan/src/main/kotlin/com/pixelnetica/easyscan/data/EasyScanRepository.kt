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
import com.pixelnetica.support.unplaitList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
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

    fun queryPageStatus(pageId: Page.Id): Flow<PageStatus>

    fun queryPageViewports(representative: String): Flow<List<PageViewport>>

    fun queryPageState(pageId: Page.Id): Flow<PageState>

    fun insertPages(
        uriList: List<Uri>,
        startPageId: Page.Id = Page.Id.Undefined,
        insertAfter: Boolean = true,
        callback: suspend EasyScanRepository.(List<Page.Id>) -> Unit = { }
    )

    fun loadPictureAsync(imageFileId: DataFile.Id): Deferred<ScanPicture>
    fun loadBitmapAsync(imageFileId: DataFile.Id, orientation: ScanOrientation): Deferred<Bitmap>

    fun checkPage(pageId: Page.Id, checked: Boolean, representative: String)

    fun checkAllPages(checked: Boolean, representative: String)

    fun deleteCheckedPages(representative: String)

    fun movePage(idFrom: Page.Id, idTo: Page.Id)

    fun reorderPages(pages: List<Page.Id>)

    fun getPagePictureAsync(pageState: PageState): Deferred<ScanPicture?>

    fun deletePages(vararg pageId: Page.Id)

    fun rotatePage(pageId: Page.Id, clockwise: Boolean)

    fun queryPageProfile(pageId: Page.Id): Flow<PageProfile?>

    fun setPageShadows(pageId: Page.Id, shadows: Boolean)

    fun setPageProfile(pageId: Page.Id, profile: RefineFeature.Profile.Type)

    fun getInputCutoutAsync(pageId: Page.Id, expand: Boolean = false): Deferred<ScanCutout?>

    fun setPageCutout(pageId: Page.Id, cutout: ScanCutout)

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
        log.d("Repository instance $instances")

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

    // Store existing picture
    private val pictureCache = WeakHashMap<DataFile, ScanPicture>()

    private fun DataFile.withdrawPictureFile(pictureFile: PictureFile) =
        withdraw(pictureFile.file).apply {
            pictureCache.insert(this) {
                pictureFile.picture
            }
        }.id

    private suspend fun DataFile.loadPicture(): ScanPicture {
        val picture = pictureCache.cache(this) {
            withContext(Dispatchers.IO) {
                FileInputStream(buildFile(pagesDir)).use { stream ->
                    ScanPicture.load(stream)
                }
            }
        }

        // Return a copy to process
        return ScanPicture(picture)
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
                    // Load picture from specified URI
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
            easyScanDao.withPages(data.keys) { page ->
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
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()
        .mapLatest { items ->
            // Prepare ScanDetector for each set of images
            val languageManager = LanguageManager.getInstance(context = context)
            val orientationDetector = languageManager.detectorPath.first()?.let {
                ScanDetector(it)
            }


            items.associate { (page, input) ->
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

                    val picture = checkNotNull(easyScanDao.getDataFile(input.inputImageFileId)) {
                        "No data file for id ${input.inputImageFileId}"
                    }.loadPicture()

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
        .onEach { data ->
            // Simple setup cutout
            // There isn't any processing
            easyScanDao.withPages(data.keys) { page ->
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
        .distinctUntilChanged()
        .onEach { items ->
            log.d("Process ${items.size} originals")
            items.associateBy { item -> item.page.id }.apply {
                easyScanDao.withPages(keys) { page ->
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
        .distinctUntilChanged()
        .mapLatest { items ->
            log.d("Process ${items.size} pending pages")
            items.associate { (page, original) ->
                log.d("Associate id=${page.id.id}")
                page.id to runCatching {
                    val picture =
                        checkNotNull(easyScanDao.getDataFile(original.originalImageFileId)) {
                            "No data file for id ${original.originalImageFileId}"
                        }.loadPicture()

                    log.d("Refine page id=${page.id.id} to ${page.profileType.name}")
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
            easyScanDao.withPages(data.keys) { page ->
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
        .mapLatest { items ->
            log.d("Process ${items.size} completes")
            items.associate { (page, complete) ->
                page.id to runCatching {
                    val picture =
                        checkNotNull(easyScanDao.getDataFile(complete.completeImageFileId)) {
                            "No data file for id ${complete.completeImageFileId}"
                        }.loadPicture()

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
            easyScanDao.withPages(data.keys) { page ->
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
        .mapLatest { completeList ->
            coroutineScope {
                completeList.map { (page, complete) ->
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
                                val picture = checkNotNull(
                                    easyScanDao.getDataFile(complete.completeImageFileId)
                                ) {
                                    "No data file for id ${complete.completeImageFileId}"
                                }.loadPicture().withOrientation(page.orientation)

                                // Clear recognition state
                                easyScanDao.resetPageText(pageId)

                                // Recognize page
                                val reader = ScanReader(
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
                                )

                                picture.read(reader)
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
        .unplaitList()
        .map { sessionItems ->
            val title = "easyScan-${
                System.currentTimeMillis()
                    .toString(16)
                    .padStart(12, '0')
                    .uppercase()
            }"

            runCatching {
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
                            val outputFile = checkNotNull(
                                easyScanDao.getDataFile(state.output.outputFileId)
                            ) {
                                "Missing output file for id=${state.output.outputFileId}"
                            }.buildFile(pagesDir)

                            outputFile.copyTo(shareFile, true)

                            shareFile
                        }

                        ShareResult("image/png", outputs)
                    }

                    ShareSession.Type.TIFF -> {
                        makeShareFile("$title.tif").let { shareFile ->
                            ImageWriterTiff(shareFile).use { writer ->
                                sessionItems.items.forEach { state ->
                                    // Load picture from Complete
                                    val picture = loadPicture(state.complete.completeImageFileId)
                                    picture.orientation = state.page.orientation

                                    writer.setupPaperSize(state.page)
                                    writer.write(picture)
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
                                writer.setImageCompression(imageCompression)

                                // Debug options to show hidden text only for DEBUG
                                writer.showHiddenText(settings.showPdfHiddenText and BuildConfig.DEBUG)

                                // Per-page processing
                                sessionItems.items.forEach { state ->

                                    // Setup per-page PDF options
                                    writer.setupPaperSize(state.page)

                                    val text =
                                        if (state.page.recognitionTask.isReady) {
                                            state.text?.modified ?: ScanText()
                                        } else {
                                            ScanText()
                                        }

                                    // Load a picture from Complete
                                    val picture: ScanPicture = loadPicture(state.complete.completeImageFileId)

                                    // Setup the picture
                                    picture.orientation = state.page.orientation
                                    picture.scanText = text

                                    // Write the picture
                                    writer.write(picture);
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
                                        val text = state.text?.modified?.toString().orEmpty()
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
            // Collect all existing files
            pagesDir.walk().forEach { file ->
                when {
                    file.isFile ->
                        if (!usedFiles.contains(file)) {
                            log.d("Delete file $file")
                            file.delete()
                        }

                    file.isDirectory ->
                        if (file.listFiles().isNullOrEmpty()) {
                            log.d("Delete empty directory $file")
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

    override fun queryPageStatus(pageId: Page.Id): Flow<PageStatus> =
        easyScanDao.queryPageStatus(pageId).filterNotNull().distinctUntilChanged()

    override fun queryPageViewports(representative: String): Flow<List<PageViewport>> =
        easyScanDao.queryPageViewports(representative).distinctUntilChanged()

    override fun queryPageState(pageId: Page.Id): Flow<PageState> =
        easyScanDao.queryPageState(pageId).filterNotNull().distinctUntilChanged()

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
        checkNotNull(easyScanDao.getDataFile(imageFileId)) {
            "Cannot get data file for id $imageFileId"
        }.loadPicture()

    override fun loadPictureAsync(imageFileId: DataFile.Id): Deferred<ScanPicture> = async {
        loadPicture(imageFileId)
    }

    override fun loadBitmapAsync(imageFileId: DataFile.Id, orientation: ScanOrientation) = async {
        loadPicture(imageFileId).createBitmap(ScanPicture.DISPLAY_BITMAP, orientation)
    }

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

    override fun getPagePictureAsync(pageState: PageState): Deferred<ScanPicture?> = async {
        with(pageState) {
            when (page.status) {
                Page.Status.Invalid, Page.Status.Initial -> null
                Page.Status.Input -> input?.inputImageFileId
                Page.Status.Original -> original?.originalImageFileId
                Page.Status.Pending -> pending?.pendingImageFileId
                Page.Status.Complete -> complete?.completeImageFileId
                // NOTE: Using image from Complete
                Page.Status.Output -> complete?.completeImageFileId
            }?.let { dataFileId: DataFile.Id ->
                easyScanDao.getDataFile(dataFileId)
            }?.let { dataFile: DataFile ->
                dataFile.loadPicture()
            }
        }
    }

    override fun deletePages(vararg pageId: Page.Id) {
        launch {
            easyScanDao.deletePages(*pageId)
        }
    }

    override fun rotatePage(pageId: Page.Id, clockwise: Boolean) {
        launch {
            easyScanDao.withPages(setOf(pageId)) { page ->
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

    override fun getInputCutoutAsync(pageId: Page.Id, expand: Boolean): Deferred<ScanCutout?> = async {
        easyScanDao.getInputCutout(pageId)?.apply {
            if (expand) {
                expand()
            }
        }
    }

    override fun setPageCutout(pageId: Page.Id, cutout: ScanCutout) {
        launch {
            easyScanDao.setPageCutout(pageId, cutout)
        }
    }

    override fun queryOriginalPicture(pageId: Page.Id): Flow<ScanPicture?> =
        easyScanDao
            .queryOriginalImage(pageId)
            .distinctUntilChanged()
            .map { imageSource ->
                imageSource?.let {
                    loadPictureAsync(it.fileId).await().withOrientation(it.orientation)
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
            .queryPageStates(pageIds)
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .mapLatest { states ->
                states.map { state ->
                    // Select
                    val (fileId, orientation) = when (state.page.status) {
                        Page.Status.Input -> Pair(
                            state.input?.inputPreviewFileId,
                            state.page.orientation
                        )

                        Page.Status.Original -> Pair(
                            state.original?.originalPreviewFileId,
                            state.page.orientation
                        )

                        Page.Status.Pending -> Pair(
                            state.pending?.pendingPreviewFileId,
                            state.page.orientation
                        )

                        Page.Status.Complete, Page.Status.Output -> Pair(
                            state.complete?.completePreviewFileId,
                            state.page.orientation
                        )
                        else -> Pair(null, null)
                    }

                    if (fileId != null && orientation != null) {
                        EasyScanRepository.PageBitmap(state.page.id, loadBitmapAsync(fileId, orientation).await())
                    } else {
                        EasyScanRepository.PageBitmap(state.page.id, null)
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
                val paperSize = ImageWriter.Paper.Size.values()[page.paper.size.ordinal]

                // NOTE: We hope than Page.PaperSize.Orientation
                // is equivalent ImageWriter.Paper.Orientation!
                val paperOrientation = ImageWriter.Paper.Orientation.values()[page.paperOrientation.ordinal]

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

