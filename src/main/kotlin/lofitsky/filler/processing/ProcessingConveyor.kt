package lofitsky.filler.processing

import javafx.scene.canvas.Canvas
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext

class ProcessingConveyor private constructor() {
    private lateinit var canvas: Canvas
    private val processingQueue = mutableListOf<IProcessor>()

    companion object {
        val fixedThreadPoolContext: ExecutorCoroutineDispatcher by lazy {
            newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors(), "fixedThreadPoolContext")
        }

        class Builder {
            private var conveyorInstance: ProcessingConveyor = ProcessingConveyor()

            fun canvas(canvas: Canvas): Builder {
                conveyorInstance.canvas = canvas
                return this
            }

            fun addProcessor(processor: IProcessor): Builder {
                conveyorInstance.processingQueue.add(processor)
                return this
            }

            fun build(): ProcessingConveyor = conveyorInstance
        }
    }

    fun start(data: Any?): Any? = processingQueue.fold(data) { acc: Any?, processor -> processor.process(acc) }
}
