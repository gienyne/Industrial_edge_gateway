import { useEffect, useMemo, useState } from 'react'
import { Box, Typography } from '@mui/material'

import EventsFilters from '../components/events/EventsFilters'
import type { EventFilterDraft } from '../components/events/EventsFilters'
import EventsTimeline from '../components/events/EventsTimeline'
import EventDetails from '../components/events/EventDetails'
import { getDevices } from '../services/deviceService'
import type { ApiDeviceSummary } from '../services/deviceService'
import { getEvents } from '../services/eventService'
import type { EventQuery } from '../services/eventService'
import type { ApiEventItem } from '../types/event'

import '../styles/Events.css'

const PAGE_SIZE = 50
const REFRESH_MS = 5000

const EMPTY_DRAFT: EventFilterDraft = {
  severity: '',
  type: '',
  deviceId: '',
  from: '',
  to: '',
}

/* Adds only the events that are newer than what the list already holds. */
function mergeNewest(
  current: ApiEventItem[],
  fresh: ApiEventItem[],
): ApiEventItem[] {
  const maxId = current.reduce((max, event) => Math.max(max, event.id), 0)
  const added = fresh.filter((event) => event.id > maxId)

  return added.length > 0 ? [...added, ...current] : current
}

/* Appends a further page, skipping events that were already loaded. */
function appendUnique(
  current: ApiEventItem[],
  more: ApiEventItem[],
): ApiEventItem[] {
  const known = new Set(current.map((event) => event.id))

  return [...current, ...more.filter((event) => !known.has(event.id))]
}

export default function Events() {
  const [devices, setDevices] = useState<ApiDeviceSummary[]>([])
  const [draft, setDraft] = useState<EventFilterDraft>(EMPTY_DRAFT)
  const [applied, setApplied] = useState<EventQuery>({})
  const [filterError, setFilterError] = useState('')

  const [events, setEvents] = useState<ApiEventItem[]>([])
  const [total, setTotal] = useState(0)
  const [hasNext, setHasNext] = useState(false)
  const [nextPage, setNextPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(false)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  // ---- Devices for the filter ------------------------------------------

  useEffect(() => {
    let cancelled = false

    getDevices()
      .then((list) => {
        if (!cancelled) setDevices(list)
      })
      .catch((loadError) => {
        console.error('Unable to load devices:', loadError)
      })

    return () => {
      cancelled = true
    }
  }, [])

  // ---- First page, each time the applied filters change ---------------------------

  useEffect(() => {
    let cancelled = false

    setLoading(true)
    setError(false)

    getEvents(applied, 0, PAGE_SIZE)
      .then((result) => {
        if (cancelled) return

        setEvents(result.events)
        setTotal(result.totalElements)
        setHasNext(result.hasNext)
        setNextPage(1)
      })
      .catch((loadError) => {
        console.error('Unable to load events:', loadError)

        if (!cancelled) {
          setError(true)
          setEvents([])
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [applied])

  // ---- New events appear by themselves ---------------------------------------------

  useEffect(() => {
    let cancelled = false

    const timer = window.setInterval(() => {
      getEvents(applied, 0, PAGE_SIZE)
        .then((result) => {
          if (cancelled) return

          setEvents((current) => mergeNewest(current, result.events))
          setTotal(result.totalElements)
          setError(false)
        })
        .catch((loadError) => {
          console.error('Unable to refresh events:', loadError)
        })
    }, REFRESH_MS)

    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [applied])

  // ---- Filters ----------------------------------------------------------------------

  function handleApply() {
    const from = draft.from ? new Date(draft.from) : undefined
    const to = draft.to ? new Date(draft.to) : undefined

    if (
      (from && Number.isNaN(from.getTime())) ||
      (to && Number.isNaN(to.getTime()))
    ) {
      setFilterError('One of the dates is not valid.')
      return
    }

    if (from && to && from >= to) {
      setFilterError('The start date must be before the end date.')
      return
    }

    setFilterError('')
    setSelectedId(null)
    setApplied({
      type: draft.type || undefined,
      severity: draft.severity || undefined,
      deviceId: draft.deviceId || undefined,
      from,
      to,
    })
  }

  function handleReset() {
    setDraft(EMPTY_DRAFT)
    setFilterError('')
    setSelectedId(null)
    setApplied({})
  }

  // ---- Load more ----------------------------------------------------------------------

  async function handleLoadMore() {
    setLoadingMore(true)

    try {
      const result = await getEvents(applied, nextPage, PAGE_SIZE)

      setEvents((current) => appendUnique(current, result.events))
      setHasNext(result.hasNext)
      setNextPage((page) => page + 1)
      setTotal(result.totalElements)
    } catch (loadError) {
      console.error('Unable to load more events:', loadError)
    } finally {
      setLoadingMore(false)
    }
  }

  // ---- Selection and related events ------------------------------------------------------

  const selected = useMemo(
    () => events.find((event) => event.id === selectedId) ?? events[0] ?? null,
    [events, selectedId],
  )

  // Same device (or both without a device), closest in time first.
  const related = useMemo(() => {
    if (!selected) return []

    const reference = new Date(selected.timestamp).getTime()

    return events
      .filter(
        (event) =>
          event.id !== selected.id && event.deviceId === selected.deviceId,
      )
      .sort(
        (a, b) =>
          Math.abs(new Date(a.timestamp).getTime() - reference) -
          Math.abs(new Date(b.timestamp).getTime() - reference),
      )
      .slice(0, 5)
  }, [events, selected])

  return (
    <Box className="events">
      <Box className="events__intro">
        <Typography variant="h1" className="events__title">
          Events
        </Typography>

        <Typography variant="body2" color="text.secondary">
          Monitor device connectivity, data quality and system events.
        </Typography>
      </Box>

      <EventsFilters
        devices={devices}
        draft={draft}
        onChange={(patch) => setDraft((current) => ({ ...current, ...patch }))}
        onApply={handleApply}
        onReset={handleReset}
        error={filterError}
      />

      <Box className="events__lower">
        <EventsTimeline
          events={events}
          selectedId={selected?.id ?? null}
          onSelect={setSelectedId}
          total={total}
          loading={loading}
          error={error}
          hasNext={hasNext}
          loadingMore={loadingMore}
          onLoadMore={() => void handleLoadMore()}
        />

        <EventDetails
          event={selected}
          related={related}
          onSelect={setSelectedId}
        />
      </Box>
    </Box>
  )
}