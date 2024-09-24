package lofitsky.filler.processing

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import lofitsky.filler.FillerApp.Companion.IMAGES_ROOT
import lofitsky.filler.common.FieldInfo
import lofitsky.filler.common.FieldInfo.Companion.SIMILARITY_THRESHOLD
import lofitsky.filler.processing.ProcessingConveyor.Companion.fixedThreadPoolContext
import lofitsky.filler.processing.RainGroupsMatcherProcessor.Companion.RainGroupPath.Companion.RainLine

class RainGroupsMatcherProcessor : IProcessor {
    companion object {
        private class FrameFieldsInfo(val file: String, val fields: List<FieldInfo>, var prev: FrameFieldsInfo? = null, var next: FrameFieldsInfo? = null) {
            companion object {
                fun splitDesc(frames: List<Map.Entry<String, List<FieldInfo>>>): List<FrameFieldsInfo> {
                    val srcImagesMap = IMAGES_ROOT.resolve("src").toFile().listFiles().associateBy { it.nameWithoutExtension }

                    val frInfos = frames
                        .sortedByDescending { it.key }
                        .map { entry ->
                            FrameFieldsInfo(srcImagesMap[entry.key.filenameWithoutExt]!!.absolutePath, entry.value)
                        }

                    frInfos.zipWithNext()
                        .forEach { (fr1, fr2) ->
                            fr1.prev = fr2
                            fr2.next = fr1
                        }
                    return frInfos
                }
            }
        }

        class RainGroupPath(val path: List<RainLine>) {
            companion object {
                private var idCounter: Int = 0
                    get() = ++field

                class RainLine(
                    val file: String,
                    val line: Pair<Pair<Double, Double>, Pair<Double, Double>>
                ) {
                    override fun toString(): String = "RainLine(file = $file, line = $line)"
                }
            }

            val id: Int = idCounter

            fun containsWithoutExt(file: String): Boolean = path.any { it.file.filenameWithoutExt == file }

            fun takeUpTo(file: String): List<RainLine>?
                = getWithoutExt(file)?.let { path.takeWhile { rl -> rl.file.filenameWithoutExt != file }.plus(it) }

            fun getWithoutExt(file: String): RainLine? = path.find { it.file.filenameWithoutExt == file }

            override fun toString(): String = "RainGroupPath(id = $id, path = $path)"
        }
    }

    override fun process(data: Any?): Any? {
        val allFramesFields = (data as? Map<String, List<FieldInfo>>)
            ?.entries
            ?.sortedByDescending { it.key }
            ?: run {
                log("!!!!!!! Failed to cast processor input data to Map<String, List<FieldInfo>> (frames with joined rain fields): $data")
                return data
            }

        runBlocking(fixedThreadPoolContext) {
            allFramesFields.zipWithNext().map { pair ->
                async { linkMatchingFields(pair) }
            }.map { it.await() }
        }

        return buildRainPaths(allFramesFields)
    }

    private fun linkMatchingFields(adjacentFrames: Pair<Map.Entry<String, List<FieldInfo>>, Map.Entry<String, List<FieldInfo>>>): Unit {
        val usedFields = mutableSetOf<FieldInfo>()

        adjacentFrames.first.value.flatMap { secondFrameField ->
            calcFieldMatchFactors(secondFrameField, adjacentFrames.second.value)
        }.sortedBy { it.third }
        .forEach { matchingPair ->
            if(usedFields.contains(matchingPair.first) || usedFields.contains(matchingPair.second)) return@forEach

            matchingPair.first.prev = matchingPair.second
            usedFields.add(matchingPair.first)
            usedFields.add(matchingPair.second)
        }
    }

    private fun calcFieldMatchFactors(field: FieldInfo, others: List<FieldInfo>): List<Triple<FieldInfo, FieldInfo, Double>>
        = others.map { f -> Triple(field, f, field.diffTo(f)) }
            .filter { it.third < SIMILARITY_THRESHOLD }

    private fun buildRainPaths(allFramesFields: List<Map.Entry<String, List<FieldInfo>>>): List<RainGroupPath> {
        val usedFields = mutableSetOf<FieldInfo>()

        val allFramesFieldsDesc = FrameFieldsInfo.splitDesc(allFramesFields)

        return allFramesFieldsDesc
            .mapNotNull frames@{ frame ->
                frame.fields.mapNotNull frameFields@{ field ->
                    if(usedFields.contains(field)) return@frameFields null

                    usedFields.add(field)
                    field.prev?.also { usedFields.add(it) }
                    retrieveFieldsChain(frame, field)
                        .onEach { usedFields.add(it.second) }
                        .zipWithNext()
                        .map { (fr2, fr1) ->
                            RainLine(fr2.first.filenameWithoutExt, fr2.second.massCenter to fr1.second.massCenter)
                        }.sortedBy { it.file }
                        .takeIf { it.isNotEmpty() }
                        ?.let { RainGroupPath(it) }
                }.takeIf { it.isNotEmpty() }
            }.flatten()
            .also { println() }
    }

    private fun retrieveFieldsChain(frame: FrameFieldsInfo, field: FieldInfo): List<Pair<String, FieldInfo>> {
        val chain = mutableListOf<Pair<String, FieldInfo>>()

        var currFrame = frame
        var currField = field
        while(currField.prev != null) {
            chain.add(currFrame.file to currField)
            currFrame = currFrame.prev!!
            currField = currField.prev!!
        }
        chain.add(currFrame.file to currField)

        return chain
    }
}
