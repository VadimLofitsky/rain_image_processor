package lofitsky.filler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

inline fun <reified T> stub(n: Int): T {
    return jacksonObjectMapper().registerKotlinModule()
        .readValue<T>(File("""/home/vadim/MyPrjs/kt/filler/src/main/resources/stub$n.txt""").readText())
}
