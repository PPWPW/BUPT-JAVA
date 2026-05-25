export type Side = 'RED' | 'BLACK'

export type PieceType = 'KING' | 'CHARIOT' | 'HORSE' | 'CANNON' | 'PAWN' | 'ADVISOR' | 'ELEPHANT'

export interface Position {
  col: number
  row: number
}

export interface ChessPiece {
  type: PieceType | null
  side: Side
  revealed: boolean
  position: Position
  alive: boolean
}

export interface MoveRecord {
  source: string
  destination: string
  type: number | null
  moveNumber: number
  side: Side
  revealMove: boolean
  notation: string
}

export type GameStatus = 'WAITING' | 'PLAYING' | 'FINISHED'

export interface GameState {
  id: string
  status: GameStatus
  redPlayer: string
  blackPlayer: string
  currentTurn: Side
  mySide: Side | null
  pieces: ChessPiece[]
  capturedPieces: ChessPiece[]
  moveHistory: MoveRecord[]
  winner: Side | null
  reason: string | null
}

export const PIECE_NAMES: Record<string, string> = {
  KING: '将',
  CHARIOT: '车',
  HORSE: '马',
  CANNON: '炮',
  PAWN: '兵',
  ADVISOR: '士',
  ELEPHANT: '象',
}

export const PIECE_NAMES_BLACK: Record<string, string> = {
  KING: '帅',
  CHARIOT: '車',
  HORSE: '馬',
  CANNON: '砲',
  PAWN: '卒',
  ADVISOR: '仕',
  ELEPHANT: '相',
}
