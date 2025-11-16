# 🌱 Waste Management – Spring Boot + MongoDB + Angular

A clean full-stack demo application for a waste-management system.

- **Backend:** Spring Boot (Java 21) + MongoDB  
- **Frontend:** Angular (standalone components)  
- **Database:** MongoDB (local development)

This project is built for learning, prototyping, and demonstrating a full client–server architecture with CI integration.

---

## 📑 Table of Contents

- [Overview](#overview)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Backend Setup](#backend-setup)
- [Frontend Setup](#frontend-setup)
- [Running the App](#running-the-app)
- [Useful Paths](#useful-paths)
- [Troubleshooting](#troubleshooting)
- [CI / GitHub Actions](#ci--github-actions)

---

## 📘 Overview

The repository contains:

- A Spring Boot backend exposing REST endpoints.
- An Angular frontend consuming those endpoints.
- A MongoDB instance running locally.
- A CI workflow using GitHub Actions to run backend tests.

The application runs entirely on your machine and does **not** require Docker.

---

## 📂 Project Structure

```
waste-management-repo/
├── .github/
│   └── workflows/            # GitHub Actions CI (runs backend tests)
├── wasteManagement/          # Spring Boot backend
│   ├── pom.xml
│   └── src/
└── frontend/                 # Angular frontend
    ├── angular.json
    └── src/
```

---

## 🧰 Prerequisites

Install these tools:

### Backend requirements  
- **Java 21 (JDK)**  
- **Maven 3.9+**  

### Frontend requirements  
- **Node.js 18+**  
- **npm**  
- (Optional but recommended) Angular CLI  
  ```
  npm install -g @angular/cli
  ```

### Database  
- **MongoDB running at:** `mongodb://localhost:27017`  
- **Database name:** `waste_management_db`  
- No authentication required for local development  
- Recommended: **MongoDB Compass** for inspection

---

# 🔧 Backend Setup

Backend located in:

```
wasteManagement/
```

### 1️⃣ MongoDB Configuration

File:  
```
wasteManagement/src/main/resources/application.properties
```

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/waste_management_db
```

### 2️⃣ Example Controller

```java
@RestController
@RequestMapping("/api/hello")
@CrossOrigin(origins = "http://localhost:4200")
public class HelloController {

    @GetMapping
    public String hello() {
        return "Hello World";
    }
}
```

### 3️⃣ Run Backend Tests

```bash
cd wasteManagement
mvn test
```

### 4️⃣ Start the Backend

```bash
cd wasteManagement
mvn spring-boot:run
```

Backend available at:

```
http://localhost:8080/api/hello
```

---

# 🎨 Frontend Setup

Frontend located in:

```
frontend/
```

### 1️⃣ Install dependencies

```bash
cd frontend
npm install
```

### 2️⃣ Run the frontend

```bash
npm start
```
or:
```bash
ng serve
```

Frontend dev server:

```
http://localhost:4200
```

### 3️⃣ Example Frontend Integration

Routes defined in:  
`src/app/app.routes.ts`

Service example:  
`src/app/core/services/hello.service.ts`

Hello page:  
`src/app/features/hello/hello-page.component.ts`

Open:

```
http://localhost:4200/hello
```

It should call the backend `/api/hello`.

---

# 🚀 Running the App (Full Workflow)

Open two terminals:

### **Terminal 1 – Backend**
```bash
cd wasteManagement
mvn spring-boot:run
```

Backend runs on:
```
http://localhost:8080/api/hello
```

### **Terminal 2 – Frontend**
```bash
cd frontend
npm start
```

Frontend runs on:
```
http://localhost:4200
```

---

# 📌 Useful Paths

### Backend
- `src/main/java/.../controller/` → REST controllers  
- `src/main/java/.../model/` → MongoDB documents  
- `src/main/resources/application.properties` → DB config  

### Frontend
- `src/app/app.routes.ts` → Routes  
- `src/app/core/services/` → API services  
- `src/app/features/hello/` → Hello example page  

---

# 🛠️ Troubleshooting

### ❗ Backend won't start
- Ensure MongoDB is running locally:
  ```
  mongod
  ```
- Verify the configured DB name exists or let Spring create it.

### ❗ Frontend shows blank page
- Check browser console errors
- Restart backend → then restart Angular
- Make sure CORS origin matches (`http://localhost:4200`)

### ❗ Port already in use
- Change Spring Boot port:
  ```
  server.port=8081
  ```
- Change Angular port:
  ```
  ng serve --port=4201
  ```

---

# 🔄 CI / GitHub Actions

Workflow file:

```
.github/workflows/ci.yml
```

On each push or PR:

1. Set up Java 21  
2. Start a MongoDB service (Docker)  
3. Run:

```bash
mvn test
```

If tests fail → CI turns red.

---

