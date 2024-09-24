package lofitsky.filler.processing

import javafx.embed.swing.SwingFXUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import lofitsky.filler.FillerApp.Companion.IMAGES_ROOT
import lofitsky.filler.FillerApp.Companion.IMG_TYPE
import lofitsky.filler.FillerApp.Companion.SRC_IMG_OFFSET_X
import lofitsky.filler.FillerApp.Companion.SRC_IMG_OFFSET_Y
import lofitsky.filler.FillerApp.Companion.WORK_IMG_H
import lofitsky.filler.FillerApp.Companion.WORK_IMG_W
import lofitsky.filler.processing.ProcessingConveyor.Companion.fixedThreadPoolContext
import lofitsky.filler.processing.RainGroupsMatcherProcessor.Companion.RainGroupPath
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class RainPathDrawerProcessor : IProcessor {
    override fun process(data: Any?): Any? {
        val paths = (data as? List<RainGroupPath>) ?: run {
            log("!!!!!!! Failed to cast processor input data to List<RainGroupPath> (list of rain fields' paths): $data")
            return data
        }

        return processFrames(paths)
    }

/*
    private fun processFrames(allPaths: List<RainGroupPath>): File {
        val workDirFile = IMAGES_ROOT.resolve("processed5").toFile().also { it.mkdir() }
        workDirFile.listFiles().forEach { it.delete() }

        println(">>> ${this::class.simpleName}: Started at ${Date()}")

        // Map<FilenameWithoutExt -> <path_id to line>>
        val linesByFramesMap = allPaths
            .flatMap { p -> p.path.map { p.id to it } }
            .groupBy { it.second.file.filenameWithoutExt }

        runBlocking(fixedThreadPoolContext) {
            IMAGES_ROOT.resolve("src").toFile().listFiles()
                .map { srcImgFile ->
                    async {
                        val linePairs = linesByFramesMap[srcImgFile.nameWithoutExtension] ?: return@async
                        val img = srcImgFile.writableImage(0, 40, WORK_IMGW, WORK_IMGH)
                        val tmpBuffImg = BufferedImage(WORK_IMGW, WORK_IMGH, BufferedImage.TYPE_INT_RGB)
                        val image = SwingFXUtils.fromFXImage(img, tmpBuffImg)
                        val gc = image.graphics
                        gc.color = RAIN_PATH_COLOR_AWT
                        gc.font = gc.font.deriveFont(24f)

                        linePairs.forEach { linePair ->
                            val (p2, p1) = linePair.second.line
                            val (x1, y1) = p1 as Pair<Int, Int>
                            val (x2, y2) = p2 as Pair<Int, Int>
                            gc.drawLine(x1, y1, x2, y2)
                            gc.drawString(linePair.first.toString(), x2, y2)
                        }

                        val newFileName = "${srcImgFile.nameWithoutExtension}.$IMG_TYPE"
                        val newFile = workDirFile.resolve(newFileName).also { it.createNewFile() }
                        tmpBuffImg.createGraphics().drawImage(image, 0, 0, null)
                        ImageIO.write(tmpBuffImg, IMG_TYPE, newFile)
                    }
                }.awaitAll()
        }

        println(">>> ${this::class.simpleName}: Finished at ${Date()}")
        println("-".repeat(100))

        return workDirFile
    }
*/

    private fun processFrames(allPaths: List<RainGroupPath>): File {
        val workDirFile = IMAGES_ROOT.resolve("processed5").toFile().also { it.mkdir() }
        workDirFile.listFiles().forEach { it.delete() }

        println(">>> ${this::class.simpleName}: Started at ${Date()}")

        runBlocking(fixedThreadPoolContext) {
            IMAGES_ROOT.resolve("src").toFile().listFiles()
                .map { srcImgFile ->
                    async {
                        val img = srcImgFile.writableImage(SRC_IMG_OFFSET_X, SRC_IMG_OFFSET_Y, WORK_IMG_W, WORK_IMG_H)
                        val tmpBuffImg = BufferedImage(WORK_IMG_W, WORK_IMG_H, BufferedImage.TYPE_INT_RGB)
                        val image = SwingFXUtils.fromFXImage(img, tmpBuffImg)

                        val gc = image.graphics
                        gc.color = RAIN_PATH_COLOR_AWT
                        gc.font = gc.font.deriveFont(12f)

                        allPaths.forEach paths@{ path ->
                            path.takeUpTo(srcImgFile.nameWithoutExtension)?.forEach { rainLine ->
                                val (p2, p1) = rainLine.line
                                val (x1, y1) = p1 as Pair<Int, Int>
                                val (x2, y2) = p2 as Pair<Int, Int>
                                gc.drawLine(x1, y1, x2, y2)
                                gc.drawString(path.id.toString(), x2, y2)
                            }
                        }

                        val newFileName = "${srcImgFile.nameWithoutExtension}.$IMG_TYPE"
                        val newFile = workDirFile.resolve(newFileName).also { it.createNewFile() }
                        tmpBuffImg.createGraphics().drawImage(image, 0, 0, null)
                        ImageIO.write(tmpBuffImg, IMG_TYPE, newFile)
                    }
                }.awaitAll()
        }

        println(">>> ${this::class.simpleName}: Finished at ${Date()}")
        println("-".repeat(100))

        return workDirFile
    }
}
