import { useQuery } from '@tanstack/react-query'
import { apiClient } from '../../api/client'
import type { Photo } from '../../types/api'

interface PhotosResponse {
  content: Photo[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

interface UsePhotosOptions {
  tag?: string | null
}

export function usePhotos(options: UsePhotosOptions = {}) {
  const { tag } = options
  
  return useQuery({
    queryKey: ['photos', tag],
    queryFn: async () => {
      // Build query params
      const params = new URLSearchParams({ size: '100' })
      if (tag) {
        params.append('tag', tag)
      }
      
      const response = await apiClient.get<PhotosResponse>(`/v1/photos?${params.toString()}`)
      return response
    },
    // Auto-refetch settings for fresh data
    refetchOnWindowFocus: true, // Refetch when user returns to tab
    refetchInterval: 5000, // Auto-refetch every 5 seconds
    staleTime: 2000, // Data is considered stale after 2 seconds
  })
}

// Hook for fetching a single photo by ID
export function usePhoto(id: string) {
  return useQuery({
    queryKey: ['photo', id],
    queryFn: async () => {
      const response = await apiClient.get<Photo>(`/v1/photos/${id}`)
      return response
    },
    enabled: !!id, // Only fetch if ID is provided
  })
}
