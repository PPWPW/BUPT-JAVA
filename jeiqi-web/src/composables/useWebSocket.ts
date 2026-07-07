import { socketService } from '../services/socket'
import { useGameStore } from '../stores/gameStore'
import { useUserStore } from '../stores/userStore'

export function useWebSocket() {
  const gameStore = useGameStore()
  const userStore = useUserStore()

  function connect(userId: string, onConnect?: () => void) {
    socketService.setMessageHandler((msg) => {
      gameStore.updateFromServer(msg)
    })

    socketService.connect(userId, () => {
      console.log('WebSocket connected')
      if (onConnect) onConnect()
    })
  }

  function joinQueue() {
    socketService.send({
      messageType: 'startMatch'
    })
  }

  function leaveQueue() {
    socketService.send({
      messageType: 'cancelMatch'
    })
  }

  function sendReady() {
    socketService.send({
      messageType: 'Ready'
    })
  }

  function makeMove(source: string, destination: string) {
    const fromCol = source.charCodeAt(0) - 97
    const fromRow = parseInt(source.substring(1))
    const piece = gameStore.pieces.find(p => p.position.col === fromCol && p.position.row === fromRow)
    const isFlip = piece ? !piece.revealed : false

    socketService.send({
      messageType: 'move',
      fromX: source.charAt(0),
      fromY: fromRow,
      toX: destination.charAt(0),
      toY: parseInt(destination.substring(1)),
      isFlip: isFlip
    })
  }

  function resign() {
    socketService.send({
      messageType: 'Resign'
    })
  }

  function requestDraw(accept: boolean) {
    socketService.send({
      messageType: 'draw',
      accept: accept
    })
  }

  function disconnect() {
    socketService.disconnect()
  }

  function subscribeGame(gameId: string) {
    socketService.subscribeToGame(gameId)
  }

  return { connect, joinQueue, leaveQueue, sendReady, makeMove, resign, requestDraw, disconnect, subscribeGame }
}
