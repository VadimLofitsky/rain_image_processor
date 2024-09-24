package lofitsky.filler

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import lofitsky.filler.drawer.AbstractLiveDrawer.Companion.ThreadCommand
import lofitsky.filler.drawer.CycleDrawer
import lofitsky.filler.drawer.ILiveDrawer
import lofitsky.filler.drawer.LiveFiller
import lofitsky.filler.processing.*
import java.nio.file.Path
import kotlin.system.exitProcess

class FillerApp : Application() {
    companion object {
        private lateinit var processingConveyor: ProcessingConveyor
        private lateinit var liveFiller: ILiveDrawer
        private lateinit var cycleDrawer: ILiveDrawer

        val IMAGES_ROOT = Path.of("/home/vadim/MyPrjs/kt/filler/src/main/resources/img")

        const val IMG_TYPE = "bmp"
        const val SRC_IMG_OFFSET_X = 0
        const val SRC_IMG_OFFSET_Y = 40
        const val WORK_IMG_W = 1278
        const val WORK_IMG_H = 895

        const val SCALE0 = 0.43364754503626035      // km/px

        private var pause = false
    }

    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(FillerApp::class.java.getResource("filler-app-view.fxml"))

        val scene = Scene(fxmlLoader.load())

        stage.apply {
            title = "Fillr"
            setScene(scene)
            isMaximized = false
            show()
        }

        (scene.lookup("#canvas") as Canvas)
            .also {
                processingConveyor = ProcessingConveyor.Companion.Builder()
                        .canvas(it)
                        .addProcessor(RainSelectorProcessor())
                        .addProcessor(RainGroupsExtractorProcessor())
                        .addProcessor(RainGroupsDrawerProcessor())
                        .addProcessor(RainGroupsJoinerProcessor())
                        .addProcessor(MergedRainGroupsDrawerProcessor())
                        .addProcessor(RainGroupsMatcherProcessor())
                        .addProcessor(RainPathDrawerProcessor())
                    .build()

                cycleDrawer = CycleDrawer(it)
                liveFiller = LiveFiller(it)

                it.addEventHandler(MouseEvent.MOUSE_RELEASED, ClickHandler)
            }

        val conveyorResult = processingConveyor.start(IMAGES_ROOT.resolve("src"))
//        val conveyorResult = processingConveyor.start(stub<Map<String, List<FieldInfo>>>(1))
//        val conveyorResult = processingConveyor.start(stub<List<RainGroupPath>>(2))

        cycleDrawer.start(conveyorResult)
//        liveFiller.start()
    }

    private object ClickHandler : EventHandler<MouseEvent> {
        override fun handle(event: MouseEvent) {
            when(event.button) {
                MouseButton.PRIMARY -> {
                    pause != pause
                    if(pause) cycleDrawer.command(ThreadCommand.PAUSE) else cycleDrawer.command(ThreadCommand.CONTINUE)
                }
                MouseButton.SECONDARY -> {
                    cycleDrawer.stop()
                    exitProcess(0)
                }
                else -> {}
            }
        }
    }
}

fun main() {
    Application.launch(FillerApp::class.java)
}
