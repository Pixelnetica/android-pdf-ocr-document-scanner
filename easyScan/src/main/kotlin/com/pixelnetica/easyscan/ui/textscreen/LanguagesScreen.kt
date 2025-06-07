package com.pixelnetica.easyscan.ui.textscreen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.design.lang.ui.LanguagesSelector
import com.pixelnetica.easyscan.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagesScreen(
    navController: NavController,
    viewModel: LanguagesScreenViewModel = hiltViewModel(),
) {
    val languagesTitle by viewModel.languagesTitle.collectAsStateWithLifecycle("")
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = languagesTitle.ifEmpty {
                        stringResource(id = R.string.btn_read_languages)
                    })
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        }
    ) { contentPadding ->
        LanguagesSelector(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            exclusiveMode = false,
        )
    }
}