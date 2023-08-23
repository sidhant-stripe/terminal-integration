package co.sidhant.terminal

import android.app.Application
import com.stripe.stripeterminal.TerminalApplicationDelegate

class TerminalIntegration: Application() {
    override fun onCreate() {
        super.onCreate()
        TerminalApplicationDelegate.onCreate(this)
    }
}