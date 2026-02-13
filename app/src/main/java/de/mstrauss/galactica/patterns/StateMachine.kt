package de.mstrauss.galactica.patterns

import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.mstrauss.galactica.R

interface State {
    fun enterState(ui: MenuUi)
    fun exitState(ui: MenuUi)
}

interface Logger {
    fun d(tag: String, message: String)
}

object AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
}

class MenuUi(
    val gametitle : View,
    val mainMenu: View,
    val singleMenu: View,
    val singlePlaygroundMenu: View,
    val multiMenu: View,
    val settingsMenu: View,
    val settingsButton: View,
    val backButton: View
) {
    companion object {
        fun bind(mainActivity: AppCompatActivity): MenuUi {
            return MenuUi(
                mainActivity.findViewById(R.id.gametitle),
                mainActivity.findViewById(R.id.menu_buttons_layout),
                mainActivity.findViewById(R.id.single_menu_layout),
                mainActivity.findViewById(R.id.single_player_playground_setting),
                mainActivity.findViewById(R.id.multi_menu_layout),
                mainActivity.findViewById(R.id.settings_menu_layout),
                mainActivity.findViewById(R.id.menu_settings_button),
                mainActivity.findViewById(R.id.menu_back_button)
            )
        }
    }

    private fun hideAll() {
        mainMenu.visibility = View.GONE
        singleMenu.visibility = View.GONE
        singlePlaygroundMenu.visibility = View.GONE
        multiMenu.visibility = View.GONE
        settingsMenu.visibility = View.GONE
        settingsButton.visibility = View.GONE
        backButton.visibility = View.GONE
    }

    fun showMainMenu() {
        hideAll()
        mainMenu.visibility = View.VISIBLE
        settingsButton.visibility = View.VISIBLE
    }

    fun showSingleMenu() {
        hideAll()
        singleMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
    }

    fun showSinglePlayground() {
        hideAll()
        singlePlaygroundMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
    }


    fun showMultiMenu() {
        hideAll()
        multiMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
    }

    fun showSettingsMenu() {
        hideAll()
        settingsMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
    }
}

// menu menu state machine

class MenuStateMachine(
    private val ui: MenuUi,
    private val logger: Logger = AndroidLogger
) {
    var state: State? = null

    fun changeState(state: State) {
        logger.d(
            "MenuStateMachine",
            "changeState: ${this.state?.javaClass?.simpleName} -> ${state.javaClass.simpleName}"
        )
        this.state?.exitState(ui)
        this.state = state
        this.state?.enterState(ui)
    }
}
class MainMenuState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("MainMenuState", "enter state: mainMenu")
        ui.showMainMenu()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("MainMenuState", "exit state: mainMenu")
    }
}

class SinglePlayerState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("SinglePlayerState", "enter state: singlePlayer")
        ui.showSingleMenu()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("SinglePlayerState", "exit state: singlePlayer")
    }
}

class SinglePlayerPlaygroundState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("SinglePlayerPlaygroundState", "enter state: singlePlayer")
        ui.showSinglePlayground()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("SinglePlayerPlaygroundState", "exit state: singlePlayer")
    }
}

class MultiPlayerState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("MultiPlayerState", "enter state: multiPlayer")
        ui.showMultiMenu()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("MultiPlayerState", "exit state: multiPlayer")
    }
}

class SettingsState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("SettingsState", "enter state: settings")
        ui.showSettingsMenu()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("SettingsState", "exit state: settings")
    }
}
