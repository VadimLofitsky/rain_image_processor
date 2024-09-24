package lofitsky.filler.drawer

import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.PixelWriter
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text
import lofitsky.filler.drawer.AbstractLiveDrawer.Companion.LiveDrawerStop
import lofitsky.filler.drawer.AbstractLiveDrawer.Companion.ThreadCommand
import java.io.File

class CycleDrawer(private val canvas: Canvas) : AbstractLiveDrawer() {
    private val gc = canvas.graphicsContext2D
    private var winPixelWriter: PixelWriter = gc.pixelWriter

    override fun draw(data: Any?): Unit {
        try {
            val processedFiles = (data as? File)?.listFiles()?.sortedBy { it.name } ?: run {
                println("!".repeat(100))
                println("!".repeat(100))
                println("!".repeat(100))
                println("${this::class.simpleName}.draw(): Failed to cast argument to File: $data")
                println("!".repeat(100))
                println("!".repeat(100))
                println("!".repeat(100))
                return
            }

            gc.font = Font.font("JetBrains Mono Regular", 16.0)

            while(true) {
                processedFiles.forEach { file ->
                    file.inputStream().use { ins ->
                        val img = Image(ins)
                        winPixelWriter.setPixels(0, 0, img.width.toInt(), img.height.toInt(), img.pixelReader, 0, 0)

                        val text = Text(file.absolutePath).apply { font = gc.font }
                        val textWidth = text.layoutBounds.width
                        val textHeight = text.layoutBounds.height
                        val textX0 = (canvas.width - textWidth) / 2

                        gc.fill = Color.WHITESMOKE
                        gc.fillRect(textX0 - 3.0, 1000.0 - textHeight + 3.0, textWidth + 6.0, textHeight + 2.0)

                        gc.fill = Color.BLACK
                        gc.fillText(file.absolutePath, textX0, 1000.0)

                        waiter()

                        Thread.sleep(500)
                        Thread.sleep(0, 1);
                    }
                }
            }
        } catch(e: LiveDrawerStop) {
            if(threadCommand == ThreadCommand.STOP) return
        } catch(e: InterruptedException) {
            if(threadCommand == ThreadCommand.STOP) return
        }
    }
}
