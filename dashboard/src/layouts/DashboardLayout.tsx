import { useState } from 'react'
import type { ReactNode } from 'react'

import {DashboardOutlined, DevicesOutlined, EventNoteOutlined, LogoutOutlined, MenuOutlined, SettingsOutlined, ShowChartOutlined,} from '@mui/icons-material'
import {AppBar, Avatar, Box, Divider, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, Toolbar, Typography,} from '@mui/material'

import '../styles/DashboardLayout.css'

interface DashboardLayoutProps {
  selectedPage: string
  onSelectPage: (page: string) => void

  /* The signed-in user, shown in the account menu. */
  userId: string
  onLogout: () => void
  children: ReactNode
}

interface NavigationItem {
  label: string
  icon: ReactNode
}

const navigationItems: NavigationItem[] = [
  { label: 'Overview', icon: <DashboardOutlined /> },
  { label: 'Devices', icon: <DevicesOutlined /> },
  { label: 'Metrics & History', icon: <ShowChartOutlined /> },
  { label: 'Events', icon: <EventNoteOutlined /> },
]

interface SidebarProps {
  selectedPage: string
  onSelectPage: (page: string) => void
  onClose?: () => void
}

function Sidebar({ selectedPage, onSelectPage, onClose }: SidebarProps) {
  const handleNavigation = (page: string) => {
    onSelectPage(page)
    onClose?.()
  }

  return (
    <Box className="dashboard-sidebar">
      <Box className="dashboard-sidebar__brand">
        <Avatar variant="rounded" className="dashboard-sidebar__logo">
          <DevicesOutlined />
        </Avatar>

        <Box>
          <Typography className="dashboard-sidebar__brand-title">
            Industrial Edge
          </Typography>

          <Typography
            variant="caption"
            className="dashboard-sidebar__brand-caption"
          >
            Gateway Dashboard
          </Typography>
        </Box>
      </Box>

      <Divider className="dashboard-sidebar__divider" />

      <Typography
        variant="overline"
        className="dashboard-sidebar__section-label"
      >
        WORKSPACE
      </Typography>

      <List className="dashboard-sidebar__navigation">
        {navigationItems.map((item) => {
          const selected = selectedPage === item.label

          return (
            <ListItemButton
              key={item.label}
              selected={selected}
              onClick={() => handleNavigation(item.label)}
              className="dashboard-sidebar__navigation-item"
            >
              <ListItemIcon
                className={`dashboard-sidebar__navigation-icon ${
                  selected ? 'dashboard-sidebar__navigation-icon--selected' : ''
                }`}
              >
                {item.icon}
              </ListItemIcon>

              <ListItemText
                primary={item.label}
                slotProps={{
                  primary: { className: 'dashboard-sidebar__navigation-text' },
                }}
              />
            </ListItemButton>
          )
        })}
      </List>

      <Box className="dashboard-sidebar__footer">
        <Divider className="dashboard-sidebar__divider" />

        <List className="dashboard-sidebar__settings">
          <ListItemButton
            selected={selectedPage === 'Settings'}
            onClick={() => handleNavigation('Settings')}
            className="dashboard-sidebar__navigation-item"
          >
            <ListItemIcon
              className={`dashboard-sidebar__navigation-icon ${
                selectedPage === 'Settings'
                  ? 'dashboard-sidebar__navigation-icon--selected'
                  : ''
              }`}
            >
              <SettingsOutlined />
            </ListItemIcon>

            <ListItemText
              primary="Settings"
              slotProps={{
                primary: { className: 'dashboard-sidebar__navigation-text' },
              }}
            />
          </ListItemButton>
        </List>
      </Box>
    </Box>
  )
}

export default function DashboardLayout({
  selectedPage,
  onSelectPage,
  userId,
  onLogout,
  children,
}: DashboardLayoutProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [accountAnchor, setAccountAnchor] = useState<HTMLElement | null>(null)

  const handleSignOut = () => {
    setAccountAnchor(null)
    onLogout()
  }

  return (
    <Box className="dashboard-layout">
      <Box className="dashboard-layout__desktop-sidebar">
        <Box className="dashboard-layout__desktop-sidebar-inner">
          <Sidebar selectedPage={selectedPage} onSelectPage={onSelectPage} />
        </Box>
      </Box>

      <Drawer
        anchor="left"
        open={mobileMenuOpen}
        onClose={() => setMobileMenuOpen(false)}
        slotProps={{ paper: { className: 'dashboard-layout__mobile-drawer' } }}
      >
        <Sidebar
          selectedPage={selectedPage}
          onSelectPage={onSelectPage}
          onClose={() => setMobileMenuOpen(false)}
        />
      </Drawer>

      <Box component="main" className="dashboard-layout__main">
        <AppBar
          position="static"
          color="inherit"
          elevation={0}
          className="dashboard-header"
        >
          <Toolbar className="dashboard-header__toolbar">
            <IconButton
              aria-label="Open navigation"
              onClick={() => setMobileMenuOpen(true)}
              className="dashboard-header__menu-button"
            >
              <MenuOutlined />
            </IconButton>

            <Typography variant="body2" className="dashboard-header__tagline">
              Edge data and system status
            </Typography>

            <IconButton
              aria-label="Account menu"
              aria-haspopup="menu"
              onClick={(event) => setAccountAnchor(event.currentTarget)}
              className="dashboard-header__account-button"
            >
              <Avatar className="dashboard-header__avatar">
                {userId.charAt(0).toUpperCase()}
              </Avatar>
            </IconButton>

            <Menu
              anchorEl={accountAnchor}
              open={accountAnchor !== null}
              onClose={() => setAccountAnchor(null)}
              anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
              transformOrigin={{ vertical: 'top', horizontal: 'right' }}
              slotProps={{ paper: { className: 'dashboard-header__menu' } }}
            >
              <Box className="dashboard-header__account-info">
                <span className="dashboard-header__account-label">
                  Signed in as
                </span>
                <span className="dashboard-header__account-name">
                  {userId}
                </span>
              </Box>

              <Divider />

              <MenuItem onClick={handleSignOut}>
                <ListItemIcon>
                  <LogoutOutlined fontSize="small" />
                </ListItemIcon>
                Sign out
              </MenuItem>
            </Menu>
          </Toolbar>
        </AppBar>

        <Box className="dashboard-layout__scroll">
          <Box className="dashboard-layout__content">{children}</Box>
        </Box>
      </Box>
    </Box>
  )
}