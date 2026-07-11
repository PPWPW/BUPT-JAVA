<template>
  <div class="replay-view">
    <div class="replay-container">
      <!-- 棋盘显示区域 (左侧) -->
      <div class="board-area">
        <ChessBoard
          :pieces="pieces"
          :selectedPos="null"
          :legalMoves="[]"
          :mySide="'red'"
          :myTurn="false"
          :lastMove="lastMove"
        />
      </div>

      <!-- 复盘控制面板与历史步骤 (右侧) -->
      <div class="control-panel">
        <h2 class="title">棋谱复盘</h2>
        
        <div v-if="notation" class="notation-details">
          <!-- 双方对决卡片 -->
          <div class="players-banner">
            <span class="player red">帅 {{ notation.redPlayerName || '红方' }}</span>
            <span class="vs">VS</span>
            <span class="player black">将 {{ notation.blackPlayerName || '黑方' }}</span>
          </div>
          
          <!-- 对局战果 -->
          <div class="result-badge" :class="resultClass">
            战果: {{ resultText }} ({{ getReasonCN(notation.reason) }})
          </div>

          <!-- 控制器按钮 -->
          <div class="replay-controls">
            <button class="btn-control" @click="step = 0" :disabled="step <= 0" title="回到开局">⏮️ 重置</button>
            <button class="btn-control" @click="prevMove" :disabled="step <= 0">◀️ 上一步</button>
            <span class="step-indicator">{{ step }} / {{ notation.moves.length }}</span>
            <button class="btn-control" @click="nextMove" :disabled="step >= notation.moves.length">下一步 ▶️</button>
            <button class="btn-control" @click="toggleAutoPlay" :class="{ playing: autoPlayTimer }">
              {{ autoPlayTimer ? '⏹️ 停止' : '▶️ 自动播放' }}
            </button>
          </div>

          <!-- 走子文字步骤流水 -->
          <div class="moves-log-wrapper" ref="movesLogRef">
            <div class="moves-grid">
              <div 
                v-for="(m, i) in decoratedMoves" 
                :key="i" 
                class="move-row" 
                :class="{ active: i < step, 'current-step': i === step - 1 }"
                @click="jumpToStep(i + 1)"
              >
                <span class="order">{{ i + 1 }}.</span>
                <span class="desc">{{ getMoveDesc(m) }}</span>
                <span class="side-indicator" :class="m.side.toLowerCase()"></span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="loading-state">加载中...</div>

        <button class="back-btn" @click="goBack">返回棋谱列表</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNotation } from '../services/api'
import ChessBoard from '../components/board/ChessBoard.vue'
import type { ChessPiece, Side, PieceType } from '../types/game'

const route = useRoute()
const router = useRouter()
const notation = ref<any>(null)
const step = ref(0)
const autoPlayTimer = ref<any>(null)
const movesLogRef = ref<HTMLElement | null>(null)

// 揭棋标准的初始物理棋盘布局 (除帅/将外其余全为暗子)
function getInitialBoard(): ChessPiece[] {
  const piecesList: ChessPiece[] = []

  const addPiece = (col: number, row: number, type: PieceType, side: Side, revealed = false) => {
    piecesList.push({
      type,
      side,
      revealed,
      position: { col, row },
      alive: true
    })
  }

  // 红方开局 (第 0 - 3 行)
  addPiece(0, 0, 'rook', 'red')
  addPiece(1, 0, 'knight', 'red')
  addPiece(2, 0, 'bishop', 'red')
  addPiece(3, 0, 'guard', 'red')
  addPiece(4, 0, 'king', 'red', true) // 将帅默认是明子
  addPiece(5, 0, 'guard', 'red')
  addPiece(6, 0, 'bishop', 'red')
  addPiece(7, 0, 'knight', 'red')
  addPiece(8, 0, 'rook', 'red')
  
  addPiece(1, 2, 'cannon', 'red')
  addPiece(7, 2, 'cannon', 'red')
  
  addPiece(0, 3, 'pawn', 'red')
  addPiece(2, 3, 'pawn', 'red')
  addPiece(4, 3, 'pawn', 'red')
  addPiece(6, 3, 'pawn', 'red')
  addPiece(8, 3, 'pawn', 'red')

  // 黑方开局 (第 6 - 9 行)
  addPiece(0, 9, 'rook', 'black')
  addPiece(1, 9, 'knight', 'black')
  addPiece(2, 9, 'bishop', 'black')
  addPiece(3, 9, 'guard', 'black')
  addPiece(4, 9, 'king', 'black', true) // 将帅默认是明子
  addPiece(5, 9, 'guard', 'black')
  addPiece(6, 9, 'bishop', 'black')
  addPiece(7, 9, 'knight', 'black')
  addPiece(8, 9, 'rook', 'black')
  
  addPiece(1, 7, 'cannon', 'black')
  addPiece(7, 7, 'cannon', 'black')
  
  addPiece(0, 6, 'pawn', 'black')
  addPiece(2, 6, 'pawn', 'black')
  addPiece(4, 6, 'pawn', 'black')
  addPiece(6, 6, 'pawn', 'black')
  addPiece(8, 6, 'pawn', 'black')

  return piecesList
}

// 核心计算属性：通过回放 0 到 step.value 步的动作，还原当前棋盘上的明暗与存活状态
const pieces = computed(() => {
  if (!notation.value) return []
  const initial = getInitialBoard()
  const typeMap: PieceType[] = ['king', 'rook', 'knight', 'cannon', 'pawn', 'guard', 'bishop']

  for (let i = 0; i < step.value; i++) {
    const m = notation.value.moves[i]
    // 解析字符坐标，例如 "b2" -> col: 1, row: 2
    const fromCol = m.source.charCodeAt(0) - 97
    const fromRow = parseInt(m.source.substring(1))
    const toCol = m.destination.charCodeAt(0) - 97
    const toRow = parseInt(m.destination.substring(1))

    // 寻找移动棋子
    const moving = initial.find(p => p.alive && p.position.col === fromCol && p.position.row === fromRow)
    if (moving) {
      // 处理吃子
      const target = initial.find(p => p.alive && p.position.col === toCol && p.position.row === toRow)
      if (target) {
        target.alive = false
      }
      // 棋子位移
      moving.position = { col: toCol, row: toRow }
      // 如果这一步翻开了暗子，赋予其真实类型，变更为明子
      if (m.revealMove && m.type !== null && m.type !== undefined) {
        moving.revealed = true
        moving.type = typeMap[m.type]
      }
    }
  }

  return initial
})

// 核心计算属性：计算每个落子历史步骤的元数据（包括移动前棋子身份、是否吃子及吃子类型），用于在侧边栏显示详情
const decoratedMoves = computed(() => {
  if (!notation.value) return []
  const initial = getInitialBoard()
  const typeMap: PieceType[] = ['king', 'rook', 'knight', 'cannon', 'pawn', 'guard', 'bishop']
  const result: any[] = []

  for (let i = 0; i < notation.value.moves.length; i++) {
    const m = notation.value.moves[i]
    const decorated = { ...m }

    const fromCol = m.source.charCodeAt(0) - 97
    const fromRow = parseInt(m.source.substring(1))
    const toCol = m.destination.charCodeAt(0) - 97
    const toRow = parseInt(m.destination.substring(1))

    const moving = initial.find(p => p.alive && p.position.col === fromCol && p.position.row === fromRow)
    
    if (moving) {
      decorated.movingType = moving.type
      decorated.movingRevealed = moving.revealed

      const target = initial.find(p => p.alive && p.position.col === toCol && p.position.row === toRow)
      if (target) {
        decorated.captured = true
        decorated.capturedType = target.type
        decorated.capturedRevealed = target.revealed
        target.alive = false
      } else {
        decorated.captured = false
      }

      moving.position = { col: toCol, row: toRow }
      
      if (m.revealMove && m.type !== null && m.type !== undefined) {
        moving.revealed = true
        moving.type = typeMap[m.type]
        decorated.revealedType = moving.type
      }
    } else {
      decorated.movingType = null
      decorated.movingRevealed = false
      decorated.captured = false
    }

    result.push(decorated)
  }

  return result
})

// 计算上一手走法，用于在 ChessBoard 上高亮黄框
const lastMove = computed(() => {
  if (!notation.value || step.value <= 0) return null
  const m = notation.value.moves[step.value - 1]
  return {
    source: m.source,
    destination: m.destination,
    type: m.type,
    moveNumber: step.value,
    side: m.side.toLowerCase() as Side,
    revealMove: m.revealMove,
    notation: m.source + m.destination
  }
})

// 结果翻译
const resultText = computed(() => {
  if (!notation.value) return ''
  const r = notation.value.result
  if (r === '1-0') return '红方获胜'
  if (r === '0-1') return '黑方获胜'
  if (r === '1/2-1/2') return '和棋'
  return r
})

const resultClass = computed(() => {
  if (!notation.value) return ''
  const r = notation.value.result
  if (r === '1-0') return 'red-win'
  if (r === '0-1') return 'black-win'
  return 'draw'
})

onMounted(async () => {
  try {
    notation.value = await getNotation(route.params.id as string)
    // 默认从第 0 步开局状态展示
    step.value = 0
  } catch (e) {
    console.error('Failed to load chess notation:', e)
  }
})

function prevMove() {
  if (step.value > 0) step.value--
}

function nextMove() {
  if (notation.value && step.value < notation.value.moves.length) {
    step.value++
  } else {
    stopAutoPlay()
  }
}

function jumpToStep(targetStep: number) {
  step.value = targetStep
}

function toggleAutoPlay() {
  if (autoPlayTimer.value) {
    stopAutoPlay()
  } else {
    autoPlayTimer.value = setInterval(() => {
      nextMove()
    }, 1500)
  }
}

// 停止自动播放
function stopAutoPlay() {
  if (autoPlayTimer.value) {
    clearInterval(autoPlayTimer.value)
    autoPlayTimer.value = null
  }
}

function goBack() {
  stopAutoPlay()
  router.push('/notations')
}

// 转换走棋中文描述，包含翻棋与吃子详情（例如：红方: g3 ➡️ g4 【暗子(翻棋:车)】）
function getMoveDesc(m: any): string {
  const sideText = m.side.toLowerCase() === 'red' ? '红' : '黑'
  const typeMapCN: Record<string, string> = {
    king: '帅/将',
    rook: '车',
    knight: '马',
    cannon: '炮',
    pawn: '兵/卒',
    guard: '仕',
    bishop: '相/象'
  }
  
  // 移动前的棋子类型名称
  const movingName = m.movingRevealed ? (typeMapCN[m.movingType] || '棋子') : '暗子'
  
  const details: string[] = []
  
  // 1. 移动棋子信息（若为翻棋则特殊标注）
  if (m.revealMove) {
    const revealedName = typeMapCN[m.revealedType] || '未知'
    details.push(`${movingName}(翻棋:${revealedName})`)
  } else {
    details.push(movingName)
  }
  
  // 2. 吃子信息
  if (m.captured) {
    const capName = m.capturedRevealed ? (typeMapCN[m.capturedType] || '棋子') : '暗子'
    details.push(`吃${capName}`)
  }
  
  const detailText = details.join('，')
  return `${sideText}方: ${m.source} ➡️ ${m.destination} 【${detailText}】`
}

function getReasonCN(reason: string): string {
  if (!reason) return '未知原因'
  const map: Record<string, string> = {
    CHECKMATE: '绝杀',
    STALEMATE: '困毙',
    TIMEOUT: '超时',
    RESIGN: '认输',
    NO_CAPTURE_DRAW: '80步和棋',
    PERPETUAL_CHECK: '长捉长将违规'
  }
  return map[reason] || reason
}

// 步数变化时，自动将步骤流水滚动到可视区域
watch(step, () => {
  nextTick(() => {
    const activeEl = movesLogRef.value?.querySelector('.current-step') as HTMLElement
    if (activeEl && movesLogRef.value) {
      const top = activeEl.offsetTop - movesLogRef.value.clientHeight / 2 + activeEl.clientHeight / 2
      movesLogRef.value.scrollTo({ top, behavior: 'smooth' })
    }
  })
})
</script>

<style scoped>
.replay-view {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1a1a2e;
  padding: 20px;
  box-sizing: border-box;
  --cell-size: min(58px, 8.5vh);
}

.replay-container {
  display: flex;
  gap: 40px;
  align-items: stretch;
  max-width: 1200px;
  width: 100%;
}

.board-area {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.control-panel {
  width: 380px;
  background: #16213e;
  border-radius: 12px;
  padding: 24px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0,0,0,0.3);
}

.title {
  text-align: center;
  color: #e94560;
  margin-top: 0;
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: bold;
}

.notation-details {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.players-banner {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: #0f3460;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: bold;
}

.player.red { color: #e94560; }
.player.black { color: #3498db; }
.vs { color: #888; font-size: 12px; }

.result-badge {
  text-align: center;
  padding: 8px;
  border-radius: 6px;
  font-size: 14px;
  margin-bottom: 16px;
  font-weight: bold;
}
.result-badge.red-win { background: rgba(233, 69, 96, 0.2); color: #e94560; }
.result-badge.black-win { background: rgba(52, 152, 219, 0.2); color: #3498db; }
.result-badge.draw { background: rgba(255, 255, 255, 0.1); color: #aaa; }

.replay-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  align-items: center;
  margin-bottom: 16px;
}

.btn-control {
  background: #0f3460;
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.2s;
}
.btn-control:hover:not(:disabled) { background: #1f4068; }
.btn-control:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-control.playing { background: #e94560; }

.step-indicator {
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  min-width: 60px;
  text-align: center;
}

.moves-log-wrapper {
  flex: 1;
  background: #1a1a2e;
  border-radius: 8px;
  padding: 12px;
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #222;
}

.moves-grid {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.move-row {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.move-row:hover { background: rgba(255, 255, 255, 0.05); color: #aaa; }
.move-row.active { color: #ccc; }
.move-row.current-step { background: rgba(233, 69, 96, 0.25); color: #fff; font-weight: bold; }

.order { width: 32px; color: #888; }
.desc { flex: 1; }
.side-indicator { width: 6px; height: 6px; border-radius: 50%; }
.side-indicator.red { background: #e94560; }
.side-indicator.black { background: #3498db; }

.back-btn {
  margin-top: 16px;
  width: 100%;
  background: #0f3460;
  color: #fff;
  border: none;
  padding: 10px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s;
}
.back-btn:hover { background: #1f4068; }

.loading-state {
  text-align: center;
  padding: 40px;
  color: #888;
}

/* 响应式适配 */
@media (max-width: 950px) {
  .replay-container {
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }
  .control-panel {
    width: 100%;
    max-width: 500px;
  }
  .replay-view {
    --cell-size: min(44px, 6.0vh);
  }
}
</style>
