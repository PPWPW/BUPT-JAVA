import { useGameStore } from '../stores/gameStore'
import { useWebSocket } from './useWebSocket'
import { useBoard } from './useBoard'

export function useGame() {
  const gameStore = useGameStore()
  const ws = useWebSocket()
  const board = useBoard()

  function handleCellClick(col: number, row: number) {
    const piece = gameStore.pieces.find(
      p => p.alive && p.position.col === col && p.position.row === row
    ) || null

    if (board.selectedPiece.value) {
      if (board.selectedPiece.value.position.col === col &&
          board.selectedPiece.value.position.row === row) {
        board.clearSelection()
        return
      }

      const isLegal = board.legalTargets.value.some(m => m.col === col && m.row === row)
      if (isLegal) {
        const from = board.posToAlgebraic(board.selectedPiece.value.position.col, board.selectedPiece.value.position.row)
        const to = board.posToAlgebraic(col, row)
        ws.makeMove(from, to)
        board.clearSelection()
        return
      }
    }

    board.selectPiece(piece, gameStore.pieces, gameStore.currentTurn === gameStore.mySide, gameStore.mySide)
  }

  return { handleCellClick, board, ws, gameStore }
}
