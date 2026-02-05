package com.etherneco.warcaby

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import com.etherneco.warcaby.game.GameStatus
import com.etherneco.warcaby.game.Move
import com.etherneco.warcaby.game.Piece
import com.etherneco.warcaby.game.PlayerColor
import com.etherneco.warcaby.game.Position

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var viewModel: GameViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var boardGrid: GridLayout
    private lateinit var statusText: TextView
    private lateinit var turnText: TextView
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)

    private val pieceViews = Array(8) { arrayOfNulls<ImageView>(8) }
    private val highlightViews = Array(8) { arrayOfNulls<View>(8) }

    private var selectedPosition: Position? = null
    private var availableMovesFromSelected: List<Move> = emptyList()
    private var suppressRestartOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[GameViewModel::class.java]
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)
        boardGrid = findViewById(R.id.boardGrid)
        statusText = findViewById(R.id.statusText)
        turnText = findViewById(R.id.turnText)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        setupBoard()
        handleStartIntent(intent)

        viewModel.gameState.observe(this) { state ->
            updateBoard(state)
            updateStatus(state)
        }
    }

    private fun setupBoard() {
        boardGrid.rowCount = 8
        boardGrid.columnCount = 8
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val cell = layoutInflater.inflate(R.layout.item_cell, boardGrid, false)
                val square = cell.findViewById<View>(R.id.squareView)
                val pieceView = cell.findViewById<ImageView>(R.id.pieceView)
                val highlightView = cell.findViewById<View>(R.id.highlightView)

                val isDark = (row + col) % 2 == 1
                square.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        if (isDark) R.color.board_dark else R.color.board_light
                    )
                )

                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(col, 1f)
                )
                cell.layoutParams = params
                cell.setOnClickListener { onCellClicked(row, col) }

                boardGrid.addView(cell)
                pieceViews[row][col] = pieceView
                highlightViews[row][col] = highlightView
            }
        }

        boardGrid.post {
            val size = boardGrid.width / 8
            for (i in 0 until boardGrid.childCount) {
                val child = boardGrid.getChildAt(i)
                val params = child.layoutParams
                params.width = size
                params.height = size
                child.layoutParams = params
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val restartIfChanged = !suppressRestartOnce
        applySettingsFromPrefs(restartIfChanged = restartIfChanged)
        suppressRestartOnce = false
    }

    override fun onStop() {
        super.onStop()
        saveGameState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStartIntent(intent)
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_new_game -> {
                selectedPosition = null
                availableMovesFromSelected = emptyList()
                viewModel.restartGame()
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        drawerLayout.closeDrawers()
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
    }

    private fun applySettingsFromPrefs(restartIfChanged: Boolean) {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE)
        val startName = prefs.getString(
            SettingsActivity.KEY_START_COLOR,
            PlayerColor.WHITE.name
        ) ?: PlayerColor.WHITE.name
        val vsComputer = prefs.getBoolean(SettingsActivity.KEY_VS_COMPUTER, false)
        val startColor = if (startName == PlayerColor.BLACK.name) {
            PlayerColor.BLACK
        } else {
            PlayerColor.WHITE
        }
        viewModel.updateSettings(startColor, vsComputer, restartIfChanged)
    }

    private fun handleStartIntent(intent: Intent?) {
        val wantsNewGame = intent?.getBooleanExtra(EXTRA_NEW_GAME, false) == true
        val wantsContinue = intent?.getBooleanExtra(EXTRA_CONTINUE, false) == true

        if (wantsContinue && restoreGameState()) {
            applySettingsFromPrefs(restartIfChanged = false)
            suppressRestartOnce = true
            return
        }

        applySettingsFromPrefs(restartIfChanged = true)
        if (wantsNewGame || wantsContinue) {
            clearSavedGame()
            viewModel.restartGame()
        }
    }

    private fun saveGameState() {
        val state = viewModel.gameState.value ?: return
        val prefs = getSharedPreferences(PREFS_GAME, MODE_PRIVATE)
        if (state.status != GameStatus.IN_PROGRESS) {
            prefs.edit().clear().apply()
            return
        }

        val boardString = StringBuilder(64)
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board.getPiece(Position(row, col))
                val symbol = when {
                    piece == null -> '.'
                    piece.color == PlayerColor.WHITE && piece.isKing -> 'W'
                    piece.color == PlayerColor.WHITE -> 'w'
                    piece.color == PlayerColor.BLACK && piece.isKing -> 'B'
                    else -> 'b'
                }
                boardString.append(symbol)
            }
        }

        prefs.edit()
            .putString(KEY_BOARD, boardString.toString())
            .putString(KEY_CURRENT_PLAYER, state.currentPlayer.color.name)
            .putString(KEY_STATUS, state.status.name)
            .apply()
    }

    private fun restoreGameState(): Boolean {
        val prefs = getSharedPreferences(PREFS_GAME, MODE_PRIVATE)
        val boardString = prefs.getString(KEY_BOARD, null) ?: return false
        val currentPlayer = prefs.getString(KEY_CURRENT_PLAYER, null) ?: return false
        val statusName = prefs.getString(KEY_STATUS, null) ?: return false
        if (boardString.length != 64) return false

        val board = com.etherneco.warcaby.game.Board()
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val index = row * 8 + col
                val symbol = boardString[index]
                val piece = when (symbol) {
                    'w' -> Piece(PlayerColor.WHITE, false)
                    'W' -> Piece(PlayerColor.WHITE, true)
                    'b' -> Piece(PlayerColor.BLACK, false)
                    'B' -> Piece(PlayerColor.BLACK, true)
                    '.' -> null
                    else -> return false
                }
                board.setPiece(Position(row, col), piece)
            }
        }

        val status = runCatching { GameStatus.valueOf(statusName) }.getOrNull() ?: return false
        val playerColor = runCatching { PlayerColor.valueOf(currentPlayer) }.getOrNull() ?: return false

        viewModel.setGameState(
            com.etherneco.warcaby.game.GameState(
                board,
                com.etherneco.warcaby.game.Player(playerColor),
                status
            )
        )
        return true
    }

    private fun clearSavedGame() {
        getSharedPreferences(PREFS_GAME, MODE_PRIVATE).edit().clear().apply()
    }

    private fun onCellClicked(row: Int, col: Int) {
        val state = viewModel.gameState.value ?: return
        if (state.status != GameStatus.IN_PROGRESS) return

        val position = Position(row, col)
        val piece = state.board.getPiece(position)
        val allMoves = viewModel.getAvailableMoves()

        if (selectedPosition == null) {
            if (piece != null && piece.color == state.currentPlayer.color) {
                val movesFrom = allMoves.filter { it.from == position }
                if (movesFrom.isNotEmpty()) {
                    selectedPosition = position
                    availableMovesFromSelected = movesFrom
                    updateHighlights()
                }
            }
            return
        }

        val chosenMove = availableMovesFromSelected.firstOrNull { it.to == position }
        if (chosenMove != null) {
            selectedPosition = null
            availableMovesFromSelected = emptyList()
            viewModel.onMove(chosenMove)
            if (chosenMove.captured.isNotEmpty()) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
            return
        }

        if (piece != null && piece.color == state.currentPlayer.color) {
            val movesFrom = allMoves.filter { it.from == position }
            selectedPosition = if (movesFrom.isNotEmpty()) position else null
            availableMovesFromSelected = movesFrom
        } else {
            selectedPosition = null
            availableMovesFromSelected = emptyList()
        }
        updateHighlights()
    }

    private fun updateBoard(state: com.etherneco.warcaby.game.GameState) {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = state.board.getPiece(Position(row, col))
                val pieceView = pieceViews[row][col] ?: continue
                val resId = when {
                    piece == null -> 0
                    piece.isKing && piece.color == PlayerColor.WHITE -> R.drawable.king_white
                    piece.isKing && piece.color == PlayerColor.BLACK -> R.drawable.king_black
                    piece.color == PlayerColor.WHITE -> R.drawable.piece_white
                    else -> R.drawable.piece_black
                }
                if (resId == 0) {
                    pieceView.setImageDrawable(null)
                } else {
                    pieceView.setImageResource(resId)
                }
            }
        }
        updateHighlights()
    }

    private fun updateStatus(state: com.etherneco.warcaby.game.GameState) {
        val playerText = if (state.currentPlayer.color == PlayerColor.WHITE) {
            getString(R.string.player_white)
        } else {
            getString(R.string.player_black)
        }

        statusText.text = when (state.status) {
            GameStatus.IN_PROGRESS -> getString(R.string.turn_text, playerText)
            GameStatus.WHITE_WON -> getString(R.string.winner_text, getString(R.string.player_white))
            GameStatus.BLACK_WON -> getString(R.string.winner_text, getString(R.string.player_black))
            GameStatus.DRAW -> getString(R.string.draw_text)
        }

        val baseTurnText = getString(R.string.turn_label, playerText)
        val hasCapture = state.status == GameStatus.IN_PROGRESS &&
            viewModel.getAvailableMoves().any { it.captured.isNotEmpty() }
        if (hasCapture) {
            val suffix = " (${getString(R.string.capture_required)})"
            val combined = baseTurnText + suffix
            val spannable = SpannableString(combined)
            val color = ContextCompat.getColor(this, R.color.capture_text)
            spannable.setSpan(
                ForegroundColorSpan(color),
                baseTurnText.length,
                combined.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            turnText.text = spannable
        } else {
            turnText.text = baseTurnText
        }
        turnText.visibility = if (state.status == GameStatus.IN_PROGRESS) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateHighlights() {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                highlightViews[row][col]?.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        selectedPosition?.let {
            highlightViews[it.row][it.col]?.setBackgroundColor(
                ContextCompat.getColor(this, R.color.highlight_selected)
            )
        }

        for (move in availableMovesFromSelected) {
            highlightViews[move.to.row][move.to.col]?.setBackgroundColor(
                ContextCompat.getColor(this, R.color.highlight_move)
            )
        }
    }

    companion object {
        const val EXTRA_NEW_GAME = "com.etherneco.warcaby.EXTRA_NEW_GAME"
        const val EXTRA_CONTINUE = "com.etherneco.warcaby.EXTRA_CONTINUE"

        private const val PREFS_GAME = "warcaby_game"
        private const val KEY_BOARD = "board"
        private const val KEY_CURRENT_PLAYER = "current_player"
        private const val KEY_STATUS = "status"
    }
}
