package com.yq.suibi.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yq.suibi.SuibiApp
import com.yq.suibi.ui.allnotes.AllNotesScreen
import com.yq.suibi.ui.editor.EditorScreen
import com.yq.suibi.ui.notes.NoteListScreen
import com.yq.suibi.ui.search.SearchScreen
import com.yq.suibi.ui.settings.SettingsScreen
import com.yq.suibi.ui.settings.ThemeScreen
import com.yq.suibi.ui.settings.WebDavScreen
import com.yq.suibi.ui.topics.TopicListScreen

object Routes {
    const val TOPICS = "topics"
    const val ALL_NOTES = "all"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val WEBDAV = "settings/webdav"
    const val THEME = "settings/theme"
    const val NOTES = "notes/{topicId}"
    const val EDITOR = "editor/{topicId}/{noteId}?q={q}"

    fun notes(topicId: Long) = "notes/$topicId"
    fun editor(topicId: Long, noteId: Long, q: String? = null): String {
        val base = "editor/$topicId/$noteId"
        return if (q.isNullOrBlank()) base else "$base?q=${java.net.URLEncoder.encode(q, "UTF-8")}"
    }
}

/** 进入动画 180ms，退出 150ms。默认是 700ms，拖得难受。 */
private const val ENTER_MS = 190
private const val EXIT_MS = 150

@Composable
fun SuibiNavHost(onPaletteChange: (String) -> Unit = {}) {
    val app = LocalContext.current.applicationContext as SuibiApp
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.TOPICS,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(ENTER_MS)) { it / 5 } +
                fadeIn(animationSpec = tween(ENTER_MS))
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(EXIT_MS)) { -it / 8 } +
                fadeOut(animationSpec = tween(EXIT_MS))
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(ENTER_MS)) { -it / 5 } +
                fadeIn(animationSpec = tween(ENTER_MS))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(EXIT_MS)) { it / 8 } +
                fadeOut(animationSpec = tween(EXIT_MS))
        }
    ) {

        composable(Routes.TOPICS) {
            TopicListScreen(
                db = app.db,
                onOpenTopic = { nav.navigate(Routes.notes(it)) },
                onOpenAllNotes = { nav.navigate(Routes.ALL_NOTES) },
                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ALL_NOTES) {
            AllNotesScreen(
                db = app.db,
                onBack = { nav.popBackStack() },
                onOpenNote = { topicId, noteId ->
                    nav.navigate(Routes.editor(topicId, noteId))
                }
            )
        }

        composable(
            route = Routes.NOTES,
            arguments = listOf(navArgument("topicId") { type = NavType.LongType })
        ) { entry ->
            val topicId = entry.arguments?.getLong("topicId") ?: 0L
            NoteListScreen(
                db = app.db,
                topicId = topicId,
                onBack = { nav.popBackStack() },
                onOpenNote = { nav.navigate(Routes.editor(topicId, it)) }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("topicId") { type = NavType.LongType },
                navArgument("noteId") { type = NavType.LongType },
                navArgument("q") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            val topicId = entry.arguments?.getLong("topicId") ?: 0L
            val noteId = entry.arguments?.getLong("noteId") ?: -1L
            val q = entry.arguments?.getString("q").orEmpty()
            EditorScreen(
                db = app.db,
                appScope = app.appScope,
                topicId = topicId,
                noteId = noteId,
                initialQuery = q,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                db = app.db,
                onBack = { nav.popBackStack() },
                onOpenNote = { topicId, noteId, query ->
                    nav.navigate(Routes.editor(topicId, noteId, query))
                },
                onOpenTopic = { topicId ->
                    nav.navigate(Routes.notes(topicId))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                store = app.settings,
                onBack = { nav.popBackStack() },
                onOpenWebDav = { nav.navigate(Routes.WEBDAV) },
                onOpenTheme = { nav.navigate(Routes.THEME) }
            )
        }

        composable(Routes.WEBDAV) {
            WebDavScreen(
                db = app.db,
                store = app.settings,
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.THEME) {
            ThemeScreen(
                store = app.settings,
                onBack = { nav.popBackStack() },
                onPaletteChange = onPaletteChange
            )
        }
    }
}