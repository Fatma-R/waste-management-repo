# 🗑️ Waste Management System

A full-stack **smart waste collection management platform** featuring automated route optimization, real-time dashboards, and role-based access control. Built for municipalities and waste management companies to optimize collection routes, reduce costs, and minimize environmental impact.

![Angular](https://img.shields.io/badge/Angular-20-DD0031?logo=angular)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)
![MongoDB](https://img.shields.io/badge/MongoDB-6+-47A248?logo=mongodb)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)

---

## ✨ Features

- **🗺️ Automated Route Planning** — Leverages [VROOM](https://github.com/VROOM-Project/vroom) + [OSRM](https://project-osrm.org/) for optimal vehicle routing
- **📊 Real-time Dashboards** — Monitor bin fill levels, vehicle locations, and collection status
- **🚨 Alert System** — Automatic alerts for overflowing bins, maintenance needs, and incidents
- **👥 Role-based Access** — Admin and Employee portals with JWT authentication
- **🗓️ Scheduled & Emergency Runs** — Auto-planning modes for daily schedules and urgent collections
- **📈 CO₂ Tracking** — 7-day emissions reports for sustainability monitoring
- **🗺️ Interactive Maps** — Leaflet-based visualization of bins, routes, and vehicles

---

## 🏗️ Architecture

```
┌─────────────────┐      ┌─────────────────────┐      ┌─────────────────┐
│   Angular 20    │ ───▶ │  Spring Boot API    │ ───▶ │    MongoDB      │
│   (port 4200)   │      │    (port 8080)      │      │   (port 27017)  │
└─────────────────┘      └──────────┬──────────┘      └─────────────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   VROOM Optimizer   │
                         │    (port 3000)      │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   OSRM Routing      │
                         │    (port 5000)      │
                         └─────────────────────┘
```

| Component | Technology | Purpose |
|-----------|------------|---------|
| Frontend | Angular 20 + Leaflet | SPA with maps and dashboards |
| Backend | Spring Boot 3 + Java 21 | REST API, business logic, scheduling |
| Database | MongoDB 6+ | Document storage for all entities |
| Optimizer | VROOM | Vehicle Routing Problem solver |
| Routing | OSRM | Road network routing engine |

---

## 📋 Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| Node.js | 18+ | For Angular frontend |
| Java JDK | 21 | For Spring Boot backend |
| MongoDB | 6+ | Local install or via Docker |
| Docker | Latest | For OSRM + VROOM containers |
| Docker Compose | v2+ | Included with Docker Desktop |

---

## 🚀 Quick Start

### Step 1: Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/waste-management-repo.git
cd waste-management-repo
```

### Step 2: Set up OSRM map data

The project is pre-configured for **Tunisia**. You need the OSRM map files in the `osrm-data/` folder at the repo root.

> ⚠️ **These files are NOT included in Git** (they're ~1Gb). You must obtain them separately.

**Option A — Use pre-processed files (recommended):**

📥 **Download:** [osrm-data from Google Drive](https://drive.google.com/file/d/1OCb5fC90UHAQmdXYPwSYdchSWtvcZ24M/view?usp=sharing)

Extract the zip contents into the `osrm-data/` folder so you have:
```
osrm-data/
├── tunisia-25-11-25.osrm
├── tunisia-25-11-25.osrm.cell_metrics
├── tunisia-25-11-25.osrm.cells
├── tunisia-25-11-25.osrm.cnbg
├── tunisia-25-11-25.osrm.datasource_names
├── tunisia-25-11-25.osrm.ebg
├── tunisia-25-11-25.osrm.edges
├── tunisia-25-11-25.osrm.enw
├── tunisia-25-11-25.osrm.fileIndex
├── tunisia-25-11-25.osrm.geometry
├── tunisia-25-11-25.osrm.icd
├── tunisia-25-11-25.osrm.maneuver_overrides
├── tunisia-25-11-25.osrm.mldgr
├── tunisia-25-11-25.osrm.names
├── tunisia-25-11-25.osrm.nbg_nodes
├── tunisia-25-11-25.osrm.partition
├── tunisia-25-11-25.osrm.properties
├── tunisia-25-11-25.osrm.ramIndex
├── tunisia-25-11-25.osrm.timestamp
├── tunisia-25-11-25.osrm.tld
├── tunisia-25-11-25.osrm.tls
├── tunisia-25-11-25.osrm.turn_duration_penalties
├── tunisia-25-11-25.osrm.turn_penalties_index
└── tunisia-25-11-25.osrm.turn_weight_penalties
```

> ✅ With pre-processed files, skip Step 3 entirely!

**Option B — Process from raw `.osm.pbf` file:**

If you only have `tunisia-25-11-25.osm.pbf`, place it in `osrm-data/` and run Step 3.

### Step 3: Build OSRM routing graph (skip if using pre-processed files)

Only needed if you have a raw `.osm.pbf` file and no `.osrm` files:

```bash
docker compose --profile tools run --rm osrm-extract
docker compose --profile tools run --rm osrm-partition
docker compose --profile tools run --rm osrm-customize
```

> ⏱️ This takes a few minutes for Tunisia. Creates all the `.osrm.*` files needed by the routing engine.

### Step 4: Start OSRM + VROOM

```bash
docker compose up -d
```

Verify services are running:
- OSRM health: http://localhost:5000/nearest/v1/car/10.0,36.8
- VROOM: http://localhost:3000/health

### Step 5: Start MongoDB and import demo data

**Option A — Local MongoDB:**
```bash
mongod  # Start MongoDB if not running as service
mongorestore --drop --db waste_management_db demo-data/waste_management_db
```

**Option B — Docker MongoDB:**
```bash
docker run -d --name mongo -p 27017:27017 mongo:6
docker cp demo-data/waste_management_db mongo:/dump
docker exec mongo mongorestore --drop --db waste_management_db /dump
```

> The `demo-data/` folder contains sample bins, vehicles, employees, and collection points for testing.

### Step 6: Start the backend

```bash
cd wasteManagement

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

API available at: http://localhost:8080/api/v1

### Step 7: Start the frontend

```bash
cd frontend
npm install
npm start
```

Application available at: http://localhost:4200

---

## 🔑 Default Login

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@example.com` | `admin123` |

> The admin account is seeded automatically on first backend startup.

---

## ⚙️ Configuration

### Backend

**File:** `wasteManagement/src/main/resources/application.properties`

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/waste_management_db
vroom.url=http://localhost:3000
jwt.secret=your-secret-key
jwt.expiration=86400000
```

### Frontend

**Development:** `frontend/src/app/environment/environment.development.ts`
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1'
};
```

**Production:** `frontend/src/app/environment/environment.prod.ts`
```typescript
export const environment = {
  production: true,
  apiUrl: '/api/v1'  // Assumes reverse proxy
};
```

### VROOM + OSRM (Docker)

The `docker-compose.yml` handles all routing configuration automatically:

```yaml
vroom:
  environment:
    VROOM_ROUTER: osrm
    VROOM_OSRM_URL: http://osrm:5000  # Internal Docker network
```

> **No additional config files needed.** VROOM connects to OSRM via environment variables.

---

## 📡 API Reference

**Base URL:** `http://localhost:8080/api/v1`

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/signin` | Login (returns JWT) |
| POST | `/auth/signup` | Register new user |

### Auto-Planning
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/auto-planning/mode` | Get current planning mode |
| POST | `/auto-planning/mode/{mode}` | Set mode: `OFF`, `SCHEDULED`, `EMERGENCY`, `BOTH` |
| POST | `/auto-planning/run/scheduled` | Trigger scheduled route planning |
| POST | `/auto-planning/run/emergency` | Trigger emergency route planning |

### Core Resources (CRUD)
| Resource | Endpoint |
|----------|----------|
| Admins | `/admins` |
| Employees | `/employees` |
| Vehicles | `/vehicles` |
| Collection Points | `/collectionPoints` |
| Bins | `/bins` |
| Bin Readings | `/bin-readings` |
| Incidents | `/incidents` |
| Alerts | `/alerts` |
| Routes (Tournees) | `/tournees` |
| Route Planning | `/tournees/plan` |
| Active Routes | `/tournees/in-progress` |
| CO₂ Reports | `/tournees/7-days-co2` |
| Depots | `/depots` |
| Route Steps | `/route-steps` |
| Assignments | `/tournee-assignments` |

### Background Jobs
| Job | Schedule | Description |
|-----|----------|-------------|
| Emergency planning | Every ~15 min | Handles urgent collection needs |
| Scheduled planning | Daily at 06:00 | Plans regular collection routes |

---

## 📁 Project Structure

```
waste-management-repo/
├── frontend/                      # Angular 20 SPA
│   ├── src/app/
│   │   ├── core/                 # Auth guards, services, layouts
│   │   ├── features/             # Feature modules
│   │   │   ├── dashboard/        # Main dashboard
│   │   │   ├── bin/              # Bin management
│   │   │   ├── vehicle/          # Vehicle management
│   │   │   ├── tournee-map/      # Route visualization
│   │   │   └── ...
│   │   ├── shared/               # Reusable components
│   │   └── environment/          # Environment configs
│   └── package.json
│
├── wasteManagement/               # Spring Boot API
│   ├── src/main/java/.../backend/
│   │   ├── controller/           # REST endpoints
│   │   ├── service/              # Business logic
│   │   ├── repository/           # MongoDB repositories
│   │   ├── model/                # Domain entities
│   │   ├── dto/                  # Request/Response DTOs
│   │   ├── vroom/                # VROOM API integration
│   │   ├── security/             # JWT authentication
│   │   └── config/               # Spring configuration
│   └── pom.xml
│
├── demo-data/                     # MongoDB sample data
│   └── waste_management_db/      # BSON exports
│
├── osrm-data/                     # OSRM map files (gitignored)
│
└── docker-compose.yml             # OSRM + VROOM services
```

---

## 🗺️ Using a Different Map Region

The project is configured for Tunisia (`tunisia-25-11-25`). To use a different region:

1. Download your region's `.osm.pbf` from [Geofabrik](https://download.geofabrik.de/)
2. Place in `osrm-data/` (e.g., `osrm-data/morocco-latest.osm.pbf`)
3. **Update `docker-compose.yml`** — change all occurrences of `tunisia-25-11-25` to your filename:
   ```yaml
   # In the osrm service:
   command: ["osrm-routed", "--algorithm", "mld", "/data/YOUR-FILENAME.osrm"]
   
   # In osrm-extract:
   command: ["osrm-extract", "-p", "/opt/car.lua", "/data/YOUR-FILENAME.osm.pbf"]
   
   # In osrm-partition and osrm-customize:
   command: ["osrm-partition", "/data/YOUR-FILENAME.osrm"]
   command: ["osrm-customize", "/data/YOUR-FILENAME.osrm"]
   ```
4. Run preprocessing:
   ```bash
   docker compose --profile tools run --rm osrm-extract
   docker compose --profile tools run --rm osrm-partition
   docker compose --profile tools run --rm osrm-customize
   ```
5. Start services: `docker compose up -d`

---

## 🐳 Docker Services

| Service | Image | Port | Description |
|---------|-------|------|-------------|
| `osrm` | `osrm/osrm-backend:v5.27.1` | 5000 | Road routing engine |
| `vroom` | `vroomvrp/vroom-docker:v1.13.0` | 3000 | Vehicle route optimizer |

**Tools (run once for map setup):**
| Service | Purpose |
|---------|---------|
| `osrm-extract` | Extracts road network from `.osm.pbf` |
| `osrm-partition` | Partitions the road graph |
| `osrm-customize` | Applies routing weights |

---

## 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| VROOM returns empty routes | Check that your coordinates are within Tunisia (or your loaded map region) |
| OSRM returns 400 errors | Ensure all `.osrm.*` files exist in `osrm-data/` and names match `docker-compose.yml` |
| `osrm-routed` fails to start | Verify the filename in docker-compose matches your actual `.osrm` file |
| MongoDB connection refused | Verify MongoDB is running on port 27017 |
| Frontend can't reach API | Check `apiUrl` in environment files matches backend |
| "No route found" | Coordinates may be outside the loaded map area |
| Containers not on same network | Run `docker compose up -d` (compose handles networking automatically) |

---

## 📝 .gitignore Recommendations

```gitignore
# Dependencies
frontend/node_modules/
frontend/.angular/

# Build outputs
frontend/dist/
wasteManagement/target/

# OSRM data (large files)
osrm-data/

# IDE
.idea/
.vscode/
*.iml

# Logs
*.log
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

## 🙏 Acknowledgments

- [VROOM Project](https://github.com/VROOM-Project/vroom) — Vehicle Routing Open-source Optimization Machine
- [OSRM](https://project-osrm.org/) — Open Source Routing Machine
- [Leaflet](https://leafletjs.com/) — Interactive maps library
- [Geofabrik](https://download.geofabrik.de/) — OpenStreetMap data extracts
- [Flaticon](https://www.flaticon.com/fr/icone-gratuite/un-camion_2554978) — Truck illustration
- [Vecteezy](https://fr.vecteezy.com/png/29935123-rouge-emplacement-icone-symbole) — Pinpoint illustration
