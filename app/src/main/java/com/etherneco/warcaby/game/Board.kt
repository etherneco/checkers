package com.etherneco.warcaby.game

class Board {
    val fields: Array<Array<Piece?>> = Array(8) { arrayOfNulls(8) }

    fun getPiece(position: Position): Piece? = fields[position.row][position.col]

    fun setPiece(position: Position, piece: Piece?) {
        fields[position.row][position.col] = piece
    }

    fun reset() {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                fields[row][col] = null
            }
        }
        for (row in 0 until 3) {
            for (col in 0 until 8) {
                if ((row + col) % 2 == 1) {
                    fields[row][col] = Piece(PlayerColor.BLACK)
                }
            }
        }
        for (row in 5 until 8) {
            for (col in 0 until 8) {
                if ((row + col) % 2 == 1) {
                    fields[row][col] = Piece(PlayerColor.WHITE)
                }
            }
        }
    }

    fun copy(): Board {
        val newBoard = Board()
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val piece = fields[row][col]
                newBoard.fields[row][col] = piece?.copy()
            }
        }
        return newBoard
    }

    fun isInside(position: Position): Boolean {
        return position.row in 0..7 && position.col in 0..7
    }
}
