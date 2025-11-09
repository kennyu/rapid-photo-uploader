import { useState, useEffect } from 'react'
import { usePhotos } from './usePhotos'
import TagEditModal from './TagEditModal'
import { apiClient } from '../../api/client'
import type { Photo } from '../../types/api'

function GalleryPage() {
  const [selectedTag, setSelectedTag] = useState<string | null>(null)
  const { data, isLoading, isError, error, refetch } = usePhotos({ tag: selectedTag })
  const [selectedPhoto, setSelectedPhoto] = useState<Photo | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [allTags, setAllTags] = useState<string[]>([])

  // Extract all unique tags from photos
  useEffect(() => {
    if (data?.content) {
      const tags = new Set<string>()
      data.content.forEach(photo => {
        photo.tags?.forEach(tag => tags.add(tag))
      })
      setAllTags(Array.from(tags).sort())
    }
  }, [data])

  const handleRefresh = () => {
    refetch()
  }

  const handlePhotoClick = (photo: Photo) => {
    setSelectedPhoto(photo)
    setIsModalOpen(true)
  }

  const handleCloseModal = () => {
    setIsModalOpen(false)
    setSelectedPhoto(null)
  }

  const handleSaveTags = async (photoId: string, tags: string[]) => {
    await apiClient.patch(`/v1/photos/${photoId}/tags`, { tags })
    // Refresh gallery to show updated tags
    await refetch()
  }

  const handleTagFilter = (tag: string | null) => {
    setSelectedTag(tag)
  }

  if (isLoading) {
    return (
      <div className="gallery-page">
        <h2>Photo Gallery</h2>
        <p>Loading photos...</p>
      </div>
    )
  }

  if (isError) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    const is403 = errorMessage.includes('403') || errorMessage.includes('Forbidden')
    
    return (
      <div className="gallery-page">
        <h2>Photo Gallery</h2>
        <div style={{ padding: '20px', backgroundColor: '#ffebee', borderRadius: '4px', color: '#c62828' }}>
          <p><strong>Error loading photos:</strong> {errorMessage}</p>
          {is403 ? (
            <p>Please make sure you're logged in. Try logging out and logging back in.</p>
          ) : (
            <p>Make sure the backend is running on http://localhost:8080</p>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="gallery-page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h2 style={{ margin: 0 }}>Photo Gallery</h2>
          <p style={{ margin: '5px 0 0 0' }}>Browse, tag, and download your uploaded photos</p>
        </div>
        <button
          onClick={handleRefresh}
          style={{
            padding: '10px 20px',
            background: '#1976d2',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '16px',
          }}
        >
          🔄 Refresh
        </button>
      </div>

      {/* Tag Filter Bar */}
      {allTags.length > 0 && (
        <div style={{
          marginBottom: '20px',
          padding: '15px',
          backgroundColor: '#f5f5f5',
          borderRadius: '8px',
          border: '1px solid #e0e0e0',
        }}>
          <div style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '10px',
            flexWrap: 'wrap',
          }}>
            <span style={{ 
              fontWeight: '600', 
              fontSize: '14px', 
              color: '#555',
              minWidth: '80px',
            }}>
              Filter by tag:
            </span>
            
            {/* All Photos Button */}
            <button
              onClick={() => handleTagFilter(null)}
              style={{
                padding: '6px 16px',
                backgroundColor: selectedTag === null ? '#1976d2' : 'white',
                color: selectedTag === null ? 'white' : '#666',
                border: selectedTag === null ? 'none' : '1px solid #ddd',
                borderRadius: '16px',
                fontSize: '13px',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                if (selectedTag !== null) {
                  e.currentTarget.style.backgroundColor = '#f0f0f0'
                }
              }}
              onMouseLeave={(e) => {
                if (selectedTag !== null) {
                  e.currentTarget.style.backgroundColor = 'white'
                }
              }}
            >
              All Photos ({data?.totalElements || 0})
            </button>

            {/* Tag Filter Buttons */}
            {allTags.map((tag) => {
              const isSelected = selectedTag === tag
              return (
                <button
                  key={tag}
                  onClick={() => handleTagFilter(tag)}
                  style={{
                    padding: '6px 16px',
                    backgroundColor: isSelected ? '#1976d2' : 'white',
                    color: isSelected ? 'white' : '#666',
                    border: isSelected ? 'none' : '1px solid #ddd',
                    borderRadius: '16px',
                    fontSize: '13px',
                    fontWeight: '500',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                  }}
                  onMouseEnter={(e) => {
                    if (!isSelected) {
                      e.currentTarget.style.backgroundColor = '#f0f0f0'
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (!isSelected) {
                      e.currentTarget.style.backgroundColor = 'white'
                    }
                  }}
                  title={`Filter by "${tag}"`}
                >
                  {tag}
                </button>
              )
            })}
          </div>
        </div>
      )}

      {data?.content && data.content.length > 0 ? (
        <div className="gallery-grid">
          <p style={{ marginBottom: '20px' }}>
            <strong>
              {selectedTag ? `Found ${data.totalElements} photos with tag "${selectedTag}"` : `Found ${data.totalElements} photos`}
            </strong> (Page {data.page + 1} of {data.totalPages})
          </p>
          <div style={{ 
            display: 'flex', 
            flexWrap: 'wrap', 
            gap: '16px',
            justifyContent: 'flex-start',
          }}>
            {data.content.map((photo) => (
              <div 
                key={photo.id} 
                className="photo-card"
                onClick={() => handlePhotoClick(photo)}
                style={{ 
                  width: '180px',
                  border: '2px solid #e0e0e0', 
                  borderRadius: '8px', 
                  overflow: 'hidden',
                  backgroundColor: '#fff',
                  boxShadow: '0 2px 6px rgba(0,0,0,0.08)',
                  transition: 'all 0.3s ease',
                  cursor: 'pointer',
                  position: 'relative',
                  flexShrink: 0,
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-4px)';
                  e.currentTarget.style.boxShadow = '0 6px 20px rgba(0,0,0,0.12)';
                  const overlay = e.currentTarget.querySelector('.metadata-overlay') as HTMLElement;
                  if (overlay) overlay.style.opacity = '1';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 2px 6px rgba(0,0,0,0.08)';
                  const overlay = e.currentTarget.querySelector('.metadata-overlay') as HTMLElement;
                  if (overlay) overlay.style.opacity = '0';
                }}
              >
                {/* Thumbnail Image - Square */}
                <div style={{ 
                  width: '180px', 
                  height: '180px', 
                  backgroundColor: '#f5f5f5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  overflow: 'hidden',
                  position: 'relative',
                }}>
                  {photo.thumbnailUrl ? (
                    <img 
                      src={photo.thumbnailUrl} 
                      alt={photo.filename || 'Photo'}
                      style={{ 
                        width: '100%', 
                        height: '100%', 
                        objectFit: 'cover',
                      }}
                      onError={(e) => {
                        // Fallback if thumbnail fails to load
                        e.currentTarget.style.display = 'none';
                        const fallback = e.currentTarget.parentElement?.querySelector('.fallback-icon') as HTMLElement;
                        if (fallback) fallback.style.display = 'flex';
                      }}
                    />
                  ) : (
                    <div className="fallback-icon" style={{ 
                      display: 'flex', 
                      flexDirection: 'column', 
                      alignItems: 'center',
                      color: '#999'
                    }}>
                      <span style={{ fontSize: '48px' }}>📷</span>
                      <span style={{ fontSize: '12px', marginTop: '8px' }}>
                        {photo.status === 'UPLOADING' ? 'Processing...' : 'No thumbnail'}
                      </span>
                    </div>
                  )}
                  
                  {/* Status Badge - Smaller */}
                  <div style={{
                    position: 'absolute',
                    top: '6px',
                    right: '6px',
                    padding: '3px 8px',
                    borderRadius: '10px',
                    fontSize: '9px',
                    fontWeight: 'bold',
                    backgroundColor: photo.status === 'COMPLETE' ? '#4caf50' : 
                                     photo.status === 'FAILED' ? '#f44336' : 
                                     photo.status === 'PROCESSING' ? '#ff9800' : '#2196f3',
                    color: 'white',
                    boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
                  }}>
                    {photo.status || 'UNKNOWN'}
                  </div>
                  
                  {/* Metadata Overlay (shown on hover) - Compact */}
                  <div 
                    className="metadata-overlay"
                    style={{
                      position: 'absolute',
                      bottom: 0,
                      left: 0,
                      right: 0,
                      background: 'linear-gradient(to top, rgba(0,0,0,0.9), transparent)',
                      padding: '30px 10px 10px',
                      opacity: 0,
                      transition: 'opacity 0.3s ease',
                      color: 'white',
                    }}
                  >
                    <p style={{ 
                      margin: '2px 0', 
                      fontSize: '11px', 
                      fontWeight: 'bold',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}>
                      {photo.filename || 'Unnamed Photo'}
                    </p>
                    <p style={{ margin: '2px 0', fontSize: '10px' }}>
                      {photo.fileSize ? (photo.fileSize / 1024 / 1024).toFixed(2) : '0.00'} MB
                    </p>
                    <p style={{ margin: '2px 0', fontSize: '9px', opacity: 0.9 }}>
                      {photo.createdAt ? new Date(photo.createdAt).toLocaleDateString() : 'Unknown'}
                    </p>
                    {photo.tags && photo.tags.length > 0 && (
                      <div style={{ marginTop: '4px', display: 'flex', flexWrap: 'wrap', gap: '3px' }}>
                        {photo.tags.slice(0, 2).map((tag, idx) => (
                          <span key={idx} style={{
                            fontSize: '8px',
                            padding: '2px 5px',
                            backgroundColor: 'rgba(255,255,255,0.2)',
                            borderRadius: '6px',
                          }}>
                            {tag}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
                
                {/* Footer - Compact */}
                <div style={{ 
                  padding: '8px 10px',
                  backgroundColor: '#fafafa',
                  borderTop: '1px solid #e0e0e0',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '8px',
                }}>
                  <p style={{ 
                    margin: 0, 
                    fontSize: '11px', 
                    fontWeight: '500',
                    color: '#333',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    flex: 1,
                  }}>
                    {photo.filename || 'Unnamed'}
                  </p>
                  {photo.downloadUrl && (
                    <a 
                      href={photo.downloadUrl} 
                      download={photo.filename}
                      style={{
                        fontSize: '16px',
                        color: '#1976d2',
                        textDecoration: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        flexShrink: 0,
                      }}
                      onClick={(e) => e.stopPropagation()}
                      title="Download"
                    >
                      ⬇
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="gallery-empty" style={{ padding: '60px 20px', textAlign: 'center', color: '#666' }}>
          <p style={{ fontSize: '18px' }}>📷 No photos yet. Upload some photos to get started!</p>
          <p style={{ marginTop: '10px' }}>Click the <strong>Upload</strong> button above to begin.</p>
        </div>
      )}

      {/* Tag Edit Modal */}
      <TagEditModal
        photo={selectedPhoto}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onSave={handleSaveTags}
      />
    </div>
  )
}

export default GalleryPage
