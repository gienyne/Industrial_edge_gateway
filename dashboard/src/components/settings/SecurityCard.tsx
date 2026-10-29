import { useState } from 'react'
import type { FormEvent } from 'react'
import {Alert, Box, Button, CircularProgress, IconButton, InputAdornment, Paper, TextField, Typography,} from '@mui/material'
import CheckCircle from '@mui/icons-material/CheckCircle'
import RadioButtonUnchecked from '@mui/icons-material/RadioButtonUnchecked'
import VisibilityOutlined from '@mui/icons-material/VisibilityOutlined'
import VisibilityOffOutlined from '@mui/icons-material/VisibilityOffOutlined'

import { ApiError } from '../../services/apiClient'
import { changePassword } from '../../services/authService'

import '../../styles/SecurityCard.css'

const MIN_PASSWORD_LENGTH = 8

// BCrypt only reads the first 72 bytes of a password, so the backend refuses anything longer.
const MAX_PASSWORD_BYTES = 72

function byteLength(value: string): number {
  return new TextEncoder().encode(value).length
}

export default function SecurityCard() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  // The rules the browser can check itself; the checklist ticks them while the user types.
  const typed = newPassword !== ''

  const requirements = [
    {
      label: `At least ${MIN_PASSWORD_LENGTH} characters`,
      met: newPassword.length >= MIN_PASSWORD_LENGTH,
    },
    {
      label: `At most ${MAX_PASSWORD_BYTES} characters`,
      met: typed && byteLength(newPassword) <= MAX_PASSWORD_BYTES,
    },
    {
      label: 'Different from the current password',
      met: typed && newPassword !== currentPassword,
    },
    {
      label: 'Same as the confirmation',
      met: typed && newPassword === confirmation,
    },
  ]

  const canSubmit =
    currentPassword !== '' &&
    requirements.every((requirement) => requirement.met) && !submitting

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    if (!canSubmit) return

    setSuccess(false)
    setSubmitting(true)
    setError('')

    try {
      await changePassword(currentPassword, newPassword)

      setCurrentPassword('')
      setNewPassword('')
      setConfirmation('')
      setSuccess(true)
    } catch (changeError) {
      console.error('Password change failed:', changeError)

      setError(
        changeError instanceof ApiError
          ? changeError.message
          : 'Your password could not be changed. Please try again.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  const visibilityToggle = (
    <InputAdornment position="end">
      <IconButton
        size="small"
        edge="end"
        aria-label={showPasswords ? 'Hide passwords' : 'Show passwords'}
        onClick={() => setShowPasswords((visible) => !visible)}
      >
        {showPasswords ? (
          <VisibilityOffOutlined fontSize="small" />
        ) : (
          <VisibilityOutlined fontSize="small" />
        )}
      </IconButton>
    </InputAdornment>
  )

  const inputType = showPasswords ? 'text' : 'password'

  return (
    <Paper variant="outlined" className="security-card">
      <Box className="security-card__layout">
        <Box
          component="form"
          noValidate
          onSubmit={(event) => void handleSubmit(event)}
          className="security-card__form"
        >
          {error && (
            <Alert severity="error" role="alert">
              {error}
            </Alert>
          )}

          {success && (
            <Alert severity="success">Your password has been updated.</Alert>
          )}

          <Box className="security-card__field">
            <label
              className="security-card__label"
              htmlFor="security-current-password"
            >
              Current password
            </label>
            <TextField
              id="security-current-password"
              fullWidth
              size="small"
              type={inputType}
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              disabled={submitting}
              slotProps={{
                htmlInput: { autoComplete: 'current-password' },
                input: { endAdornment: visibilityToggle },
              }}
            />
          </Box>

          <Box className="security-card__field">
            <label
              className="security-card__label"
              htmlFor="security-new-password"
            >
              New password
            </label>
            <TextField
              id="security-new-password"
              fullWidth
              size="small"
              type={inputType}
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              disabled={submitting}
              slotProps={{
                htmlInput: { autoComplete: 'new-password' },
                input: { endAdornment: visibilityToggle },
              }}
            />
          </Box>

          <Box className="security-card__field">
            <label
              className="security-card__label"
              htmlFor="security-confirm-password"
            >
              Confirm new password
            </label>
            <TextField
              id="security-confirm-password"
              fullWidth
              size="small"
              type={inputType}
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              disabled={submitting}
              slotProps={{
                htmlInput: { autoComplete: 'new-password' },
                input: { endAdornment: visibilityToggle },
              }}
            />
          </Box>

          <Button
            type="submit"
            variant="contained"
            disabled={!canSubmit}
            className="security-card__submit"
            startIcon={
              submitting ? (
                <CircularProgress size={16} color="inherit" />
              ) : undefined
            }
          >
            {submitting ? 'Updating...' : 'Update password'}
          </Button>
        </Box>

        <Box component="aside" className="security-card__requirements">
          <Typography className="security-card__requirements-title">
            Password requirements
          </Typography>

          <ul className="security-card__checklist">
            {requirements.map((requirement) => (
              <li
                key={requirement.label}
                className="security-card__check"
                data-met={requirement.met}
              >
                {requirement.met ? <CheckCircle /> : <RadioButtonUnchecked />}
                <span>{requirement.label}</span>
              </li>
            ))}
          </ul>
        </Box>
      </Box>
    </Paper>
  )
}