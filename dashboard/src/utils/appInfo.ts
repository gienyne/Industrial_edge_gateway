export const APP_INFO = {
  name: 'Industrial Edge Gateway',
  version: '0.1.0',

  description:
    'Industrial Edge Gateway collects industrial data from OPC UA servers and ESP32 devices, ' +
    'transmits it via MQTT using the Sparkplug B protocol, and makes time-series data available ' +
    'for visualization through a web dashboard.',

  // Data flow from industrial devices to the web dashboard.
  flow: [
    { label: 'Data Sources', detail: 'OPC UA · ESP32' },
    { label: 'Edge Gateway', detail: 'C++17 · Sparkplug B' },
    { label: 'MQTT Broker', detail: 'MQTT · Sparkplug B' },
    { label: 'Backend API', detail: 'Spring Boot · REST' },
    { label: 'Time-Series Database', detail: 'TimescaleDB · PostgreSQL' },
    { label: 'Web Dashboard', detail: 'React · TypeScript · MUI' },
  ],

  stack: [
    {
      layer: 'Edge Gateway',
      items: ['C++17', 'open62541pp', 'Eclipse Paho', 'Protocol Buffers'],
    },
    {
      layer: 'Backend',
      items: ['Java', 'Spring Boot', 'Spring Security', 'Flyway'],
    },
    {
      layer: 'Database',
      items: ['TimescaleDB', 'PostgreSQL'],
    },
    {
      layer: 'Dashboard',
      items: ['React', 'TypeScript', 'Material UI', 'Vite'],
    },
  ],

  credits: [
    { label: 'Developed by', value: 'Dimitry Ntofeu Nyatcha' },
    { label: 'Supervised by', value: 'Prof. Dr. Christian Überall' },
    { label: 'Laboratory', value: 'Smart Factory Mittelhessen' },
  ],
};
