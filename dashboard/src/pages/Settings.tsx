import { useState } from 'react'
import type { ReactNode } from 'react'
import { Box, Button, Paper, Typography } from '@mui/material'
import ArrowBackOutlined from '@mui/icons-material/ArrowBackOutlined'
import ChevronRight from '@mui/icons-material/ChevronRight'
import InfoOutlined from '@mui/icons-material/InfoOutlined'
import PersonOutlineOutlined from '@mui/icons-material/PersonOutlineOutlined'
import VerifiedUserOutlined from '@mui/icons-material/VerifiedUserOutlined'

import AccountCard from '../components/settings/AccountCard'
import SecurityCard from '../components/settings/SecurityCard'
import AboutCard from '../components/settings/AboutCard'

import '../styles/Settings.css'

type SettingsProps = {
  userId: string
}

type SectionKey = 'account' | 'security' | 'about'

type Section = {
  key: SectionKey
  title: string
  description: string
  icon: ReactNode
}

const SECTIONS: Section[] = [
  {
    key: 'account',
    title: 'Account',
    description: 'The account you are signed in with and its session status.',
    icon: <PersonOutlineOutlined />,
  },
  {
    key: 'security',
    title: 'Security',
    description: 'Change the password you use to sign in.',
    icon: <VerifiedUserOutlined />,
  },
  {
    key: 'about',
    title: 'About',
    description: 'Version, data flow, technologies and credits.',
    icon: <InfoOutlined />,
  },
]


export default function Settings({ userId }: SettingsProps) {
  const [openKey, setOpenKey] = useState<SectionKey | null>(null)

  const open = SECTIONS.find((section) => section.key === openKey) ?? null

  if (open) {
    return (
      <Box className="settings">
        <Button
          startIcon={<ArrowBackOutlined />}
          onClick={() => setOpenKey(null)}
          className="settings__back"
        >
          Settings
        </Button>

        <Box className="settings__intro">
          <Typography variant="h1" className="settings__title">
            {open.title}
          </Typography>

          <Typography variant="body2" color="text.secondary">
            {open.description}
          </Typography>
        </Box>

        {open.key === 'account' && <AccountCard userId={userId} />}
        {open.key === 'security' && <SecurityCard />}
        {open.key === 'about' && <AboutCard />}
      </Box>
    )
  }

  return (
    <Box className="settings">
      <Box className="settings__intro">
        <Typography variant="h1" className="settings__title">
          Settings
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Manage your account and application information.
        </Typography>
      </Box>

      <Paper variant="outlined" className="settings-menu">
        {SECTIONS.map((section) => (
          <button
            key={section.key}
            type="button"
            className="settings-menu__row"
            onClick={() => setOpenKey(section.key)}
          >
            <span className="settings-menu__icon">{section.icon}</span>

            <span className="settings-menu__text">
              <span className="settings-menu__title">{section.title}</span>
              <span className="settings-menu__description">
                {section.description}
              </span>
            </span>

            <ChevronRight className="settings-menu__chevron" />
          </button>
        ))}
      </Paper>
    </Box>
  )
}