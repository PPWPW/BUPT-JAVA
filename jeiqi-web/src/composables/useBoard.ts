import { ref } from 'vue'
import type { ChessPiece } from '../types/game'

const COLS = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i']

export function useBoard() {
  const selectedPiece = ref<ChessPiece | null>(null)
  const legalTargets = ref<{ col: number; row: number }[]>([])

  function calculateLegalMoves(piece: ChessPiece, allPieces: ChessPiece[]): { col: number; row: number }[] {
    const pos = piece.position
    if (!piece.revealed) {
      return getHiddenMoves(pos, piece.side, allPieces)
    }
    switch (piece.type) {
      case 'king': return getKingMoves(pos, piece.side, allPieces)
      case 'rook': return getChariotMoves(pos, piece.side, allPieces)
      case 'knight': return getHorseMoves(pos, piece.side, allPieces)
      case 'cannon': return getCannonMoves(pos, piece.side, allPieces)
      case 'pawn': return getPawnMoves(pos, piece.side, allPieces)
      case 'guard': return getAdvisorMoves(pos, piece.side, allPieces, false)
      case 'bishop': return getElephantMoves(pos, piece.side, allPieces, false)
      default: return []
    }
  }

  function selectPiece(piece: ChessPiece | null, allPieces: ChessPiece[], myTurn: boolean, mySide: string | null) {
    if (!piece || piece.side !== mySide || !myTurn) {
      selectedPiece.value = null
      legalTargets.value = []
      return
    }
    selectedPiece.value = piece
    legalTargets.value = calculateLegalMoves(piece, allPieces)
  }

  function clearSelection() {
    selectedPiece.value = null
    legalTargets.value = []
  }

  function posToAlgebraic(col: number, row: number): string {
    return COLS[col] + row
  }

  function algebraicToPos(s: string): { col: number; row: number } {
    return { col: COLS.indexOf(s[0]), row: parseInt(s[1]) }
  }

  return { selectedPiece, legalTargets, selectPiece, clearSelection, posToAlgebraic, algebraicToPos }
}

function occupied(pos: { col: number; row: number }, pieces: ChessPiece[]): ChessPiece | null {
  return pieces.find(p => p.alive && p.position.col === pos.col && p.position.row === pos.row) || null
}

function inBounds(col: number, row: number): boolean {
  return col >= 0 && col <= 8 && row >= 0 && row <= 9
}

function canMoveTo(col: number, row: number, side: string, pieces: ChessPiece[]): { col: number; row: number } | null {
  if (!inBounds(col, row)) return null
  const target = occupied({ col, row }, pieces)
  if (target && target.side === side) return null
  return { col, row }
}

function getInitialPieceType(col: number, row: number): string | null {
  const normRow = row >= 5 ? 9 - row : row
  if (normRow === 0) {
    if (col === 0 || col === 8) return 'rook'
    if (col === 1 || col === 7) return 'knight'
    if (col === 2 || col === 6) return 'bishop'
    if (col === 3 || col === 5) return 'guard'
    if (col === 4) return 'king'
  } else if (normRow === 2) {
    if (col === 1 || col === 7) return 'cannon'
  } else if (normRow === 3) {
    if (col === 0 || col === 2 || col === 4 || col === 6 || col === 8) return 'pawn'
  }
  return null
}

function getHiddenMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[]): { col: number; row: number }[] {
  const type = getInitialPieceType(pos.col, pos.row)
  if (!type) return []
  switch (type) {
    case 'king': return getKingMoves(pos, side, pieces)
    case 'rook': return getChariotMoves(pos, side, pieces)
    case 'knight': return getHorseMoves(pos, side, pieces)
    case 'cannon': return getCannonMoves(pos, side, pieces)
    case 'pawn': return getPawnMoves(pos, side, pieces)
    case 'guard': return getAdvisorMoves(pos, side, pieces, true)
    case 'bishop': return getElephantMoves(pos, side, pieces, true)
    default: return []
  }
}

function getKingMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[]): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const dirs = [[-1, 0], [1, 0], [0, -1], [0, 1]]
  const [minR, maxR] = side === 'red' ? [0, 2] : [7, 9]
  for (const [dc, dr] of dirs) {
    const col = pos.col + dc, row = pos.row + dr
    if (col < 3 || col > 5 || row < minR || row > maxR) continue
    const m = canMoveTo(col, row, side, pieces)
    if (m) moves.push(m)
  }
  return moves
}

function getChariotMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[]): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const dirs = [[-1, 0], [1, 0], [0, -1], [0, 1]]
  for (const [dc, dr] of dirs) {
    let col = pos.col + dc, row = pos.row + dr
    while (inBounds(col, row)) {
      const target = occupied({ col, row }, pieces)
      if (target) {
        if (target.side !== side) moves.push({ col, row })
        break
      }
      moves.push({ col, row })
      col += dc; row += dr
    }
  }
  return moves
}

function getHorseMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[]): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const horseMoves = [[-1, -2], [1, -2], [-1, 2], [1, 2], [-2, -1], [-2, 1], [2, -1], [2, 1]]
  const legs = [[0, -1], [0, -1], [0, 1], [0, 1], [-1, 0], [-1, 0], [1, 0], [1, 0]]
  for (let i = 0; i < horseMoves.length; i++) {
    const [dc, dr] = horseMoves[i]
    const [lc, lr] = legs[i]
    if (occupied({ col: pos.col + lc, row: pos.row + lr }, pieces)) continue
    const m = canMoveTo(pos.col + dc, pos.row + dr, side, pieces)
    if (m) moves.push(m)
  }
  return moves
}

function getCannonMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[]): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const dirs = [[-1, 0], [1, 0], [0, -1], [0, 1]]
  for (const [dc, dr] of dirs) {
    let col = pos.col + dc, row = pos.row + dr
    let jumped = false
    while (inBounds(col, row)) {
      const target = occupied({ col, row }, pieces)
      if (!jumped) {
        if (target) { jumped = true }
        else { moves.push({ col, row }) }
      } else {
        if (target) {
          if (target.side !== side) moves.push({ col, row })
          break
        }
      }
      col += dc; row += dr
    }
  }
  return moves
}

function getPawnMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[]): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const forward = side === 'red' ? 1 : -1
  const crossed = side === 'red' ? pos.row >= 5 : pos.row <= 4
  const m = canMoveTo(pos.col, pos.row + forward, side, pieces)
  if (m) moves.push(m)
  if (crossed) {
    const ml = canMoveTo(pos.col - 1, pos.row, side, pieces)
    if (ml) moves.push(ml)
    const mr = canMoveTo(pos.col + 1, pos.row, side, pieces)
    if (mr) moves.push(mr)
  }
  return moves
}

function getAdvisorMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[], restricted = false): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const dirs = [[-1, -1], [-1, 1], [1, -1], [1, 1]]
  const [minR, maxR] = side === 'red' ? [0, 2] : [7, 9]
  for (const [dc, dr] of dirs) {
    const col = pos.col + dc, row = pos.row + dr
    if (restricted) {
      if (col < 3 || col > 5 || row < minR || row > maxR) continue
    }
    const m = canMoveTo(col, row, side, pieces)
    if (m) moves.push(m)
  }
  return moves
}

function getElephantMoves(pos: { col: number; row: number }, side: string, pieces: ChessPiece[], restricted = false): { col: number; row: number }[] {
  const moves: { col: number; row: number }[] = []
  const elephantMoves = [[-2, -2], [-2, 2], [2, -2], [2, 2]]
  const eyes = [[-1, -1], [-1, 1], [1, -1], [1, 1]]
  const [minR, maxR] = side === 'red' ? [0, 4] : [5, 9]
  for (let i = 0; i < elephantMoves.length; i++) {
    const [dc, dr] = elephantMoves[i]
    const [ec, er] = eyes[i]
    const col = pos.col + dc, row = pos.row + dr
    if (restricted) {
      if (row < minR || row > maxR) continue
    }
    if (occupied({ col: pos.col + ec, row: pos.row + er }, pieces)) continue
    const m = canMoveTo(col, row, side, pieces)
    if (m) moves.push(m)
  }
  return moves
}
