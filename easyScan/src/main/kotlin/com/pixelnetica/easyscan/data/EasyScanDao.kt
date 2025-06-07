package com.pixelnetica.easyscan.data

import android.graphics.RectF
import android.net.Uri
import androidx.room.*
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.scanning.RefineFeature
import com.pixelnetica.scanning.ScanCutout
import com.pixelnetica.scanning.ScanOrientation
import com.pixelnetica.scanning.ScanText
import com.pixelnetica.support.Tag
import kotlinx.coroutines.flow.Flow

// Partial page id. Don't confuse with Page.Id!
data class PageId(
    @Embedded(prefix = "page")
    val id: Page.Id,
) {
    // Simple access
    constructor(page: Page): this(page.id)
    val pageId get() = id.id
}


data class PageStatus(
    @Embedded
    val id: PageId,
    val status: Page.Status,
) {
    constructor(page: Page, status: Page.Status): this(PageId(page), status)
}

data class PageFailedStatus(
    @Embedded
    val pageStatus: PageStatus,
    val errorMessage: String
) {
    constructor(page: Page, errorMessage: String): this(PageStatus(page, Page.Status.Invalid), errorMessage)
}

data class PageInputStatus(
    @Embedded
    val pageStatus: PageStatus,
    val orientation: ScanOrientation,
    val initialUri: Uri?,
) {
    constructor(page: Page, orientation: ScanOrientation): this(
        PageStatus(page, Page.Status.Input),
        orientation,
        null,
    )
}

data class PageOriginalStatus(
    @Embedded
    val pageStatus: PageStatus,
    val cutout: ScanCutout,
    val undefinedCutout: Boolean,
    val resetCutout: Page.ResetCutout,
    val recognitionStatus: Int
    ) {
    constructor(
        page: Page,
        cutout: ScanCutout,
        undefinedCutout: Boolean,
    ) : this(
        PageStatus(page, Page.Status.Original),
        cutout,
        undefinedCutout,
        Page.ResetCutout.Reset,
        Page.RecognitionTask.Nothing,   // Clear recognition after each crop!
    )
}

data class PageCompleteStatus(
    @Embedded
    val pageStatus: PageStatus,
    val recognitionStatus: Int
) {
    constructor(page: Page): this(
        PageStatus(page, Page.Status.Complete),
        Page.RecognitionTask.Nothing,   // Clear recognition results after each processing
    )
}

data class PageInput(
    @Embedded
    val page: Page,
    @Relation(
        parentColumn = "pageId",
        entityColumn = "inputId",
    )
    val input: Input,
)

data class PageOriginal(
    @Embedded
    val page: Page,
    @Relation(
        parentColumn = "pageId",
        entityColumn = "originalId",
    )
    val original: Original,
)

data class PagePending(
    @Embedded
    val page: Page,
    @Relation(
        parentColumn = "pageId",
        entityColumn = "originalId",
    )
    val original: Original,
    @Relation(
        parentColumn = "pageId",
        entityColumn = "pendingId",
    )
    val pending: Pending?,  // nullable for compatibility with previous database
)

data class PageComplete(
    @Embedded
    val page: Page,
    @Relation(
        parentColumn = "pageId",
        entityColumn = "completeId",
    )
    val complete: Complete,
)
data class RepresentationChecked(
    val representationId: Page.Id,
    val representative: String,
    val checked: Boolean,
)

data class PageOrder(
    @Embedded
    val id: PageId,
    val orderIndex: Int,
) {
    constructor(pageId: Page.Id, orderIndex: Int): this(PageId(pageId), orderIndex)
    constructor(page: Page, orderIndex: Int): this(PageId(page), orderIndex)
    val pageId get () = id.id
}

data class PageOrientation(
    @Embedded
    val id: PageId,
    val orientation: ScanOrientation,
) {
    constructor(pageId: Page.Id, orientation: ScanOrientation): this(PageId(pageId), orientation)
}

data class DetectedOrientation(
    @Embedded
    val id: PageId,
    val orientation: ScanOrientation,
    val autoDetectOrientation: Boolean,
) {
    constructor(pageId: Page.Id, orientation: ScanOrientation): this(PageId(pageId), orientation, false)
}

data class PageProfile(
    @Embedded
    val id: PageId,
    val strongShadows: Boolean,
    val profileType: RefineFeature.Profile.Type
)

data class PageStrongShadows(
    @Embedded
    val id: PageId,
    val strongShadows: Boolean,
) {
    constructor(pageId: Page.Id, strongShadows: Boolean):
            this(PageId(pageId), strongShadows)
}

data class PageProfileType(
    @Embedded
    val id: PageId,
    val profileType: RefineFeature.Profile.Type,
) {
    constructor(pageId: Page.Id, profileType: RefineFeature.Profile.Type):
            this(PageId(pageId), profileType)
}

data class PageCutout(
    @Embedded
    val id: PageId,
    val resetCutout: Page.ResetCutout,
    val cutout: ScanCutout?,
) {
    constructor(pageId: Page.Id, resetCutout: Page.ResetCutout, cutout: ScanCutout?):
            this(PageId(pageId), resetCutout, cutout)
}

data class PagePaper(
    @Embedded
    val id: PageId,
    @ColumnInfo(name = "paperSizeId")
    val paper: Page.Paper,
    val paperOrientation: Page.Paper.Orientation,
)

/**
 * List files
 */
@DatabaseView(
    """
        SELECT fileId FROM DataFile WHERE fileId NOT IN
        (
            SELECT inputImageFileId FROM Input UNION
            SELECT inputPreviewFileId FROM Input UNION
            SELECT originalImageFileId FROM Original UNION
            SELECT originalPreviewFileId FROM Original UNION
            SELECT pendingImageFileId FROM Pending UNION
            SELECT pendingPreviewFileId FROM Pending UNION
            SELECT completeImageFileId FROM Complete UNION
            SELECT completePreviewFileId FROM Complete UNION
            SELECT outputFileId FROM Output
        )
    """
)
data class UselessFile(
    @Embedded(prefix = "file")
    val id: DataFile.Id,
)

@DatabaseView(
    """
    SELECT * FROM Page 
        LEFT JOIN Input ON pageId=inputId 
        LEFT JOIN Original ON pageId=originalId
        LEFT JOIN Pending ON pageId=pendingId
        LEFT JOIN Complete ON pageId=completeId
        LEFT JOIN Output ON pageId=outputId
        LEFT JOIN Recognition ON pageId=recognitionId
    """
)
data class PageState(
    @Embedded
    val page: Page,
    @Embedded
    val input: Input?,
    @Embedded
    val original: Original?,
    @Embedded
    val pending: Pending?,
    @Embedded
    val complete: Complete?,
    @Embedded
    val output: Output?,
    @Embedded
    val recognition: Recognition?,
)

/**
 * Page State plus optional Representative
 */
data class PageViewport(
    @Embedded
    val state: PageState,
    @Embedded
    val representation: Representation?
)

/**
 * Partial update recognised text
 */
sealed class RecognizedText(
    @Embedded(prefix = "text")
    val id: Page.Id) {

    // Cancel recognition
    class Cancelled(id: Page.Id): RecognizedText(id)

    class Cleared(id: Page.Id): RecognizedText(id)

    // Update only modified text
    class Modified(
        id: Page.Id,
        val modified: ScanText,
    ): RecognizedText(id)

    // Update both original and modified
    class Recognized(
        id: Page.Id,
        val original: ScanText,
        val modified: ScanText,
    ): RecognizedText(id) {
        constructor(id: Page.Id, text: ScanText): this(id, text, text)
    }

    object Undefined: RecognizedText(Page.Id.Undefined)
}

class PageRecognitionTask(
    @Embedded
    val id: PageId,
    @Embedded(prefix = "recognition")
    val task: Page.RecognitionTask
) {
    constructor(pageId: Page.Id, task: Page.RecognitionTask): this(PageId(pageId), task)
}

/**
 * To get image file with specified orientation
 */
data class ImageSource(
    @Embedded(prefix = "file")
    val fileId: DataFile.Id,
    val orientation: ScanOrientation,
)

data class ShareSessionId(
    @Embedded(prefix = "shareSession")
    val id: ShareSession.Id
) {
    constructor(session: ShareSession): this(session.id)
}

@DatabaseView(
    """
        SELECT * 
            FROM ShareItem
            INNER JOIN Page ON ShareItem.sharePageId = Page.pageId
            INNER JOIN Complete ON ShareItem.sharePageId = Complete.completeId
            INNER JOIN Output ON ShareItem.sharePageId = Output.outputId
            LEFT JOIN Text ON ShareItem.sharePageId = Text.textId
        WHERE Page.status = 'Output' AND Page.recognitionStatus < 2
    """
)
data class ShareItemState(
    @Embedded
    val item: ShareItem,

    @Embedded
    val page: Page,

    @Embedded
    val complete: Complete,

    @Embedded
    val output: Output,

    @Embedded
    val text: Text?,
)

@DatabaseView(
    """
        SELECT shareSessionId AS pendingShareSessionId, COUNT(pageId) AS pendingCount
        FROM ShareSession
            INNER JOIN (SELECT shareItemId, sharePageId FROM ShareItem) ON shareSessionId = shareItemId
            LEFT JOIN (SELECT pageId, status, recognitionStatus FROM Page) ON sharePageId = pageId
                AND status != 'Output' OR recognitionStatus > 1
        GROUP BY shareSessionId
        """
)
data class PendingShares(
    @Embedded("pendingShareSession")
    val id: ShareSession.Id,

    val pendingCount: Int,
)

data class ShareSessionItems(
    @Embedded
    val session: ShareSession,

    @Relation(
        entity = ShareItemState::class,
        parentColumn = "shareSessionId",
        entityColumn = "shareItemId",
    )
    val items: List<ShareItemState>
)
@Dao
interface EasyScanDao {

    // Working with data files
    @Insert
    suspend fun insertDataFile(dataFile: DataFile): Long

    @Update
    suspend fun updateDataFile(dataFile: DataFile)

    @Transaction
    suspend fun createDataFile(block: suspend (fileId: DataFile.Id) -> DataFile): DataFile {
        // Insert empty entry
        val fileId = DataFile.Id(insertDataFile(DataFile()))
        val dataFile = block(fileId).copy(id = fileId)
        updateDataFile(dataFile)
        return dataFile
    }

    @Query(
        """
        SELECT * FROM DataFile WHERE fileId=:fileId
        """
    )
    suspend fun getDataFile(fileId: DataFile.Id): DataFile?

    @Query(
        """
            DELETE FROM DataFile WHERE fileId IN (SELECT fileId FROM UselessFile)
        """
    )
    suspend fun clearUselessFiles()

    @Query(
        """
            SELECT * FROM DataFile
        """
    )
    fun queryDataFiles(): Flow<List<DataFile>>


    @Insert
    suspend fun insertPage(page: Page): Long

    @Update
    suspend fun updatePage(page: Page)

    @Update(entity = Page::class)
    suspend fun updatePageStatus(pageStatus: PageStatus)

    @Update(entity = Page::class)
    suspend fun updatePageInput(item: PageInputStatus)

    @Update(entity = Page::class)
    suspend fun updatePageOriginal(item: PageOriginalStatus)

    @Update(entity = Page::class)
    suspend fun updatePageComplete(item: PageCompleteStatus)

    @Update(entity = Page::class)
    suspend fun failedPage(pageFailedStatus: PageFailedStatus)

    @Update(entity = Page::class)
    suspend fun updatePageOrientation(pageOrientation: PageOrientation)

    @Update(entity = Page::class)
    suspend fun updatePageDetectedOrientation(detectedOrientation: DetectedOrientation)

    @Transaction
    suspend fun setPageOrientation(pageId: Page.Id, orientation: ScanOrientation) {
        updatePageOrientation(PageOrientation(pageId, orientation))
        // Rebuild output when orientation is changes
        ensurePageStatus(pageId, Page.Status.Complete)
    }

    @Query(
        """
            SELECT COUNT(pageId) FROM Page
        """
    )
    fun queryPageCount(): Flow<Int>

    @Query(
        """
            SELECT pageId FROM Page ORDER BY orderIndex
        """
    )
    fun queryPageIds(): Flow<List<Page.Id>>

    @Query(
        """
            SELECT pageId, status FROM Page WHERE pageId = :pageId LIMIT 1
        """
    )
    fun queryPageStatus(pageId: Page.Id): Flow<PageStatus?>

    @Query(
        """
            SELECT * FROM PageState 
            LEFT JOIN Representation ON pageId = representationId AND representative = :representative
            ORDER BY orderIndex
        """
    )
    fun queryPageViewports(representative: String): Flow<List<PageViewport>>

    @Query(
        """
            SELECT * FROM PageState
            WHERE pageId = :pageId
            LIMIT 1
        """
    )

    fun queryPageState(pageId: Page.Id): Flow<PageState?>
    @Query(
        """
            SELECT * FROM PageState
            WHERE pageId in (:pageIds)
        """
    )
    fun queryPageStates(pageIds: List<Page.Id>): Flow<List<PageState>>

    @Query(
        """
        SELECT * FROM Page WHERE status='Initial'
        """
    )
    fun queryInitialPages(): Flow<List<Page>>

    @Transaction
    @Query(
        """
        SELECT * FROM Page WHERE status='Input'
        """
    )
    fun queryPageInputs(): Flow<List<PageInput>>

    @Transaction
    @Query(
        """
            SELECT * FROM Page WHERE status='Original'
        """
    )
    fun queryPageOriginals(): Flow<List<PageOriginal>>

    @Transaction
    @Query(
        """
            SELECT * FROM Page WHERE status='Pending'
        """
    )
    fun queryPendingPages(): Flow<List<PagePending>>

    @Transaction
    @Query(
        """
            SELECT * FROM Page WHERE status='Complete'
        """
    )
    fun queryPageCompletes(): Flow<List<PageComplete>>

    @Transaction
    @Query(
        """
            SELECT * FROM Page WHERE status IN ('Complete', 'Output') AND recognitionStatus > 1
        """
    )
    fun queryRecognitionTasks(): Flow<List<PageComplete>>

    @Query(
        """
        SELECT * from Page WHERE pageId=:pageId
        """
    )
    suspend fun getPage(pageId: Page.Id): Page?

    @Query(
        """
            SELECT pageId FROM Page
        """
    )
    suspend fun getAllPages(): List<PageId>

    @Query(
        """
            SELECT pageId, orderIndex FROM Page ORDER BY orderIndex
        """
    )
    suspend fun getPageOrders(): List<PageOrder>

    @Transaction
    suspend fun withPages(pageIds: Set<Page.Id>, block: suspend EasyScanDao.(Page) -> Unit) {
        pageIds.mapNotNull { getPage(it) }.forEach {
            this@EasyScanDao.block(it)
        }
    }

    @Upsert
    suspend fun insertInput(input: Input)

    @Upsert
    suspend fun insertOriginal(original: Original)

    @Upsert
    suspend fun insertPending(pending: Pending)

    @Upsert
    suspend fun insertComplete(complete: Complete)

    @Upsert
    suspend fun insertOutput(output: Output)

    @Query(
        """
            SELECT COALESCE(MAX(orderIndex), -1) FROM Page
        """
    )
    suspend fun getTopOrder(): Int

    @Query(
        """
            SELECT orderIndex FROM Page WHERE pageId = :pageId
        """
    )
    suspend fun getPageOrder(pageId: Page.Id): Int

    @Query(
        """
            UPDATE Page SET orderIndex = orderIndex + :count WHERE orderIndex >= :start
        """
    )
    suspend fun offsetPageOrder(start: Int, count: Int)

    @Update(entity = Page::class)
    suspend fun setPageOrder(order: PageOrder)

    @Transaction
    suspend fun insertPages(
        uriList: List<Uri>,
        startPageId: Page.Id,
        insertAfter: Boolean,
        transform: suspend (page: Page) -> Page = { it }
    ): List<Page.Id> {
        val start = when (startPageId) {
            Page.Id.Undefined ->
                if (insertAfter) {
                    // Insert to tail
                    getTopOrder() + 1
                } else {
                    // insert to head
                    0
                }

            else -> getPageOrder(startPageId).let {
                if (insertAfter) {
                    it + 1
                } else {
                    it
                }
            }
        }

        // Reorder all next pages
        offsetPageOrder(start, uriList.size)

        return uriList.mapIndexed { index, uri ->
            Page.Id(
                insertPage(
                    transform(
                        Page(
                            Page.Id.Initial,
                            Page.Status.Initial,
                            uri,
                            start + index,
                        )
                    )
                )
            )
        }
    }

    @Transaction
    suspend fun movePage(idFrom: Page.Id, idTo: Page.Id) {
        // Must be equal
        // list.add(indexTo, list.removeAt(indexFrom))
        val orderTo = getPageOrder(idTo)
        offsetPageOrder(orderTo, 1)

        setPageOrder(PageOrder(idFrom, orderTo))
        reorderPages()
    }

    @Transaction
    suspend fun reorderPages() {
        // Skip page index holes
        reorderPages(getPageOrders().map { it.pageId })
    }

    @Transaction
    suspend fun reorderPages(pages: List<Page.Id>) {
        pages.forEachIndexed { index, id ->
            setPageOrder(PageOrder(id, index))
        }
    }

    @Upsert(entity = Representation::class)
    suspend fun checkPages(vararg checked: RepresentationChecked)

    @Transaction
    suspend fun checkAllPages(checked: Boolean, representative: String) {
        val checkedPages = getAllPages().map {
            RepresentationChecked(it.id, representative, checked)
        }
        checkPages(*checkedPages.toTypedArray())
    }

    @Query(
        """
            DELETE FROM Page WHERE pageId IN 
            (SELECT representationId FROM Representation WHERE checked != 0 AND representative=:representative)
        """
    )
    suspend fun deleteCheckedPagesInternal(representative: String)

    @Transaction
    suspend fun deleteCheckedPages(representative: String) {
        deleteCheckedPagesInternal(representative)
        reorderPages()
    }

    @Delete(entity = Page::class)
    suspend fun deletePage(pageId: PageId)

    @Transaction
    suspend fun deletePages(vararg pageIds: Page.Id) {
        pageIds.forEach {
            deletePage(PageId(it))
        }

        reorderPages()
    }

    @Query(
        """
            SELECT pageId, strongShadows, profileType FROM Page WHERE pageId = :pageId LIMIT 1
        """
    )
    fun queryPageProfile(pageId: Page.Id): Flow<PageProfile?>

    @Update(entity = Page::class)
    fun setPageShadows(pageStrongShadows: PageStrongShadows)

    @Update(entity = Page::class)
    fun setPageProfile(pageProfileType: PageProfileType)

    /**
     * Change page status to lower than current.
     */
    @Query(
        """
            UPDATE Page SET status = :requiredStatus
            WHERE pageId = :pageId AND status IN (:expectedStatus)
        """
    )
    suspend fun ensurePageStatus(pageId: Page.Id, requiredStatus: Page.Status, expectedStatus: Set<Page.Status>)

    suspend fun ensurePageStatus(pageId: Page.Id, status: Page.Status) =
        ensurePageStatus(pageId, status, status.expected())

    @Transaction
    suspend fun setPageShadows(pageId: Page.Id, shadows: Boolean) {
        setPageShadows(PageStrongShadows(pageId, shadows))
        ensurePageStatus(pageId, Page.Status.Pending)
    }

    @Transaction
    suspend fun setPageProfile(pageId: Page.Id, profileType: RefineFeature.Profile.Type) {
        setPageProfile(PageProfileType(pageId, profileType))
        ensurePageStatus(pageId, Page.Status.Pending)
    }

    @Query(
        """
            SELECT inputCutout FROM Input WHERE inputId = :pageId
        """
    )
    suspend fun getInputCutout(pageId: Page.Id): ScanCutout?

    @Update(entity = Page::class)
    suspend fun updatePageCutout(pageCutout: PageCutout)

    @Transaction
    suspend fun setPageCutout(pageId: Page.Id, cutout: ScanCutout) {
        updatePageCutout(PageCutout(pageId, Page.ResetCutout.Setup, cutout))
        ensurePageStatus(pageId, Page.Status.Input)
    }

    @Query(
        """
            SELECT * FROM Text WHERE textId = :pageId
        """
    )
    suspend fun getPageText(pageId: Page.Id): Text?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryInsertText(text: Text)

    // Update only modified text
    @Update(entity = Text::class)
    suspend fun updateText(modified: RecognizedText.Modified)

    // Update both original and modified
    @Update(entity = Text::class)
    suspend fun updateText(recognized: RecognizedText.Recognized)

    @Upsert
    suspend fun updateRecognition(progress: Recognition)

    @Update(entity = Page::class)
    suspend fun updatePageRecognitionTask(pageRecognitionTask: PageRecognitionTask)

    @Transaction
    suspend fun resetPageText(pageId: Page.Id) {
        // Keep existing text
        tryInsertText(Text(pageId, ScanText(), ScanText()))

        // Cleanup progress
        updateRecognition(Recognition(pageId))
    }
    @Transaction
    suspend fun updateTexts(list: List<RecognizedText>) = list.forEach { text ->
        // Select between ready and nothing
        val ready = when (text) {
            is RecognizedText.Cancelled -> {
                // Keep previous recognized text
                getPageText(text.id)?.original?.isTextReady == true
            }

            is RecognizedText.Cleared -> {
                updateText(RecognizedText.Recognized(text.id, ScanText(), ScanText()))
                false
            }

            is RecognizedText.Modified -> {
                updateText(text)
                text.modified.isReady
            }

            is RecognizedText.Recognized -> {
                updateText(text)
                text.original.isReady
            }

            is RecognizedText.Undefined -> false
        }

        // Update recognition task as ready or nothing
        updatePageRecognitionTask(
            PageRecognitionTask(
                text.id,
                Page.RecognitionTask(ready)
            )
        )

        // Cleanup progress
        updateRecognition(Recognition(text.id))
    }

    @Query(
        """
            SELECT originalImageFileId as fileId, orientation 
                FROM Page INNER JOIN Original ON pageId = originalId 
                WHERE pageId = :pageId AND status IN (:availableStatus)
                LIMIT 1
        """
    )
    fun queryOriginalImage(pageId: Page.Id, availableStatus: Set<Page.Status>): Flow<ImageSource?>

    fun queryOriginalImage(pageId: Page.Id) =
        queryOriginalImage(pageId, Page.Status.Original.available())

    @Query(
        """
            SELECT lookup FROM Recognition WHERE recognitionId = :pageId LIMIT 1
        """
    )
    fun queryRecognitionLookup(pageId: Page.Id): Flow<RectF?>

    @Query(
        """
            SELECT progress FROM Recognition WHERE recognitionId = :pageId LIMIT 1
        """
    )
    fun queryRecognitionProgress(pageId: Page.Id): Flow<Int?>

    @Query(
        """
            SELECT Text.original 
                FROM Page INNER JOIN Text ON pageId = textId 
                AND textId = :pageId AND recognitionStatus = 1
            LIMIT 1
        """
    )
    fun queryOriginalText(pageId: Page.Id): Flow<ScanText?>

    @Query(
        """
            SELECT Text.modified 
                FROM Page INNER JOIN Text ON pageId = textId 
                AND textId = :pageId AND recognitionStatus = 1
            LIMIT 1
        """
    )
    fun queryModifiedText(pageId: Page.Id): Flow<ScanText?>

    @Query(
        """
            SELECT 
                pageId,
                recognitionStatus,
                recognitionJob,
                recognitionLanguages,
                recognitionModifiedText
        FROM Page WHERE pageId = :pageId
        """
    )
    fun queryRecognitionTask(pageId: Page.Id): Flow<PageRecognitionTask?>

    /**
     * Setup recognition job
     */
    @Query(
        """
            UPDATE Page SET 
                recognitionJob = :job,
                recognitionLanguages = :languages,
                recognitionModifiedText = :modified,
                recognitionStatus = recognitionStatus + 2
            WHERE pageId = :pageId
        """
    )
    suspend fun startRecognition(pageId: Page.Id, job: Page.RecognitionTask.Job, languages: String?, modified: ScanText?)

    @Query(
        """
            SELECT recognitionStatus FROM Page WHERE pageId = :pageId
        """
    )
    suspend fun getRecognitionStatus(pageId: Page.Id): Int?
    @Transaction
    suspend fun ensureRecognition(pageId: Page.Id, hasText: Boolean) {
        val status = getRecognitionStatus(pageId)
        if (status != null) {
            val pageHasText = status > 0
            when {
                // Need recognition
                hasText && !pageHasText ->
                    startRecognition(
                        pageId,
                        Page.RecognitionTask.Job.Recognize,
                        null,
                        null,
                    )

                // Need to clear text
                !hasText && pageHasText ->
                    startRecognition(
                        pageId,
                        Page.RecognitionTask.Job.Clear,
                        null,
                        null,
                    )
            }
        }
    }

    @Query(
        """
            SELECT COUNT(pageId) > 0 FROM Page WHERE pageId IN (:pages) AND recognitionStatus > 0
        """
    )
    fun queryPagesHaveText(pages: List<Page.Id>): Flow<Boolean>

    @Insert
    suspend fun insertShareSession(session: ShareSession): Long

    @Delete(entity = ShareSession::class)
    suspend fun deleteShareSession(sessionId: ShareSessionId)

    suspend fun deleteShareSession(sessionId: ShareSession.Id) =
        deleteShareSession(ShareSessionId(sessionId))

    @Query("""
        SELECT shareSessionId FROM ShareSession
    """)
    @Transaction
    fun queryShareSessionIds(): Flow<List<ShareSession.Id>>

    @Query(
        """
            SELECT COUNT(shareSessionId) FROM ShareSession 
            WHERE NOT EXISTS (
                SELECT DISTINCT shareItemId FROM ShareItem WHERE ShareSession.shareSessionId = ShareItem.shareItemId
            )
        """
    )
    fun queryOrphanShareSessions(): Flow<Int>

    @Query(
        """
            DELETE FROM ShareSession
            WHERE NOT EXISTS (
                SELECT shareItemId FROM ShareItem WHERE ShareSession.shareSessionId = ShareItem.shareItemId 
            )
        """
    )
    fun deleteOrphanShareSessions()

    @Insert
    suspend fun insertShareItem(item: ShareItem)

    @Transaction
    suspend fun createNewShareSession(type: ShareSession.Type, vararg pages: Page.Id) {
        val sessionId = ShareSession.Id(insertShareSession(
            ShareSession(
                ShareSession.Id.Initial,
                type,
            ))
        )

        pages.forEachIndexed { index, pageId ->
            insertShareItem(
                ShareItem(
                    sessionId,
                    index,
                    pageId,
                )
            )
        }
    }

    @Query(
        """
            SELECT ShareSession.* 
                FROM ShareSession
                JOIN PendingShares ON shareSessionId = pendingShareSessionId
                    AND (pendingCount = 0)
        """
    )
    @Transaction
    fun queryShareSessionItems(): Flow<List<ShareSessionItems>>

    @Query(
        """
            SELECT COALESCE(COUNT(shareItemId), 0) 
                FROM ShareItem GROUP BY shareItemId
        """
    )
    fun queryHasShareSessions(): Flow<Int?>

/*
    @Query(
        """
            SELECT * FROM PendingShares
        """
    )
    fun queryPendingShares(): Flow<List<PendingShares>>
*/

    @Query(
        """
            SELECT paperSizeId FROM Page WHERE pageId = :pageId
        """
    )
    fun queryPagePaperSize(pageId: Page.Id): Flow<Page.Paper?>

    @Query(
        """
            SELECT paperOrientation FROM Page WHERE pageId = :pageId
        """
    )
    fun queryPagePaperOrientation(pageId: Page.Id): Flow<Page.Paper.Orientation?>

    @Update(entity = Page::class)
    suspend fun updatePagePaper(paper: PagePaper)

    /**
     * Build a set of page status great than this.
     * Used to safe update status e.g. we are trying to set status [Page.Status.Complete]
     * when page has status [Page.Status.Original]
     */
    private fun Page.Status.expected(): Set<Page.Status> =
        Page.Status.entries.filter {
            it.ordinal > this.ordinal
        }.toSet()


    /**
     * Build a set of page status great OR equal than this.
     * Used to check page status
     */
    private fun Page.Status.available(): Set<Page.Status> =
        Page.Status.entries.filter {
            it.ordinal >= this.ordinal
        }.toSet()

    companion object : Tag by AppTagger("EasyScanDao")
}