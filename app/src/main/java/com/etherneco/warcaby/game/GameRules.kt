package com.etherneco.warcaby.game

class GameRules {

    fun isMoveValid(board: Board, move: Move, player: Player): Boolean {
        return getAvailableMoves(board, player).any { it == move }
    }

    fun getAvailableMoves(board: Board, player: Player): List<Move> {
        val captureMoves = mutableListOf<Move>()
        val normalMoves = mutableListOf<Move>()

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val position = Position(row, col)
                val piece = board.getPiece(position) ?: continue
                if (piece.color != player.color) continue

                val captures = captureMovesForPiece(board, position, piece)
                if (captures.isNotEmpty()) {
                    captureMoves.addAll(captures)
                } else {
                    normalMoves.addAll(normalMovesForPiece(board, position, piece))
                }
            }
        }

        return if (captureMoves.isNotEmpty()) captureMoves else normalMoves
    }

    fun hasCapture(board: Board, player: Player): Boolean {
        return getAvailableMoves(board, player).any { it.captured.isNotEmpty() }
    }

    private fun normalMovesForPiece(board: Board, position: Position, piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        if (piece.isKing) {
            val directions = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
            for ((dr, dc) in directions) {
                var r = position.row + dr
                var c = position.col + dc
                while (r in 0..7 && c in 0..7) {
                    val target = Position(r, c)
                    if (board.getPiece(target) != null) break
                    moves.add(Move(position, target))
                    r += dr
                    c += dc
                }
            }
        } else {
            val dir = if (piece.color == PlayerColor.WHITE) -1 else 1
            for (dc in listOf(-1, 1)) {
                val target = Position(position.row + dir, position.col + dc)
                if (board.isInside(target) && board.getPiece(target) == null) {
                    moves.add(Move(position, target))
                }
            }
        }
        return moves
    }

    private fun captureMovesForPiece(board: Board, position: Position, piece: Piece): List<Move> {
        val results = mutableListOf<Move>()

        fun dfs(currentPosition: Position, currentBoard: Board, captured: MutableList<Position>) {
            val steps = if (piece.isKing) {
                kingCaptureSteps(currentBoard, currentPosition, piece.color)
            } else {
                manCaptureSteps(currentBoard, currentPosition, piece.color)
            }

            if (steps.isEmpty()) {
                if (captured.isNotEmpty()) {
                    results.add(Move(position, currentPosition, captured.toList()))
                }
                return
            }

            for (step in steps) {
                val nextBoard = currentBoard.copy()
                nextBoard.setPiece(currentPosition, null)
                nextBoard.setPiece(step.to, Piece(piece.color, piece.isKing))
                nextBoard.setPiece(step.captured, null)

                val nextCaptured = captured.toMutableList()
                nextCaptured.add(step.captured)
                dfs(step.to, nextBoard, nextCaptured)
            }
        }

        dfs(position, board, mutableListOf())
        return results
    }

    private fun manCaptureSteps(board: Board, position: Position, color: PlayerColor): List<CaptureStep> {
        val steps = mutableListOf<CaptureStep>()
        val rowSteps = listOf(-1, 1)
        for (dr in rowSteps) {
            for (dc in listOf(-1, 1)) {
                val mid = Position(position.row + dr, position.col + dc)
                val landing = Position(position.row + 2 * dr, position.col + 2 * dc)
                if (!board.isInside(landing)) continue
                val midPiece = board.getPiece(mid)
                if (midPiece != null && midPiece.color != color && board.getPiece(landing) == null) {
                    steps.add(CaptureStep(landing, mid))
                }
            }
        }
        return steps
    }

    private fun kingCaptureSteps(board: Board, position: Position, color: PlayerColor): List<CaptureStep> {
        val steps = mutableListOf<CaptureStep>()
        val directions = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in directions) {
            var r = position.row + dr
            var c = position.col + dc
            var capturedPos: Position? = null
            while (r in 0..7 && c in 0..7) {
                val current = Position(r, c)
                val piece = board.getPiece(current)
                if (piece == null) {
                    if (capturedPos != null) {
                        steps.add(CaptureStep(current, capturedPos))
                    }
                } else {
                    if (piece.color == color) break
                    if (capturedPos != null) break
                    capturedPos = current
                }
                r += dr
                c += dc
            }
        }
        return steps
    }

    private data class CaptureStep(
        val to: Position,
        val captured: Position
    )
}
