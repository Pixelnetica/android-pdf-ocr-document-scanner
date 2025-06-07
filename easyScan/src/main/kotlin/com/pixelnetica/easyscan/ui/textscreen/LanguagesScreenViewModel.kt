package com.pixelnetica.easyscan.ui.textscreen

import androidx.lifecycle.ViewModel
import com.pixelnetica.design.lang.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguagesScreenViewModel
@Inject constructor(private val languageManager: LanguageManager): ViewModel() {
    val languagesTitle = languageManager.getDisplayString()
}