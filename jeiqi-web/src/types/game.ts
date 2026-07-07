export type Side = 'red' | 'black'

export type PieceType = 'king' | 'rook' | 'knight' | 'cannon' | 'pawn' | 'guard' | 'bishop'

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
  king: '帅',
  rook: '车',
  knight: '马',
  cannon: '炮',
  pawn: '兵',
  guard: '仕',
  bishop: '相',
}

export const PIECE_NAMES_BLACK: Record<string, string> = {
  king: '将',
  rook: '车',
  knight: '马',
  cannon: '炮',
  pawn: '卒',
  guard: '士',
  bishop: '象',
}
