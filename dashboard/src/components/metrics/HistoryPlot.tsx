import { useEffect, useId, useMemo, useRef, useState } from 'react'
import type { MouseEvent } from 'react'
import { Box, Typography } from '@mui/material'

import type { ApiMetricHistory } from '../../services/metricService'
import { formatPlain } from '../../utils/historyFormat'

import '../../styles/HistoryPlot.css'

type HistoryPlotProps = {

  data: ApiMetricHistory | null
  loading: boolean
  error: boolean
  
  /* Shown when there is nothing to load yet. */
  idleMessage?: string

  /**
   * Last moment (ms) for which the state of an On/Off metric is known.
   * The state line stops there
   * Default: no limit (the device is online).
   */
  knownUntil?: number

}

const MARGIN = { top: 14, right: 18, bottom: 30, left: 54 }

type Point = { t: number; v: number; lo?: number; hi?: number }

function niceTicks(min: number, max: number, count = 4): number[] {
  const span = max - min
  const rawStep = span / count
  const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)))
  const normalized = rawStep / magnitude

  let niceStep = 10
  if (normalized <= 1) niceStep = 1
  else if (normalized <= 2) niceStep = 2
  else if (normalized <= 5) niceStep = 5

  const step = niceStep * magnitude
  const start = Math.floor(min / step) * step
  const ticks: number[] = []

  for (let value = start; value <= max + step * 0.5; value += step) {
    ticks.push(Number(value.toFixed(10)))
  }

  return ticks
}

function formatTick(time: number, spanMs: number): string {
  const date = new Date(time)

  if (spanMs > 86_400_000 * 1.5) {
    return new Intl.DateTimeFormat(undefined, {
      day: '2-digit',
      month: '2-digit',
    }).format(date)
  }

  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

function formatTooltipTime(time: number): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'short',
    timeStyle: 'medium',
  }).format(new Date(time))
}

/**
 * Hand-drawn SVG chart (no chart library).
 * - Numeric metrics: line + soft gradient (the backend sends time-bucket averages).
 * - Boolean metrics: step line + light bands for the "Yes" periods
 *   (the backend sends state changes only). The line stops at the last contact
 *   of the device, because after that its state is not known.
 */
export default function HistoryPlot({
  data,
  loading,
  error,
  idleMessage = 'No data to display',
  knownUntil = Number.POSITIVE_INFINITY,
}: HistoryPlotProps) {
  const gradientId = useId().replace(/:/g, '')
  const plotRef = useRef<HTMLDivElement>(null)

  const [size, setSize] = useState({ width: 0, height: 0 })
  const [hoverX, setHoverX] = useState<number | null>(null)

  // ---- Size of the drawing area -------------------------------------------

  useEffect(() => {
    const element = plotRef.current

    if (!element) return

    const observer = new ResizeObserver((entries) => {
      const box = entries[0].contentRect
      setSize({ width: box.width, height: box.height })
    })

    observer.observe(element)

    return () => observer.disconnect()
  }, [])

  // ---- Geometry ---------------------------------------------------------------

  const isStep = data?.aggregation === 'state_changes'
  const isBoolean = data?.dataType === 'Boolean'
  const unit = data?.unit ?? null

  const points: Point[] = useMemo(() => {
    if (!data) return []

    return data.points
      .map((point) => ({
        t: new Date(point.timestamp).getTime(),
        v:
          typeof point.value === 'boolean'
            ? point.value
              ? 1
              : 0
            : Number(point.value),
        lo: typeof point.min === 'number' ? point.min : undefined,
        hi: typeof point.max === 'number' ? point.max : undefined,
      }))
      .filter((point) => Number.isFinite(point.t) && Number.isFinite(point.v))
  }, [data])

  const plotWidth = Math.max(0, size.width - MARGIN.left - MARGIN.right)
  const plotHeight = Math.max(0, size.height - MARGIN.top - MARGIN.bottom)

  const geometry = useMemo(() => {
    if (!data || plotWidth <= 0 || plotHeight <= 0) return null

    const xMin = new Date(data.from).getTime()
    const xMax = new Date(data.to).getTime()

    let yMin = 0
    let yMax = 1
    let yTicks: number[] = [0, 1]

    if (!isBoolean && points.length > 0) {

      // The scale also covers the real extremes (min/max), not only the averages.
      let low = Math.min(...points.map((point) => point.lo ?? point.v))
      let high = Math.max(...points.map((point) => point.hi ?? point.v))

      if (low === high) {
        const pad = Math.max(1, Math.abs(low) * 0.05)
        low -= pad
        high += pad
      }

      yTicks = niceTicks(low, high, 4)
      yMin = yTicks[0]
      yMax = yTicks[yTicks.length - 1]
    }

    const xScale = (time: number) =>
      MARGIN.left +
      ((time - xMin) / Math.max(1, xMax - xMin)) * plotWidth

    const yScale = (value: number) =>
      MARGIN.top + (1 - (value - yMin) / Math.max(1e-9, yMax - yMin)) * plotHeight

    const baseY = MARGIN.top + plotHeight

    // A state is only a fact while the device was connected: the state line and
    // the "Yes" bands stop at the last contact.
    const lastPointTime = points.length > 0 ? points[points.length - 1].t : xMin
    const stateEnd = Math.min(xMax, Math.max(knownUntil, lastPointTime))

    // True when the backend sent min/max for every point of a numeric metric.
    const hasRange =
      !isBoolean &&
      !isStep &&
      points.length > 0 &&
      points.every((point) => point.lo !== undefined && point.hi !== undefined)

    let linePath = ''
    let areaPath = ''
    let envelopePath = ''

    if (points.length > 0) {
      if (isStep) {
        points.forEach((point, index) => {
          const x = xScale(Math.max(point.t, xMin))
          const y = yScale(point.v)

          linePath += index === 0 ? `M${x},${y}` : ` H${x} V${y}`
        })

        linePath += ` H${xScale(stateEnd)}`
      } else {
        points.forEach((point, index) => {
          const x = xScale(point.t)
          const y = yScale(point.v)

          linePath += index === 0 ? `M${x},${y}` : ` L${x},${y}`
        })

        const firstX = xScale(points[0].t)
        const lastX = xScale(points[points.length - 1].t)
        areaPath = `${linePath} L${lastX},${baseY} L${firstX},${baseY} Z`

        if (hasRange) {

          // Soft band between the lowest and the highest raw value of each bucket.
          const upper = points
            .map(
              (point, index) =>
                `${index === 0 ? 'M' : 'L'}${xScale(point.t)},${yScale(point.hi ?? point.v)}`,
            )
            .join(' ')

          const lower = [...points]
            .reverse()
            .map((point) => `L${xScale(point.t)},${yScale(point.lo ?? point.v)}`)
            .join(' ')

          envelopePath = `${upper} ${lower} Z`
        }
      }
    }

    // On/Off metrics: one band per period during which the state is "Yes".
    const bands =  isBoolean && isStep ? points.map((point, index) => ({
            x1: xScale(Math.max(point.t, xMin)),
            x2: xScale(
              index < points.length - 1 ? points[index + 1].t : stateEnd,
            ),
            on: point.v === 1,
          }))
        : []

    const xTickCount = plotWidth > 520 ? 6 : 4
    const xTicks = Array.from({ length: xTickCount }, (_, index) =>
      xMin + ((xMax - xMin) * index) / (xTickCount - 1),
    )

    return {
      xMin,
      xMax,
      stateEnd,
      yTicks,
      xTicks,
      xScale,
      yScale,
      baseY,
      linePath,
      areaPath,
      envelopePath,
      hasRange,
      bands,
    }
  }, [data, points, plotWidth, plotHeight, isBoolean, isStep, knownUntil])

  // ---- Hover -----------------------------------------------------------------

  const hoveredPoint: Point | null = useMemo(() => {
    if (hoverX === null || !geometry || points.length === 0) return null

    const time =
      geometry.xMin +
      ((hoverX - MARGIN.left) / Math.max(1, plotWidth)) *
        (geometry.xMax - geometry.xMin)

    if (isStep) {

      // After the last contact the state is not known: no value to show.
      if (time > geometry.stateEnd) return null

      let current = points[0]

      for (const point of points) {
        if (point.t <= time) current = point
      }

      return current
    }

    let nearest = points[0]

    for (const point of points) {
      if (Math.abs(point.t - time) < Math.abs(nearest.t - time)) {
        nearest = point
      }
    }

    return nearest
  }, [hoverX, geometry, points, plotWidth, isStep])

  function handleMouseMove(event: MouseEvent<SVGSVGElement>) {
    const rect = event.currentTarget.getBoundingClientRect()
    const x = event.clientX - rect.left

    setHoverX(Math.min(Math.max(x, MARGIN.left), MARGIN.left + plotWidth))
  }

  // ---- Render ----------------------------------------------------------------

  const hoveredX = hoveredPoint && geometry ? geometry.xScale(
          isStep ? Math.max(hoveredPoint.t, geometry.xMin) : hoveredPoint.t,
        )
      : 0

  const tooltipValue = hoveredPoint
    ? isBoolean
      ? hoveredPoint.v === 1
        ? 'Yes'
        : 'No'
      : unit
        ? `${formatPlain(hoveredPoint.v)} ${unit}`
        : formatPlain(hoveredPoint.v)
    : ''

  function message(text: string) {
    return (
      <Typography
        variant="body2"
        color="text.secondary"
        className="history-plot__message"
      >
        {text}
      </Typography>
    )
  }

  return (
    <Box ref={plotRef} className="history-plot">
      {error ? (
        message('Unable to load the history')
      ) : loading && !data ? (
        message('Loading history...')
      ) : !data ? (
        message(idleMessage)
      ) : points.length === 0 ? (
        message('No data in this period')
      ) : geometry ? (
        <>
          <svg
            width={size.width}
            height={size.height}
            className="history-plot__svg"
            onMouseMove={handleMouseMove}
            onMouseLeave={() => setHoverX(null)}
            role="img"
            aria-label={`History of ${data.metricName}`}
          >
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#2563eb" stopOpacity="0.28" />
                <stop offset="100%" stopColor="#2563eb" stopOpacity="0.02" />
              </linearGradient>
            </defs>

            {geometry.yTicks.map((tick) => (
              <g key={tick}>
                <line
                  className="history-plot__grid"
                  x1={MARGIN.left}
                  x2={MARGIN.left + plotWidth}
                  y1={geometry.yScale(tick)}
                  y2={geometry.yScale(tick)}
                />
                <text
                  className="history-plot__axis-label"
                  x={MARGIN.left - 10}
                  y={geometry.yScale(tick)}
                  textAnchor="end"
                  dominantBaseline="middle"
                >
                  {isBoolean ? (tick === 1 ? 'Yes' : 'No') : formatPlain(tick)}
                </text>
              </g>
            ))}

            {geometry.xTicks.map((tick, index) => (
              <text
                key={tick}
                className="history-plot__axis-label"
                x={geometry.xScale(tick)}
                y={geometry.baseY + 20}
                textAnchor={
                  index === 0
                    ? 'start'
                    : index === geometry.xTicks.length - 1
                      ? 'end'
                      : 'middle'
                }
              >
                {formatTick(tick, geometry.xMax - geometry.xMin)}
              </text>
            ))}

            {isStep ? (
              geometry.bands
                .filter((band) => band.on)
                .map((band, index) => (
                  <rect
                    key={index}
                    className="history-plot__band"
                    x={band.x1}
                    y={MARGIN.top}
                    width={Math.max(3, band.x2 - band.x1)}
                    height={plotHeight}
                    rx={2}
                  />
                ))
            ) : geometry.hasRange ? (
              <path
                className="history-plot__envelope"
                d={geometry.envelopePath}
              />
            ) : (
              <path
                d={geometry.areaPath}
                fill={`url(#${gradientId})`}
                stroke="none"
              />
            )}

            <path
              d={geometry.linePath}
              className="history-plot__line"
              fill="none"
            />

            {hoveredPoint && (
              <g>
                <line
                  className="history-plot__cursor"
                  x1={hoveredX}
                  x2={hoveredX}
                  y1={MARGIN.top}
                  y2={geometry.baseY}
                />
                <circle
                  className="history-plot__dot"
                  cx={hoveredX}
                  cy={geometry.yScale(hoveredPoint.v)}
                  r={4.5}
                />
              </g>
            )}
          </svg>

          {hoveredPoint && (
            <Box
              className="history-plot__tooltip"
              style={{
                left: Math.min(Math.max(hoveredX, 90), size.width - 90),
              }}
            >
              <span className="history-plot__tooltip-value">
                {tooltipValue}
              </span>
              {geometry.hasRange &&
                hoveredPoint.lo !== undefined &&
                hoveredPoint.hi !== undefined && (
                  <span className="history-plot__tooltip-range">
                    Min {formatPlain(hoveredPoint.lo)} · Max{' '}
                    {formatPlain(hoveredPoint.hi)}
                  </span>
                )}
              <span className="history-plot__tooltip-time">
                {formatTooltipTime(hoveredPoint.t)}
              </span>
            </Box>
          )}
        </>
      ) : null}
    </Box>
  )
}