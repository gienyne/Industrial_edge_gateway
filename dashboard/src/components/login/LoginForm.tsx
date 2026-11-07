
import { useState } from 'react'
import type { FormEvent } from 'react'
import {Alert, Box, Button, CircularProgress, IconButton, InputAdornment, TextField, Typography,} from '@mui/material'
import VisibilityOutlined from '@mui/icons-material/VisibilityOutlined'
import VisibilityOffOutlined from '@mui/icons-material/VisibilityOffOutlined'

import '../../styles/LoginForm.css'

type LoginFormProps = {
  onSubmit: (userId: string, password: string) => void
  submitting: boolean
  error: string
}

export default function LoginForm({
  onSubmit,
  submitting,
  error,
}: LoginFormProps) {
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const canSubmit = userId.trim() !== '' && password !== '' && !submitting

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (canSubmit) {
      onSubmit(userId.trim(), password)
    }
  }

  return (
    <Box
      component="form"
      noValidate
      onSubmit={handleSubmit}
      className="login-form"
    >
      <Box className="login-form__heading">
        <Typography variant="h1" className="login-form__title">
          Sign in
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Sign in with your user ID and password to access the dashboard.
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" className="login-form__error" role="alert">
          {error}
        </Alert>
      )}

      <Box className="login-form__field">
        <label className="login-form__label" htmlFor="login-user-id">
          User ID
        </label>

        <TextField
          id="login-user-id"
          autoFocus
          fullWidth
          size="small"
          value={userId}
          onChange={(event) => setUserId(event.target.value)}
          disabled={submitting}
          slotProps={{
            htmlInput: {
              autoComplete: 'username',
              'aria-label': 'User ID',
            },
          }}
        />
      </Box>

      <Box className="login-form__field">
        <label className="login-form__label" htmlFor="login-password">
          Password
        </label>

        <TextField
          id="login-password"
          fullWidth
          size="small"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={submitting}
          slotProps={{
            htmlInput: {
              autoComplete: 'current-password',
              'aria-label': 'Password',
            },
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    size="small"
                    edge="end"
                    aria-label={
                      showPassword ? 'Hide password' : 'Show password'
                    }
                    onClick={() =>
                      setShowPassword((visible) => !visible)
                    }
                    disabled={submitting}
                  >
                    {showPassword ? (
                      <VisibilityOffOutlined fontSize="small" />
                    ) : (
                      <VisibilityOutlined fontSize="small" />
                    )}
                  </IconButton>
                </InputAdornment>
              ),
            },
          }}
        />
      </Box>

      <Button
        type="submit"
        variant="contained"
        disabled={!canSubmit}
        className="login-form__submit"
        startIcon={
          submitting ? (
            <CircularProgress size={16} color="inherit" />
          ) : undefined
        }
      >
        {submitting ? 'Signing in...' : 'Sign in'}
      </Button>
    </Box>
  )
}
