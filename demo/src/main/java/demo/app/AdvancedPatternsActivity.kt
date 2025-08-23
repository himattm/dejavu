package demo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import demo.app.ui.AdvancedPatternsScreen

class AdvancedPatternsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { AdvancedPatternsScreen() }
  }
}
