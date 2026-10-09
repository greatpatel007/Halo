package com.halo.core.designsystem.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.halo.core.designsystem.theme.HaloTheme

/** Wraps @Preview content in the theme on the canvas color, so previews need no literal colors. */
@Composable
fun HaloPreview(content: @Composable ColumnScope.() -> Unit) {
    HaloTheme {
        Column(
            Modifier
                .background(HaloTheme.colors.bg.canvas)
                .padding(HaloTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(HaloTheme.spacing.md),
            content = content,
        )
    }
}
