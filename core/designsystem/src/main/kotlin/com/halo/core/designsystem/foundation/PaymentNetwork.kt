package com.halo.core.designsystem.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.halo.core.designsystem.theme.HaloAccentColors
import com.halo.core.designsystem.theme.HaloTheme

/**
 * The network a payment UI represents (decision D8): colour carries meaning — on-chain is Bitcoin orange,
 * Lightning is brand yellow. A presentation enum: features map their domain network onto it.
 */
enum class HaloPaymentNetwork { Onchain, Lightning }

/** The accent family that represents [network]. */
@Composable
@ReadOnlyComposable
fun networkColors(network: HaloPaymentNetwork): HaloAccentColors = when (network) {
    HaloPaymentNetwork.Onchain -> HaloTheme.colors.bitcoin
    HaloPaymentNetwork.Lightning -> HaloTheme.colors.lightning
}
