package id.my.agungdh.rakawikchat.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import id.my.agungdh.rakawikchat.ui.chat.ChatScreen
import id.my.agungdh.rakawikchat.ui.conversations.ConversationsScreen
import id.my.agungdh.rakawikchat.ui.login.LoginScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LOGIN = "login"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{conversationId}/{recipientName}"

    fun chat(conversationId: String, recipientName: String) =
        "chat/$conversationId/${URLEncoder.encode(recipientName, "UTF-8")}"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.CONVERSATIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CONVERSATIONS) {
            ConversationsScreen(
                onChatOpen = { conversationId, recipientName ->
                    navController.navigate(Routes.chat(conversationId, recipientName))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("recipientName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            val recipientName = URLDecoder.decode(
                backStackEntry.arguments?.getString("recipientName") ?: "",
                "UTF-8"
            )
            ChatScreen(
                conversationId = conversationId,
                recipientName = recipientName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
