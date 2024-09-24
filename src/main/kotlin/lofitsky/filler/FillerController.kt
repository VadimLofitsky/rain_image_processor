package lofitsky.filler

import javafx.fxml.FXML
import javafx.scene.control.Label

class FillerController {
    @FXML
    private lateinit var welcomeText: Label

    @FXML
    private fun onHelloButtonClick() {
        welcomeText.text = "Welcome to JavaFX Application!"
    }
}
