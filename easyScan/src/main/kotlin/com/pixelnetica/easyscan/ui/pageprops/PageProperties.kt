package com.pixelnetica.easyscan.ui.pageprops

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.data.Page
import com.pixelnetica.easyscan.ui.main.NavDialog
import com.pixelnetica.support.Spinner
import com.pixelnetica.support.Tag

@Composable
fun PageProperties(
    navController: NavController,
    viewModel: PagePropertiesViewModel = hiltViewModel()
) = NavDialog(
        navController = navController,
        title = stringResource(id = R.string.page_props),
    ) {
    // Page preview
    val previewBitmap by viewModel.preview.collectAsStateWithLifecycle(null)
    val previewImage = previewBitmap?.asImageBitmap()
    val previewModifier = Modifier
        .align(Alignment.CenterHorizontally)
    if (previewImage != null) {
        com.pixelnetica.composability.PreviewBox(
            bitmap = previewImage,
            modifier = previewModifier,
        )
    } else {
        com.pixelnetica.composability.PreviewBox(
            painter = painterResource(id = R.drawable.document_photo_icon),
            modifier = previewModifier,
        )
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))


    // Load paper string names and paper unique id
    val context = LocalContext.current
    val paperMapper = remember {
        PaperMapper(context, R.array.paper_size_id_display)
    }

    val paper by viewModel.paper.collectAsStateWithLifecycle(
        initialValue = Page.Paper.Predefined(Page.Paper.Predefined.Size.A4)
    )
    var paperSizeDisplayIndex by remember(paper) {
        mutableIntStateOf(paperMapper.displayIndexOf(paper).coerceAtLeast(0))
    }

    Spinner(
        label = stringResource(id = R.string.page_props_paper_label),
        options = paperMapper.displayList.toTypedArray(),
        index = paperSizeDisplayIndex,
    ) {
        paperSizeDisplayIndex = it
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    val orientation by viewModel.paperOrientation.collectAsStateWithLifecycle(
        initialValue = Page.Paper.Orientation.Auto
    )
    var orientationIndex by remember(orientation) {
        mutableIntStateOf(orientation.ordinal)
    }
    Spinner(
        label = stringResource(id = R.string.page_props_paper_orientation),
        options = stringArrayResource(id = R.array.page_props_paper_orientation_list),
        index = orientationIndex,
    ) {
        orientationIndex = it
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    // Has text:
    val pageHasText by viewModel.hasText.collectAsStateWithLifecycle(false)
    var pageHasTextState by remember(pageHasText) {
        Logger.log.d("Initial page has text $pageHasText")
        mutableStateOf(pageHasText)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = pageHasTextState,
            onCheckedChange = {
                pageHasTextState = it
            },
        )
        Text(
            text = stringResource(id = R.string.page_recognize_text),
            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
        )
    }

    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

    Button(
        onClick = {
            viewModel.savePaper(
                paper = paperMapper.getPaperForDisplayIndex(paperSizeDisplayIndex),
                paperOrientation = Page.Paper.Orientation.values()[orientationIndex]
            )
            viewModel.ensurePageText(pageHasTextState)
            navController.popBackStack()
        },
        modifier = Modifier.align(Alignment.End),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(id = android.R.string.ok),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = stringResource(id = android.R.string.ok))
    }
}

private object Logger: Tag by AppTagger("PageProperties")
