package com.pixelnetica.easyscan.diagnostics

/**
 * A small, vendor-agnostic seam for crash and error reporting.
 *
 * The app never talks to a reporting SDK directly. It logs through the usual
 * `log.w` / `log.e` calls, and [AppTagger][com.pixelnetica.easyscan.AppTagger]
 * forwards those to whichever [DiagnosticsSink] is installed. The default sink
 * drops everything, so the app behaves identically whether or not a reporting
 * provider is wired in — and this build of the sample has none.
 *
 * To send diagnostics somewhere, implement [DiagnosticsSink] and assign it to
 * [AppDiagnostics.sink] once at startup.
 */

/** Receives warnings and errors the app has logged. */
interface DiagnosticsSink {

    /**
     * Records a non-fatal warning.
     *
     * @param tag the logging tag the message was written under
     * @param message the text that was logged
     */
    fun recordWarning(tag: String, message: String)

    /**
     * Records a non-fatal error.
     *
     * @param tag the logging tag the message was written under
     * @param message the text that was logged
     */
    fun recordError(tag: String, message: String)
}

/** Default sink: drops everything. */
object NoOpDiagnosticsSink : DiagnosticsSink {
    override fun recordWarning(tag: String, message: String) = Unit
    override fun recordError(tag: String, message: String) = Unit
}

/**
 * App-wide diagnostics entry point. Replace [sink] to start forwarding.
 *
 * Only warnings and errors reach a sink. Debug, info and verbose logging stays
 * local: an incident channel that also carries routine tracing stops being
 * useful at the first busy screen.
 */
object AppDiagnostics {

    /** Where warnings and errors go. Defaults to dropping them. */
    @Volatile
    var sink: DiagnosticsSink = NoOpDiagnosticsSink

    /** Forwards a warning to the installed sink. */
    fun warning(tag: String, message: String) = sink.recordWarning(tag, message)

    /** Forwards an error to the installed sink. */
    fun error(tag: String, message: String) = sink.recordError(tag, message)
}
