# 揭棋在线对弈平台 —— 核心工程原理图集 (排版修正版)

如果您在 VS Code 中已经安装了 **Markdown Preview Enhanced**，只需在此页面按快捷键 **`Ctrl + M` 然后按 `V`**，右侧就会自动渲染出精美的拓扑图、时序图和 UML 类图。
**右键点击生成的图表，选择“Save as PNG”即可保存为高清图片，然后直接插入您的 Word 实验报告中**！

---

### 1. 系统网络与分层拓扑架构图 (已修正换行)
展示 Web 客户端、TCP 异构对弈端、双通信网关、Service 业务服务层、游戏引擎层及 JPA 本地 H2 持久化层之间的全双工调用关系。

```mermaid
graph TD
    subgraph Client ["客户端层 (Client Side)"]
        Vue["Vue 3 网页客户端"]
        Store["Pinia 全局状态机"]
        WS_Client["WebSocket 长连接客户端"]
        TCP_Client["TCP Socket 异构客户端"]
    end

    subgraph Server ["服务端核心 (Spring Boot Server)"]
        subgraph Gateway ["网络接入网关层"]
            WS_Handler["GameWebSocketHandler<br/>(WebSocket长连接网关)"]
            TCP_Handler["TcpClientHandler<br/>(TCP Socket网关)"]
            Timer["GameTimerManager<br/>(服务端高精度计时器)"]
        end
        
        subgraph Service ["业务逻辑层"]
            GameSvc["GameService<br/>(对局生命周期调度服务)"]
            MatchSvc["MatchmakingService<br/>(玩家匹配队列)"]
            NotSvc["NotationService<br/>(棋谱持久化服务)"]
        end

        subgraph Engine ["对弈核心引擎层"]
            Rule["RuleEngine<br/>(走棋合法与胜负校验)"]
            Gen["MoveGenerator<br/>(合法走步生成器)"]
            Flow["GameFlow<br/>(轮流走子与流程控制)"]
            Assign["RandomPieceAssigner<br/>(15枚暗子随机分配)"]
        end
    end

    subgraph Data ["数据持久化层"]
        JPA["Spring Data JPA<br/>(ORM 实体映射)"]
        H2["H2 Relational Database<br/>(本地文件数据库)"]
    end

    Vue --> Store
    Vue --> WS_Client
    WS_Client <-->|WebSocket: 8887| WS_Handler
    TCP_Client <-->|TCP Socket: 8888| TCP_Handler
    WS_Handler --> GameSvc
    TCP_Handler --> GameSvc
    WS_Handler --> MatchSvc
    TCP_Handler --> MatchSvc
    GameSvc --> Flow
    Flow --> Rule
    Flow --> Gen
    Flow --> Assign
    GameSvc --> NotSvc
    NotSvc --> JPA
    JPA <--> H2
```

---

### 2. 大厅匹配与对弈初始化 —— 时序流程图 (已修正换行)
描述多房间模式下，玩家加入匹配队列、线程安全撮合、Game 独立随机翻子器初始化以及 GAME_START 消息分发的完整生命周期。

```mermaid
sequenceDiagram
    autonumber
    actor PlayerA as 玩家 A (执红)
    actor PlayerB as 玩家 B (执黑)
    participant MS as MatchmakingService<br/>(匹配服务)
    participant GS as GameService<br/>(对局管理)
    participant G as Game<br/>(对局状态机)
    participant WSH as GameWebSocketHandler<br/>(网关)

    PlayerA->>WSH: 发送 JOIN_QUEUE (进入大厅匹配队列)
    WSH->>MS: joinQueue(PlayerA)
    PlayerB->>WSH: 发送 JOIN_QUEUE (进入大厅匹配队列)
    WSH->>MS: joinQueue(PlayerB)
    
    Note over MS: 匹配线程 tryMatch() 轮询检测到积攒满 2 人
    MS->>MS: 将 PlayerA & PlayerB 出队
    MS->>GS: createGame(PlayerA, PlayerB)
    GS->>G: 实例化 Game (分配 6位唯一房间号)
    Note over G: 创建独立且 Transient 的 RandomPieceAssigner (随机装填30枚暗子)
    GS->>WSH: 游戏状态就绪，触发通知广播
    WSH->>PlayerA: 发送 GAME_START 帧 (确认执红 / 颜色代码 0)
    WSH->>PlayerB: 发送 GAME_START 帧 (确认执黑 / 颜色代码 1)
    WSH->>PlayerA: 推送初始脱敏棋盘 (除了将帅外，其余30颗全为未翻开暗子 "?")
    WSH->>PlayerB: 推送初始脱敏棋盘 (除了将帅外，其余30颗全为未翻开暗子 "?")
```

---

### 3. 吃暗子落子判定与防作弊脱敏 —— 时序流程图 (已修正换行)
详细展示了红方吃黑方暗子时，RuleEngine 走法过滤、RandomPieceAssigner 随机类型分配以及向不同连接（吃子方、防守方、观众）进行数据隔离脱敏传输的物理机制。

```mermaid
sequenceDiagram
    autonumber
    actor PlayerA as 玩家 A (当前回合执步 - 执红)
    actor PlayerB as 玩家 B (对手防守方 - 执黑)
    participant RE as RuleEngine<br/>(规则引擎)
    participant B as ChessBoard<br/>(物理棋盘)
    participant PA as RandomPieceAssigner<br/>(翻子器)
    participant WSH as GameWebSocketHandler<br/>(网关)
    actor Spec as 观战端 (观众)

    PlayerA->>WSH: 发送 MOVE 指令 (坐标 b2 移动至 b5)
    WSH->>RE: validateMove(b2, b5)
    Note over RE: 校验起止点/禁止自残/走棋是否合法，放行
    RE->>B: movePiece(b2, b5)
    Note over B: 清空 b2 格点，将被吃暗子设为 alive = false
    WSH->>PA: assignType(BLACK) (随机抽取黑方剩余暗子池)
    PA-->>WSH: 返回真实类型 [黑炮] (CANNON)
    Note over WSH: 对不同角色客户端进行数据包隔离脱敏封装
    WSH->>PlayerA: 发送 moveResult (被吃子类型为: "CANNON", 翻开类型: "CANNON")
    WSH->>PlayerB: 发送 moveResult (被吃子类型脱敏为: "NULL", 翻开类型: "CANNON")
    WSH->>Spec: 发送 moveResult (capturedType: "NULL", flip: "CANNON")
    Note over PlayerA: 前端更新 DOM：吃子区多出一枚明子 [黑炮]
    Note over PlayerB: 前端更新 DOM：自己棋子被吃，但仅在吃子区多出灰色 [暗子]
```

---

### 4. 平台核心领域类图 (UML Class Diagram - 中文对照版)
展示对弈平台后端的核心面向对象实体模型及各组件间的一对一、一对多聚合与依赖关系。

```mermaid
classDiagram
    class Player_玩家 {
        +String id [唯一标识UUID]
        +String name [玩家账号名称]
        +Side side [执子红黑颜色]
    }
    class Game_对局状态机 {
        +String id [对局房间号]
        +Player redPlayer [红方玩家实例]
        +Player blackPlayer [黑方玩家实例]
        +ChessBoard board [棋盘物理模型]
        +GameStatus status [对局生命周期状态]
        +List~Move~ moveHistory [步骤历史流水]
        +RandomPieceAssigner pieceAssigner [瞬时暗子分配器]
        +long gameStartTime [对局开启时间戳]
        +switchTurn() [轮替回合走子方]
    }
    class ChessBoard_棋盘 {
        +int ROWS [棋盘行数:10]
        +int COLS [棋盘列数:9]
        -ChessPiece[][] grid [二维棋子物理网格]
        -List~ChessPiece~ redPieces [红方存活棋子列表]
        -List~ChessPiece~ blackPieces [黑方存活棋子列表]
        -List~ChessPiece~ capturedPieces [被俘获棋子列表]
        +isPositionValid() [核心越界校验]
        +getPieceAt() [获取格点棋子]
        +movePiece() [坐标落子位移]
        +revealPiece() [揭开暗子身份]
    }
    class ChessPiece_棋子 {
        -PieceType type [真实棋子类型]
        -Side side [所属红黑颜色]
        -boolean revealed [明明暗标记]
        -Position position [当前坐标]
        -boolean alive [存活状态]
        +isKing() [判定是否为将帅]
        +reveal() [状态翻转为明子]
    }
    class Position_坐标 {
        -int col [列坐标 0-8 映射 a-i]
        -int row [行坐标 0-9 映射 0-9]
        +static fromAlgebraic() [解析 'a1' 式代数坐标]
        +toAlgebraic() [输出代数坐标]
    }
    class Move_走子记录 {
        -String source [起步格坐标]
        -String destination [落子目标坐标]
        -Integer type [首步翻出的棋子]
        -Side side [此步走子执方]
        -long turnStartTime [当前步计时起点]
        -long serverReceiveTime [服务端收到时间]
        -int moveNumber [手数序号]
    }

    Game_对局状态机 "1" *-- "2" Player_玩家 : 包含 contains
    Game_对局状态机 "1" *-- "1" ChessBoard_棋盘 : 拥有 owns
    Game_对局状态机 "1" *-- "*" Move_走子记录 : 记录 records
    ChessBoard_棋盘 "1" *-- "*" ChessPiece_棋子 : 管理 manages
    ChessPiece_棋子 "1" *-- "1" Position_坐标 : 位于 located_at
```
