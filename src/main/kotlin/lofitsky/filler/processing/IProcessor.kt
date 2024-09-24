package lofitsky.filler.processing

interface IProcessor {
    fun log(msg: String): Unit = println("${this::class.java.simpleName}: $msg")

    fun process(data: Any?): Any?
}
