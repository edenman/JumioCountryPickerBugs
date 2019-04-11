package co.recharge.jumiocountrypickerbugs

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class MyApp : Application() {
  override fun onCreate() {
    super.onCreate()
    Timber.plant(DebugTree())
  }
}
