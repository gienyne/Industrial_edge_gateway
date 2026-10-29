import { useMemo } from 'react'
import { Box, Button, Paper, Typography } from '@mui/material'

import type { ApiEventItem } from '../../types/event'
import { SEVERITY_LABEL, getEventVisual, getSeverity } from './eventVisual'

import '../../styles/EventsTimeline.css'

type EventsTimelineProps = {
  events: ApiEventItem[]
  selectedId: number | null
  onSelect: (id: number) => void
  total: number
  loading: boolean
  error: boolean
  hasNext: boolean
  loadingMore: boolean
  onLoadMore: () => void
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
}

function dayLabel(timestamp: string): string {
  const date = new Date(timestamp)

  if (Number.isNaN(date.getTime())) return 'Unknown date'

  const diffDays = Math.round(
    (startOfDay(new Date()) - startOfDay(date)) / 86_400_000,
  )

  if (diffDays === 0) return 'Today'
  if (diffDays === 1) return 'Yesterday'

  return new Intl.DateTimeFormat(undefined, { dateStyle: 'full' }).format(date)
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)

  if (Number.isNaN(date.getTime())) return '—'

  return new Intl.DateTimeFormat(undefined, { timeStyle: 'medium' }).format(date)
}

export default function EventsTimeline({
  events,
  selectedId,
  onSelect,
  total,
  loading,
  error,
  hasNext,
  loadingMore,
  onLoadMore,
}: EventsTimelineProps) {
  // Events arrive newest first; consecutive events of the same day share a heading.
  const groups = useMemo(() => {
    const result: { label: string; items: ApiEventItem[] }[] = []

    for (const event of events) {
      const label = dayLabel(event.timestamp)
      const last = result[result.length - 1]

      if (last && last.label === label) {
        last.items.push(event)
      } else {
        result.push({ label, items: [event] })
      }
    }

    return result
  }, [events])

  return (
    <Paper variant="outlined" className="events-timeline">
      <Box className="events-timeline__header">
        <Box>
          <Typography variant="h6" className="events-timeline__title">
            Event Timeline
          </Typography>

          <Typography variant="body2" color="text.secondary">
            Newest events first
          </Typography>
        </Box>

        <Typography className="events-timeline__count">{total}</Typography>
      </Box>

      <Box className="events-timeline__list">
        {error ? (
          <Typography
            variant="body2"
            color="text.secondary"
            className="events-timeline__message"
          >
            Unable to load events. Please check the backend connection.
          </Typography>
        ) : loading && events.length === 0 ? (
          <Typography
            variant="body2"
            color="text.secondary"
            className="events-timeline__message"
          >
            Loading events...
          </Typography>
        ) : events.length === 0 ? (
          <Typography
            variant="body2"
            color="text.secondary"
            className="events-timeline__message"
          >
            No events match these filters
          </Typography>
        ) : (
          <>
            {groups.map((group) => (
              <Box key={group.label} className="events-timeline__group">
                <Typography
                  variant="caption"
                  className="events-timeline__day"
                >
                  {group.label}
                </Typography>

                {group.items.map((event) => {
                  const visual = getEventVisual(event.type)
                  const severity = getSeverity(event)

                  return (
                    <Box
                      key={event.id}
                      role="button"
                      tabIndex={0}
                      className="events-timeline__row"
                      data-severity={severity.toLowerCase()}
                      data-selected={event.id === selectedId}
                      onClick={() => onSelect(event.id)}
                      onKeyDown={(keyEvent) => {
                        if (keyEvent.key === 'Enter' || keyEvent.key === ' ') {
                          keyEvent.preventDefault()
                          onSelect(event.id)
                        }
                      }}
                    >
                      <Box className="events-timeline__icon">
                        {visual.icon}
                      </Box>

                      <Box className="events-timeline__text">
                        <Typography
                          variant="body2"
                          className="events-timeline__message-text"
                        >
                          {event.message || visual.label}
                        </Typography>

                        <Typography
                          variant="caption"
                          color="text.secondary"
                          className="events-timeline__metadata"
                        >
                          <span className="events-timeline__source">
                            {event.deviceId ?? event.edgeNodeId ?? 'System'}
                          </span>
                          {' · '}
                          {visual.label}
                        </Typography>
                      </Box>

                      <Box className="events-timeline__meta">
                        <span
                          className="events-badge"
                          data-severity={severity.toLowerCase()}
                        >
                          {SEVERITY_LABEL[severity]}
                        </span>
                        <span className="events-timeline__time">
                          {formatTime(event.timestamp)}
                        </span>
                      </Box>
                    </Box>
                  )
                })}
              </Box>
            ))}

            {hasNext && (
              <Button
                variant="text"
                onClick={onLoadMore}
                disabled={loadingMore}
                className="events-timeline__more"
              >
                {loadingMore ? 'Loading...' : 'Load more'}
              </Button>
            )}
          </>
        )}
      </Box>
    </Paper>
  )
}