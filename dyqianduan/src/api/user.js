import request from '../utils/request'

export const getUserById = (id) => {
  return request.get(`/users/${id}`)
}

export const updateBackground = (background) => {
  return request.put('/users/background', { background })
}