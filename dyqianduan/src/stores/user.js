import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setRefreshToken = (newRefreshToken) => {
    refreshToken.value = newRefreshToken
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  const setUser = (newUser) => {
    user.value = newUser
    localStorage.setItem('user', JSON.stringify(newUser))
  }

  const logout = () => {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  const handleLogin = async (email, password) => {
    const result = await login(email, password)
    if (result.code === 200) {
      setToken(result.data.token)
      setRefreshToken(result.data.refreshToken)
      setUser(result.data.user)
      return true
    }
    throw new Error(result.message)
  }

  const handleRegister = async (email, password, username) => {
    const result = await register(email, password, username)
    if (result.code === 200) {
      return true
    }
    throw new Error(result.message)
  }

  const loadUserFromStorage = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      user.value = JSON.parse(storedUser)
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    setToken,
    setUser,
    logout,
    handleLogin,
    handleRegister,
    loadUserFromStorage,
  }
})
