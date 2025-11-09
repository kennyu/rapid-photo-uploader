// Authentication service for managing JWT tokens and auth state

const AUTH_TOKEN_KEY = 'authToken'
const USER_KEY = 'user'

export interface LoginCredentials {
  email: string
  password: string
}

export interface RegisterData {
  email: string
  password: string
  fullName: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  userId: string
  email: string
  fullName: string
  expiresIn: number
}

export interface User {
  id: string
  email: string
  fullName: string
}

// Store auth token in localStorage
export const setAuthToken = (token: string): void => {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
}

// Get auth token from localStorage
export const getAuthToken = (): string | null => {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

// Remove auth token from localStorage
export const clearAuthToken = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

// Store user info
export const setUser = (user: User): void => {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

// Get user info
export const getUser = (): User | null => {
  const userStr = localStorage.getItem(USER_KEY)
  return userStr ? JSON.parse(userStr) : null
}

// Check if user is authenticated
export const isAuthenticated = (): boolean => {
  return getAuthToken() !== null
}

// Login user
export const login = async (credentials: LoginCredentials): Promise<AuthResponse> => {
  const response = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || 'Login failed')
  }

  const data: AuthResponse = await response.json()
  
  // Store token and user info
  setAuthToken(data.token)
  setUser({
    id: data.userId,
    email: data.email,
    fullName: data.fullName,
  })

  return data
}

// Register new user
export const register = async (userData: RegisterData): Promise<AuthResponse> => {
  const response = await fetch('http://localhost:8080/api/v1/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData),
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(error || 'Registration failed')
  }

  const data: AuthResponse = await response.json()
  
  // Store token and user info
  setAuthToken(data.token)
  setUser({
    id: data.userId,
    email: data.email,
    fullName: data.fullName,
  })

  return data
}

// Logout user
export const logout = (): void => {
  clearAuthToken()
}

