import type { ReactNode } from 'react'
import { Box, Typography } from '@mui/material'

type SettingsSectionProps = {
  title: string
  description: string
  children: ReactNode
}

/* A titled block of the Settings page: heading and short description above its card. */
export default function SettingsSection({
  title,
  description,
  children,
}: SettingsSectionProps) {
  return (
    <Box component="section" className="settings-section" aria-label={title}>
      <Box className="settings-section__heading">
        <Typography
          variant="h3"
          component="h2"
          className="settings-section__title"
        >
          {title}
        </Typography>

        <Typography variant="body2" color="text.secondary">
          {description}
        </Typography>
      </Box>

      {children}
    </Box>
  )
}