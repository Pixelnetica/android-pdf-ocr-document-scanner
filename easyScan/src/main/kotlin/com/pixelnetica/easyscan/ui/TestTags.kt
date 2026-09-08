package com.pixelnetica.easyscan.ui

/**
 * Stable tags for UI-tree nodes that carry no accessibility text of their own.
 * The navigation roots expose them to UI-automation frameworks as resource ids
 * (testTagsAsResourceId); they have no effect on the rendered UI.
 */
object TestTags {
    /** The back arrow in a screen's top bar (at most one per screen). */
    const val NAV_BACK = "nav_back"

    /** The close cross of a navigation dialog. */
    const val DIALOG_CLOSE = "dialog_close"

    // Page list
    const val LIST_ITEM = "page_item"
    const val LIST_CLEAR_SELECTION = "list_clear_selection"
    const val LIST_ITEM_CHECKBOX = "list_item_checkbox"
    const val LIST_ITEM_DRAG_HANDLE = "list_item_drag_handle"

    // The dialog shown instead of opening a page whose image is gone
    const val PAGE_UNAVAILABLE_DELETE = "page_unavailable_delete"
    const val PAGE_UNAVAILABLE_CANCEL = "page_unavailable_cancel"

    /** The share dialog's refusal when a selected page has no image. */
    const val SHARE_UNAVAILABLE = "share_unavailable"

    /**
     * One cell of the share dialog's preview strip, carried whether the cell
     * shows its preview or the placeholder, so the strip's length can be
     * checked independently of whether the previews have loaded.
     */
    const val SHARE_PREVIEW_CELL = "share_preview_cell"

    // Page slider bottom bar
    const val SLIDER_ROTATE = "slider_rotate"
    const val SLIDER_CROP = "slider_crop"
    const val SLIDER_PROFILE = "slider_profile"
    const val SLIDER_OCR = "slider_ocr"

    // Color-profile dropdown items
    const val PROFILE_STRONG_SHADOWS = "profile_strong_shadows"
    const val PROFILE_BITONAL = "profile_bitonal"
    const val PROFILE_MONOCHROME = "profile_monochrome"
    const val PROFILE_COLORED = "profile_colored"
    const val PROFILE_ORIGINAL = "profile_original"

    // Crop screen bottom bar
    const val CROP_ROTATE_CCW = "crop_rotate_ccw"
    const val CROP_ROTATE_CW = "crop_rotate_cw"
    const val CROP_EXPAND = "crop_expand"
    const val CROP_REVERT = "crop_revert"

    // Share dialog export buttons
    const val SHARE_AS_IMAGE = "share_as_image"
    const val SHARE_AS_TIFF = "share_as_tiff"
    const val SHARE_AS_PDF = "share_as_pdf"
    const val SHARE_AS_TEXT = "share_as_text"

    // Settings controls
    const val SETTINGS_THEME = "settings_theme"
    const val SETTINGS_DYNAMIC_COLORS = "settings_dynamic_colors"
    const val SETTINGS_AUTO_ORIENTATION = "settings_auto_orientation"
    const val SETTINGS_PDF_COMPRESSION = "settings_pdf_compression"
    const val SETTINGS_HIDDEN_TEXT = "settings_hidden_text"
    const val SETTINGS_LANGUAGES = "settings_languages"

    // Page properties dialog
    const val PROPS_PAPER_SIZE = "props_paper_size"
    const val PROPS_ORIENTATION = "props_orientation"
    const val PROPS_RECOGNIZE = "props_recognize"
    const val PROPS_OK = "props_ok"
}
