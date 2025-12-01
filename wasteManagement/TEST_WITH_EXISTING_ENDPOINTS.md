# Testing getAvailableVehiclesForTournee with Existing Endpoints

## Authentication Required ⚠️

All POST requests require **ADMIN authentication**. You'll get a 401 error if not authenticated.

### Step 0: Sign In First (Get JWT Token)

**POST** `http://localhost:8080/api/v1/auth/signin`

```json
{
  "email": "admin@example.com",
  "password": "admin123"
}
```

**Response:**
```json
{
  "id": "...",
  "email": "admin@example.com",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "roles": ["ADMIN"]
}
```

**Save the `token` value**

### Step 0b: Add Token to Postman Headers

For all following requests, add this header:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Or in Postman:
1. Click the **Auth** tab
2. Select **Bearer Token**
3. Paste your token

---

### Step 1: Create 2 Vehicles
**POST** `http://localhost:8080/api/v1/vehicles`

**Vehicle 1:**
```json
{
  "plateNumber": "TN-2024-001",
  "capacityVolumeL": 5000,
  "coordinates": [36.8065, 10.1967],
  "fuelType": "DIESEL",
  "status": "AVAILABLE"
}
```
Save the returned `id` as `VEHICLE_1_ID`

**Vehicle 2:**
```json
{
  "plateNumber": "TN-2024-002",
  "capacityVolumeL": 3000,
  "coordinates": [36.8100, 10.1900],
  "fuelType": "ELECTRIC",
  "status": "AVAILABLE"
}
```
Save the returned `id` as `VEHICLE_2_ID`

---

### Step 2: Create 2 Tournees
**POST** `http://localhost:8080/api/v1/tournees`

**Tournee 1:**
```json
{
  "tourneeType": "PLASTIC",
  "status": "PLANNED",
  "plannedKm": 45.5,
  "plannedCO2": 12.3,
  "startedAt": "2025-11-28T08:00:00Z",
  "finishedAt": "2025-11-28T17:00:00Z",
  "steps": []
}
```
Save the returned `id` as `TOURNEE_1_ID`

**Tournee 2:**
```json
{
  "tourneeType": "PAPER",
  "status": "PLANNED",
  "plannedKm": 32.0,
  "plannedCO2": 8.5,
  "startedAt": "2025-11-28T09:00:00Z",
  "finishedAt": "2025-11-28T17:00:00Z",
  "steps": []
}
```
Save the returned `id` as `TOURNEE_2_ID`

---

### Step 3: Test getAvailableVehiclesForTournee (Before Assignments)
**POST** `http://localhost:8080/api/v1/vehicles/available-for-tournee`

**Request Body (use Tournee 1):**
```json
{
  "id": "TOURNEE_1_ID",
  "tourneeType": "PLASTIC",
  "status": "PLANNED",
  "plannedKm": 45.5,
  "plannedCO2": 12.3,
  "startedAt": "2025-11-28T08:00:00Z",
  "finishedAt": "2025-11-28T17:00:00Z",
  "steps": []
}
```

**Expected Response:** Both vehicles available (since no assignments yet)
```json
[
  {
    "id": "VEHICLE_1_ID",
    "plateNumber": "TN-2024-001",
    "capacityVolumeL": 5000,
    "fuelType": "DIESEL",
    "status": "AVAILABLE"
  },
  {
    "id": "VEHICLE_2_ID",
    "plateNumber": "TN-2024-002",
    "capacityVolumeL": 3000,
    "fuelType": "ELECTRIC",
    "status": "AVAILABLE"
  }
]
```

---

### Step 4: Create Tournee Assignment (Make Vehicle 1 Busy)
**POST** `http://localhost:8080/api/v1/tournee-assignments`

**Assignment (Vehicle 1 → Tournee 1):**
```json
{
  "tourneeId": "TOURNEE_1_ID",
  "employeeId": "ANY_EXISTING_EMPLOYEE_ID",
  "vehicleId": "VEHICLE_1_ID",
  "shiftStart": "2025-11-28T08:00:00Z",
  "shiftEnd": "2025-11-28T17:00:00Z"
}
```
This makes Vehicle 1 busy from 8 AM to 5 PM

---

### Step 5: Test Again (After Assignment)
**POST** `http://localhost:8080/api/v1/vehicles/available-for-tournee`

**Request 1 - For Tournee 1 (8 AM - 5 PM):**
```json
{
  "id": "TOURNEE_1_ID",
  "tourneeType": "PLASTIC",
  "status": "PLANNED",
  "plannedKm": 45.5,
  "plannedCO2": 12.3,
  "startedAt": "2025-11-28T08:00:00Z",
  "finishedAt": "2025-11-28T17:00:00Z",
  "steps": []
}
```

**Expected Response:** Only Vehicle 2 available
```json
[
  {
    "id": "VEHICLE_2_ID",
    "plateNumber": "TN-2024-002",
    "capacityVolumeL": 3000,
    "fuelType": "ELECTRIC",
    "status": "AVAILABLE"
  }
]
```

**Request 2 - For Tournee 2 (9 AM - 5 PM):**
```json
{
  "id": "TOURNEE_2_ID",
  "tourneeType": "PAPER",
  "status": "PLANNED",
  "plannedKm": 32.0,
  "plannedCO2": 8.5,
  "startedAt": "2025-11-28T09:00:00Z",
  "finishedAt": "2025-11-28T17:00:00Z",
  "steps": []
}
```

**Expected Response:** Only Vehicle 2 available (because Vehicle 1 overlaps 9 AM - 5 PM)
```json
[
  {
    "id": "VEHICLE_2_ID",
    "plateNumber": "TN-2024-002",
    "capacityVolumeL": 3000,
    "fuelType": "ELECTRIC",
    "status": "AVAILABLE"
  }
]
```

---

## Summary of Existing Endpoints Used

| Method | Endpoint | Requires Auth | Purpose |
|--------|----------|---------------|---------|
| POST | `/api/v1/auth/signin` | ❌ No | Get JWT token (FIRST) |
| POST | `/api/v1/vehicles` | ✅ ADMIN | Create vehicles |
| POST | `/api/v1/tournees` | ✅ ADMIN | Create tournees |
| POST | `/api/v1/vehicles/available-for-tournee` | ✅ ADMIN | **Test the availability logic** |
| POST | `/api/v1/tournee-assignments` | ✅ ADMIN | Create assignments to make vehicles busy |
| GET | `/api/v1/vehicles` | ❌ No | View all vehicles |
| GET | `/api/v1/tournees` | ❌ No | View all tournees |

---

## Test Workflow Summary

1. **Sign in** → Get JWT token (`/api/v1/auth/signin`)
2. **Add Authorization header** → Use Bearer token in all following requests
3. **Create 2 vehicles** → Get IDs
4. **Create 2 tournees** → Get IDs
5. **Test availability** → Both vehicles should be available
6. **Create an assignment** → Assign Vehicle 1 to Tournee 1
7. **Test availability again** → Only Vehicle 2 should be available for overlapping time slots
8. **Verify the logic works correctly**

---

## Key Points

- The `getAvailableVehiclesForTournee()` method checks for **time overlap** between assignments
- Vehicle 1 assigned 8 AM - 5 PM will be marked as busy for any tournee overlapping that time
- Non-overlapping tournees (e.g., 6 PM - 7 PM) would still have Vehicle 1 available

