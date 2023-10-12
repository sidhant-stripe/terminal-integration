package co.sidhant.terminal.stripe

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import co.sidhant.terminal.ui.theme.TerminalIntegrationTheme
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlin.properties.Delegates

class ReaderActivity : ComponentActivity() {
    private var discoveryCancelable: Cancelable? = null
    private val locationId = "tml_FONFQqvv1TShhy"
    private var isApplicationDebuggable by Delegates.notNull<Boolean>()
    private val readerList = mutableListOf<Reader>()
    private var ttpaConnected = false
    private var noActiveReader = false
    private var displayText = mutableStateOf("starting...")

    private val terminal get() = TerminalManager.terminal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var text by remember { displayText }
            TerminalIntegrationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Text(text)
                        Button(onClick = {
                            if(ttpaConnected) {
                                takePayment()
                            }
                            else {
                                connectToTtpaReader()
                            }
                        }) {
                            Text("Create Payment Intent")
                        }
                    }


                }
            }
        }
        isApplicationDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        TerminalManager.initTerminal(applicationContext)
        onDiscoverReaders()
    }

    fun takePayment(amountInCents: Long = 1000L ) {
        val params = PaymentIntentParameters.Builder()
            .setAmount(amountInCents)
            .setCurrency("usd")
            .build()
        terminal.createPaymentIntent(params, object : PaymentIntentCallback {
            override fun onFailure(e: TerminalException) {
                Log.e("ReaderActivity", e.errorMessage)
            }

            override fun onSuccess(paymentIntent: PaymentIntent) {
                Log.v("ReaderActivity", "Successfully created payment intent: ${paymentIntent.id}")

                val cancelable = terminal.collectPaymentMethod(paymentIntent,
                    object : PaymentIntentCallback {
                        override fun onSuccess(paymentIntent: PaymentIntent) {
                            val pm = paymentIntent.paymentMethod
                            val card = pm?.cardPresentDetails ?: pm?.interacPresentDetails
                            Log.v("ReaderActivity", "PI status: ${paymentIntent.status}")
                            Log.v("ReaderActivity", "Successfully collected payment method: ${paymentIntent.id}")
                        }

                        override fun onFailure(e: TerminalException) {
                            // Placeholder for handling exception
                        }
                    }
                )
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun onDiscoverReaders() {
        // Save this cancelable to an instance variable
        discoveryCancelable = TerminalManager.terminal.discoverReaders(
            config = DiscoveryConfiguration.LocalMobileDiscoveryConfiguration(),
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
                    Log.v("ReaderActivity", "Finished discovering readers, attempting to connect")
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
        val config =
            ConnectionConfiguration.LocalMobileConnectionConfiguration("tml_FONFQqvv1TShhy")
        TerminalManager.terminal.connectLocalMobileReader(reader, config,
            object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    Log.v("SetupViewModel", "Connected to reader: ${reader.deviceType.name}")
                    displayText.value = "Connected to reader: ${reader.deviceType.name}"
                    if (!noActiveReader) {
                        noActiveReader = true
                        terminal.disconnectReader(
                            object : Callback {
                                override fun onSuccess() {
                                    Log.v("SetupViewModel", "Disconnected from reader")
                                    displayText.value = "Disconnected from reader"
                                    terminal.connectLocalMobileReader(
                                        reader,
                                        ConnectionConfiguration.LocalMobileConnectionConfiguration(
                                            locationId
                                        ),
                                        object : ReaderCallback {
                                            override fun onSuccess(reader: Reader) {
                                                displayText.value = "Connected to reader: ${reader.deviceType.name}"
                                                Log.v(
                                                    "SetupViewModel",
                                                    "Connected to reader: ${reader.deviceType.name}"
                                                )
                                            }

                                            override fun onFailure(e: TerminalException) {
                                                Log.v(
                                                    "SetupViewModel",
                                                    "failed to connect to reader: ${e.errorMessage}"
                                                )
                                                displayText.value = "failed to connect to reader: ${e.errorMessage}"
                                            }
                                        })
                                }

                                override fun onFailure(e: TerminalException) {
                                    Log.e(
                                        "SetupViewModel",
                                        "Failed to disconnect from reader: ${e.errorMessage}"
                                    )
                                }
                            })
                }

                }

                override fun onFailure(e: TerminalException) {
                }
            })
    }

    fun connectToTtpaReader() {
        if (!ttpaConnected) {
            Log.v("ReaderActivity", "Attempting to connect to TTPA reader")
            displayText.value = "Attempting to connect to TTPA reader"
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
            override fun onSuccess() {}
            override fun onFailure(e: TerminalException) {}
        })
    }

}