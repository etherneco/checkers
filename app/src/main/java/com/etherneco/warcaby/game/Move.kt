package com.etherneco.warcaby.game

data class Move(
    val from: Position,
    val to: Position,
    val captured: List<Position> = emptyList()
)
