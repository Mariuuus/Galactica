package de.mstrauss.galactica.patterns

import android.util.Log
import android.view.View
import android.widget.TextView
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
    val backButton: View,
    val campaignLevels: View,
    val subtitle: TextView,
    val clientLobby: View,
    val hostLobby: View,
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
                mainActivity.findViewById(R.id.menu_back_button),
                mainActivity.findViewById(R.id.campaign_levels),
                mainActivity.findViewById(R.id.subtitle),
                mainActivity.findViewById(R.id.multiplayer_lobby_client),
                mainActivity.findViewById(R.id.multiplayer_lobby_host),
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
        campaignLevels.visibility = View.GONE
        clientLobby.visibility = View.GONE
        hostLobby.visibility = View.GONE
    }

    fun showMainMenu() {
        hideAll()
        mainMenu.visibility = View.VISIBLE
        settingsButton.visibility = View.VISIBLE
        subtitle.text = "Das Spiel!"
    }

    fun showSingleMenu() {
        hideAll()
        singleMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        subtitle.text = "Einzelspieler"
    }

    fun showSinglePlayground() {
        hideAll()
        singlePlaygroundMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        subtitle.text = "Einzelspieler (Freies Spiel)"
    }

    fun showSingleCampaign() {
        hideAll()
        campaignLevels.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        subtitle.text = "Kampagne"
    }

    fun showMultiMenu() {
        hideAll()
        multiMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        subtitle.text = "Mehrspieler"
    }

    fun showMultiHost() {
        hideAll()
        hostLobby.visibility = View.VISIBLE
        subtitle.text = "Mehrspieler | Host"
    }

    fun showMultiClient() {
        hideAll()
        clientLobby.visibility = View.VISIBLE
        subtitle.text = "Mehrspieler | Client"
    }

    fun showSettingsMenu() {
        hideAll()
        settingsMenu.visibility = View.VISIBLE
        backButton.visibility = View.VISIBLE
        subtitle.text = "Einstellungen"
    }
}

// menu state machine
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
        Log.d("SinglePlayerPlaygroundState", "enter state: SinglePlayerPlayground")
        ui.showSinglePlayground()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("SinglePlayerPlaygroundState", "exit state: SinglePlayerPlayground")
    }
}

class SinglePlayerCampaignState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("SinglePlayerCampaignState", "enter state: SinglePlayerCampaign")
        ui.showSingleCampaign()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("SinglePlayerCampaignState", "exit state: SinglePlayerCampaign")
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

class MultiPlayerHostLobbyState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("MultiPlayerHostLobbyState", "enter state: MultiPlayerHostLobby")
        ui.showMultiHost()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("MultiPlayerHostLobbyState", "exit state: MultiPlayerHostLobby")
    }
}

class MultiPlayerClientLobbyState() : State {
    override fun enterState(ui: MenuUi) {
        Log.d("MultiPlayerClientLobbyState", "enter state: MultiPlayerClientLobby")
        ui.showMultiClient()
    }

    override fun exitState(ui: MenuUi) {
        Log.d("MultiPlayerClientLobbyState", "exit state: MultiPlayerClientLobby")
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
