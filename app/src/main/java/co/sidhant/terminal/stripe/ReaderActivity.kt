package co.sidhant.terminal.stripe

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import co.sidhant.terminal.ui.theme.TerminalIntegrationTheme
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                    // A text composable that says "Hello World"
                    Text("Hello World")
                    Button(onClick = {
                        createPaymentIntent()
                    }) {
                        Text("Create Payment Intent")
                    }



                }
            }
        }
        isApplicationDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        TerminalManager.initTerminal(applicationContext)
        onDiscoverReaders()
    }
    fun createPaymentIntent() {
        val params = PaymentIntentParameters.Builder()
            .setAmount(1000L)
            .setCurrency("usd")
            .build()
        TerminalManager.terminal.createPaymentIntent(params, object : PaymentIntentCallback {
            override fun onFailure(e: TerminalException) {
                Log.e("ReaderActivity", e.errorMessage)
            }

            override fun onSuccess(paymentIntent: PaymentIntent) {
                Log.v("ReaderActivity", "Successfully created payment intent: ${paymentIntent.id}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(applicationContext, "Successfully created payment intent: ${paymentIntent.id}", Toast.LENGTH_LONG).show()
                }
                val cancelable = TerminalManager.terminal.collectPaymentMethod(paymentIntent,
                    object : PaymentIntentCallback {
                        override fun onSuccess(paymentIntent: PaymentIntent) {
                            val pm = paymentIntent.paymentMethod
                            val card = pm?.cardPresentDetails ?: pm?.interacPresentDetails
                            Log.v("ReaderActivity", "PI status: ${paymentIntent.status}")
                            Log.v("ReaderActivity", "Successfully collected payment method: ${paymentIntent.id}")
                            Log.v("ReaderActivity", "Successfully collected payment method: ${pm?.cardDetails}")
                            Log.v("ReaderActivity", "Successfully collected payment method: ${card?.brand} ${card?.cardholderName}")
                            // Placeholder for business logic on card before processing paymentIntent
                        }

                        override fun onFailure(exception: TerminalException) {
                            // Placeholder for handling exception
                        }
                    })
                //TODO: confirm payment use terminal test app and figure out how that's happening there
            }
        })
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
        val config = ConnectionConfiguration.LocalMobileConnectionConfiguration("tml_FONFQqvv1TShhy")
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