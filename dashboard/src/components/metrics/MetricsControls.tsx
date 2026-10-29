import {Box, Button, FormControl, MenuItem, Paper, Select, TextField, ToggleButton, ToggleButtonGroup, Typography,} from '@mui/material'
import CalendarMonthOutlined from '@mui/icons-material/CalendarMonthOutlined'
import RefreshOutlined from '@mui/icons-material/RefreshOutlined'

import type { ApiDeviceSummary, ApiMeasurement,} from '../../services/deviceService'

import { RANGES } from '../../utils/metricHistory'
import type { RangeKey } from '../../utils/metricHistory'

import '../../styles/MetricsControls.css'

type MetricsControlsProps = {
  devices: ApiDeviceSummary[]
  deviceId: string
  onDeviceChange: (deviceId: string) => void
  /* Only the metrics that can be charted. */
  metrics: ApiMeasurement[]
  metric: string
  onMetricChange: (metric: string) => void
  rangeKey: RangeKey
  onRangeChange: (range: RangeKey) => void
  customFrom: string
  customTo: string
  onCustomFromChange: (value: string) => void
  onCustomToChange: (value: string) => void
  customError: string
  onLoad: () => void
}

export default function MetricsControls({
  devices,
  deviceId,
  onDeviceChange,
  metrics,
  metric,
  onMetricChange,
  rangeKey,
  onRangeChange,
  customFrom,
  customTo,
  onCustomFromChange,
  onCustomToChange,
  customError,
  onLoad,
}: MetricsControlsProps) {
  return (
    <Paper variant="outlined" className="metrics-controls">
      <Box className="metrics-controls__field">
        <span className="metrics-controls__label">Device</span>
        <FormControl size="small" className="metrics-controls__select">
          <Select
            value={deviceId}
            displayEmpty
            onChange={(event) => onDeviceChange(event.target.value)}
            inputProps={{ 'aria-label': 'Device' }}
          >
            {devices.map((device) => (
              <MenuItem key={device.deviceId} value={device.deviceId}>
                {device.deviceId}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Box className="metrics-controls__field">
        <span className="metrics-controls__label">Metric</span>
        <FormControl size="small" className="metrics-controls__select">
          <Select
            value={metric}
            displayEmpty
            onChange={(event) => onMetricChange(event.target.value)}
            inputProps={{ 'aria-label': 'Metric' }}
            disabled={metrics.length === 0}
          >
            {metrics.map((value) => (
              <MenuItem key={value.metricName} value={value.metricName}>
                {value.metricName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Box className="metrics-controls__field">
        <span className="metrics-controls__label">Time range</span>
        <ToggleButtonGroup
          exclusive
          size="small"
          value={rangeKey}
          onChange={(_, value: RangeKey | null) => {
            if (value) onRangeChange(value)
          }}
          className="metrics-controls__ranges"
          aria-label="Time range"
        >
          {RANGES.map((range) => (
            <ToggleButton key={range.key} value={range.key}>
              {range.label}
            </ToggleButton>
          ))}
          <ToggleButton value="custom" aria-label="Custom range">
            <CalendarMonthOutlined fontSize="small" />
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      <Button
        variant="contained"
        startIcon={<RefreshOutlined />}
        onClick={onLoad}
        className="metrics-controls__load"
      >
        Load
      </Button>

      {rangeKey === 'custom' && (
        <Box className="metrics-controls__custom">
          <TextField
            size="small"
            type="datetime-local"
            label="From"
            value={customFrom}
            onChange={(event) => onCustomFromChange(event.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
          />
          <TextField
            size="small"
            type="datetime-local"
            label="To"
            value={customTo}
            onChange={(event) => onCustomToChange(event.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
          />

          {customError && (
            <Typography variant="caption" color="error">
              {customError}
            </Typography>
          )}
        </Box>
      )}
    </Paper>
  )
}