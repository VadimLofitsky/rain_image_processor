package lofitsky.filler.drawer

import kotlin.properties.Delegates.observable

abstract class AbstractLiveDrawer : ILiveDrawer {
    companion object {
        enum class ThreadCommand {
            PAUSE,
            CONTINUE,
            STOP,
        }

        class ThreadCommander(private val onNewCommand: (command: ThreadCommand) -> Unit) {
            var command: ThreadCommand by observable(ThreadCommand.CONTINUE) { _, _, newValue -> onNewCommand(newValue) }
                private set

            fun stop(): Unit {
                command = ThreadCommand.STOP
            }

            fun command(cmd: ThreadCommand) {
                command = cmd
            }
        }

        class LiveDrawerStop : Exception()
    }

    private lateinit var thisThread: Thread
    protected var threadCommand: ThreadCommand = ThreadCommand.CONTINUE
    private val commander: ThreadCommander = ThreadCommander { threadCommand = it }

    protected abstract fun draw(data: Any?): Unit

    private var canContinue = true
    protected fun waiter(): ThreadCommand {
        while(!canContinue) {
            when(threadCommand) {
                ThreadCommand.STOP -> throw LiveDrawerStop()
                ThreadCommand.PAUSE -> canContinue = false
                ThreadCommand.CONTINUE -> canContinue = true
                else -> {}
            }
        }
        return threadCommand
    }

    override fun start(data: Any?): Thread {
        thisThread = Thread { draw(data) }.also { it.start() }
        return thisThread
    }

    override fun stop(): Unit {
        commander.stop()
    }

    override fun command(cmd: ThreadCommand): Unit {
        commander.command(cmd)
    }
}
