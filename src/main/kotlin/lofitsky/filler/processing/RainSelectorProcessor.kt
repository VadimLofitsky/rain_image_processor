package lofitsky.filler.processing

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import lofitsky.filler.FillerApp.Companion.IMAGES_ROOT
import lofitsky.filler.FillerApp.Companion.IMG_TYPE
import lofitsky.filler.FillerApp.Companion.WORK_IMG_H
import lofitsky.filler.FillerApp.Companion.WORK_IMG_W
import lofitsky.filler.processing.ProcessingConveyor.Companion.fixedThreadPoolContext
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.abs

class RainSelectorProcessor : IProcessor {
    override fun process(data: Any?): Any? {
        val files = (data as? Path)?.toFile()?.listFiles() ?: run {
            log("!!!!!!! Failed to cast processor input data to Path (path to source images): $data")
            return data
        }

        if(files.isEmpty()) return null

        val workDirFile = IMAGES_ROOT.resolve("processed1").toFile().also { it.mkdir() }
        workDirFile.listFiles().forEach { it.delete() }

        return files
//                .take(1)
            .sortedBy { it.name }
            .toList()
            .map { file ->
                print("Processing $file...")
                val bImg = ImageIO.read(file).getSubimage(0, 40, WORK_IMG_W, WORK_IMG_H)

                runBlocking(fixedThreadPoolContext) {
                    (0 until WORK_IMG_W)
                        .map { x ->
                            async {
                                (0 until WORK_IMG_H).forEach { y ->
                                    bImg.setRGB(x, y, calcColor(bImg.getRGB(x, y)))
                                }
                            }
                        }.forEach { it.await() }
                }

                val path = workDirFile
                    .resolve(file.nameWithoutExtension + ".$IMG_TYPE")
                    .toString()

                val tmpBuffImg = BufferedImage(WORK_IMG_W, WORK_IMG_H, BufferedImage.TYPE_INT_RGB)
                tmpBuffImg.createGraphics().drawImage(bImg, 0, 0, null)
                val processedFile = File(path)
                if(ImageIO.write(tmpBuffImg, IMG_TYPE, processedFile)) {
                    print("finished\n")
                    println("Saved to $path")
                    processedFile.absolutePath
                } else {
                    print("finished\n")
                    println("-------------------- Failed to saved!")
                    null
                }
            }
    }

    private fun calcColor(rgb: Int): Int {
        val rgbComponents = (0..2).map { rgb.shr(8 * it).and(0xff) }

        val absColor = rgbComponents.sumOf { it * it }.toDouble()
        if(absColor < 3600) return 0

        val avg = rgbComponents.average()
        return if(rgbComponents.all { abs(it - avg) / avg < .05 }) 0 else BLUE_AWT
    }
}
