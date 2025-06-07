package com.pixelnetica.easyscan.ui.pagescreen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pixelnetica.camera.CameraContract
import com.pixelnetica.composability.painterDrawables
import com.pixelnetica.composability.rememberDrawable
import com.pixelnetica.easyscan.R
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.ui.viewitem.PageViewProfile
import com.pixelnetica.support.ImagePicker
import com.pixelnetica.support.Tag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PageSliderScreen(
    navController: NavController,
    viewModel: PageSliderViewModel = hiltViewModel()
) {
    val initialPages = remember {
        emptyList<PageSliderViewModel.PageContent>()
    }

    val pages by viewModel.pageContent.collectAsStateWithLifecycle(initialPages)
    val showPageId by viewModel.showPageId.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState {
        pages.size
    }

    // Close itself if no pages in viewModel
    LaunchedEffect(pages) {
        if (pages.isEmpty() && pages !== initialPages) {
            navController.popBackStack()
        }
    }

    val currentPage by remember(pagerState, pages) {
        derivedStateOf {
            pages.getOrNull(pagerState.currentPage)
        }
    }

    val showPosition by remember(pagerState, pages, showPageId) {
        derivedStateOf {
            pages.indexOfFirst {
                it.asViewId() == showPageId
            }
        }
    }

    val pageProfile = currentPage?.profile?.collectAsStateWithLifecycle(null)

    // Profile Menu
    val showProfileMenu = remember {
        mutableStateOf(false)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ImagePicker.create(LocalContext.current)) { uriList ->
        //When the user has selected a photo, its URI is returned here
        viewModel.insertNewPages(currentPage, uriList)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = CameraContract()
    ) { uriList ->
        viewModel.insertNewPages(currentPage, uriList)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                title = {
                    Text(
                        text =
                        if (pagerState.currentPage < pages.size) {
                            "${pagerState.currentPage + 1} / ${pages.size}"
                        } else {
                            ""
                        }
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            cameraLauncher.launch(CameraContract.CameraParams())
                        },
                        enabled = currentPage != null || pages.isEmpty()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = stringResource(id = R.string.page_camera),
                        )
                    }

                    IconButton(
                        onClick = {
                            imagePicker.launch(Unit)
                        },
                        enabled = currentPage != null || pages.isEmpty()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_album),
                            contentDescription = stringResource(id = R.string.page_add_from_album),
                        )
                    }

                    IconButton(
                        onClick = {
                            currentPage?.let {
                                navController.navigate("sharePages/${it.asArg()}")
                            }
                        },
                        enabled = currentPage != null,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = stringResource(id = R.string.page_share),
                        )
                    }

                    IconButton(
                        onClick = {
                                  currentPage?.let {
                                      navController.navigate("pageProps/${it.asArg()}")
                                  }

                        },
                        enabled = currentPage != null,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_page_property),
                            contentDescription = stringResource(id = R.string.page_props_paper_label),
                        )
                    }

                    IconButton(onClick = {
                        currentPage?.deletePage()
                    }, enabled = currentPage != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash_bin),
                            contentDescription = stringResource(id = R.string.page_delete),
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Spacer(modifier = Modifier.weight(0.5F, true))

                IconButton(
                    onClick = {
                        currentPage?.rotatePage()
                    },
                enabled = currentPage != null
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_rotate_ccw),
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.weight(1.0F, true))

                IconButton(
                    onClick = {
                        // Show cutout screen
                        currentPage?.let {
                            navController.navigate("pageCutout/${it.asArg()}")
                        }
                    },
                    enabled = currentPage != null) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_crop_rotate),
                        contentDescription = null,
                    )
                }

                Spacer(modifier = Modifier.weight(1.0F, true))

                IconButton(
                    onClick = {
                        showProfileMenu.value = true
                              },
                    enabled = pageProfile?.value != null
                ) {
                    when (pageProfile?.value?.profile) {
                        PageViewProfile.Type.Original ->
                            Icon(
                                painterResource(id = R.drawable.ic_profile_original),
                                contentDescription = null,
                            )

                        PageViewProfile.Type.Bitonal ->
                            Icon(
                                painterResource(id = R.drawable.ic_profile_bw),
                                contentDescription = null,
                            )

                        PageViewProfile.Type.Monochrome ->
                            Icon(
                                painterDrawables(
                                    rememberDrawable(id = R.drawable.ic_profile_gray),
                                    rememberDrawable(id = R.drawable.ic_profile_border, tint = LocalContentColor.current),
                                ),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )

                        PageViewProfile.Type.Colored ->
                            Icon(
                                painterDrawables(
                                    rememberDrawable(id = R.drawable.ic_profile_color),
                                    rememberDrawable(id = R.drawable.ic_profile_border, tint = LocalContentColor.current),
                                ),
                                contentDescription = null,
                                tint = Color.Unspecified,
                            )

                        null -> Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    }

                    ProfileMenu(
                        showProfileMenu = showProfileMenu,
                        pageProfile = pageProfile,
                        currentPage = currentPage,
                    )
                }

                Spacer(modifier = Modifier.weight(1.0F, true))

                IconButton(onClick = {
                    currentPage?.let {
                        navController.navigate("pageText/${it.asArg()}")
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ocr),
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.weight(0.5F, true))
            }
        }
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState,
            contentPadding = contentPadding,
            key = { index ->
                pages[index].asKey()
            }
        ) { index ->
            PageView(
                modifier = Modifier.fillMaxSize(),
                pages[index],
            )
        }

        // Normal scroll to show position
        LaunchedEffect(showPosition) {
            if (showPosition != -1) {
                pagerState.scrollToPage(showPosition)
                // Don't scroll again to start page
                viewModel.resetShowPage()
            }
        }

        // Show placeholder image
        if (pages.isEmpty()) {
            Image(
                painter = painterResource(id = R.drawable.document_photo_icon),
                contentDescription = null,
            )
        }
    }
 }

@Composable
fun ProfileMenu(
    showProfileMenu: MutableState<Boolean>,
    pageProfile: State<PageViewProfile?>?,
    currentPage: PageSliderViewModel.PageContent?) {

    var showMenu by showProfileMenu
    val shadows = pageProfile?.value?.shadows
    val profile = pageProfile?.value?.profile

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = {
            showMenu = false
        }
    ) {
        // Strong Shadows...
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.page_color_strong_shadows))
            },
            onClick = {
                currentPage?.setShadows(shadows = shadows != true)
                showMenu = false
            },
            leadingIcon = {
                when (pageProfile?.value?.shadows) {
                    true -> Icon(
                        painterResource(id = R.drawable.ic_strong_shadows),
                        contentDescription = null,
                    )

                    false -> Icon(
                        painterResource(id = R.drawable.ic_strong_shadows_off),
                        contentDescription = null,
                    )

                    null -> Unit
                }
            },
            trailingIcon = {
                if (shadows == true) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                }
            },
        )

        // Line
        HorizontalDivider()

        // Original profile
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.page_color_original))
            },
            onClick = {
                currentPage?.setProfile(profile = PageViewProfile.Type.Original)
                showMenu = false
            },
            leadingIcon = {
                Icon(
                    painterResource(id = R.drawable.ic_profile_original),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (profile == PageViewProfile.Type.Original) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                    )
                }
            },
        )

        // BW profile
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.page_color_binary))
            },
            onClick = {
                currentPage?.setProfile(profile = PageViewProfile.Type.Bitonal)
                showMenu = false
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_profile_bw),
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (profile == PageViewProfile.Type.Bitonal) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                }
            },
        )

        // Gray profile
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.page_color_gray))
            },
            onClick = {
                currentPage?.setProfile(profile = PageViewProfile.Type.Monochrome)
                showMenu = false
            },
            leadingIcon = {
                Icon(
                    painter = painterDrawables(
                        rememberDrawable(id = R.drawable.ic_profile_gray),
                        rememberDrawable(id = R.drawable.ic_profile_border, tint = LocalContentColor.current),
                    ),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            },
            trailingIcon = {
                if (profile == PageViewProfile.Type.Monochrome) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                }
            },
        )

        // Color profile
        DropdownMenuItem(
            text = {
                Text(text = stringResource(id = R.string.page_color_color))
            },
            onClick = {
                currentPage?.setProfile(PageViewProfile.Type.Colored)
                showMenu = false
            },
            leadingIcon = {
                Icon(
                    painter = painterDrawables(
                        rememberDrawable(id = R.drawable.ic_profile_color),
                        rememberDrawable(id = R.drawable.ic_profile_border, tint = LocalContentColor.current),
                        ),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            },
            trailingIcon = {
                if (profile == PageViewProfile.Type.Colored) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

internal object Logger: Tag by AppTagger("PageSliderScreen")