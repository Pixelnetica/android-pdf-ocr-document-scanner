package com.pixelnetica.easyscan.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pixelnetica.easyscan.AppTagger
import com.pixelnetica.easyscan.analytics.AnalyticsEvent
import com.pixelnetica.easyscan.analytics.AppAnalytics
import com.pixelnetica.easyscan.ui.cropscreen.CropScreen
import com.pixelnetica.easyscan.ui.pagelist.PageListScreen
import com.pixelnetica.easyscan.ui.pageprops.PageProperties
import com.pixelnetica.easyscan.ui.pagescreen.PageSliderScreen
import com.pixelnetica.easyscan.ui.settings.SettingsDialog
import com.pixelnetica.easyscan.ui.pageunavailable.PageUnavailableDialog
import com.pixelnetica.easyscan.ui.sharedialog.ShareDialog
import com.pixelnetica.easyscan.ui.textscreen.LanguagesScreen
import com.pixelnetica.easyscan.ui.textscreen.TextScreen
import com.pixelnetica.support.Tag

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainNavigation(navController: NavHostController = rememberNavController()) {
    TrackScreenViews(navController)
    NavHost(
        navController = navController,
        startDestination = "pageList",
        // Expose UI-tree tags as resource ids for UI-automation frameworks
        modifier = Modifier.semantics { testTagsAsResourceId = true },
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
                navController = navController
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

        dialog(
            route = "pageUnavailable/{pageId}",
            arguments = listOf(
                navArgument("pageId") {
                    type = NavType.LongType
                })
        ) {
            PageUnavailableDialog(navController = navController)
        }

        // TODO: Add more destinations
    }
}

/**
 * Reports one screen view per navigation destination.
 *
 * The whole app is drawn by a single activity, so the automatic screen
 * tracking an analytics SDK performs would see one screen for the entire
 * session. Observing the navigation controller is what makes the report
 * describe the app instead of the activity.
 *
 * The route *template* is logged, never the resolved route: templates like
 * `pageSlider/{startPageId}` resolve to a concrete page id, and a screen name
 * must not carry one.
 */
@Composable
private fun TrackScreenViews(navController: NavHostController) {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                AppAnalytics.log(AnalyticsEvent.ScreenView(route))
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
}

private object Logger: Tag by AppTagger("MainNavigation")