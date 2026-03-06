package demo.app

import androidx.compose.ui.window.ComposeUIViewController
import dejavu.Dejavu
import demo.app.navigation.DemoApp

fun MainViewController(): platform.UIKit.UIViewController {
    Dejavu.enable(logToNSLog = true)
    return ComposeUIViewController { DemoApp() }
}
