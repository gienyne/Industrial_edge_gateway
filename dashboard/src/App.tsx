import { useEffect, useState } from 'react'
import { Box, CircularProgress } from '@mui/material'

import DashboardLayout from './layouts/DashboardLayout'
import Overview from './pages/Overview'
import Devices from './pages/Devices'
import DeviceDetails from './pages/DeviceDetails'
import MetricsHistory from './pages/MetricsHistory'
import Events from './pages/Events'
import Settings from './pages/Settings'
import Login from './pages/Login'
import { setUnauthorizedHandler } from './services/apiClient'
import { getCurrentUser, logout } from './services/authService'

function App() {
  // undefined = still checking the session, null = nobody signed in
  const [user, setUser] = useState<string | null | undefined>(undefined)
  const [selectedPage, setSelectedPage] = useState('Overview')
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(
    null,
  )

  useEffect(() => {
    // An expired session (401 on any request) sends the user back to the login page.
    setUnauthorizedHandler(() => setUser(null))

    getCurrentUser()
      .then((current) => setUser(current.userId))
      .catch(() => setUser(null))

    return () => setUnauthorizedHandler(null)
  }, [])

  const handleSelectPage = (page: string) => {
    setSelectedPage(page)
    setSelectedDeviceId(null)
  }

  const handleSelectDevice = (deviceId: string) => {
    setSelectedDeviceId(deviceId)
  }

  const handleBackToDevices = () => {
    setSelectedDeviceId(null)
    setSelectedPage('Devices')
  }

  const handleLogout = async () => {
    try {
      await logout()
    } catch (error) {
      console.error('Sign out failed:', error)
    } finally {
      setUser(null)
      setSelectedPage('Overview')
      setSelectedDeviceId(null)
    }
  }

  if (user === undefined) {
    return (
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
        }}
      >
        <CircularProgress size={28} />
      </Box>
    )
  }

  if (user === null) {
    return <Login onLoggedIn={setUser} />
  }

  const userId: string = user

  function renderPage() {
    switch (selectedPage) {
      case 'Devices':
        return selectedDeviceId ? (
          <DeviceDetails
            deviceId={selectedDeviceId}
            onBack={handleBackToDevices}
          />
        ) : (
          <Devices onSelectDevice={handleSelectDevice} />
        )

      case 'Metrics & History':
        return <MetricsHistory />

      case 'Events':
        return <Events />

      case 'Settings':
        return <Settings userId={userId} />

      default:
        return <Overview />
    }
  }

  return (
    <DashboardLayout
      selectedPage={selectedPage}
      onSelectPage={handleSelectPage}
      userId={userId}
      onLogout={() => void handleLogout()}
    >
      {renderPage()}
    </DashboardLayout>
  )
}

export default App