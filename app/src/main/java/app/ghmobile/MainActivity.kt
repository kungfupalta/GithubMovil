package app.ghmobile

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        Api.token = Store.token
        setContent { GhTheme { App() } }
    }
}

private fun NavBackStackEntry.arg(k: String): String = arguments?.getString(k).orEmpty()

@Composable
fun App() {
    val nav = rememberNavController()
    val start = if (Store.token.isNotEmpty()) "home" else "login"
    NavHost(nav, startDestination = start) {
        composable("login") {
            LoginScreen {
                nav.navigate("home") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("home") {
            HomeScreen(nav) {
                Store.token = ""
                Api.token = ""
                nav.navigate("login") { popUpTo(0) }
            }
        }
        composable("newrepo") { NewRepoScreen(nav) }
        composable("repo/{owner}/{name}") { e -> RepoScreen(nav, e.arg("owner"), e.arg("name")) }
        composable(
            "file/{owner}/{name}?path={path}&ref={ref}",
            arguments = listOf(
                navArgument("path") { type = NavType.StringType; defaultValue = "" },
                navArgument("ref") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { e -> FileScreen(nav, e.arg("owner"), e.arg("name"), e.arg("path"), e.arg("ref")) }
        composable("run/{owner}/{name}/{id}") { e -> RunScreen(nav, e.arg("owner"), e.arg("name"), e.arg("id")) }
        composable(
            "issue/{owner}/{name}/{num}?pr={pr}",
            arguments = listOf(navArgument("pr") { type = NavType.StringType; defaultValue = "0" }),
        ) { e -> IssueScreen(nav, e.arg("owner"), e.arg("name"), e.arg("num"), e.arg("pr") == "1") }
    }
}

fun fileRoute(full: String, path: String, ref: String) =
    "file/$full?path=${Uri.encode(path)}&ref=${Uri.encode(ref)}"
