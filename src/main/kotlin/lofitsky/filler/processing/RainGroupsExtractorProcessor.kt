package lofitsky.filler.processing

import javafx.scene.image.Image
import javafx.scene.image.PixelReader
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import lofitsky.filler.FillerApp.Companion.IMAGES_ROOT
import lofitsky.filler.common.FieldInfo
import lofitsky.filler.processing.ProcessingConveyor.Companion.fixedThreadPoolContext
import java.io.File
import java.util.*
import kotlin.properties.Delegates
import kotlin.system.measureTimeMillis

class RainGroupsExtractorProcessor : IProcessor {
    override fun process(data: Any?): Any? {
        val files = (data as? List<String?>)?.filterNotNull()?.map { File(it) } ?: run {
            log("!!!!!!! Failed to cast processor input data to List<String?> (images paths with rain only): $data")
            return data
        }

        if(files.isEmpty()) return null

        println("${this::class.simpleName}: found fields:")
        val allFramesFieldsMap = processFiles(files).onEach { (fn, fs) ->
            println("\n$fn")
            fs.forEachIndexed { i, fi -> println("\t#${i + 1}: $fi") }
        }

        return allFramesFieldsMap
    }

    companion object {
        private class Filler(file: File) {
            var imgW by Delegates.notNull<Int>()
                private set
            var imgH by Delegates.notNull<Int>()
                private set
            private var imgH_1 by Delegates.notNull<Int>()

            private var srcImg: Image
            var img: WritableImage
                private set

            private var pixelReader: PixelReader
            private var pixelWriter: PixelWriter

            private val scanQueue = LinkedHashSet<Pair<Int, Int>>()

            init {
                file.inputStream().use { ins ->
                    srcImg = Image(ins)
                    imgW = srcImg.width.toInt()
                    imgH = srcImg.height.toInt()
                    imgH_1 = imgH - 1
                    img = WritableImage(srcImg.pixelReader, imgW, imgH)
                    pixelReader = img.pixelReader
                    pixelWriter = img.pixelWriter
                }
            }

            fun scan(): List<FieldInfo> {
                val fields = LinkedList<FieldInfo>()

                (0 until imgH).forEach { y ->
                    var x = 0
                    while(x < imgW) {
                        val color = pixelReader.getArgb(x, y)
                        if(color != BLACK && color != FILL_COLOR) {
                            val fieldInfo = FieldInfo()

                            processLine(x, y, fieldInfo)
                            while(scanQueue.isNotEmpty()) {
                                val last = scanQueue.last()
                                val (xx, yy) = last
                                scanQueue.remove(last)
                                processLine(xx, yy, fieldInfo)
                            }

                            if(fieldInfo.weight != 0) fields.add(fieldInfo)
                        }

                        ++x
                    }
                }

                return fields.toList()
            }

            private fun processLine(x: Int, y: Int, fieldInfo: FieldInfo): Unit {
                var xx = x
                var color = pixelReader.getArgb(xx, y)
                var nearLinePixelColor: Int

                while(xx >= 0 && color != BLACK && color != FILL_COLOR && color != BORDER_COLOR) {
                    pixelWriter.setArgb(xx, y, FILL_COLOR)
                    fieldInfo.add(xx, y)

                    if(!scanQueue.contains(xx to y - 1) && y > 0) {
                        nearLinePixelColor = pixelReader.getArgb(xx, y - 1)
                        if(nearLinePixelColor != BLACK && nearLinePixelColor != FILL_COLOR && nearLinePixelColor != BORDER_COLOR) scanQueue.add(
                            xx to y - 1
                        )
                    }

                    if(!scanQueue.contains(xx to y + 1) && y < imgH_1) {
                        nearLinePixelColor = pixelReader.getArgb(xx, y + 1)
                        if(nearLinePixelColor != BLACK && nearLinePixelColor != FILL_COLOR && nearLinePixelColor != BORDER_COLOR) scanQueue.add(
                            xx to y + 1
                        )
                    }

                    --xx
                    if(xx >= 0) color = pixelReader.getArgb(xx, y)
                }
                if(color == BLACK || xx == -1) {
                    pixelWriter.setArgb(xx + 1, y, BORDER_COLOR)
                    fieldInfo.border.add(xx + 1 to y)
                }

                xx = x + 1
                color = if(xx < imgW) pixelReader.getArgb(xx, y) else BLUE
                while(xx < imgW && color != BLACK && color != FILL_COLOR && color != BORDER_COLOR) {
                    pixelWriter.setArgb(xx, y, FILL_COLOR)
                    fieldInfo.add(xx, y)

                    if(!scanQueue.contains(xx to y - 1) && y > 0) {
                        nearLinePixelColor = pixelReader.getArgb(xx, y - 1)
                        if(nearLinePixelColor != BLACK && nearLinePixelColor != FILL_COLOR && nearLinePixelColor != BORDER_COLOR) scanQueue.add(
                            xx to y - 1
                        )
                    }

                    if(!scanQueue.contains(xx to y + 1) && y < imgH_1) {
                        nearLinePixelColor = pixelReader.getArgb(xx, y + 1)
                        if(nearLinePixelColor != BLACK && nearLinePixelColor != FILL_COLOR && nearLinePixelColor != BORDER_COLOR) scanQueue.add(
                            xx to y + 1
                        )
                    }

                    ++xx
                    if(xx < imgW) color = pixelReader.getArgb(xx, y)
                }
                if(color == BLACK || xx == imgW) {
                    pixelWriter.setArgb(xx - 1, y, BORDER_COLOR)
                    fieldInfo.border.add(xx - 1 to y)
                }
            }
        }
    }

    private fun processFiles(files: List<File>): Map<String, List<FieldInfo>> {
        val workDirFile = IMAGES_ROOT.resolve("processed2").toFile().also { it.mkdir() }
        workDirFile.listFiles().forEach { it.delete() }

        val processedFiles = files.sortedBy { it.name }
//            .take(1)
        println(">>> ${this::class.simpleName}: Started at ${Date()}")
        return runBlocking(fixedThreadPoolContext) {
            processedFiles.map { file ->
                async {
                    val filler = Filler(file)
                    var fields: List<FieldInfo>
                    measureTimeMillis {
                        fields = filler.scan()
                    }.also { println("Scanned ${file.name} for $it ms") }

                    file.absolutePath to fields
                }
            }.associate { it.await() }
            .also { println(">>> ${this::class.simpleName}: Finished at ${Date()}") }
            .also { println("-".repeat(100)) }
        }
    }
}
