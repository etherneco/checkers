package com.etherneco.warcaby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.etherneco.warcaby.game.Board
import com.etherneco.warcaby.game.GameEngine
import com.etherneco.warcaby.game.GameRules
import com.etherneco.warcaby.game.GameState
import com.etherneco.warcaby.game.GameStatus
import com.etherneco.warcaby.game.Move
import com.etherneco.warcaby.game.Player
import com.etherneco.warcaby.game.PlayerColor

class GameViewModel : ViewModel() {
    private val rules = GameRules()
    private val engine = GameEngine(rules)
    private val computerColor = PlayerColor.BLACK

    private var startingColor: PlayerColor = PlayerColor.WHITE
    private var vsComputer: Boolean = false

    private val _gameState = MutableLiveData(createInitialState(startingColor))
    val gameState: LiveData<GameState> = _gameState

    fun onMove(move: Move) {
        val current = _gameState.value ?: return
        if (vsComputer && current.currentPlayer.color == computerColor) {
            return
        }
        var nextState = engine.makeMove(current, move)
        if (nextState === current) {
            _gameState.value = nextState
            return
        }
        if (vsComputer) {
            nextState = makeComputerMoveIfNeeded(nextState)
        }
        _gameState.value = nextState
    }

    fun restartGame() {
        _gameState.value = createInitialState(startingColor)
        val current = _gameState.value ?: return
        if (vsComputer && current.currentPlayer.color == computerColor) {
            _gameState.value = makeComputerMoveIfNeeded(current)
        }
    }

    fun getAvailableMoves(): List<Move> {
        val current = _gameState.value ?: return emptyList()
        return rules.getAvailableMoves(current.board, current.currentPlayer)
    }

    fun setStartingColor(color: PlayerColor) {
        startingColor = color
        restartGame()
    }

    fun setVsComputer(enabled: Boolean) {
        vsComputer = enabled
        restartGame()
    }

    fun updateSettings(color: PlayerColor, enabled: Boolean, restartIfChanged: Boolean = true) {
        val changed = startingColor != color || vsComputer != enabled
        startingColor = color
        vsComputer = enabled
        if (restartIfChanged && changed) {
            restartGame()
        }
    }

    fun setGameState(state: GameState) {
        _gameState.value = state
    }

    fun getStartingColor(): PlayerColor = startingColor

    fun isVsComputer(): Boolean = vsComputer

    fun hasCapture(): Boolean {
        val current = _gameState.value ?: return false
        return rules.hasCapture(current.board, current.currentPlayer)
    }

    private fun createInitialState(startingColor: PlayerColor): GameState {
        val board = Board()
        board.reset()
        return GameState(board, Player(startingColor), GameStatus.IN_PROGRESS)
    }

    private fun makeComputerMoveIfNeeded(state: GameState): GameState {
        if (!vsComputer) return state
        if (state.status != GameStatus.IN_PROGRESS) return state
        if (state.currentPlayer.color != computerColor) return state

        val moves = rules.getAvailableMoves(state.board, state.currentPlayer)
        if (moves.isEmpty()) return state
        val move = moves.random()
        return engine.makeMove(state, move)
    }
}
