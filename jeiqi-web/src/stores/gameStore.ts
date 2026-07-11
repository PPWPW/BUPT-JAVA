import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChessPiece, GameState, MoveRecord, Side, PieceType } from '../types/game'
import { useUserStore } from './userStore'

export const useGameStore = defineStore('game', () => {
  const gameId = ref<string | null>(null)
  const status = ref<string>('WAITING')
  const redPlayer = ref('')
  const blackPlayer = ref('')
  const currentTurn = ref<Side>('red')
  const mySide = ref<Side | null>(null)
  const pieces = ref<ChessPiece[]>([])
  const capturedPieces = ref<ChessPiece[]>([])
  const moveHistory = ref<MoveRecord[]>([])
  const winner = ref<Side | null>(null)
  const reason = ref<string | null>(null)
  const selectedPos = ref<{ col: number; row: number } | null>(null)
  const legalMoves = ref<{ col: number; row: number }[]>([])
  const drawRequestReceived = ref(false)
  const drawRejected = ref(false)
  const remainingSeconds = ref(60)
  
  const activeEmoteTop = ref<{ content: string; type: 'EMOTE' | 'PHRASE' } | null>(null)
  const activeEmoteBottom = ref<{ content: string; type: 'EMOTE' | 'PHRASE' } | null>(null)
  const opponentMuted = ref(false)
  const rematchStatus = ref<'NONE' | 'SENT' | 'RECEIVED' | 'DECLINED'>('NONE')

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
    const userStore = useUserStore()
    switch (msg.messageType) {
      case 'matchSuccess':
        reset()
        gameId.value = msg.roomId
        status.value = 'MATCHED'
        // Auto-send Ready when matching succeeds
        import('../composables/useWebSocket').then(({ useWebSocket }) => {
          const { sendReady } = useWebSocket()
          sendReady()
        })
        break
      case 'gameStart':
        status.value = 'PLAYING'
        mySide.value = msg.yourColor as Side
        currentTurn.value = 'red' // red always starts
        if (msg.initialBoard) {
          pieces.value = msg.initialBoard.map((cell: any) => {
            const col = cell.x.charCodeAt(0) - 97
            const row = cell.y
            const side: Side = row < 5 ? 'red' : 'black'
            return {
              type: cell.piece as PieceType,
              side: side,
              revealed: cell.visible,
              position: { col, row },
              alive: true
            }
          })
        }
        break
      case 'moveResult':
        if (msg.success && msg.valid) {
          const m = msg.move
          const fromCol = m.fromX.charCodeAt(0) - 97
          const fromRow = m.fromY
          const toCol = m.toX.charCodeAt(0) - 97
          const toRow = m.toY

          const movingPiece = pieces.value.find(p => p.alive && p.position.col === fromCol && p.position.row === fromRow)
          if (movingPiece) {
            const targetPiece = pieces.value.find(p => p.alive && p.position.col === toCol && p.position.row === toRow)
            if (targetPiece) {
              targetPiece.alive = false
              let capType = targetPiece.type
              let capRevealed = targetPiece.revealed

              if (msg.capturedType) {
                if (msg.capturedType === 'NULL') {
                  capType = null
                  capRevealed = false
                } else {
                  capType = msg.capturedType as PieceType
                  capRevealed = true
                }
              } else if (msg.flipResult && movingPiece.revealed) {
                if (msg.flipResult === 'NULL') {
                  capType = null
                  capRevealed = false
                } else {
                  capType = msg.flipResult as PieceType
                  capRevealed = true
                }
              }

              capturedPieces.value.push({
                type: capType,
                side: targetPiece.side,
                revealed: capRevealed,
                position: { col: toCol, row: toRow },
                alive: false
              })
            }

            movingPiece.position = { col: toCol, row: toRow }

            if (msg.flipResult && !movingPiece.revealed) {
              movingPiece.revealed = true
              movingPiece.type = msg.flipResult as PieceType
            }

            moveHistory.value.push({
              source: m.fromX + m.fromY,
              destination: m.toX + m.toY,
              type: movingPiece.revealed ? 1 : null,
              moveNumber: moveHistory.value.length + 1,
              side: movingPiece.side,
              revealMove: fromCol === toCol && fromRow === toRow,
              notation: m.fromX + m.fromY + m.toX + m.toY + (msg.flipResult ? ',' + msg.flipResult : '')
            })

            currentTurn.value = currentTurn.value === 'red' ? 'black' : 'red'
          }
        }
        break
      case 'roomInfo':
        break
      case 'timeout':
        status.value = 'FINISHED'
        winner.value = msg.winnerId === userStore.username ? mySide.value : (mySide.value === 'red' ? 'black' : 'red')
        reason.value = 'timeout'
        break
      case 'gameOver':
        winner.value = msg.winner as Side | null
        reason.value = msg.reason as string
        status.value = 'FINISHED'
        break
      case 'drawRequest':
        drawRequestReceived.value = true
        break
      case 'drawRejected':
        drawRejected.value = true
        break
      case 'loginResult':
        if (!msg.success) {
          alert('登录连接已失效（服务器已重启），请点击确定重新登录或注册！')
          userStore.logout()
          window.location.href = '/'
        } else if (msg.activeGameId) {
          gameId.value = msg.activeGameId
          status.value = 'PLAYING'
        }
        break
      case 'boardState':
        reset()
        gameId.value = msg.roomId
        status.value = msg.status
        redPlayer.value = msg.redPlayerName
        blackPlayer.value = msg.blackPlayerName
        currentTurn.value = msg.currentTurn as Side
        mySide.value = msg.mySide === 'spectator' ? null : (msg.mySide as Side)
        remainingSeconds.value = msg.remainingSeconds !== undefined ? msg.remainingSeconds : 60
        
        if (msg.pieces) {
          pieces.value = msg.pieces.map((p: any) => ({
            type: p.type as PieceType,
            side: p.side as Side,
            revealed: p.revealed,
            position: { col: p.position.col, row: p.position.row },
            alive: p.alive !== undefined ? p.alive : true
          }))
        }
        if (msg.capturedPieces) {
          capturedPieces.value = msg.capturedPieces.map((p: any) => ({
            type: p.type as PieceType,
            side: p.side as Side,
            revealed: p.revealed,
            position: { col: p.position.col, row: p.position.row },
            alive: false
          }))
        }
        if (msg.moveHistory) {
          moveHistory.value = msg.moveHistory.map((m: any) => ({
            source: m.source,
            destination: m.destination,
            type: m.type,
            moveNumber: m.moveNumber,
            side: m.side as Side,
            revealMove: m.revealMove,
            notation: m.source + m.destination
          }))
        }
        break
      case 'roomCreated':
        reset()
        gameId.value = msg.roomId
        status.value = 'WAITING'
        break
      case 'chatMessage':
        {
          const isMyMessage = msg.senderSide === mySide.value
          const targetRef = isMyMessage ? activeEmoteBottom : activeEmoteTop
          
          if (!isMyMessage && opponentMuted.value) {
            break
          }

          targetRef.value = {
            content: msg.content,
            type: msg.chatType
          }
          setTimeout(() => {
            if (targetRef.value && targetRef.value.content === msg.content) {
              targetRef.value = null
            }
          }, 3000)
        }
        break
      case 'rematchRequest':
        rematchStatus.value = 'RECEIVED'
        break
      case 'rematchDeclined':
        if (msg.reason === 'opponentLeft') {
          alert('对方已离开房间')
        } else {
          alert('对方拒绝了再来一局的申请')
        }
        rematchStatus.value = 'NONE'
        break
      case 'error':
        console.error('Server error:', msg.message)
        break
    }
  }

  function reset() {
    gameId.value = null
    status.value = 'WAITING'
    redPlayer.value = ''
    blackPlayer.value = ''
    pieces.value = []
    capturedPieces.value = []
    moveHistory.value = []
    winner.value = null
    reason.value = null
    selectedPos.value = null
    legalMoves.value = []
    drawRequestReceived.value = false
    drawRejected.value = false
    activeEmoteTop.value = null
    activeEmoteBottom.value = null
    rematchStatus.value = 'NONE'
    remainingSeconds.value = 60
  }

  return {
    gameId, status, redPlayer, blackPlayer, currentTurn, mySide,
    pieces, capturedPieces, moveHistory, winner, reason,
    selectedPos, legalMoves,
    drawRequestReceived, drawRejected, remainingSeconds,
    activeEmoteTop, activeEmoteBottom, opponentMuted, rematchStatus,
    setGame, updateFromServer, reset,
  }
})
