# Antigravity Workspace Constraints & Rules

## 1. Scope & Isolation Constraints
- All file reading, writing, and searching (`grep_search`, `list_dir`, etc.) MUST be strictly limited to the current project workspace directory (`C:\Users\spdf\Desktop\jieqi-game`).
- DO NOT read, search, or reference any sibling directories under the global brain folder `C:\Users\spdf\.gemini\antigravity\brain` that belong to other conversation IDs or unrelated homework sessions (e.g., standard Java OOP Homework 1 or Homework 2).
- If a requested file, requirement, or document cannot be found within the local workspace directory, ask the user directly for its location or input rather than initiating a global filesystem search.

## 2. Project Specifications
- This is the **Jieqi Chess Game Project** (揭棋对弈程序设计).
- Technologies: Spring Boot (backend), Vue 3 + Vite (frontend), WebSocket (real-time communication), H2 Database (local file-based persistence).
- Focus only on the codebase and logic inside this repository.
