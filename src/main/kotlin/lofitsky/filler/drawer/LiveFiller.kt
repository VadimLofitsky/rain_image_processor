package lofitsky.filler.drawer

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.image.PixelReader
import javafx.scene.image.PixelWriter
import javafx.scene.paint.Color
import lofitsky.filler.common.FieldInfo
import lofitsky.filler.drawer.AbstractLiveDrawer.Companion.LiveDrawerStop
import java.io.File
import java.nio.IntBuffer
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.measureTimeMillis

class LiveFiller(private var canvas: Canvas) : AbstractLiveDrawer() {
    private var gc: GraphicsContext = canvas.graphicsContext2D
    private var pixelWriter: PixelWriter = gc.pixelWriter
    private lateinit var srcImg: Image
    private lateinit var pixelReader: PixelReader
    private lateinit var buffer: IntBuffer

    private var imgW by Delegates.notNull<Int>()
    private var imgH by Delegates.notNull<Int>()
    private var imgH_1 by Delegates.notNull<Int>()

    private val BLACK: Int = 255.shl(24)
    private val RED: Int = BLACK.or(255)
    private val GREEN: Int = BLACK.or(255.shl(8))
    private val BLUE: Int = BLACK.or(255.shl(16))
    private var fillColor: Int = GREEN
    private var borderColor: Int = Int.MAX_VALUE

    private fun prepare(file: File): Unit {
        file.inputStream().use { ins ->
            srcImg = Image(ins)
            imgW = srcImg.width.toInt()
            imgH = srcImg.height.toInt()
            imgH_1 = imgH - 1
            pixelReader = srcImg.pixelReader
            buffer = IntBuffer.allocate(imgW * imgH)
            pixelReader.getPixels(0, 0, imgW, imgH, PixelFormat.getIntArgbInstance(), buffer, imgW)
            pixelWriter.setPixels(0, 0, imgW, imgH, pixelReader, 0, 0)
        }
    }

    private val scanQueue = HashSet<Pair<Int, Int>>()
    private lateinit var fields: LinkedList<Triple<Int, Int, Int>>

    private fun getArgb(x: Int, y: Int): Int = buffer[y * imgW + x]
    private fun setArgb(x: Int, y: Int, color: Int): Unit {
        pixelWriter.setArgb(x, y, color)
        buffer.put(y * imgW + x, color)
    }

    private fun scan(): Unit {
        fields = LinkedList<Triple<Int, Int, Int>>()
        try {
            (0 until imgH).forEach { y ->
                var x = 0
                while(x < imgW) {
                    waiter()

                    val color = getArgb(x, y)
                    if(color != BLACK && color != fillColor) {

                        val fieldInfo = FieldInfo()

                        processLine(x, y, fieldInfo)
                        while(scanQueue.isNotEmpty()) {
                            val last = scanQueue.last()
                            scanQueue.remove(last)
                            val (xx, yy) = last
//                        println("processLine($xx, $yy)")
                            processLine(xx, yy, fieldInfo)
//                        Thread.sleep(50)
                        }

                        if(fieldInfo.weight != 0) {
                            fields.add(
                                Triple(
                                    fieldInfo.massCenter.first.toInt(),
                                    fieldInfo.massCenter.second.toInt(),
                                    fieldInfo.weight
                                )
                            )
                            println("#${fields.size}: ${fields.last.first}, ${fields.last.second} = ${fields.last.third}")
                            gc.fillText(
                                "#${fields.size}: ${fields.last.first}, ${fields.last.second} = ${fields.last.third}",
                                fieldInfo.massCenter.first,
                                fieldInfo.massCenter.second
                            )
                        }
                    }

                    ++x
                }
            }
        } catch(e: LiveDrawerStop) {
            return
        }
    }

    private fun processLine(x: Int, y: Int, fieldInfo: FieldInfo): Unit {
        var xx = x
        var color = getArgb(xx, y)
        var nearLinePixelColor: Int

        while(xx >= 0 && color != BLACK && color != fillColor && color != borderColor) {
            waiter()

            setArgb(xx, y, fillColor)
            fieldInfo.add(xx, y)

            if(!scanQueue.contains(xx to y - 1) && y > 0) {
                nearLinePixelColor = getArgb(xx, y - 1)
                if(nearLinePixelColor != BLACK && nearLinePixelColor != fillColor && nearLinePixelColor != borderColor) scanQueue.add(xx to y - 1)
            }

            if(!scanQueue.contains(xx to y + 1) && y < imgH_1) {
                nearLinePixelColor = getArgb(xx, y + 1)
                if(nearLinePixelColor != BLACK && nearLinePixelColor != fillColor && nearLinePixelColor != borderColor) scanQueue.add(xx to y + 1)
            }

            --xx
            if(x >= 0) color = getArgb(xx, y)
        }
        if(color == BLACK || xx == -1) setArgb(xx + 1, y, borderColor)

        xx = x + 1
        color = if(xx < imgW) getArgb(xx, y) else BLUE
        while(xx < imgW && color != BLACK && color != fillColor && color != borderColor) {
            waiter()

            setArgb(xx, y, fillColor)
            fieldInfo.add(xx, y)

            if(!scanQueue.contains(xx to y - 1) && y > 0) {
                nearLinePixelColor = getArgb(xx, y - 1)
                if(nearLinePixelColor != BLACK && nearLinePixelColor != fillColor && nearLinePixelColor != borderColor) scanQueue.add(xx to y - 1)
            }

            if(!scanQueue.contains(xx to y + 1) && y < imgH_1) {
                nearLinePixelColor = getArgb(xx, y + 1)
                if(nearLinePixelColor != BLACK && nearLinePixelColor != fillColor && nearLinePixelColor != borderColor) scanQueue.add(xx to y + 1)
            }

            ++xx
            color = getArgb(xx, y)
        }
        if(color == BLACK || xx == imgW) setArgb(xx - 1, y, borderColor)
    }

    override fun draw(data: Any?) {
        Thread.sleep(1000)

        gc.fill = Color.WHITE
        val processedFiles = (data as? File)?.listFiles()?.sortedBy { it.name }?.take(1) ?: run {
            println("!".repeat(100))
            println("!".repeat(100))
            println("!".repeat(100))
            println("${this::class.simpleName}.draw(): Failed to cast argument to File: $data")
            println("!".repeat(100))
            println("!".repeat(100))
            println("!".repeat(100))
            return
        }

        processedFiles.forEach { file ->
            prepare(file)

            measureTimeMillis {
                println("Scanning ${file.name}")
                scan()
            }.also { println(it) }
        }
    }
}
