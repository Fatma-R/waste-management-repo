# waste-management-repo
Waste Management – Spring Boot + MongoDB + Angular
A full-stack waste management application built with:

Backend: Spring Boot (Java 21) + MongoDB
Frontend: Angular (standalone components)
Database: MongoDB (local instance)


Project Structure
waste-management-repo/
├── .github/
│   └── workflows/          # GitHub Actions CI (runs backend tests)
├── wasteManagement/         # Spring Boot backend
│   ├── pom.xml
│   └── src/
└── frontend/                # Angular frontend
    ├── angular.json
    └── src/

Prerequisites
Install the following tools locally:

Java 21 (JDK)
Maven 3.9+
Node.js 18+ and npm
Angular CLI (optional but recommended):

bash  npm install -g @angular/cli

MongoDB running on localhost:27017

Database: waste_management_db
No authentication required for local development
Optional: Use MongoDB Compass to inspect data




Backend Setup (Spring Boot + MongoDB)
1. Configuration
# Waste Management — Full Stack

An example full-stack waste management application.

- Backend: Spring Boot (Java 21) + MongoDB
- Frontend: Angular (standalone components)
- Database: MongoDB (local)

---

## Table of contents

- [Overview](#overview)
- [Quick start (Windows / PowerShell)](#quick-start-windows--powershell)
- [Project structure](#project-structure)
- [Running the app](#running-the-app)
- [Useful paths](#useful-paths)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Overview

This repo contains a Spring Boot backend and an Angular frontend for a waste management demo app. It's aimed at local development and CI testing.

The frontend expects the backend API to be available at http://localhost:8080 during development.

## Quick start (Windows / PowerShell)

1. Ensure prerequisites are installed:

- Java 21 (JDK)
- Maven 3.9+
- Node.js 18+ and npm
- MongoDB (running on localhost:27017)

2. Start the backend (from repo root):

```powershell
cd .\wasteManagement
mvn spring-boot:run
```

3. In a second terminal, start the frontend:

```powershell
cd .\frontend
npm install   # only needed the first time or after changes to package.json
npm start
# or: npx ng serve --open
```

Open the frontend at http://localhost:4200 and the API at http://localhost:8080/api/hello

Notes:
- Start the backend first so the frontend can fetch API data during development.
- The dev server runs on port 4200 by default.

## Project structure (high level)

```
waste-management-repo/
├── .github/               # CI workflows
├── wasteManagement/       # Spring Boot backend
└── frontend/              # Angular frontend
        └── src/               # Angular sources (components, services, routes)
```

Useful frontend files:

- `src/app/app.ts` — application bootstrap
- `src/app/app.routes.ts` — route definitions
- `src/app/core/services/hello.ts` — example service
- `src/app/features/hello/hello-page/` — hello page (component template, styles)

## Running the app

- Backend API (after mvn spring-boot:run):

    - http://localhost:8080/api/hello

- Frontend:

    - http://localhost:4200
    - Example route: http://localhost:4200/hello

## Troubleshooting

- MongoDB connection errors: make sure `mongod` is running and accessible at `localhost:27017`.
- Port conflicts: ensure ports 8080 and 4200 are free or change them in the app configuration.
- If the frontend is blank at http://localhost:4200:
    1. Check the frontend terminal for compile/runtime errors.
    2. Ensure the backend is running (the hello endpoint should return `Hello World`).
    3. Open browser DevTools console for runtime JS errors.

## Contributing

1. Fork the repo and create a feature branch.
2. Run and test locally (backend + frontend).
3. Open a PR with a clear description and any relevant testing notes.

---
