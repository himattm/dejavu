package demo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import demo.app.ui.SharedStateStressScreen

class SharedStateStressActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { SharedStateStressScreen() }
  }
}
