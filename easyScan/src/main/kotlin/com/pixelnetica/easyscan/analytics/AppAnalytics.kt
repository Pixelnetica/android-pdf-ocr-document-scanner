package com.pixelnetica.easyscan.analytics

/**
 * A small, vendor-agnostic analytics seam.
 *
 * The app logs typed events through [AppAnalytics.log]; a backend decides what
 * to do with them. The default backend does nothing, so the app behaves
 * identically whether or not an analytics provider is wired in — and in this
 * sample none is.
 *
 * **No event may carry document content.** Names, paths, page text and OCR
 * results never appear in a parameter; only counts, durations, enumerated
 * modes, outcomes and format tokens do. That is what keeps the app's Play data
 * safety declaration limited to app interactions, and it is a rule to preserve
 * when adding events rather than a property of the current set.
 */

/** Where a page came from. */
enum class AnalyticsSource(val value: String) {
    CAMERA("camera"),
    GALLERY("gallery"),
}

/** How a scan input ended. */
enum class AnalyticsResult(val value: String) {
    SUCCESS("success"),
    CANCELLED("cancelled"),
    FAILURE("failure"),
}

/** What opened the crop editor. */
enum class AnalyticsEditorTrigger(val value: String) {
    /** The app opened it: detection failed, or smart crop is off. */
    AUTO("auto"),

    /** The user opened it from the toolbar. */
    MANUAL("manual"),
}

/**
 * A single analytics event: one event name plus its parameters, which is all a
 * backend needs to forward it.
 *
 * The names are the plain strings the analytics vendor expects rather than
 * constants from its SDK — that is deliberate, and it is what lets this file
 * stay free of any vendor dependency and ship in the published sample.
 */
sealed interface AnalyticsEvent {

    /** The event name, lower snake_case. */
    val name: String

    /** The event parameters. Values are String, Long or Double. */
    val parameters: Map<String, Any>

    /**
     * A screen was shown.
     *
     * Logged manually because the app draws every screen from one activity
     * through Navigation Compose, and automatic screen tracking reports the
     * activity — which would be a single screen for the whole session.
     *
     * @property route the navigation route *template*, never the resolved
     *   route: a resolved one carries page ids.
     */
    data class ScreenView(val route: String) : AnalyticsEvent {
        override val name = "screen_view"
        override val parameters = mapOf("screen_name" to route)
    }

    /** A page capture or import was started. */
    data class ScanStarted(val source: AnalyticsSource) : AnalyticsEvent {
        override val name = "scan_started"
        override val parameters = mapOf("source" to source.value)
    }

    /** A page capture or import finished, one way or another. */
    data class ScanCompleted(
        val source: AnalyticsSource,
        val result: AnalyticsResult,
        val pageCount: Int,
    ) : AnalyticsEvent {
        override val name = "scan_completed"
        override val parameters = mapOf(
            "source" to source.value,
            "result" to result.value,
            "page_count" to pageCount.toLong(),
        )
    }

    /** The crop editor was opened. */
    data class EditorOpened(val trigger: AnalyticsEditorTrigger) : AnalyticsEvent {
        override val name = "editor_opened"
        override val parameters = mapOf("trigger" to trigger.value)
    }

    /**
     * A document was exported or shared.
     *
     * @property format the export format token, for example `pdf` — never a
     *   file name
     */
    data class ShareDocument(
        val format: String,
        val pageCount: Int,
    ) : AnalyticsEvent {
        override val name = "share"
        override val parameters = mapOf(
            "content_type" to "document",
            "item_id" to format,
            "page_count" to pageCount.toLong(),
        )
    }
}

/** Receives logged events. Replace the backend to forward them somewhere. */
interface AnalyticsBackend {
    /** Handles one event. */
    fun log(event: AnalyticsEvent)
}

/** Default backend: drops every event. */
object NoOpAnalyticsBackend : AnalyticsBackend {
    override fun log(event: AnalyticsEvent) = Unit
}

/** App-wide analytics entry point. Swap [backend] to start forwarding events. */
object AppAnalytics {

    /** Where events go. Defaults to dropping them. */
    @Volatile
    var backend: AnalyticsBackend = NoOpAnalyticsBackend

    /** Logs one event through the installed backend. */
    fun log(event: AnalyticsEvent) = backend.log(event)
}
