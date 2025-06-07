package com.pixelnetica.easyscan.data

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pixelnetica.scanning.RefineFeature
import com.pixelnetica.scanning.ScanCutout
import com.pixelnetica.scanning.ScanOrientation
import com.pixelnetica.scanning.ScanText
import kotlinx.parcelize.Parcelize
import java.util.*

@Entity
data class Page(
    @Embedded(prefix = "page")
    @PrimaryKey(autoGenerate = true)
    val id: Id,

    /**
     * Current page state
     */
    val status: Status,

    /**
     * Valid only when [status] equal [Status.Initial]
     * To small to create separate table.
     */
    val initialUri: Uri?,

    /**
     * Order in page list
     */
    val orderIndex: Int,

    /**
     * message for status [Status.Invalid]
     */
    val errorMessage: String? = null,

    /**
     * Page was created
     */
    val dateCreated: Date = Date(),

    /**
     * Optional page title. Not used yet
     */
    val title: String? = "",

    /**
     * Common orientation for all states
     */
    val orientation: ScanOrientation = ScanOrientation.Undefined,

    /**
     * User-defined cutout.
     */
    val cutout: ScanCutout? = null,

    /**
     * Page need an user correction
     */
    @ColumnInfo(defaultValue = "0")
    val undefinedCutout: Boolean = false,

    /**
     * Set cutout to auto-detect value
     */
    val resetCutout: ResetCutout = ResetCutout.Reset,

    val strongShadows: Boolean = false,

    val profileType: RefineFeature.Profile.Type = RefineFeature.Profile.Type.Bitonal,

    @ColumnInfo(defaultValue = "0")
    val autoDetectOrientation: Boolean = false,

    /**
     * Page paper size and orientation
     */
    @ColumnInfo(name = "paperSizeId", typeAffinity = ColumnInfo.INTEGER, defaultValue = "-4")
    val paper: Paper = Paper.Predefined(Paper.Predefined.Size.A4),
    @ColumnInfo(defaultValue = "Portrait")
    val paperOrientation: Paper.Orientation = Paper.Orientation.Portrait,

    @Embedded(prefix = "recognition")
    val recognitionTask: RecognitionTask = RecognitionTask(),

    ) {
    @Parcelize
    data class Id(
        @ColumnInfo(name = "Id")
        val id: Long
    ) : Parcelable {
        companion object {
            val Initial = Id(0L)
            val Undefined = Id(-1L)
        }
    }

    enum class Status {
        // Haven't an image
        Invalid,
        Initial,

        // Must have the image
        Input,
        Original,
        Pending,
        Complete,
        Output,
        ;

        /**
         * Check status great or equal than [required]
         */
        fun isAtLeast(required: Status): Boolean =
            this.ordinal >= required.ordinal

        fun distinctAtLeast(required: Status): Status =
            if (this.isAtLeast(required)) {
                required
            } else {
                this
            }
    }

    sealed class Paper {

        enum class Orientation {
            Auto,
            Portrait,
            Landscape,
        }

        data class Id(val id: Long) : Paper()
        data class Predefined(val size: Size): Paper() {
            /**
             * Some predefined paper sizes
             */
            enum class Size {
                Letter,
                Legal,
                A3,
                A4,
                A5,
                B4,
                B5,
                Executive,
                US4x6,
                US4x8,
                US5x7,
                COMM10,
                BusinessCard,
            }
        }

        fun toLong(): Long =
            when(this) {
            // Store paper id as is
            is Id -> id
            // Store predefined as negative
            is Predefined -> -(size.ordinal.toLong() + 1)
        }
        companion object {
            fun fromLong(value: Long): Paper =
                if (value >= 0) {
                    Paper.Id(value)
                } else {
                    // NOTE: value is negative!
                    val index = -(value + 1).toInt()
                    Predefined(Page.Paper.Predefined.Size.values()[index])
                }
        }
    }
    /**
     * [status] increments every time from [RecognitionTask.Nothing]
     * by 2 to differ many recognition requests
     *
     */
    data class RecognitionTask(
        @ColumnInfo(name = "Status", defaultValue = "0")
        val status: Int = Nothing,

        @ColumnInfo(name = "Job", defaultValue = "Cancel")
        val job: Job = Job.Cancel,

        /**
         * null to recognize with default settings
         * recognize with specified languages
         */
        @ColumnInfo(name = "Languages")
        val languages: String? = null,

        @ColumnInfo(name = "ModifiedText")
        val modifiedText: ScanText? = null,
        ) {
        val isNothing get() = status == Nothing
        val isReady get() = status == Ready
        val isPending get() = status > Ready

        // Ready or Nothing
        constructor(ready: Boolean) : this(
            if (ready) Ready else Nothing
        )

        enum class Job {
            /**
             * cancel current task and doesn't change result
             */
            Cancel,

            /**
             * remove recognition result
             */
            Clear,

            /**
             * replace modified text
             */
            Modify,

            /**
             * start to recognize
             */
            Recognize
        }

        companion object {
            /**
             * Page was not recognized and haven't got any recognition result
             */
            const val Nothing = 0

            /**
             * Page has recognition result
             */
            private const val Ready = 1
        }
    }


    enum class ResetCutout {
        Reset,
        Setup,
        Expand
    }

    enum class ChangeOrientation {
        Reset,
        None,
        Rotate90,
        Rotate180,
        Rotate270,
    }
}

