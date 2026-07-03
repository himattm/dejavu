import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import demo.app.navigation.DemoApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // CanvasBasedWindow was removed in Compose Multiplatform 1.11; ComposeViewport
    // is the replacement. It creates its own canvas inside the given container
    // element, so index.html now hosts a <div id="ComposeTarget"> instead of a <canvas>.
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        DemoApp()
    }
}
