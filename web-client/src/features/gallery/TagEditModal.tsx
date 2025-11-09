import { useState, useEffect } from 'react'
import type { Photo } from '../../types/api'

interface TagEditModalProps {
  photo: Photo | null
  isOpen: boolean
  onClose: () => void
  onSave: (photoId: string, tags: string[]) => Promise<void>
}

function TagEditModal({ photo, isOpen, onClose, onSave }: TagEditModalProps) {
  const [tags, setTags] = useState<string[]>([])
  const [newTag, setNewTag] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (photo) {
      setTags([...(photo.tags || [])])
      setNewTag('')
      setError('')
    }
  }, [photo])

  if (!isOpen || !photo) return null

  const handleAddTag = () => {
    const trimmedTag = newTag.trim().toLowerCase()
    
    if (!trimmedTag) {
      setError('Tag cannot be empty')
      return
    }
    
    if (tags.includes(trimmedTag)) {
      setError('Tag already exists')
      return
    }
    
    if (tags.length >= 10) {
      setError('Maximum 10 tags allowed')
      return
    }
    
    setTags([...tags, trimmedTag])
    setNewTag('')
    setError('')
  }

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter(tag => tag !== tagToRemove))
    setError('')
  }

  const handleSave = async () => {
    setIsSaving(true)
    setError('')
    
    try {
      await onSave(photo.id, tags)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save tags')
    } finally {
      setIsSaving(false)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleAddTag()
    }
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.7)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: 'white',
          borderRadius: '12px',
          padding: '30px',
          maxWidth: '500px',
          width: '90%',
          maxHeight: '80vh',
          overflow: 'auto',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ marginBottom: '20px' }}>
          <h2 style={{ margin: '0 0 10px 0', fontSize: '24px', color: '#333' }}>
            Edit Tags
          </h2>
          <p style={{ 
            margin: 0, 
            fontSize: '14px', 
            color: '#666',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
          }}>
            {photo.filename}
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div style={{
            padding: '10px 15px',
            marginBottom: '15px',
            backgroundColor: '#ffebee',
            borderRadius: '6px',
            color: '#c62828',
            fontSize: '14px',
          }}>
            {error}
          </div>
        )}

        {/* Current Tags */}
        <div style={{ marginBottom: '20px' }}>
          <label style={{ 
            display: 'block', 
            marginBottom: '10px', 
            fontSize: '14px', 
            fontWeight: '600',
            color: '#555',
          }}>
            Current Tags ({tags.length}/10)
          </label>
          
          {tags.length > 0 ? (
            <div style={{ 
              display: 'flex', 
              flexWrap: 'wrap', 
              gap: '8px',
              padding: '12px',
              backgroundColor: '#f5f5f5',
              borderRadius: '8px',
              minHeight: '50px',
            }}>
              {tags.map((tag) => (
                <div
                  key={tag}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    padding: '6px 12px',
                    backgroundColor: '#1976d2',
                    color: 'white',
                    borderRadius: '16px',
                    fontSize: '13px',
                    fontWeight: '500',
                  }}
                >
                  <span>{tag}</span>
                  <button
                    onClick={() => handleRemoveTag(tag)}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'white',
                      cursor: 'pointer',
                      fontSize: '16px',
                      lineHeight: '1',
                      padding: '0',
                      marginLeft: '2px',
                    }}
                    title="Remove tag"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <div style={{
              padding: '20px',
              textAlign: 'center',
              backgroundColor: '#f5f5f5',
              borderRadius: '8px',
              color: '#999',
              fontSize: '14px',
            }}>
              No tags yet. Add some below!
            </div>
          )}
        </div>

        {/* Add New Tag */}
        <div style={{ marginBottom: '25px' }}>
          <label style={{ 
            display: 'block', 
            marginBottom: '10px', 
            fontSize: '14px', 
            fontWeight: '600',
            color: '#555',
          }}>
            Add New Tag
          </label>
          <div style={{ display: 'flex', gap: '10px' }}>
            <input
              type="text"
              value={newTag}
              onChange={(e) => setNewTag(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Enter tag name..."
              style={{
                flex: 1,
                padding: '10px 15px',
                border: '2px solid #e0e0e0',
                borderRadius: '8px',
                fontSize: '14px',
                outline: 'none',
                transition: 'border-color 0.2s',
              }}
              onFocus={(e) => e.target.style.borderColor = '#1976d2'}
              onBlur={(e) => e.target.style.borderColor = '#e0e0e0'}
              disabled={isSaving}
            />
            <button
              onClick={handleAddTag}
              disabled={!newTag.trim() || isSaving}
              style={{
                padding: '10px 20px',
                backgroundColor: !newTag.trim() || isSaving ? '#ccc' : '#1976d2',
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                fontSize: '14px',
                fontWeight: '600',
                cursor: !newTag.trim() || isSaving ? 'not-allowed' : 'pointer',
                transition: 'background-color 0.2s',
              }}
            >
              Add
            </button>
          </div>
          <p style={{ 
            margin: '8px 0 0 0', 
            fontSize: '12px', 
            color: '#999',
          }}>
            Press Enter to add, or click the Add button
          </p>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
          <button
            onClick={onClose}
            disabled={isSaving}
            style={{
              padding: '10px 20px',
              backgroundColor: 'transparent',
              color: '#666',
              border: '2px solid #e0e0e0',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: '600',
              cursor: isSaving ? 'not-allowed' : 'pointer',
            }}
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            style={{
              padding: '10px 30px',
              backgroundColor: isSaving ? '#ccc' : '#4caf50',
              color: 'white',
              border: 'none',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: '600',
              cursor: isSaving ? 'not-allowed' : 'pointer',
            }}
          >
            {isSaving ? 'Saving...' : 'Save Tags'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default TagEditModal

