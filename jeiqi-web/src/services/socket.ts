class SocketService {
  private socket: WebSocket | null = null
  private onMessage: ((msg: any) => void) | null = null
  private heartbeatInterval: any = null

  connect(userId: string, callback: () => void) {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const wsUrl = `${protocol}://${window.location.host}/ws`
    this.socket = new WebSocket(wsUrl)

    this.socket.onopen = () => {
      console.log('Raw WebSocket connected')
      // Login automatically upon connecting
      const password = localStorage.getItem('userPassword') || '123456'
      this.send({
        messageType: 'Login',
        userId: userId,
        password: password
      })

      this.heartbeatInterval = setInterval(() => {
        this.send({
          messageType: 'ping',
          timestamp: Date.now()
        })
      }, 10000)

      callback()
    }

    this.socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.messageType === 'pong') {
          return
        }
        if (this.onMessage) {
          this.onMessage(msg)
        }
      } catch (e) {
        console.error('Error parsing WS message:', e)
      }
    }

    this.socket.onclose = () => {
      console.log('WebSocket closed')
      if (this.heartbeatInterval) {
        clearInterval(this.heartbeatInterval)
      }
    }

    this.socket.onerror = (err) => {
      console.error('WebSocket error:', err)
    }
  }

  subscribeToGame(gameId: string) {
    // Under raw WebSockets, the server manages session subscriptions automatically.
  }

  setMessageHandler(handler: (msg: any) => void) {
    this.onMessage = handler
  }

  send(body: Record<string, unknown>) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(body))
    } else {
      console.warn('Socket not open, message not sent:', body)
    }
  }

  disconnect() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
    }
    if (this.socket) {
      this.socket.close()
    }
  }
}

export const socketService = new SocketService()
