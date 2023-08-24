package co.sidhant.terminal.stripe

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import co.sidhant.terminal.ui.theme.TerminalIntegrationTheme
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlin.properties.Delegates

class ReaderActivity: ComponentActivity() {
    private var discoveryCancelable: Cancelable? = null
    private val locationId = "tml_FONFQqvv1TShhy"
    private var isApplicationDebuggable by Delegates.notNull<Boolean>()
    private val readerList = mutableListOf<Reader>()
    private var ttpaConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            TerminalIntegrationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text("Hello World")
                }
            }
        }
        isApplicationDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        onDiscoverReaders()
    }

    fun onDiscoverReaders() {
        val config = DiscoveryConfiguration(
            timeout = 0,
            discoveryMethod = DiscoveryMethod.LOCAL_MOBILE,
            isSimulated = isApplicationDebuggable,
            location = locationId
        )
        // Save this cancelable to an instance variable
        discoveryCancelable = TerminalManager.terminal.discoverReaders(config,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    readers.forEach { reader ->
                        Log.v("ReaderActivity", "Discovered reader: ${reader.deviceType.name}")
                        readerList.add(reader)
                    }
                    connectToTtpaReader()
                }
            },
            object : Callback {
            override fun onSuccess() {
                Log.v("ReaderActivity","Finished discovering readers, attempting to connect")
                connectToTtpaReader()
            }

            override fun onFailure(e: TerminalException) {
                Log.e("ReaderActivity", e.errorMessage)
                e.printStackTrace()
            }
        })
    }

    fun connectReader(reader: Reader) {
        // after selecting a reader to connect to
        val config = ConnectionConfiguration.LocalMobileConnectionConfiguration("locationId")
        TerminalManager.terminal.connectLocalMobileReader(reader, config, object :
            ReaderCallback {
            override fun onSuccess(r: Reader) {
                Log.v("ReaderActivity", "Connected to mobile device")
            }

            override fun onFailure(e: TerminalException) {
                e.printStackTrace()
            }
        })
    }

    fun connectToTtpaReader() {
        if(!ttpaConnected) {
            Log.v("ReaderActivity", "Attempting to connect to TTPA reader")
            val ttpaReader = readerList.firstOrNull { it.deviceType == DeviceType.COTS_DEVICE }
            ttpaReader?.let {
                connectReader(it)
                ttpaConnected = true
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // If you're leaving the activity or fragment without selecting a reader,
        // make sure you cancel the discovery process or the SDK will be stuck in
        // a discover readers phase
        discoveryCancelable?.cancel(object : Callback {
            override fun onSuccess() { }
            override fun onFailure(e: TerminalException) { }
        })
    }

}