package com.etherneco.warcaby.game

data class GameState(
    val board: Board,
    val currentPlayer: Player,
    val status: GameStatus
)
