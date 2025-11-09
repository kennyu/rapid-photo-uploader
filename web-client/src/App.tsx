import { useState, useEffect } from 'react'
import './App.css'
import { UploadPage } from './features/upload'
import { GalleryPage } from './features/gallery'
import { LoginPage, RegisterPage } from './features/auth'
import { isAuthenticated, logout, getUser } from './features/auth/authService'
import Button from './common/Button'

type Page = 'upload' | 'gallery' | 'login' | 'register'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('login')
  const [authenticated, setAuthenticated] = useState(false)
  const [userName, setUserName] = useState('')

  useEffect(() => {
    // Check auth status on mount and update state
    const checkAuth = () => {
      const auth = isAuthenticated()
      setAuthenticated(auth)
      
      if (auth) {
        const user = getUser()
        setUserName(user?.fullName || '')
      } else {
        setCurrentPage('login')
      }
    }

    checkAuth()
    
    // Re-check auth on storage changes (for multi-tab sync)
    window.addEventListener('storage', checkAuth)
    return () => window.removeEventListener('storage', checkAuth)
  }, [])

  const handleLogout = () => {
    logout()
    setAuthenticated(false)
    setUserName('')
    setCurrentPage('login')
  }

  const handleLoginSuccess = () => {
    setAuthenticated(true)
    const user = getUser()
    setUserName(user?.fullName || '')
    setCurrentPage('upload') // Redirect to upload page after login
  }

  const handleRegisterSuccess = () => {
    setAuthenticated(true)
    const user = getUser()
    setUserName(user?.fullName || '')
    setCurrentPage('upload') // Redirect to upload page after registration
  }

  const renderPage = () => {
    switch (currentPage) {
      case 'upload':
        return authenticated ? <UploadPage /> : <LoginPage onLoginSuccess={handleLoginSuccess} />
      case 'gallery':
        return authenticated ? <GalleryPage /> : <LoginPage onLoginSuccess={handleLoginSuccess} />
      case 'login':
        return <LoginPage onLoginSuccess={handleLoginSuccess} />
      case 'register':
        return <RegisterPage onRegisterSuccess={handleRegisterSuccess} />
      default:
        return authenticated ? <UploadPage /> : <LoginPage onLoginSuccess={handleLoginSuccess} />
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h1>RapidPhotoUpload</h1>
            <p>High-Volume Photo Upload System</p>
          </div>
          {authenticated && (
            <div style={{ textAlign: 'right' }}>
              <p style={{ margin: '0 0 10px 0' }}>Welcome, {userName}!</p>
              <Button onClick={handleLogout} variant="secondary">
                Logout
              </Button>
            </div>
          )}
        </div>

        <nav className="app-nav">
          {authenticated ? (
            <>
              <Button
                onClick={() => setCurrentPage('upload')}
                variant={currentPage === 'upload' ? 'primary' : 'secondary'}
              >
                Upload
              </Button>
              <Button
                onClick={() => setCurrentPage('gallery')}
                variant={currentPage === 'gallery' ? 'primary' : 'secondary'}
              >
                Gallery
              </Button>
            </>
          ) : (
            <>
              <Button
                onClick={() => setCurrentPage('login')}
                variant={currentPage === 'login' ? 'primary' : 'secondary'}
              >
                Login
              </Button>
              <Button
                onClick={() => setCurrentPage('register')}
                variant={currentPage === 'register' ? 'primary' : 'secondary'}
              >
                Register
              </Button>
            </>
          )}
        </nav>
      </header>

      <main className="app-main">{renderPage()}</main>
    </div>
  )
}

export default App
