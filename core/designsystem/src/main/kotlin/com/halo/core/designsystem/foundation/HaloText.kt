package com.halo.core.designsystem.foundation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.halo.core.designsystem.theme.HaloTheme
import com.halo.core.designsystem.theme.LocalHaloContentColor

/**
 * Text with Halo defaults: trimmed line boxes (from the type tokens) and a color inherited from the
 * container via [LocalHaloContentColor]. Numbers that are values must use a Paper Mono style.
 */
@Composable
fun HaloText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = HaloTheme.typography.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = resolve(style, color, textAlign),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

@Composable
fun HaloText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = HaloTheme.typography.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = resolve(style, color, textAlign),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

@Composable
private fun resolve(style: TextStyle, color: Color, textAlign: TextAlign): TextStyle {
    val resolvedColor = color.takeOrElse { style.color.takeOrElse { LocalHaloContentColor.current } }
    return style.copy(
        color = resolvedColor,
        textAlign = if (textAlign != TextAlign.Unspecified) textAlign else style.textAlign,
    )
}
