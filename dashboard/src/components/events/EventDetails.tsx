import { Box, Paper, Typography } from '@mui/material'
import type { ApiEventItem } from '../../types/event'
import { SEVERITY_LABEL, getEventVisual, getSeverity } from './eventVisual'

import '../../styles/EventDetails.css'

type EventDetailsProps = {

  event: ApiEventItem | null

  /* Other events of the same device (or of the Edge Node), closest in time first. */
  related: ApiEventItem[]
  onSelect: (id: number) => void
}

function formatFull(timestamp: string): string {
  const date = new Date(timestamp)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'medium',
  }).format(date)
}

function formatShort(timestamp: string): string {
  const date = new Date(timestamp)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date)
}

export default function EventDetails({
  event,
  related,
  onSelect,
}: EventDetailsProps) {
  if (!event) {
    return (
      <Paper variant="outlined" className="event-details">
        <Box className="event-details__header">
          <Typography variant="h6" className="event-details__title">
            Event Details
          </Typography>
        </Box>

        <Typography
          variant="body2"
          color="text.secondary"
          className="event-details__empty"
        >
          Select an event to see its details
        </Typography>
      </Paper>
    )
  }

  const visual = getEventVisual(event.type)
  const severity = getSeverity(event)

  return (
    <Paper variant="outlined" className="event-details">
      <Box className="event-details__header">
        <Typography variant="h6" className="event-details__title">
          Event Details
        </Typography>
      </Box>

      <Box className="event-details__body">
        <Box
          className="event-details__summary"
          data-severity={severity.toLowerCase()}
        >
          <Box className="event-details__icon">{visual.icon}</Box>

          <Box className="event-details__summary-text">
            <Typography className="event-details__summary-title">
              {event.message || visual.label}
            </Typography>

            <Typography variant="body2" color="text.secondary">
              {formatFull(event.timestamp)}
            </Typography>
          </Box>

          <span
            className="events-badge"
            data-severity={severity.toLowerCase()}
          >
            {SEVERITY_LABEL[severity]}
          </span>
        </Box>

        <Box className="event-details__fields">
          <Box className="event-details__field">
            <span className="event-details__label">Device</span>
            <span className="event-details__value">
              {event.deviceId ?? '—'}
            </span>
          </Box>

          <Box className="event-details__field">
            <span className="event-details__label">Edge node</span>
            <span className="event-details__value">
              {event.edgeNodeId ?? '—'}
            </span>
          </Box>

          <Box className="event-details__field">
            <span className="event-details__label">Group</span>
            <span className="event-details__value">
              {event.groupId ?? '—'}
            </span>
          </Box>

          <Box className="event-details__field">
            <span className="event-details__label">Type</span>
            <span className="event-details__value">{visual.label}</span>
          </Box>

          <Box className="event-details__field">
            <span className="event-details__label">Event ID</span>
            <span className="event-details__value">#{event.id}</span>
          </Box>
        </Box>

        <Typography variant="subtitle2" className="event-details__related-title">
          Related Events
        </Typography>

        {related.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No other loaded events for{' '}
            {event.deviceId ?? 'this edge node'}
          </Typography>
        ) : (
          <Box className="event-details__related">
            {related.map((item) => {
              const itemVisual = getEventVisual(item.type)

              return (
                <Box
                  key={item.id}
                  role="button"
                  tabIndex={0}
                  className="event-details__related-row"
                  data-severity={getSeverity(item).toLowerCase()}
                  onClick={() => onSelect(item.id)}
                  onKeyDown={(keyEvent) => {
                    if (keyEvent.key === 'Enter' || keyEvent.key === ' ') {
                      keyEvent.preventDefault()
                      onSelect(item.id)
                    }
                  }}
                >
                  <Box className="event-details__related-icon">
                    {itemVisual.icon}
                  </Box>

                  <Box className="event-details__related-text">
                    <span className="event-details__related-message">
                      {item.message || itemVisual.label}
                    </span>
                    <span className="event-details__related-time">
                      {formatShort(item.timestamp)}
                    </span>
                  </Box>
                </Box>
              )
            })}
          </Box>
        )}
      </Box>
    </Paper>
  )
}