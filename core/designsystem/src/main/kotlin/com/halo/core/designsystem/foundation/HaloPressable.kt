package com.halo.core.designsystem.foundation

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.halo.core.designsystem.theme.HaloHaptic
import com.halo.core.designsystem.theme.HaloTheme

/**
 * The single entry point for anything tappable. Bundles the four things every press must do:
 * 1. scale down (Snap) and back (Quick) — [pressScale]
 * 2. bloom a touch-origin highlight clipped to [shape] — [HaloIndication]
 * 3. play a haptic token on click — [haptic]
 * 4. expose accessibility semantics — [role], [onClickLabel]
 *
 * Pass your own [interactionSource] to drive extra press-reactive visuals (e.g. a corner morph).
 */
@Composable
fun HaloPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    haptic: HaloHaptic? = HaloHaptic.Tap,
    pressedScale: Float = HALO_PRESSED_SCALE,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = remember(shape) { HaloIndication(shape) },
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val haptics = HaloTheme.haptics
    Box(
        modifier = Modifier
            .pressScale(source, if (enabled) pressedScale else 1f)
            .then(modifier)
            .clickable(
                interactionSource = source,
                indication = indication,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
            ) {
                haptic?.let(haptics::perform)
                onClick()
            },
        contentAlignment = contentAlignment,
        propagateMinConstraints = true,
        content = content,
    )
}
