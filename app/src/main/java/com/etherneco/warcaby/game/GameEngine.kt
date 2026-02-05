package com.etherneco.warcaby.game

class GameEngine(
    private val rules: GameRules
) {
    fun makeMove(gameState: GameState, move: Move): GameState {
        if (gameState.status != GameStatus.IN_PROGRESS) return gameState
        if (!rules.isMoveValid(gameState.board, move, gameState.currentPlayer)) return gameState

        val newBoard = gameState.board.copy()
        val piece = newBoard.getPiece(move.from) ?: return gameState
        newBoard.setPiece(move.from, null)
        for (captured in move.captured) {
            newBoard.setPiece(captured, null)
        }

        val promoted = shouldPromote(piece, move.to)
        val movedPiece = piece.copy(isKing = piece.isKing || promoted)
        newBoard.setPiece(move.to, movedPiece)

        val nextPlayer = Player(if (gameState.currentPlayer.color == PlayerColor.WHITE) PlayerColor.BLACK else PlayerColor.WHITE)
        val status = evaluateStatus(newBoard, nextPlayer)

        return GameState(newBoard, nextPlayer, status)
    }

    private fun evaluateStatus(board: Board, nextPlayer: Player): GameStatus {
        val whitePieces = countPieces(board, PlayerColor.WHITE)
        val blackPieces = countPieces(board, PlayerColor.BLACK)
        if (whitePieces == 0) return GameStatus.BLACK_WON
        if (blackPieces == 0) return GameStatus.WHITE_WON

        val moves = rules.getAvailableMoves(board, nextPlayer)
        if (moves.isEmpty()) {
            return if (nextPlayer.color == PlayerColor.WHITE) GameStatus.BLACK_WON else GameStatus.WHITE_WON
        }
        return GameStatus.IN_PROGRESS
    }

    private fun countPieces(board: Board, color: PlayerColor): Int {
        var count = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                if (board.fields[row][col]?.color == color) {
                    count++
                }
            }
        }
        return count
    }

    private fun shouldPromote(piece: Piece, target: Position): Boolean {
        return !piece.isKing && ((piece.color == PlayerColor.WHITE && target.row == 0) ||
            (piece.color == PlayerColor.BLACK && target.row == 7))
    }
}
