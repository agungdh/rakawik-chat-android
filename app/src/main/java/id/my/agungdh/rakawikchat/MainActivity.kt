package id.my.agungdh.rakawikchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import id.my.agungdh.rakawikchat.ui.navigation.NavGraph
import id.my.agungdh.rakawikchat.ui.theme.RakawikChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RakawikChatTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
