package lofitsky.filler.processing

import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import lofitsky.filler.FillerApp.Companion.IMAGES_ROOT
import lofitsky.filler.FillerApp.Companion.IMG_TYPE
import lofitsky.filler.FillerApp.Companion.WORK_IMG_H
import lofitsky.filler.FillerApp.Companion.WORK_IMG_W
import lofitsky.filler.common.FieldInfo
import lofitsky.filler.processing.ProcessingConveyor.Companion.fixedThreadPoolContext
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.sqrt

class RainGroupsDrawerProcessor : IProcessor {
    override fun process(data: Any?): Any? {
        val allFramesFieldsMap = (data as? Map<String, List<FieldInfo>>) ?: run {
            log("!!!!!!! Failed to cast processor input data to Map<String, List<FieldInfo>> (frames with extracted rain fields): $data")
            return data
        }

        processFields(allFramesFieldsMap)

        return allFramesFieldsMap
    }

    private fun processFields(allFramesFieldsMap: Map<String, List<FieldInfo>>): Unit {
        val workDirFile = IMAGES_ROOT.resolve("processed3").toFile().also { it.mkdir() }
        workDirFile.listFiles().forEach { it.delete() }

        println(">>> ${this::class.simpleName}: Started at ${Date()}")
        return runBlocking(fixedThreadPoolContext) {
            allFramesFieldsMap.entries.map { entry ->
                async {
                    val fields = entry.value

                    val filename = entry.key
                    val img = File(filename).writableImage

                    val tmpBuffImg = BufferedImage(WORK_IMG_W, WORK_IMG_H, BufferedImage.TYPE_INT_RGB)
                    val image = SwingFXUtils.fromFXImage(img, tmpBuffImg)

                    val gc = image.graphics
                    val prevColor = gc.color
                    gc.color = java.awt.Color.RED
                    fields.forEachIndexed { i, t ->
                        val x = t.massCenter.first.toInt()
                        val y = t.massCenter.second.toInt()
                        gc.drawString("#${i + 1}: ($x, $y) = ${t.weight}", x, y)
                        if(x - 1 >= 0) image.setRGB(x - 1, y, RED)
                        if(y - 1 >= 0) image.setRGB(x, y - 1, RED)
                        image.setRGB(x, y, RED)
                        if(x + 1 < WORK_IMG_W) image.setRGB(x + 1, y, RED)
                        if(x + 1 < WORK_IMG_H) image.setRGB(x, y + 1, RED)
                        val r = sqrt(t.weight.toDouble()).toInt()
                        val d = 2 * r
                        gc.drawArc(x - r, y - r, d, d, 0, 360)
                    }

                    gc.color = prevColor
                    val newFile = workDirFile.resolve(filename.filename).also { it.createNewFile() }
                    tmpBuffImg.createGraphics().drawImage(image, 0, 0, null)
                    ImageIO.write(tmpBuffImg, IMG_TYPE, newFile)
                }
            }.forEach { it.await() }
            .also { println(">>> ${this::class.simpleName}: Finished at ${Date()}") }
            .also { println("-".repeat(100)) }
        }
    }
}
