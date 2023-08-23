package co.sidhant.terminal.stripe

import com.stripe.Stripe
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import co.sidhant.terminal.BuildConfig.STRIPE_SECRET_KEY
import com.stripe.model.terminal.ConnectionToken
import com.stripe.param.terminal.ConnectionTokenCreateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TokenProvider: ConnectionTokenProvider {
    init {
        Stripe.apiKey = STRIPE_SECRET_KEY
    }

    val params = ConnectionTokenCreateParams.builder().build()

    private suspend fun getConnectionToken(): ConnectionToken {
        return ConnectionToken.create(params)
    }

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val secret = getConnectionToken().secret
                callback.onSuccess(secret)
            }
        } catch (e: Exception) {
            callback.onFailure(
                ConnectionTokenException("Failed to fetch connection token", e)
            )
        }
    }


}