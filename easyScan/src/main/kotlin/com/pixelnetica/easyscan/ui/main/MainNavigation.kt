package com.pixelnetica.easyscan.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.ui.cropscreen.CropScreen
import com.pixelnetica.easyscan.ui.pagelist.PageListScreen
import com.pixelnetica.easyscan.ui.pageprops.PageProperties
import com.pixelnetica.easyscan.ui.pagescreen.PageSliderScreen
import com.pixelnetica.easyscan.ui.settings.SettingsDialog
import com.pixelnetica.easyscan.ui.sharedialog.ShareDialog
import com.pixelnetica.easyscan.ui.textscreen.LanguagesScreen
import com.pixelnetica.easyscan.ui.textscreen.TextScreen
import com.pixelnetica.support.Tag

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "pageList",
    ) {
        composable(route = "pageList") {
            PageListScreen(navController)
        }
        composable(
            route = "pageSlider/{startPageId}",
            arguments = listOf(
                navArgument("startPageId") {
                    // TODO: Replace to NavType.ParcelableType<PageViewId>()
                    // when Google will be ready
                    type = NavType.LongType
                }
            ),
        ) {
            PageSliderScreen(navController)
        }
        composable(
            route= "pageCutout/{cropPageId}",
            arguments = listOf(
                navArgument("cropPageId") {
                    type = NavType.LongType
                }
            )
        ) {
            CropScreen(
                navController = navController,
            )
        }
        composable(
            route = "pageText/{textPageId}",
            arguments = listOf(
                navArgument("textPageId") {
                    type = NavType.LongType
                }
            )
        ) {
            TextScreen(
                navController = navController,
            )
        }

        composable(
            route = "languagesScreen"
        ) {
            LanguagesScreen(navController = navController)
        }

        dialog(
            route = "sharePages/{pages}",
        ) {
            ShareDialog(navController = navController)
        }

        dialog(
            route = "pageProps/{pageId}",
            arguments = listOf(
                navArgument("pageId") {
                    type = NavType.LongType
                })
        ) {
            PageProperties(navController = navController)
        }

        dialog(
            route = "settings",
        ) {
            SettingsDialog(navController = navController)
        }

        // TODO: Add more destinations
    }
}

private object Logger: Tag by AppTagger("MainNavigation")