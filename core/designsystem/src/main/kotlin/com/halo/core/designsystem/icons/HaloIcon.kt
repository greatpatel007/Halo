package com.halo.core.designsystem.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor

/**
 * Draws a [HaloIcons] vector tinted with the inherited content color.
 * @param contentDescription null for decorative icons (the parent describes the action).
 */
@Composable
fun HaloIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = HaloTheme.sizes.iconLg,
) {
    val painter = rememberVectorPainter(icon)
    val color = tint.takeOrElse { LocalHaloContentColor.current }
    val filter = remember(color) { ColorFilter.tint(color) }
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .size(size)
            .then(semantics)
            .paint(painter, colorFilter = filter, contentScale = ContentScale.Fit),
    )
}
