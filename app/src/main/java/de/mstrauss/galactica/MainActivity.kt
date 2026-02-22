package de.mstrauss.galactica

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.mstrauss.galactica.patterns.MainMenuState
import de.mstrauss.galactica.patterns.MenuStateMachine
import de.mstrauss.galactica.patterns.MenuUi
import de.mstrauss.galactica.patterns.MultiPlayerState
import de.mstrauss.galactica.patterns.SettingsState
import de.mstrauss.galactica.patterns.SinglePlayerPlaygroundState
import de.mstrauss.galactica.patterns.SinglePlayerState


class MainActivity : AppCompatActivity() {

    private lateinit var stateMachine: MenuStateMachine


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        // from https://stackoverflow.com/questions/74002879/android-studio-full-screen-deprecated
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController =
            WindowInsetsControllerCompat(window, window.decorView)
        // Hide system bars
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        stateMachine = MenuStateMachine(MenuUi.bind(this))
        findViewById<View>(R.id.menu_singleplayer_button).setOnClickListener {
            stateMachine.changeState(SinglePlayerState())
        }
        findViewById<View>(R.id.menu_multiplayer_button).setOnClickListener {
            stateMachine.changeState(MultiPlayerState())
        }
        findViewById<View>(R.id.menu_settings_button_inner).setOnClickListener {
            stateMachine.changeState(SettingsState())
        }
        findViewById<View>(R.id.menu_back_button_inner).setOnClickListener {
            stateMachine.changeState(MainMenuState())
        }
        findViewById<View>(R.id.single_player_playground_button).setOnClickListener {
            stateMachine.changeState((SinglePlayerPlaygroundState()))
        }
        findViewById<View>(R.id.start_single_player_game_button).setOnClickListener {
            startActivity(
                SinglePlayerActivity.createIntent(
                    context = this,
                    gridRows = 7,
                    gridCols = 9,
                    planetAmount = 4,
                    allowedMoves = 7 * 9
                )
            )
        }
        stateMachine.changeState(MainMenuState())
    }
}
