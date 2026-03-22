import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import demo.app.navigation.DemoApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dejavu Demo",
    ) {
        DemoApp()
    }
}
