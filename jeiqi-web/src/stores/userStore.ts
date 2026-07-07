import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as api from '../services/api'

export const useUserStore = defineStore('user', () => {
  const userId = ref<string | null>(localStorage.getItem('userId'))
  const username = ref<string | null>(localStorage.getItem('username'))
  const stats = ref<any>(null)

  async function doRegister(user: string, pass: string) {
    const data = await api.register(user, pass)
    if (data.error) return data.error
    userId.value = data.id
    username.value = data.username
    localStorage.setItem('userId', data.id)
    localStorage.setItem('username', data.username)
    localStorage.setItem('userPassword', pass)
    return null
  }

  async function doLogin(user: string, pass: string) {
    const data = await api.login(user, pass)
    if (data.error) return data.error
    userId.value = data.id
    username.value = data.username
    localStorage.setItem('userId', data.id)
    localStorage.setItem('username', data.username)
    localStorage.setItem('userPassword', pass)
    return null
  }

  async function loadStats() {
    if (!userId.value) return
    stats.value = await api.getUserStats(userId.value)
  }

  function logout() {
    userId.value = null
    username.value = null
    stats.value = null
    localStorage.clear()
  }

  return { userId, username, stats, doRegister, doLogin, loadStats, logout }
})
