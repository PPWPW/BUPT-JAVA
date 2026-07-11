const BASE = '/api'

export async function register(username: string, password: string) {
  const res = await fetch(`${BASE}/users/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  return res.json()
}

export async function login(username: string, password: string) {
  const res = await fetch(`${BASE}/users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  return res.json()
}

export async function getUserStats(userId: string) {
  const res = await fetch(`${BASE}/users/${userId}/stats`)
  return res.json()
}

export async function getNotations() {
  const res = await fetch(`${BASE}/notations`)
  return res.json()
}

export async function getNotation(gameId: string) {
  const res = await fetch(`${BASE}/notations/${gameId}`)
  return res.json()
}

export async function clearNotations() {
  const res = await fetch(`${BASE}/notations`, {
    method: 'DELETE'
  })
  return res.ok
}
