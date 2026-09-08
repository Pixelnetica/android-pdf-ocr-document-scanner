package com.pixelnetica.easyscan

import com.pixelnetica.easyscan.diagnostics.AppDiagnostics
import com.pixelnetica.support.Tag
import com.pixelnetica.support.Tagger

/**
 * The app's logging tag, and the point where warnings and errors also become
 * diagnostics.
 *
 * Everything still reaches logcat exactly as before. On top of that, `w` and
 * `e` are handed to [AppDiagnostics], which forwards them to whatever
 * reporting sink is installed — nothing, by default. Routing happens here so
 * call sites keep writing plain `log.e(...)` and no screen has to know a crash
 * reporter exists.
 *
 * Only warnings and errors are forwarded. Debug and below stay local: an
 * incident channel that also carries routine tracing stops being useful at the
 * first busy screen.
 */
open class AppTagger(subTag: String) : Tagger("Pixelnetica.EasyScan.$subTag") {

    override val log: Tag.Log = DiagnosticsForwardingLog(TAG, super.log)

    /**
     * Logs through [delegate] as usual, and additionally reports warnings and
     * errors as diagnostics, tagged with the component that raised them.
     */
    private class DiagnosticsForwardingLog(
        private val tag: String,
        private val delegate: Tag.Log,
    ) : Tag.Log by delegate {

        override fun w(message: String) {
            delegate.w(message)
            AppDiagnostics.warning(tag, message)
        }

        override fun e(message: String) {
            delegate.e(message)
            AppDiagnostics.error(tag, message)
        }
    }
}
