import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChessPiece, GameState, MoveRecord, Side } from '../types/game'

export const useGameStore = defineStore('game', () => {
  const gameId = ref<string | null>(null)
  const status = ref<string>('WAITING')
  const redPlayer = ref('')
  const blackPlayer = ref('')
  const currentTurn = ref<Side>('RED')
  const mySide = ref<Side | null>(null)
  const pieces = ref<ChessPiece[]>([])
  const capturedPieces = ref<ChessPiece[]>([])
  const moveHistory = ref<MoveRecord[]>([])
  const winner = ref<Side | null>(null)
  const reason = ref<string | null>(null)
  const selectedPos = ref<{ col: number; row: number } | null>(null)
  const legalMoves = ref<{ col: number; row: number }[]>([])

  function setGame(data: GameState) {
    gameId.value = data.id
    status.value = data.status
    redPlayer.value = data.redPlayer
    blackPlayer.value = data.blackPlayer
    currentTurn.value = data.currentTurn
    mySide.value = data.mySide
    pieces.value = data.pieces
    capturedPieces.value = data.capturedPieces
    moveHistory.value = data.moveHistory
    winner.value = data.winner
    reason.value = data.reason
  }

  function updateFromServer(msg: any) {
    const p = msg.payload
    switch (msg.type) {
      case 'MATCH_FOUND':
        gameId.value = p.gameId
        mySide.value = p.side as Side
        status.value = 'PLAYING'
        break
      case 'GAME_START':
        currentTurn.value = p.turn as Side
        if (p.pieces) {
          pieces.value = p.pieces
        }
        break
      case 'MOVE_RESULT':
        if (p.move) {
          const moveStr = p.move as string
          const src = moveStr.slice(0, 2)
          const dst = moveStr.slice(2, 4)
          moveHistory.value.push({
            source: src,
            destination: dst,
            type: p.revealedType ? 1 : null,
            moveNumber: moveHistory.value.length + 1,
            side: currentTurn.value,
            revealMove: src === dst,
            notation: moveStr,
          })
        }
        if (p.pieces) {
          pieces.value = p.pieces
        }
        if (p.capturedPieces) {
          capturedPieces.value = p.capturedPieces
        }
        break
      case 'TURN_NOTIFY':
        currentTurn.value = p.turn as Side
        break
      case 'GAME_OVER':
        winner.value = p.winner as Side | null
        reason.value = p.reason as string
        status.value = 'FINISHED'
        break
      case 'ERROR':
        console.error('Server error:', p.message)
        break
    }
  }

  function reset() {
    gameId.value = null
    status.value = 'WAITING'
    pieces.value = []
    capturedPieces.value = []
    moveHistory.value = []
    winner.value = null
    reason.value = null
    selectedPos.value = null
    legalMoves.value = []
  }

  return {
    gameId, status, redPlayer, blackPlayer, currentTurn, mySide,
    pieces, capturedPieces, moveHistory, winner, reason,
    selectedPos, legalMoves,
    setGame, updateFromServer, reset,
  }
})
