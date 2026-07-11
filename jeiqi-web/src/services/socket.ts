class SocketService {
  private socket: WebSocket | null = null
  private onMessage: ((msg: any) => void) | null = null
  private heartbeatInterval: any = null
  private userId: string | null = null
  private connectCallback: (() => void) | null = null
  private isReconnecting = false
  private reconnectTimer: any = null

  connect(userId: string, callback: () => void) {
    this.userId = userId
    this.connectCallback = callback
    
    // BINGO: If socket is already OPEN, invoke the callback immediately!
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected, executing callback immediately')
      callback()
      return
    }
    
    this.doConnect()
  }

  private doConnect() {
    // Prevent duplicated connection attempts
    if (this.socket && (this.socket.readyState === WebSocket.CONNECTING || this.socket.readyState === WebSocket.OPEN)) {
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    const wsUrl = `${protocol}://${window.location.host}/ws`
    console.log(`Connecting to WebSocket: ${wsUrl}`)
    this.socket = new WebSocket(wsUrl)

    this.socket.onopen = () => {
      console.log('Raw WebSocket connected')
      this.isReconnecting = false
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }

      // Login automatically upon connecting
      const password = localStorage.getItem('userPassword') || '123456'
      this.send({
        messageType: 'Login',
        userId: this.userId || '',
        password: password
      })

      if (this.heartbeatInterval) {
        clearInterval(this.heartbeatInterval)
      }
      this.heartbeatInterval = setInterval(() => {
        this.send({
          messageType: 'ping',
          timestamp: Date.now()
        })
      }, 10000)

      // Trigger the connection success callback (which will request getBoardState to restore play)
      if (this.connectCallback) {
        this.connectCallback()
      }
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
        this.heartbeatInterval = null
      }
      this.triggerReconnect()
    }

    this.socket.onerror = (err) => {
      console.error('WebSocket error:', err)
      this.socket?.close()
    }
  }

  private triggerReconnect() {
    if (this.isReconnecting) return
    this.isReconnecting = true
    console.log('WebSocket connection lost. Attempting to reconnect in 3 seconds...')
    this.reconnectTimer = setTimeout(() => {
      this.doConnect()
    }, 3000)
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
    this.isReconnecting = false
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
    if (this.socket) {
      this.socket.close()
    }
  }
}

export const socketService = new SocketService()
