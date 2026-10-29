import { Chip } from '@mui/material'
import FiberManualRecord from '@mui/icons-material/FiberManualRecord'

export type DeviceStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN'

interface StatusChipProps {
  status: DeviceStatus
}

const statusConfig = {
  ONLINE: { label: 'Online', background: '#DCFCE7', text: '#166534', dot: '#16A34A' },
  OFFLINE: { label: 'Offline', background: '#FEE2E2', text: '#991B1B', dot: '#DC2626' },
  UNKNOWN: { label: 'Unknown', background: '#FEF3C7', text: '#92400E', dot: '#D97706' },
} as const

export default function StatusChip({ status }: StatusChipProps) {
  const config = statusConfig[status]

  return (
    <Chip
      size="small"
      label={config.label}
      icon={<FiberManualRecord sx={{ fontSize: 10 }} />}
      sx={{
        bgcolor: config.background,
        color: config.text,
        fontWeight: 600,
        '& .MuiChip-icon': { color: config.dot, ml: 1 },
      }}
    />
  )
}