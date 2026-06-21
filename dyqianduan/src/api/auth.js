import request from '../utils/request'

export const login = (email, password) => {
  return request.post('/auth/login', { email, password })
}

export const register = (email, password, username) => {
  return request.post('/auth/register', { email, password, username })
}
