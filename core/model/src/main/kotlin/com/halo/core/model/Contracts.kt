package com.halo.core.model

/** Typed failures a wallet backend can report. UI maps each to copy + a recovery action. */
sealed class WalletError(message: String) : Exception(message) {
    class InsufficientFunds(val available: Sats, val requested: Sats) :
        WalletError("Insufficient funds: $available available, $requested requested")
    class InvalidDestination(val raw: String) : WalletError("Invalid destination")
    class RouteNotFound : WalletError("No Lightning route found")
    class Network(cause: Throwable? = null) : WalletError("Network unavailable") {
        init { cause?.let(::initCause) }
    }
}
