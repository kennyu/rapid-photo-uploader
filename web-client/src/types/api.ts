// API types and interfaces
export interface ApiResponse<T> {
  data: T
  error?: string
}

export interface Photo {
  id: string
  filename: string
  fileSize: number
  userId: string
  status: 'UPLOADING' | 'PROCESSING' | 'COMPLETE' | 'FAILED'
  contentType: string
  tags: string[]
  createdAt: string  // This is the upload timestamp
  updatedAt: string
  thumbnailUrl?: string  // Pre-signed URL for thumbnail (1 hour expiration)
  downloadUrl?: string   // Pre-signed URL for full photo (1 hour expiration)
}

export interface User {
  id: string
  username: string
  email: string
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  userId: string
  email: string
  fullName: string
  expiresIn: number
}
