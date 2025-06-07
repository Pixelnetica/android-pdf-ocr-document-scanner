package com.pixelnetica.support

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.pixelnetica.easyscan.AppTagger

sealed class ImagePicker: ActivityResultContract<Unit, List<Uri>>() {
    companion object {
        fun create(context: Context): ImagePicker =
            // Using VisualMediaPicker only if Google Media available
            if (getGmsPicker(context) != null) {
                VisualMediaPicker()
            } else {
                // Using standard content picker.
                // Permissions weren't checked
                ImageContentPicker()
            }

        @JvmStatic
        internal fun getGmsPicker(context: Context): ResolveInfo? {
            return context.packageManager.resolveActivity(
                Intent("com.google.android.gms.provider.action.PICK_IMAGES"),
                PackageManager.MATCH_DEFAULT_ONLY or
                        (if (Build.VERSION.SDK_INT >= 24) PackageManager.MATCH_SYSTEM_ONLY else 0)
            )
        }
    }
}

private class VisualMediaPicker: ImagePicker() {
    private val contract = ActivityResultContracts.PickMultipleVisualMedia()
    override fun createIntent(context: Context, input: Unit): Intent =
        contract.createIntent(context, PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> =
        contract.parseResult(resultCode, intent)

}

private class ImageContentPicker: ImagePicker() {
    private val contract = ActivityResultContracts.GetMultipleContents()
    override fun createIntent(context: Context, input: Unit): Intent =
        contract.createIntent(context, "image/*")

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> =
        contract.parseResult(resultCode, intent)
}

private object Logger: Tag by AppTagger("ImagePicker")