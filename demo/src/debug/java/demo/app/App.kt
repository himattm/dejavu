package demo.app

import android.app.Application
import dejavu.Dejavu

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    Dejavu.enable(app = this, logToLogcat = true)
  }
}
