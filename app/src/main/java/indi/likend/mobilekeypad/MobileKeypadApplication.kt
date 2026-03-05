package indi.likend.mobilekeypad

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import indi.likend.mobilekeypad.data.utils.ActivityStateMonitor
import javax.inject.Inject

@HiltAndroidApp
class MobileKeypadApplication : Application() {
    @Inject
    lateinit var monitor: ActivityStateMonitor
}
