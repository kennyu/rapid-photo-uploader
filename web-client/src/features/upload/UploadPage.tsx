import { useState, useRef } from 'react'
import './UploadPage.css'

interface FileWithProgress {
  file: File
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  uploadJobId?: string
  error?: string
}

function UploadPage() {
  const [selectedFiles, setSelectedFiles] = useState<FileWithProgress[]>([])
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleFileSelect = (files: FileList | null) => {
    if (!files) return

    const newFiles: FileWithProgress[] = Array.from(files)
      .filter((file) => {
        // Validate file type
        if (!file.type.startsWith('image/')) {
          alert(`${file.name} is not an image file`)
          return false
        }
        // Validate file size (10MB)
        if (file.size > 10 * 1024 * 1024) {
          alert(`${file.name} is larger than 10MB`)
          return false
        }
        return true
      })
      .map((file) => ({
        file,
        progress: 0,
        status: 'pending' as const,
      }))

    setSelectedFiles((prev) => {
      const combined = [...prev, ...newFiles]
      if (combined.length > 100) {
        alert('Maximum 100 files allowed')
        return combined.slice(0, 100)
      }
      return combined
    })
  }

  const uploadFile = async (fileWithProgress: FileWithProgress, index: number) => {
    const { file } = fileWithProgress
    const token = localStorage.getItem('authToken')

    try {
      // Update status to uploading
      setSelectedFiles((prev) =>
        prev.map((f, i) => (i === index ? { ...f, status: 'uploading' as const, progress: 10 } : f))
      )

      // Step 1: Get pre-signed URL
      const initResponse = await fetch('http://localhost:8080/api/v1/photos/upload/initiate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          filename: file.name,
          fileSize: file.size,
          contentType: file.type,
        }),
      })

      if (!initResponse.ok) {
        throw new Error(`Failed to initiate upload: ${initResponse.statusText}`)
      }

      const { preSignedUrl, uploadJobId } = await initResponse.json()

      setSelectedFiles((prev) =>
        prev.map((f, i) => (i === index ? { ...f, progress: 30, uploadJobId } : f))
      )

      // Step 2: Upload to S3
      const s3Response = await fetch(preSignedUrl, {
        method: 'PUT',
        headers: {
          'Content-Type': file.type,
        },
        body: file,
      })

      if (!s3Response.ok) {
        throw new Error(`S3 upload failed: ${s3Response.statusText}`)
      }

      setSelectedFiles((prev) =>
        prev.map((f, i) => (i === index ? { ...f, progress: 80 } : f))
      )

      // Step 3: Notify backend
      const completeResponse = await fetch(`http://localhost:8080/api/v1/uploads/${uploadJobId}/complete`, {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      })

      if (!completeResponse.ok) {
        console.warn('Failed to notify backend, but file uploaded successfully')
      }

      // Success!
      setSelectedFiles((prev) =>
        prev.map((f, i) => (i === index ? { ...f, status: 'success' as const, progress: 100 } : f))
      )
    } catch (error) {
      console.error('Upload error:', error)
      setSelectedFiles((prev) =>
        prev.map((f, i) =>
          i === index
            ? { ...f, status: 'error' as const, error: error instanceof Error ? error.message : 'Unknown error' }
            : f
        )
      )
    }
  }

  const handleUploadAll = async () => {
    const filesToUpload = selectedFiles.filter((f) => f.status === 'pending' || f.status === 'error')

    // Upload in batches of 10 for better performance
    const batchSize = 10
    for (let i = 0; i < filesToUpload.length; i += batchSize) {
      const batch = filesToUpload.slice(i, i + batchSize)
      await Promise.all(
        batch.map((fileWithProgress) => {
          const index = selectedFiles.indexOf(fileWithProgress)
          return uploadFile(fileWithProgress, index)
        })
      )
    }

    alert('Upload complete!')
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    handleFileSelect(e.dataTransfer.files)
  }

  const removeFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index))
  }

  const clearAll = () => {
    setSelectedFiles([])
  }

  const uploadStats = {
    total: selectedFiles.length,
    pending: selectedFiles.filter((f) => f.status === 'pending').length,
    uploading: selectedFiles.filter((f) => f.status === 'uploading').length,
    success: selectedFiles.filter((f) => f.status === 'success').length,
    error: selectedFiles.filter((f) => f.status === 'error').length,
  }

  return (
    <div className="upload-page">
      <div className="upload-container">
        <h1>📸 Upload Photos</h1>
        <p className="subtitle">Select up to 100 photos (max 10MB each)</p>

        {/* Drag & Drop Area */}
        <div
          className={`drop-zone ${isDragging ? 'dragging' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <div className="drop-zone-content">
            <div className="upload-icon">📁</div>
            <p className="drop-zone-text">
              <strong>Click to select</strong> or drag and drop photos here
            </p>
            <p className="drop-zone-hint">JPEG, PNG, GIF, WebP • Max 10MB • Up to 100 files</p>
          </div>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept="image/*"
            onChange={(e) => handleFileSelect(e.target.files)}
            style={{ display: 'none' }}
          />
        </div>

        {/* Stats & Actions */}
        {selectedFiles.length > 0 && (
          <div className="upload-stats">
            <div className="stats-grid">
              <div className="stat-item">
                <span className="stat-label">Total:</span>
                <span className="stat-value">{uploadStats.total}</span>
              </div>
              <div className="stat-item success">
                <span className="stat-label">✅ Uploaded:</span>
                <span className="stat-value">{uploadStats.success}</span>
              </div>
              <div className="stat-item pending">
                <span className="stat-label">⏳ Pending:</span>
                <span className="stat-value">{uploadStats.pending}</span>
              </div>
              {uploadStats.error > 0 && (
                <div className="stat-item error">
                  <span className="stat-label">❌ Failed:</span>
                  <span className="stat-value">{uploadStats.error}</span>
                </div>
              )}
            </div>

            <div className="action-buttons">
              <button
                className="btn btn-primary"
                onClick={handleUploadAll}
                disabled={uploadStats.pending === 0 && uploadStats.error === 0}
              >
                {uploadStats.uploading > 0 ? 'Uploading...' : 'Upload All'}
              </button>
              <button className="btn btn-secondary" onClick={clearAll}>
                Clear All
              </button>
            </div>
          </div>
        )}

        {/* File List */}
        {selectedFiles.length > 0 && (
          <div className="file-list">
            {selectedFiles.map((fileWithProgress, index) => (
              <div key={index} className={`file-item ${fileWithProgress.status}`}>
                <div className="file-info">
                  <div className="file-name">{fileWithProgress.file.name}</div>
                  <div className="file-size">{(fileWithProgress.file.size / 1024).toFixed(2)} KB</div>
                </div>

                <div className="file-status">
                  {fileWithProgress.status === 'pending' && <span className="status-badge pending">Pending</span>}
                  {fileWithProgress.status === 'uploading' && (
                    <div className="progress-container">
                      <div className="progress-bar" style={{ width: `${fileWithProgress.progress}%` }} />
                      <span className="progress-text">{fileWithProgress.progress}%</span>
                    </div>
                  )}
                  {fileWithProgress.status === 'success' && <span className="status-badge success">✓ Success</span>}
                  {fileWithProgress.status === 'error' && (
                    <span className="status-badge error" title={fileWithProgress.error}>
                      ✗ Failed
                    </span>
                  )}
                </div>

                {fileWithProgress.status !== 'uploading' && (
                  <button className="remove-btn" onClick={() => removeFile(index)} title="Remove">
                    ✕
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default UploadPage
