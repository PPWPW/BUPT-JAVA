import { Client, IFrame, IMessage } from '@stomp/stompjs'

class SocketService {
  private client: Client | null = null
  private onMessage: ((msg: any) => void) | null = null

  connect(userId: string, callback: () => void) {
    const wsUrl = `ws://${window.location.hostname}:8080/ws`
    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })

    this.client.onConnect = () => {
      this.client?.subscribe(`/user/queue/game`, (msg: IMessage) => {
        if (this.onMessage) {
          this.onMessage(JSON.parse(msg.body))
        }
      })
      callback()
    }

    this.client.onStompError = (frame: IFrame) => {
      console.error('STOMP error:', frame.headers['message'])
    }

    this.client.activate()
  }

  subscribeToGame(gameId: string) {
    this.client?.subscribe(`/topic/game/${gameId}`, (msg: IMessage) => {
      if (this.onMessage) {
        this.onMessage(JSON.parse(msg.body))
      }
    })
  }

  setMessageHandler(handler: (msg: any) => void) {
    this.onMessage = handler
  }

  send(destination: string, body: Record<string, unknown>) {
    this.client?.publish({
      destination,
      body: JSON.stringify(body),
    })
  }

  disconnect() {
    this.client?.deactivate()
  }
}

export const socketService = new SocketService()
