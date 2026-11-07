
import { useState } from 'react'
import { Box, Typography } from '@mui/material'
import DevicesOutlined from '@mui/icons-material/DevicesOutlined'
import SensorsOutlined from '@mui/icons-material/SensorsOutlined'
import ShowChartOutlined from '@mui/icons-material/ShowChartOutlined'
import EventNoteOutlined from '@mui/icons-material/EventNoteOutlined'

import LoginForm from '../components/login/LoginForm'
import { ApiError } from '../services/apiClient'
import { login } from '../services/authService'

import '../styles/Login.css'

type LoginProps = {
  onLoggedIn: (userId: string) => void
}

const HIGHLIGHTS = [
  {
    icon: <SensorsOutlined />,
    title: 'Live device monitoring',
    text: 'Real-time connectivity and device status.',
  },
  {
    icon: <ShowChartOutlined />,
    title: 'Measurement history',
    text: 'Explore historical industrial data.',
  },
  {
    icon: <EventNoteOutlined />,
    title: 'Events & data quality',
    text: 'Track connectivity events and data gaps.',
  },
]

function messageFor(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return 'Invalid user ID or password.'
    }

    if (error.status === 403) {
      return 'The security check failed. Please reload the page and try again.'
    }

    return error.message
  }

  return 'Unable to reach the server. Please check the backend connection.'
}

export default function Login({ onLoggedIn }: LoginProps) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(userId: string, password: string) {
    setSubmitting(true)
    setError('')

    try {
      const result = await login(userId, password)
      onLoggedIn(result.userId)
    } catch (loginError) {
      console.error('Login failed:', loginError)
      setError(messageFor(loginError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Box className="login">
      <Box className="login__brand">
        <Box className="login__brand-header">
          <Box className="login__logo">
            <DevicesOutlined />
          </Box>

          <Box>
            <Typography className="login__brand-title">
              Industrial Edge
            </Typography>

            <Typography
              variant="caption"
              className="login__brand-caption"
            >
              Gateway Dashboard
            </Typography>
          </Box>
        </Box>

        <Box className="login__pitch">
          <Typography variant="h2" className="login__headline">
            Industrial monitoring.
            <br />
            From edge to dashboard.
          </Typography>

          <Box className="login__highlights">
            {HIGHLIGHTS.map((item) => (
              <Box key={item.title} className="login__highlight">
                <Box className="login__highlight-icon">
                  {item.icon}
                </Box>

                <Box className="login__highlight-content">
                  <span className="login__highlight-title">
                    {item.title}
                  </span>

                  <span className="login__highlight-text">
                    {item.text}
                  </span>
                </Box>
              </Box>
            ))}
          </Box>
        </Box>
      </Box>

      <Box className="login__panel">
        <LoginForm
          onSubmit={(userId, password) =>
            void handleSubmit(userId, password)
          }
          submitting={submitting}
          error={error}
        />

        <Box className="login__copyright">
          <span>© 2026 Dimitry ntofeu nyatcha</span>
        </Box>
      </Box>
    </Box>
  )
}
