package co.sidhant.terminal.stripe

import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.log.LogLevel

object TerminalManager {
    private val listener = object: TerminalListener {

        override fun onUnexpectedReaderDisconnect(reader: Reader) {
            TODO("Not yet implemented")
        }
    }

    private val tokenProvider = TokenProvider()

    fun initTerminal(context: Context) {
        if(!Terminal.isInitialized()) {
            Terminal.initTerminal(context, LogLevel.VERBOSE, tokenProvider, listener)
        }
    }
}