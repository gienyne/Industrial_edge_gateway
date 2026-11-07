import {Box, Button, FormControl, MenuItem, Paper, Select, TextField, Typography,} from '@mui/material'

import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import CheckOutlined from '@mui/icons-material/CheckOutlined'

import type { ApiDeviceSummary } from '../../services/deviceService'
import type { EventSeverity, EventType } from '../../types/event'
import { EVENT_TYPE_OPTIONS, SEVERITY_OPTIONS } from './eventVisual'

import '../../styles/EventsFilters.css'

export type EventFilterDraft = {
  severity: EventSeverity | ''
  type: EventType | ''
  deviceId: string
  from: string
  to: string
}

type EventsFiltersProps = {
  devices: ApiDeviceSummary[]
  draft: EventFilterDraft
  onChange: (patch: Partial<EventFilterDraft>) => void
  onApply: () => void
  onReset: () => void
  error: string
}

export default function EventsFilters({
  devices,
  draft,
  onChange,
  onApply,
  onReset,
  error,
}: EventsFiltersProps) {
  return (
    <Paper variant="outlined" className="events-filters">
      <Box className="events-filters__field">
        <span className="events-filters__label">Severity</span>
        <FormControl size="small" className="events-filters__select">
          <Select
            value={draft.severity}
            displayEmpty
            onChange={(event) =>
              onChange({ severity: event.target.value as EventSeverity | '' })
            }
            inputProps={{ 'aria-label': 'Severity' }}
          >
            <MenuItem value="">All</MenuItem>
            {SEVERITY_OPTIONS.map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Box className="events-filters__field">
        <span className="events-filters__label">Event type</span>
        <FormControl size="small" className="events-filters__select">
          <Select
            value={draft.type}
            displayEmpty
            onChange={(event) =>
              onChange({ type: event.target.value as EventType | '' })
            }
            inputProps={{ 'aria-label': 'Event type' }}
          >
            <MenuItem value="">All</MenuItem>
            {EVENT_TYPE_OPTIONS.map((option) => (
              <MenuItem key={option.value} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Box className="events-filters__field">
        <span className="events-filters__label">Device</span>
        <FormControl size="small" className="events-filters__select">
          <Select
            value={draft.deviceId}
            displayEmpty
            onChange={(event) => onChange({ deviceId: event.target.value })}
            inputProps={{ 'aria-label': 'Device' }}
          >
            <MenuItem value="">All</MenuItem>
            {devices.map((device) => (
              <MenuItem key={device.deviceId} value={device.deviceId}>
                {device.deviceId}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Box className="events-filters__field events-filters__dates">
        <span className="events-filters__label">Period</span>
        <Box className="events-filters__date-row">
          <TextField
            size="small"
            type="datetime-local"
            value={draft.from}
            onChange={(event) => onChange({ from: event.target.value })}
            slotProps={{ htmlInput: { 'aria-label': 'From' } }}
          />
          <ArrowForwardIcon 
            className="events-filters__arrow"
            aria-hidden="true"
          />
          <TextField
            size="small"
            type="datetime-local"
            value={draft.to}
            onChange={(event) => onChange({ to: event.target.value })}
            slotProps={{ htmlInput: { 'aria-label': 'To' } }}
          />
        </Box>
      </Box>

      <Box className="events-filters__actions">
        <Button
          variant="contained"
          startIcon={<CheckOutlined />}
          onClick={onApply}
          className="events-filters__apply"
        >
          Apply
        </Button>

        <Button
          variant="text"
          onClick={onReset}
          className="events-filters__reset"
        >
          Reset
        </Button>
      </Box>

      {error && (
        <Typography
          variant="caption"
          color="error"
          className="events-filters__error"
        >
          {error}
        </Typography>
      )}
    </Paper>
  )
}