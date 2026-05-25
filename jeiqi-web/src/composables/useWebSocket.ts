import { socketService } from '../services/socket'
import { useGameStore } from '../stores/gameStore'

export function useWebSocket() {
  const gameStore = useGameStore()

  function connect(userId: string) {
    socketService.setMessageHandler((msg) => {
      gameStore.updateFromServer(msg)
    })

    socketService.connect(userId, () => {
      console.log('WebSocket connected')
    })
  }

  function joinQueue() {
    socketService.send('/app/join', {
      playerId: '',
      payload: {},
    })
  }

  function leaveQueue() {
    socketService.send('/app/leave', {
      playerId: '',
      payload: {},
    })
  }

  function makeMove(source: string, destination: string) {
    socketService.send('/app/move', {
      gameId: gameStore.gameId,
      playerId: '',
      payload: { source, destination },
    })
  }

  function resign() {
    socketService.send('/app/resign', {
      gameId: gameStore.gameId,
      playerId: '',
      payload: {},
    })
  }

  function requestDraw(accept: boolean) {
    socketService.send('/app/draw', {
      gameId: gameStore.gameId,
      playerId: '',
      payload: { accept },
    })
  }

  function disconnect() {
    socketService.disconnect()
  }

  function subscribeGame(gameId: string) {
    socketService.subscribeToGame(gameId)
  }

  return { connect, joinQueue, leaveQueue, makeMove, resign, requestDraw, disconnect, subscribeGame }
}
