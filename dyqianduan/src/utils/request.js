import axios from 'axios'
import { notifySessionExpired, refreshAccessToken } from './authSession'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && data.code !== 200) {
      const error = new Error(data.message || '请求失败')
      error.response = response
      return Promise.reject(error)
    }
    return response.data
  },
  async (error) => {
    const originalRequest = error.config
    const requestUrl = originalRequest?.url || ''
    const isAuthenticationRequest = [
      '/auth/login',
      '/auth/login/code',
      '/auth/register',
      '/auth/register/code',
      '/auth/forgot-password',
      '/auth/reset-password',
    ].some((path) => requestUrl.endsWith(path))

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry && !isAuthenticationRequest) {
      originalRequest._retry = true
      try {
        const newToken = await refreshAccessToken()
        originalRequest.headers = originalRequest.headers || {}
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return request(originalRequest)
      } catch (refreshError) {
        return Promise.reject(refreshError)
      }
    }

    if (error.response?.status === 401 && !isAuthenticationRequest) notifySessionExpired('expired')
    return Promise.reject(error)
  },
)

export default request
