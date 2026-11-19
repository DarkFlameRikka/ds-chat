package com.deepseekclone.app.ui

import android.net.Uri
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepseekclone.app.data.AppContainer
import com.deepseekclone.app.ui.screens.chat.ChatScreen
import com.deepseekclone.app.ui.screens.history.HistoryDrawerContent
import com.deepseekclone.app.ui.screens.home.HomeScreen
import com.deepseekclone.app.ui.screens.settings.ApiKeyScreen
import com.deepseekclone.app.ui.screens.settings.FontSizeScreen
import com.deepseekclone.app.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun AppRoot(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val conversations by container.conversationStore.conversations.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val currentConversationId = if (route.startsWith("chat/")) {
        backStackEntry?.arguments?.getString("id")
    } else {
        null
    }

    fun goHome() {
        navController.navigate("home") {
            popUpTo("home") { inclusive = true }
            launchSingleTop = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.background,
            ) {
                HistoryDrawerContent(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onSelect = { id ->
                        scope.launch { drawerState.close() }
                        navController.navigate("chat/$id") { launchSingleTop = true }
                    },
                    onNewChat = {
                        scope.launch { drawerState.close() }
                        goHome()
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    },
                    onDelete = { id ->
                        scope.launch {
                            container.conversationStore.delete(id)
                            if (currentConversationId == id) goHome()
                        }
                    },
                    onPin = { id, pinned -> scope.launch { container.conversationStore.setPinned(id, pinned) } },
                    onRename = { id, title -> scope.launch { container.conversationStore.rename(id, title) } },
                    onBatchDelete = { ids ->
                        scope.launch {
                            ids.forEach { container.conversationStore.delete(it) }
                            if (currentConversationId in ids) goHome()
                        }
                    },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    container = container,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onStartChat = { id, text, voice ->
                        navController.navigate("chat/$id?initial=${Uri.encode(text)}&voice=$voice")
                    },
                )
            }
            composable(
                route = "chat/{id}?initial={initial}&voice={voice}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("initial") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("voice") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val initial = entry.arguments?.getString("initial")
                ChatScreen(
                    container = container,
                    conversationId = id,
                    initialText = initial,
                    startVoiceMode = entry.arguments?.getBoolean("voice") == true,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNewChat = { goHome() },
                    onOpenSettings = { navController.navigate("settings") },
                    onDeleted = { goHome() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings = container.settingsRepository,
                    onBack = { navController.popBackStack() },
                    onEditApiKey = { navController.navigate("apiKey") },
                    onOpenFontSize = { navController.navigate("fontSize") },
                    onOpenApiKeyGuide = { navController.navigate("apiKeyGuide") },
                    container = container,
                )
            }
            composable("apiKeyGuide") {
                com.deepseekclone.app.ui.screens.settings.ApiKeyGuideScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("fontSize") {
                FontSizeScreen(
                    settings = container.settingsRepository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("apiKey") {
                ApiKeyScreen(
                    settings = container.settingsRepository,
                    client = container.chatClient,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
