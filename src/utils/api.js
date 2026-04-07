const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function getAuthHeaders() {
  const token = localStorage.getItem('token')
  return token ? { 'Authorization': `Bearer ${token}` } : {}
}

async function apiGet(path) {
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() }
    })
    
    // 如果返回 403，说明 token 无效或过期
    if (res.status === 403) {
      console.warn(`API GET ${path} 403 Forbidden - Token 可能无效或已过期`)
      // 清除无效的 token 和用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转到登录页
      window.location.href = '/login'
      return { code: 403, message: '未授权，请重新登录', data: null }
    }
    
    const result = await res.json()
    return result
  } catch (e) {
    console.warn(`API GET ${path} failed:`, e.message)
    return { code: -1, message: e.message, data: null }
  }
}

async function apiPost(path, body) {
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(body)
    })
    
    // 如果返回 403，说明 token 无效或过期
    if (res.status === 403) {
      console.warn(`API POST ${path} 403 Forbidden - Token 可能无效或已过期`)
      // 清除无效的 token 和用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转到登录页
      window.location.href = '/login'
      return { code: 403, message: '未授权，请重新登录', data: null }
    }
    
    const result = await res.json()
    return result
  } catch (e) {
    console.warn(`API POST ${path} failed:`, e.message)
    return { code: -1, message: e.message, data: null }
  }
}

async function apiPut(path, body) {
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() },
      body: JSON.stringify(body)
    })
    
    // 如果返回 403，说明 token 无效或过期
    if (res.status === 403) {
      console.warn(`API PUT ${path} 403 Forbidden - Token 可能无效或已过期`)
      // 清除无效的 token 和用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转到登录页
      window.location.href = '/login'
      return { code: 403, message: '未授权，请重新登录', data: null }
    }
    
    const result = await res.json()
    return result
  } catch (e) {
    console.warn(`API PUT ${path} failed:`, e.message)
    return { code: -1, message: e.message, data: null }
  }
}

async function apiDelete(path) {
  try {
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json', ...getAuthHeaders() }
    })
    
    // 如果返回 403，说明 token 无效或过期
    if (res.status === 403) {
      console.warn(`API DELETE ${path} 403 Forbidden - Token 可能无效或已过期`)
      // 清除无效的 token 和用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      // 跳转到登录页
      window.location.href = '/login'
      return { code: 403, message: '未授权，请重新登录', data: null }
    }
    
    const result = await res.json()
    return result
  } catch (e) {
    console.warn(`API DELETE ${path} failed:`, e.message)
    return { code: -1, message: e.message, data: null }
  }
}

async function apiUpload(path, formData) {
  try {
    const token = localStorage.getItem('token')
    const headers = token ? { 'Authorization': `Bearer ${token}` } : {}
    const res = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers,
      body: formData
    })
    const result = await res.json()
    return result
  } catch (e) {
    console.warn(`API UPLOAD ${path} failed:`, e.message)
    return { code: -1, message: e.message, data: null }
  }
}

export { API_BASE, apiGet, apiPost, apiPut, apiDelete, apiUpload }