package lofitsky.filler.processing

import java.awt.Color

internal fun Int.Companion.rgba(blue: Int, green: Int, red: Int, alpha: Int = 255): Int = alpha.shl(24) + blue.shl(16) + green.shl(8) + red
internal val Int.Companion.BLACK: Int by lazy { Int.rgba(0, 0, 0) }

val BLACK: Int = Int.BLACK
val RED: Int = Int.rgba(0, 0, 255)
val GREEN: Int = Int.rgba(0, 255, 0)
val BLUE: Int = Int.rgba(255, 0, 0)
val BLUE_AWT = Color(0, 0, 255).rgb
val FILL_COLOR: Int = GREEN
val BORDER_COLOR: Int = RED.or(BLUE)
val JOINED_GROUPS_COLOR: Int = Int.rgba(37, 233, 255)
val JOINED_GROUPS_COLOR_AWT: Color = Color(255, 233, 37)
val RAIN_PATH_COLOR_AWT: Color = Color(236, 133, 38)
