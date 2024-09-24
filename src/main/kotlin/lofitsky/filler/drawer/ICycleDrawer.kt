package lofitsky.filler.drawer

import java.util.concurrent.atomic.AtomicBoolean

interface ICycleDrawer {
    fun draw(pause: AtomicBoolean): Unit
}
