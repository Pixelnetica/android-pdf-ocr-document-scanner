package com.pixelnetica.easyscan.ui.settings

import androidx.lifecycle.ViewModel
import com.pixelnetica.design.lang.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsDialogViewModel @Inject constructor(
    val languageManager: LanguageManager
): ViewModel() {
    val languages = languageManager.getDisplayString(2)
}