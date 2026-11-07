# Industrial Edge Gateway Dashboard

Web-based monitoring dashboard for the `industrial_edge_gateway` project. It provides an overview of connected devices, live measurement values, historical data, and system events through the backend REST API.

## Features

| Page | Description |
|---|---|
| **Overview** | System health, MQTT and database status, device counts, device list, and recent activity. |
| **Devices** | Searchable device list with detailed views of current values, measurement history, and recent readings. |
| **Metrics & History** | Historical measurements for any device metric, over predefined or custom time ranges. |
| **Events** | Event timeline with filters for severity, type, device, and time range, plus an event details panel. |
| **Settings** | Account information, password management, and project information. |
| **Login** | Authentication with a backend user account. Sign out from the account menu in the header. |

## Technology Stack

- **React** and **TypeScript** — user interface and application logic
- **Material UI (MUI)** — interface components
- **Vite** — development server and production build tooling
- **SVG** — custom-rendered history charts

## Getting Started

The backend and its database must be configured and running before starting the dashboard. See the backend README for setup instructions.

From the dashboard directory, run:

```bash
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) or [http://localhost:5174](http://localhost:5174) (it depends on the port you use) and sign in with a user account configured in the backend.

To create a production build, run:

```bash
npm run build
```

The generated files are written to the `dist/` directory.

## Backend Communication

All API requests use the `/api` path and are handled through `src/services/apiClient.ts`.

- **Development proxy:** Vite forwards API requests to the backend, as configured in `vite.config.ts`.
- **Session management:** Requests include the session cookie. If the backend returns `401 Unauthorized`, the dashboard redirects the user to the login page.
- **CSRF protection:** The API client includes the CSRF token header on `POST` requests.
- **Automatic refresh:** Overview, Devices, and Device Details refresh every 2 seconds. Events and the history chart for an online device refresh every 5 seconds.

## Project Structure

```text
src/
├── pages/        Page-level components
├── components/   Reusable UI components
├── layouts/      Shared navigation and page layout
├── services/     API client and resource-specific requests
├── utils/        Formatting, metric thresholds, and app information
├── styles/       Component and page stylesheets
└── types/        TypeScript types for API responses
```

## Notes on Measurement Data

- **Offline devices:** On/Off metric states are only available while a device is connected. Charts stop at the last recorded contact; periods without measurements are displayed as *No data in this period* rather than as a flat line.
- **Metric thresholds:** `src/utils/metricLimits.ts` defines the thresholds used to highlight out-of-range values. Currently, `tankLevel` is the only metric with configured thresholds.
- **Project information:** Version, data flow, technology, and credits displayed on the About page are maintained in `src/utils/appInfo.ts`.