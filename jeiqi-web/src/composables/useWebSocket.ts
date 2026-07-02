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
    socketService.send('/app/join', {
      playerId: userStore.userId || '',
      payload: { username: userStore.username || 'Unknown' },
    })
  }

  function leaveQueue() {
    socketService.send('/app/leave', {
      playerId: userStore.userId || '',
      payload: {},
    })
  }

  function makeMove(source: string, destination: string) {
    socketService.send('/app/move', {
      gameId: gameStore.gameId,
      playerId: userStore.userId || '',
      payload: { source, destination },
    })
  }

  function resign() {
    socketService.send('/app/resign', {
      gameId: gameStore.gameId,
      playerId: userStore.userId || '',
      payload: {},
    })
  }

  function requestDraw(accept: boolean) {
    socketService.send('/app/draw', {
      gameId: gameStore.gameId,
      playerId: userStore.userId || '',
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
