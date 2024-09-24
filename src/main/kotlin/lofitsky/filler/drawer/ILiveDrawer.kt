package lofitsky.filler.drawer

interface ILiveDrawer {
    fun start(data: Any?): Thread
    fun stop(): Unit
    fun command(cmd: AbstractLiveDrawer.Companion.ThreadCommand)
}
