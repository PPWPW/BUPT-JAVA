<template>
  <div class="game-view">
    <!-- Waiting Room Screen -->
    <div v-if="gameStore.status === 'WAITING'" class="waiting-screen">
      <div class="waiting-card animate-fade-in">
        <h2>等待对手加入...</h2>
        <div class="pulse-ring"></div>
        <p class="subtitle">分享房间号给好友即可开始对局</p>
        
        <div class="room-id-box">
          <span class="room-label">房间号</span>
          <span class="room-value">{{ gameStore.gameId }}</span>
          <button class="btn-primary copy-btn" @click="onCopyRoomId">
            {{ copied ? '已复制' : '复制' }}
          </button>
        </div>
        
        <button class="btn-secondary cancel-waiting-btn" @click="onExitToLobby">取消创建</button>
      </div>
    </div>

    <!-- Active Game Screen -->
    <div v-else class="game-main animate-fade-in">
      <div class="game-board-area">
        <!-- Top Player Row (Opponent) -->
        <div class="player-row top" :class="{ active: isTopActive }">
          <div class="player-info">
            <span class="avatar-dot" :class="topPlayerSide"></span>
            <span class="username">{{ topPlayerName }}</span>
            
            <!-- Floating bubble for top player -->
            <transition name="pop">
              <div v-if="gameStore.activeEmoteTop" class="chat-bubble top">
                {{ gameStore.activeEmoteTop.content }}
              </div>
            </transition>

            <div class="captured-mini-list">
              <div 
                v-for="(p, index) in topCaptures" 
                :key="index" 
                class="mini-piece" 
                :class="[p.side.toLowerCase(), { hidden: !p.revealed }]"
              >
                {{ getPieceText(p) }}
              </div>
            </div>
          </div>
          
          <div class="player-row-right">
            <div class="player-timer" :class="{ active: isTopActive, black: topPlayerSide === 'black', low: isTopActive && topTime <= 10 }">
              {{ topTime }}s
            </div>
            <!-- Mute button (only for players, not spectators) -->
            <button 
              v-if="gameStore.mySide" 
              class="mute-btn" 
              :class="{ muted: gameStore.opponentMuted }"
              @click="gameStore.opponentMuted = !gameStore.opponentMuted"
              :title="gameStore.opponentMuted ? '取消屏蔽对方' : '屏蔽对方表情'"
            >
              {{ gameStore.opponentMuted ? '🔇' : '🔊' }}
            </button>
          </div>
        </div>

        <!-- Chessboard Grid -->
        <div class="board-container-wrapper">
          <ChessBoard
            :pieces="gameStore.pieces"
            :selectedPos="gameStore.selectedPos"
            :legalMoves="gameStore.legalMoves"
            :mySide="gameStore.mySide"
            :myTurn="isMyTurn"
            :lastMove="lastMove"
            @cellClick="onCellClick"
          />
        </div>

        <!-- Bottom Player Row (User) -->
        <div class="player-row bottom" :class="{ active: isBottomActive }">
          <div class="player-info">
            <span class="avatar-dot" :class="bottomPlayerSide"></span>
            <span class="username">{{ bottomPlayerName }}</span>
            
            <!-- Floating bubble for bottom player -->
            <transition name="pop">
              <div v-if="gameStore.activeEmoteBottom" class="chat-bubble bottom">
                {{ gameStore.activeEmoteBottom.content }}
              </div>
            </transition>

            <div class="captured-mini-list">
              <div 
                v-for="(p, index) in bottomCaptures" 
                :key="index" 
                class="mini-piece" 
                :class="[p.side.toLowerCase(), { hidden: !p.revealed }]"
              >
                {{ getPieceText(p) }}
              </div>
            </div>
          </div>
          
          <div class="player-row-right">
            <!-- Chat panel trigger button -->
            <div v-if="gameStore.mySide" class="chat-trigger-wrapper">
              <button class="chat-trigger-btn" @click.stop="toggleChatPanel" title="发送表情或常用语">💬</button>
              
              <!-- Popover Panel -->
              <div v-if="showChatPanel" class="chat-popover-panel" @click.stop>
                <div class="chat-popover-tabs">
                  <button :class="{ active: activeTab === 'emoji' }" @click="activeTab = 'emoji'">表情</button>
                  <button :class="{ active: activeTab === 'phrase' }" @click="activeTab = 'phrase'">常用语</button>
                </div>
                <div class="chat-popover-body">
                  <div v-if="activeTab === 'emoji'" class="emoji-picker-grid">
                    <button v-for="em in emojis" :key="em" @click="sendEmoji(em)" class="emoji-btn">{{ em }}</button>
                  </div>
                  <div v-else class="phrase-picker-list">
                    <button v-for="ph in phrases" :key="ph" @click="sendPhrase(ph)" class="phrase-btn">{{ ph }}</button>
                  </div>
                </div>
              </div>
              
              <!-- Backdrop to close popup on click outside -->
              <div v-if="showChatPanel" class="chat-popover-backdrop" @click="showChatPanel = false"></div>
            </div>

            <div class="player-timer" :class="{ active: isBottomActive, black: bottomPlayerSide === 'black', low: isBottomActive && bottomTime <= 10 }">
              {{ bottomTime }}s
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel Sidebar -->
      <div class="sidebar">
        <div class="room-details-card">
          <div class="room-title">房间详情</div>
          <div class="room-info-row">
            <span class="label">房间号:</span>
            <span class="value">{{ gameStore.gameId }}</span>
          </div>
          <div class="room-info-row" v-if="gameStore.mySide === null">
            <span class="value spec-badge">正在观战中</span>
          </div>
          <div class="turn-status-banner" :class="{ 'my-turn': isMyTurn && gameStore.mySide }">
            <span class="pulse-dot"></span>
            {{ turnMessage }}
          </div>
        </div>

        <MoveHistory :moves="gameStore.moveHistory" />

        <div class="sidebar-actions">
          <template v-if="gameStore.mySide && gameStore.status === 'PLAYING'">
            <button class="btn-primary resign-btn" @click="onResign">认输</button>
            <button class="btn-secondary draw-btn" @click="onDraw">求和</button>
          </template>
          <button class="btn-secondary lobby-btn" @click="onExitToLobby">返回大厅</button>
        </div>
      </div>
    </div>

    <!-- Game Results Overlay Modal -->
    <GameResult
      :visible="showResult"
      :winner="gameStore.winner"
      :reason="gameStore.reason"
      :mySide="gameStore.mySide"
      :showReplay="true"
      :rematchStatus="gameStore.rematchStatus"
      @close="onExitToLobby"
      @replay="router.push(`/replay/${gameStore.gameId}`)"
      @requestRematch="onRequestRematch"
      @acceptRematch="onAcceptRematch"
      @declineRematch="onDeclineRematch"
    />

    <!-- Draw Request Modal -->
    <div v-if="showDrawRequestModal" class="overlay-draw">
      <div class="draw-dialog">
        <h3>求和申请</h3>
        <p>对方发起了求和申请，您是否同意和棋？</p>
        <div class="draw-btns">
          <button class="btn-primary" @click="onAcceptDraw">同意</button>
          <button class="btn-secondary" @click="onRejectDraw">拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useUserStore } from '../stores/userStore'
import { useGame } from '../composables/useGame'
import { useTimer } from '../composables/useTimer'
import ChessBoard from '../components/board/ChessBoard.vue'
import MoveHistory from '../components/game/MoveHistory.vue'
import GameResult from '../components/game/GameResult.vue'

const router = useRouter()
const route = useRoute()
const gameStore = useGameStore()
const userStore = useUserStore()
const { handleCellClick, board, ws } = useGame()
const timer = useTimer(60)
const showResult = ref(false)
const showDrawRequestModal = ref(false)
const copied = ref(false)

const isMyTurn = computed(() => gameStore.currentTurn === gameStore.mySide)

const lastMove = computed(() => {
  if (gameStore.moveHistory.length === 0) return null
  return gameStore.moveHistory[gameStore.moveHistory.length - 1]
})

const turnMessage = computed(() => {
  if (gameStore.status === 'FINISHED') return '对局已结束'
  if (!gameStore.mySide) {
    return gameStore.currentTurn === 'red' ? '红方思考中...' : '黑方思考中...'
  }
  return isMyTurn.value ? '您的回合，请走子' : '对方正在思考中...'
})

// Player profile computed properties
const bottomPlayerSide = computed(() => {
  return gameStore.mySide === 'black' ? 'black' : 'red'
})

const topPlayerSide = computed(() => {
  return gameStore.mySide === 'black' ? 'red' : 'black'
})

const bottomPlayerName = computed(() => {
  if (gameStore.mySide === 'black') return gameStore.blackPlayer || '黑方玩家'
  return gameStore.redPlayer || '红方玩家'
})

const topPlayerName = computed(() => {
  if (gameStore.mySide === 'black') return gameStore.redPlayer || '红方玩家'
  return gameStore.blackPlayer || '黑方玩家'
})

const isBottomActive = computed(() => {
  if (gameStore.status !== 'PLAYING') return false
  return gameStore.currentTurn === bottomPlayerSide.value
})

const isTopActive = computed(() => {
  if (gameStore.status !== 'PLAYING') return false
  return gameStore.currentTurn === topPlayerSide.value
})

const bottomTime = computed(() => {
  return isBottomActive.value ? timer.remaining.value : 60
})

const topTime = computed(() => {
  return isTopActive.value ? timer.remaining.value : 60
})

const bottomCaptures = computed(() => {
  const side = bottomPlayerSide.value
  return gameStore.capturedPieces.filter(p => p.side !== side)
})

const topCaptures = computed(() => {
  const side = topPlayerSide.value
  return gameStore.capturedPieces.filter(p => p.side !== side)
})

const PIECE_NAMES: Record<string, string> = {
  king: '帅', rook: '车', knight: '马', cannon: '炮', pawn: '兵', guard: '仕', bishop: '相'
}

const PIECE_NAMES_BLACK: Record<string, string> = {
  king: '将', rook: '车', knight: '马', cannon: '炮', pawn: '卒', guard: '士', bishop: '象'
}

function getPieceText(p: any): string {
  if (!p.revealed || !p.type) return '?'
  return p.side === 'red' ? PIECE_NAMES[p.type] || '?' : PIECE_NAMES_BLACK[p.type] || '?'
}

function onCellClick(col: number, row: number) {
  if (!gameStore.mySide) return // Spectators cannot play moves
  handleCellClick(col, row)
}

function onResign() {
  ws.resign()
}

function onDraw() {
  ws.requestDraw(true)
}

function onAcceptDraw() {
  ws.requestDraw(true)
  showDrawRequestModal.value = false
  gameStore.drawRequestReceived = false
}

function onRejectDraw() {
  ws.requestDraw(false)
  showDrawRequestModal.value = false
  gameStore.drawRequestReceived = false
}

function onCopyRoomId() {
  if (gameStore.gameId) {
    navigator.clipboard.writeText(gameStore.gameId)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  }
}

function onExitToLobby() {
  if (gameStore.mySide && gameStore.status === 'PLAYING') {
    if (confirm('对局正在进行中，直接返回大厅将视作认输，是否确定？')) {
      ws.resign()
      setTimeout(() => {
        if (gameStore.gameId) {
          ws.leaveRoom(gameStore.gameId)
        }
        router.push('/')
      }, 300)
    }
  } else {
    if (gameStore.gameId) {
      ws.leaveRoom(gameStore.gameId)
    }
    router.push('/')
  }
}

onMounted(() => {
  const roomId = route.params.id as string
  const isSpectator = route.query.spectate === 'true'

  if (!userStore.userId) {
    router.push('/')
    return
  }

  ws.connect(userStore.username!, () => {
    if (isSpectator) {
      ws.spectateGame(roomId)
    } else {
      ws.getBoardState(roomId)
    }
  })
})

watch(() => gameStore.status, (s) => {
  if (s === 'PLAYING') {
    timer.reset(60)
    timer.start()
    showResult.value = false
  }
  if (s === 'FINISHED') {
    timer.stop()
    showResult.value = true
  }
})

watch(() => gameStore.currentTurn, () => {
  if (gameStore.status === 'PLAYING') {
    timer.reset(60)
    timer.start()
  }
})

watch(() => gameStore.drawRequestReceived, (val) => {
  showDrawRequestModal.value = val
})

watch(() => gameStore.drawRejected, (val) => {
  if (val) {
    alert('对方拒绝了您的求和申请。')
    gameStore.drawRejected = false
  }
})

const showChatPanel = ref(false)
const activeTab = ref<'emoji' | 'phrase'>('emoji')
const emojis = ['😄', '😭', '😠', '👍', '😮', '🤝']
const phrases = [
  '祝你好运！',
  '承让承让！',
  '手滑了...',
  '再来一局？',
  '精彩的对局！',
  '思考中...'
]

function toggleChatPanel() {
  showChatPanel.value = !showChatPanel.value
}

function sendEmoji(emoji: string) {
  if (gameStore.gameId) {
    ws.sendChat(gameStore.gameId, 'EMOTE', emoji)
  }
  showChatPanel.value = false
}

function sendPhrase(phrase: string) {
  if (gameStore.gameId) {
    ws.sendChat(gameStore.gameId, 'PHRASE', phrase)
  }
  showChatPanel.value = false
}

function onRequestRematch() {
  if (gameStore.gameId) {
    ws.requestRematch(gameStore.gameId)
    gameStore.rematchStatus = 'SENT'
  }
}

function onAcceptRematch() {
  if (gameStore.gameId) {
    ws.acceptRematch(gameStore.gameId)
  }
}

function onDeclineRematch() {
  if (gameStore.gameId) {
    ws.declineRematch(gameStore.gameId)
    gameStore.rematchStatus = 'NONE'
  }
}

onUnmounted(() => {
  if (gameStore.gameId) {
    ws.leaveRoom(gameStore.gameId)
  }
  gameStore.reset()
})
</script>

<style scoped>
.game-view {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  overflow: hidden;
  padding: 10px;
  background: #1a1a2e;
  box-sizing: border-box;
  --cell-size: min(76px, 7.8vh);
}

.game-main {
  display: flex;
  gap: 30px;
  align-items: stretch;
}

/* Board Area & Player Rows */
.game-board-area {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
}

.player-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: calc(var(--cell-size) * 9);
  margin-left: calc(var(--cell-size) * 0.5);
  padding: 4px 8px;
  box-sizing: border-box;
  background: transparent;
  transition: all 0.3s;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.avatar-dot.red {
  background: #e94560;
  box-shadow: 0 0 6px #e94560;
}

.avatar-dot.black {
  background: #3498db;
  box-shadow: 0 0 6px #3498db;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #a0aec0;
}

.player-row.active .username {
  color: #fff;
  font-weight: bold;
}

.captured-mini-list {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}

.mini-piece {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  box-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.mini-piece.red {
  background: #fff5e6;
  color: #c0392b;
  border: 1px solid #c0392b;
}

.mini-piece.black {
  background: #fff5e6;
  color: #2c3e50;
  border: 1px solid #2c3e50;
}

.mini-piece.hidden {
  background: #1e293b;
  color: #64748b;
  border: 1px solid #475569;
}

.player-timer {
  background: #16213e;
  color: #4b5563;
  border: 1px solid #1f2937;
  padding: 4px 10px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
  font-weight: bold;
  transition: all 0.3s;
}

.player-timer.active {
  background: #e94560;
  color: #fff;
  border-color: #e94560;
}

.player-timer.active.black {
  background: #3498db;
  border-color: #3498db;
}

.player-timer.active.low {
  animation: timer-pulse 1s infinite alternate;
}

.board-container-wrapper {
  border: 2px solid #2d3748;
  border-radius: 8px;
  padding: 4px;
  background: #2d3748;
}

/* Sidebar styling */
.sidebar {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.room-details-card {
  background: #16213e;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #1f2937;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}

.room-title {
  font-size: 15px;
  font-weight: bold;
  color: #e94560;
  border-bottom: 1px solid #1f2937;
  padding-bottom: 6px;
}

.room-info-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.room-info-row .label {
  color: #8892b0;
}

.room-info-row .value {
  font-weight: bold;
  color: #fff;
}

.spec-badge {
  background: rgba(243, 156, 18, 0.15);
  color: #f39c12;
  border: 1px solid #f39c12;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  display: inline-block;
}

.turn-status-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #0f172a;
  border-radius: 6px;
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}

.turn-status-banner.my-turn {
  color: #2ecc71;
  background: rgba(46, 204, 113, 0.1);
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9ca3af;
}

.turn-status-banner.my-turn .pulse-dot {
  background: #2ecc71;
  animation: pulse 1.5s infinite;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.resign-btn {
  background: #c0392b !important;
}

.resign-btn:hover {
  background: #e74c3c !important;
}

/* Waiting Room styling */
.waiting-screen {
  position: fixed;
  inset: 0;
  background: #1a1a2e;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.waiting-card {
  background: #16213e;
  padding: 40px;
  border-radius: 12px;
  border: 1px solid #1f2937;
  text-align: center;
  max-width: 400px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
}

.waiting-card h2 {
  color: #fff;
  font-size: 24px;
  margin-bottom: 24px;
}

.pulse-ring {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: 4px solid #e94560;
  margin: 0 auto 24px auto;
  animation: pulse-ring-animation 1.5s infinite ease-in-out;
}

.subtitle {
  color: #8892b0;
  font-size: 14px;
  margin-bottom: 24px;
}

.room-id-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #0f172a;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #1f2937;
  margin-bottom: 30px;
}

.room-label {
  color: #8892b0;
  font-size: 13px;
}

.room-value {
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  letter-spacing: 1px;
}

.copy-btn {
  padding: 6px 12px !important;
  font-size: 12px !important;
}

.cancel-waiting-btn {
  width: 100%;
  padding: 10px !important;
}

/* Animations */
.animate-fade-in {
  animation: fadeIn 0.4s ease-out forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes pulse-ring-animation {
  0% { transform: scale(0.85); opacity: 0.5; box-shadow: 0 0 0 0 rgba(233, 69, 96, 0.4); }
  50% { transform: scale(1); opacity: 1; box-shadow: 0 0 0 10px rgba(233, 69, 96, 0); }
  100% { transform: scale(0.85); opacity: 0.5; box-shadow: 0 0 0 0 rgba(233, 69, 96, 0); }
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 1; }
  50% { transform: scale(1.2); opacity: 0.5; }
  100% { transform: scale(0.9); opacity: 1; }
}

@keyframes timer-pulse {
  0% { transform: scale(1); background: #e94560; }
  100% { transform: scale(1.05); background: #ff0000; box-shadow: 0 0 10px rgba(255,0,0,0.65); }
}

/* Draw dialog overlay */
.overlay-draw {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.draw-dialog {
  background: #16213e;
  padding: 24px 36px;
  border-radius: 12px;
  text-align: center;
  border: 1px solid #e94560;
  max-width: 320px;
}

.draw-dialog h3 {
  color: #e94560;
  margin-bottom: 12px;
}

.draw-dialog p {
  color: #ccc;
  margin-bottom: 20px;
}

.draw-btns {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* Speech bubbles & Chat popover styles */
.player-row-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mute-btn {
  background: transparent;
  border: none;
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
  opacity: 0.6;
  transition: opacity 0.2s;
  user-select: none;
}

.mute-btn:hover {
  opacity: 1;
}

.chat-trigger-wrapper {
  position: relative;
  display: inline-block;
  line-height: 1;
}

.chat-trigger-btn {
  background: transparent;
  border: none;
  font-size: 16px;
  cursor: pointer;
  opacity: 0.8;
  transition: transform 0.2s;
  user-select: none;
  padding: 0;
}

.chat-trigger-btn:hover {
  transform: scale(1.2);
  opacity: 1;
}

/* Chat bubble styling */
.player-info {
  position: relative;
}

.chat-bubble {
  position: absolute;
  top: -34px;
  left: 24px;
  background: #27ae60;
  color: white;
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(0,0,0,0.3);
  z-index: 100;
  white-space: nowrap;
}

.chat-bubble::after {
  content: '';
  position: absolute;
  bottom: -5px;
  left: 10px;
  border-width: 5px 5px 0;
  border-style: solid;
  border-color: #27ae60 transparent;
  display: block;
  width: 0;
}

.chat-bubble.top {
  background: #34495e;
}

.chat-bubble.top::after {
  border-color: #34495e transparent;
}

/* Popover Panel styling */
.chat-popover-panel {
  position: absolute;
  bottom: 28px;
  right: -80px;
  width: 220px;
  background: #16213e;
  border: 1px solid #1f2937;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.4);
  z-index: 1000;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-popover-backdrop {
  position: fixed;
  inset: 0;
  z-index: 999;
  background: transparent;
}

.chat-popover-tabs {
  display: flex;
  border-bottom: 1px solid #1f2937;
}

.chat-popover-tabs button {
  flex: 1;
  background: #0f172a;
  border: none;
  color: #8892b0;
  padding: 8px 0;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 0;
}

.chat-popover-tabs button.active {
  background: #16213e;
  color: #fff;
}

.chat-popover-body {
  padding: 10px;
  background: #16213e;
  max-height: 150px;
  overflow-y: auto;
}

.emoji-picker-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.emoji-btn {
  background: #1f2937;
  border: none;
  font-size: 18px;
  padding: 6px 0;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.emoji-btn:hover {
  background: #374151;
}

.phrase-picker-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.phrase-btn {
  background: #1f2937;
  border: none;
  color: #fff;
  font-size: 11px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s;
}

.phrase-btn:hover {
  background: #374151;
}

/* Animations */
.pop-enter-active {
  animation: pop-in 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.pop-leave-active {
  transition: opacity 0.2s;
}
.pop-leave-to {
  opacity: 0;
}

@keyframes pop-in {
  0% {
    transform: scale(0.6) translateY(8px);
    opacity: 0;
  }
  100% {
    transform: scale(1) translateY(0);
    opacity: 1;
  }
}
</style>
