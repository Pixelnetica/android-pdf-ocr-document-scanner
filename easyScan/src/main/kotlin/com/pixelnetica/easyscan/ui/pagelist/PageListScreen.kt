package com.pixelnetica.easyscan.ui.pagelist

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.camera.CameraContract
import com.pixelnetica.composability.DraggableItem
import com.pixelnetica.composability.drawableResource
import com.pixelnetica.composability.isDragging
import com.pixelnetica.composability.isScrolledToEnd
import com.pixelnetica.composability.rememberDragDropState
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.ui.viewitem.*
import com.pixelnetica.support.ImagePicker
import com.pixelnetica.support.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageListScreen(
    navController: NavController,
    viewModel: PageListViewModel = hiltViewModel(),
) {

    val imagePicker = rememberLauncherForActivityResult(
        contract = ImagePicker.create(LocalContext.current)) { uriList ->
        //When the user has selected a photo, its URI is returned here
        viewModel.createNewPages(uriList)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = CameraContract()) { uriList ->
        viewModel.createNewPages(uriList)
    }


    val totalPages by viewModel.totalPages.collectAsStateWithLifecycle(emptyList())
    val checkedPages by viewModel.checkedPages.collectAsStateWithLifecycle(emptyList())
    val checkedCount = checkedPages.size

    // Process back button for checked items
    BackHandler(enabled = checkedCount > 0) {
        viewModel.checkAllPages(false)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    var isScrolledToEnd by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier =
            if (checkedCount > 0) {
                Modifier
            } else {
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            },
        topBar = {
            if (checkedCount > 0) {
                // Action Mode for checked pages
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.checkAllPages(false) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null
                            )
                        }

                    },
                    title = { Text(checkedCount.toString()) },
                    actions = {
                        // Select All
                        if (checkedCount != totalPages.size) {
                            // Don't show SELECT ALL if all items are selected
                            IconButton(onClick = {
                                viewModel.checkAllPages(true)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_select_all),
                                    contentDescription = stringResource(id = R.string.checked_pages_select_all)
                                )
                            }
                        }

                        // Share
                        IconButton(
                            onClick = {
                                val arg = checkedPages.joinToString(",") { it.asArg() }
                                navController.navigate("sharePages/$arg")
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = stringResource(id = R.string.checked_pages_share)
                            )
                        }

                        // Show properties only if one page is checked
                        if (checkedCount == 1) {
                            IconButton(
                                onClick = {
                                    navController.navigate(
                                        "pageProps/${checkedPages.first().asArg()}"
                                    )
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_page_property),
                                    contentDescription = stringResource(id = R.string.page_props)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteCheckedPages() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_trash_bin),
                                contentDescription = stringResource(id = R.string.checked_pages_delete)
                            )
                        }
                    },
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        // TODO: Replace to app icon
                        Icon(
                            painter = drawableResource(id = R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified,
                        )
                    },
                    title = {
                        Text(stringResource(id = R.string.app_name))
                    },

                    actions = {
                        // Load from album
                        IconButton(onClick = {
                            imagePicker.launch(Unit)
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_album),
                                contentDescription = stringResource(id = R.string.add_from_album)
                            )
                        }

                        // Show settings
                        IconButton(
                            onClick = {
                                navController.navigate("settings")
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = stringResource(id = R.string.action_settings)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },

        floatingActionButton = {
            AnimatedVisibility(visible = !isScrolledToEnd) {
                FloatingActionButton(
                    onClick = {
                        cameraLauncher.launch(CameraContract.CameraParams())
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_camera),
                        contentDescription = stringResource(id = R.string.fab_desc)
                    )
                }
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            PageListContent(navController, {
                Logger.log.d("List is scroll to end $it")
                isScrolledToEnd = it
            })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageListContent(
    navController: NavController,
    onScrolledToEnd: (isScrolledToEnd: Boolean) -> Unit,
    viewModel: PageListViewModel = hiltViewModel(),
) {
    // Clean selection on startup
    LaunchedEffect(Unit) {
        viewModel.checkAllPages(false)
    }

    val pages by viewModel.viewItems.collectAsStateWithLifecycle(emptyList())

    val hasChecked by remember {
        derivedStateOf {
            pages.any { viewItem ->
                viewItem.isChecked
            }
        }
    }

    // Need to drag-n-drop
    val cachedPages = remember {
        mutableStateListOf<PageViewItem>()
    }

    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { from, to ->
        cachedPages.apply {
            add(to, removeAt(from))
        }
        viewModel.reorderPages(cachedPages)
    }

    if (!dragDropState.isDragging) {
        cachedPages.clear()
        cachedPages.addAll(pages)
    }

    // Notify parent to hide FAB
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrolledToEnd }.collect {
            onScrolledToEnd(it)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        itemsIndexed(
            items = cachedPages,
            key = { _, item -> item.id },
            contentType = { _, _ -> PageViewItem::class },
            itemContent = { index, item ->
                DraggableItem(dragDropState, index) { isDragging ->
                    PageListItem(
                        navController = navController,
                        viewModel = viewModel,
                        viewItem = item,
                        dragDropState = dragDropState,
                        hasChecked = hasChecked,
                        isDragging = isDragging,
                    )
                }
            })
    }
}

private object Logger: Tag by AppTagger("PageListScreen")