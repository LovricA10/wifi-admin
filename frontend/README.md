# WiFi Admin Frontend

React + Vite + TypeScript frontend for the WiFi Admin REST API.

## Prerequisites

- Node.js 18 or later
- npm 9 or later
- WiFi Admin backend running on `http://localhost:8081`

## Setup

```bash
npm install
```

## Development

```bash
npm run dev
```

Opens at `http://localhost:5173`.

## Configuration

Copy `.env.example` to `.env.local` and adjust if your backend runs on a different URL:

```bash
cp .env.example .env.local
```

```
VITE_API_BASE_URL=http://localhost:8081
```

## Build

```bash
npm run build
```

Output is in `dist/`.

## Features

- Look up WiFi configuration by CPE ID
- Edit SSID, WiFi band, encryption type and password
- Client-side validation (required fields, password enforcement for non-OPEN encryption)
- Backend health indicator (auto-refreshes every 30 seconds)
- Error messages from the API are displayed inline

## Docker

The frontend can be built and run in Docker via `docker-compose.app.yml` from the project root:

```bash
docker compose -f docker-compose.app.yml up --build frontend
```
