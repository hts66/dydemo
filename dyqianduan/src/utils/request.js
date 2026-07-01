import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

let isRefreshing = false
let pendingRequests = []

const processPendingRequests = (error) => {
  pendingRequests.forEach((callback) => {
    if (error) {
      callback(error)
    } else {
      callback()
    }
  })
  pendingRequests = []
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
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

    if (error.response?.status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingRequests.push((err) => {
            if (err) {
              reject(err)
            } else {
              resolve(request(originalRequest))
            }
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) {
          throw new Error('没有刷新令牌')
        }

        const response = await request.post('/auth/refresh', { refreshToken })
        
        if (response.code === 200) {
          localStorage.setItem('token', response.data.token)
          localStorage.setItem('refreshToken', response.data.refreshToken)
          
          originalRequest.headers.Authorization = `Bearer ${response.data.token}`
          
          processPendingRequests()
          return request(originalRequest)
        } else {
          throw new Error(response.message)
        }
      } catch (err) {
        processPendingRequests(err)
        
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        window.location.href = '/login'
        
        return Promise.reject(err)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default request
