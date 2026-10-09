package com.halo.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn icon set. 24dp grid, 2dp optical padding (live area 2..22), 1.75 stroke, round caps and
 * joins. Paths are authored in SVG path syntax so they read like the spec. Tint is applied at draw time
 * (see [HaloIcon]); the black here is only a placeholder paint.
 */
object HaloIcons {
    val Send: ImageVector by lazy { icon("send", "M7 17L17 7", "M8.5 7H17V15.5") }
    val Receive: ImageVector by lazy { icon("receive", "M17 7L7 17", "M15.5 17H7V8.5") }
    val Scan: ImageVector by lazy {
        icon(
            "scan",
            "M4 9V6.5A2.5 2.5 0 0 1 6.5 4H9",
            "M15 4H17.5A2.5 2.5 0 0 1 20 6.5V9",
            "M20 15V17.5A2.5 2.5 0 0 1 17.5 20H15",
            "M9 20H6.5A2.5 2.5 0 0 1 4 17.5V15",
            "M7.5 12H16.5",
        )
    }
    val Swap: ImageVector by lazy {
        icon("swap", "M16 4L19 7L16 10", "M5 7H19", "M8 14L5 17L8 20", "M19 17H5")
    }
    val Bolt: ImageVector by lazy { icon("bolt", "M13 3L5 13.5H11.5L10.5 21L19 10.5H12.5L13 3Z") }
    val Bitcoin: ImageVector by lazy {
        icon(
            "bitcoin",
            "M8 5V19",
            "M6 5H13.25A3.5 3.5 0 0 1 13.25 12H8",
            "M8 12H14.25A3.5 3.5 0 0 1 14.25 19H6",
            "M10 3V5", "M13 3V5", "M10 19V21", "M13 19V21",
        )
    }
    val Copy: ImageVector by lazy {
        icon(
            "copy",
            "M11 9H17A2 2 0 0 1 19 11V17A2 2 0 0 1 17 19H11A2 2 0 0 1 9 17V11A2 2 0 0 1 11 9Z",
            "M15 9V7A2 2 0 0 0 13 5H7A2 2 0 0 0 5 7V13A2 2 0 0 0 7 15H9",
        )
    }
    val Check: ImageVector by lazy { icon("check", "M5 12.5L10 17.5L19 7.5") }
    val Close: ImageVector by lazy { icon("close", "M6 6L18 18", "M18 6L6 18") }
    val Back: ImageVector by lazy { icon("back", "M19 12H5", "M11 6L5 12L11 18") }
    val ChevronRight: ImageVector by lazy { icon("chevron_right", "M9.5 6L15.5 12L9.5 18") }
    val ChevronDown: ImageVector by lazy { icon("chevron_down", "M6 9.5L12 15.5L18 9.5") }
    val Settings: ImageVector by lazy {
        icon(
            "settings",
            "M4 8H11.5", "M16.5 8H20", "M16.5 8A2.5 2.5 0 1 1 11.5 8A2.5 2.5 0 1 1 16.5 8Z",
            "M4 16H7.5", "M12.5 16H20", "M12.5 16A2.5 2.5 0 1 1 7.5 16A2.5 2.5 0 1 1 12.5 16Z",
        )
    }
    val Eye: ImageVector by lazy {
        icon(
            "eye",
            "M2.5 12C4.5 7.5 8 5 12 5C16 5 19.5 7.5 21.5 12C19.5 16.5 16 19 12 19C8 19 4.5 16.5 2.5 12Z",
            "M15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12Z",
        )
    }
    val EyeOff: ImageVector by lazy {
        icon(
            "eye_off",
            "M2.5 12C4.5 7.5 8 5 12 5C16 5 19.5 7.5 21.5 12C19.5 16.5 16 19 12 19C8 19 4.5 16.5 2.5 12Z",
            "M15 12A3 3 0 1 1 9 12A3 3 0 1 1 15 12Z",
            "M4 4L20 20",
        )
    }
    val Clock: ImageVector by lazy {
        icon("clock", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12Z", "M12 7.5V12L15 14")
    }
    val Plus: ImageVector by lazy { icon("plus", "M12 5V19", "M5 12H19") }
    val Search: ImageVector by lazy {
        icon("search", "M17 11A6 6 0 1 1 5 11A6 6 0 1 1 17 11Z", "M15.5 15.5L20 20")
    }
    val Info: ImageVector by lazy {
        icon("info", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12Z", "M12 11V16.5", "M12 7.75V7.76")
    }
    val Warning: ImageVector by lazy {
        icon("warning", "M12 4L21 19.5H3L12 4Z", "M12 10V13.5", "M12 16.5V16.51")
    }
    val Grid: ImageVector by lazy {
        icon("grid", "M4 9H20", "M4 15H20", "M9 4V20", "M15 4V20")
    }
    val Gauge: ImageVector by lazy {
        icon("gauge", "M4.5 17A8.5 8.5 0 1 1 19.5 17", "M12 13L15.5 9.5")
    }
    val Wave: ImageVector by lazy {
        icon("wave", "M3 12C5 7 7 7 9 12C11 17 13 17 15 12C17 7 19 7 21 12")
    }

    /** Every icon with its name — drives the Showroom icon grid. */
    val all: List<Pair<String, ImageVector>> by lazy {
        listOf(
            "Send" to Send, "Receive" to Receive, "Scan" to Scan, "Swap" to Swap,
            "Bolt" to Bolt, "Bitcoin" to Bitcoin, "Copy" to Copy, "Check" to Check,
            "Close" to Close, "Back" to Back, "ChevronRight" to ChevronRight, "ChevronDown" to ChevronDown,
            "Settings" to Settings, "Eye" to Eye, "EyeOff" to EyeOff, "Clock" to Clock,
            "Plus" to Plus, "Search" to Search, "Info" to Info, "Warning" to Warning,
            "Grid" to Grid, "Gauge" to Gauge, "Wave" to Wave,
        )
    }
}

/** Icon stroke on the 24-unit viewport (matches `HaloStroke.icon`). */
private const val ICON_STROKE = 1.75f

private fun icon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = ICON_STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
