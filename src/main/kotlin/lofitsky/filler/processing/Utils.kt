package lofitsky.filler.processing

import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import lofitsky.filler.FillerApp.Companion.SCALE0
import java.io.File
import kotlin.math.pow

internal val File.writableImage: WritableImage get() = inputStream().use { ins ->
        val srcImg = Image(ins)
        WritableImage(srcImg.pixelReader, srcImg.width.toInt(), srcImg.height.toInt())
    }

internal fun File.writableImage(x: Int, y: Int, width: Int, height: Int): WritableImage = inputStream().use { ins ->
        WritableImage(Image(ins).pixelReader, x, y, width, height)
    }

internal val imgFilenameRe: Regex = Regex("""^.*/id\d+-z(.*)-n\d+-\d+-\d+\..*$""")
internal val String.zoom: Int get() = imgFilenameRe.find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 0

internal fun zoomFactor2(zoom: Int): Double = 2.0.pow(zoom).pow(2)
internal fun zoomedScale(zoom: Int): Double = SCALE0 / 2.0.pow(zoom)

internal val String.filename: String get() = substringAfterLast(File.separator)
internal val String.filenameWithoutExt: String get() = filename.substringBeforeLast(".")
