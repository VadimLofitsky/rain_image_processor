package lofitsky.filler.processing

import lofitsky.filler.common.FieldInfo
import lofitsky.filler.common.FieldInfo.Companion.JOIN_THRESHOLD2

class RainGroupsJoinerProcessor : IProcessor {
    override fun process(data: Any?): Any? {
        val allFramesFieldsMap = (data as? Map<String, List<FieldInfo>>)
            ?.takeIf { it.isNotEmpty() } ?: run {
                log("!!!!!!! Failed to cast processor input data to Map<String, List<FieldInfo>> (frames with extracted rain fields) or map is empty: $data")
                return data
            }

        val zoom = allFramesFieldsMap.entries.first().key.zoom

        val mergedFields = mergeFields(allFramesFieldsMap, zoom)

        println("${this::class.simpleName}: merged fields:")
        mergedFields.forEach { (fn, fs) ->
            println("\n$fn")
            fs.forEachIndexed { i, fi -> println("\t#${i + 1}: $fi") }
        }

        return mergedFields
    }

    private fun findCandidates(k: Int, currField: FieldInfo, fields: List<FieldInfo>, zoom: Int): MutableSet<Int> {
        if(fields.isEmpty()) return mutableSetOf()

        val zoomedJoinThreshold = JOIN_THRESHOLD2 * zoomFactor2(zoom)
        return fields.mapIndexed { i, field ->
            if(currField.border.any { (cx, cy) ->
                field.border.any { (x, y) ->
                    val dx = cx - x
                    val dy = cy - y
                    (dx * dx + dy * dy) <= zoomedJoinThreshold
                }
            }) (k + i + 1) else null
        }.filterNotNull().sorted().toMutableSet()
    }

    private fun getChainsOf(candidates: MutableList<MutableSet<Int>?>, n: Int): MutableSet<Int>? = candidates[n]?.flatMap { i ->
        getChainsOf(candidates, i) ?: return@flatMap emptySet()
    }?.also {
        candidates[n]!!.removeAll(it)
        if(candidates[n]!!.isEmpty()) candidates[n] = null
    }?.plus(n)?.toMutableSet()

    private fun mergeFields(allFramesFieldsMap: Map<String, List<FieldInfo>>, zoom: Int): Map<String, List<FieldInfo>> = allFramesFieldsMap.mapValues { page ->
        if(page.value.size == 1) return@mapValues page.value

        val mergeLists: MutableList<MutableSet<Int>?> = page.value.mapIndexed { i, field ->
            val candidates = findCandidates(i, field, page.value.takeLast(page.value.size - i - 1), zoom)
            println("Candidates for #$i: $candidates")
            candidates
        }.toMutableList()

        println("\nmergeLists:")
        mergeLists.forEachIndexed { i, s -> println("$i: $s") }
        println()

        mergeLists.mapIndexed { n, fields ->
                fields?.flatMap { getChainsOf(mergeLists, it) ?: emptyList() }?.distinct()?.onEach { mergeLists[it] = null }?.plus(n)?.sorted()
            }.filterNotNull()
            .map { fieldsToMerge -> fieldsToMerge.drop(1).fold(page.value[fieldsToMerge[0]]) { acc, n -> acc.add(page.value[n]) } }
    }
}
