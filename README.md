# 🗑️ Waste Management System

Full‑stack waste collection management platform with route planning (VROOM + OSRM), dashboards, and role‑based access.

- Frontend: Angular 20 (standalone components) + Leaflet
- Backend: Spring Boot (Java 21) + MongoDB + JWT auth
- Routing/Optimization: VROOM (port 3000) using OSRM (port 5000)

## Architecture

```
Angular (4200)  ->  Spring Boot API (8080)  ->  MongoDB (27017)
                         |
                         v
                      VROOM (3000)  ->  OSRM (5000)
```

## Prerequisites

- Node.js 18+ + npm
- Java 21 (JDK)
- MongoDB 6+
- Docker + Docker Compose (for OSRM/VROOM)

## Quick Start

### 1) Start OSRM + VROOM

From repo root:

```bash
docker compose up -d
```

OSRM needs map data. Put an `.osm.pbf` file at `osrm-data/map.osm.pbf`, then run once:

```bash
docker compose --profile tools run --rm osrm-extract
docker compose --profile tools run --rm osrm-partition
docker compose --profile tools run --rm osrm-customize
```

### 2) Start MongoDB

MongoDB URI used by default: `mongodb://localhost:27017/waste_management_db`

### 3) Start the backend

```bash
cd wasteManagement
./mvnw spring-boot:run
```

Backend API base URL (dev): `http://localhost:8080/api/v1`

### 4) Start the frontend

```bash
cd frontend
npm install
npm start
```

Frontend: `http://localhost:4200`

## Configuration

### Frontend API base URL

Files:
- `frontend/src/app/environment/environment.development.ts` (used by `ng serve`)
- `frontend/src/app/environment/environment.prod.ts` (used by production builds via `angular.json` file replacement)

Dev default:

```ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

Prod default:

```ts
export const environment = {
  production: true,
  apiUrl: '/api/v1'
};
```

If you deploy the frontend separately from the backend, you’ll need a reverse proxy (or change `apiUrl`).

### Backend properties

Files:
- `wasteManagement/src/main/resources/application.properties` (dev defaults)
- `wasteManagement/src/main/resources/application-prod.properties` (prod profile)

Key settings:
- MongoDB: `spring.data.mongodb.uri=...`
- VROOM: `vroom.url=http://localhost:3000`

Note: the Spring backend calls **VROOM**; VROOM then calls **OSRM** (so there is no `osrm.url` property in the backend).

## API Overview

Base path: `/api/v1`

### Auth
- `POST /auth/signin`
- `POST /auth/signup`

### Auto-planning
- `GET /auto-planning/mode`
- `POST /auto-planning/mode/{mode}`
- `POST /auto-planning/run/scheduled`
- `POST /auto-planning/run/emergency`

The backend also runs scheduled jobs:
- emergency loop every ~15 minutes
- scheduled cycle at 06:00 (only when mode allows it)

### Core resources (CRUD-style)
- `/admins`, `/employees`, `/vehicles`
- `/collectionPoints`, `/bins`, `/bin-readings`
- `/incidents`, `/alerts`
- `/tournees`, `/tournees/plan`, `/tournees/in-progress`, `/tournees/7-days-co2`
- `/depots`, `/route-steps`, `/tournee-assignments`

## Default Credentials

On first run, the backend seeds an admin user:

- Email: `admin@example.com`
- Password: `admin123`

## Project Structure

```
waste-management-repo/
  frontend/                 # Angular app
  wasteManagement/          # Spring Boot app
  docker-compose.yml        # OSRM + VROOM
  osrm-data/                # local map + generated OSRM files (not committed)
```

## GitHub Notes

- Commit: source code + `docker-compose.yml` + docs/scripts.
- Don’t commit: `frontend/node_modules/`, `frontend/dist/`, OSRM artifacts (`osrm-data/`, `*.osrm*`, `*.osm.pbf`), Docker volumes.
