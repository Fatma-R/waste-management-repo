# Testing getAvailableEmployeeForTournee with Existing Endpoints

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

### Step 1: Create 2 Employees

**Note:** Employees require a linked User. First, you need to create users or use existing user IDs.

**POST** `http://localhost:8080/api/v1/employees`

**Employee 1:**
```json
{
  "user": {
    "id": "EXISTING_USER_ID_1"
  },
  "skill": "DRIVER"
}
```

Save the returned `id` as `EMPLOYEE_1_ID`

**Employee 2:**
```json
{
  "user": {
    "id": "EXISTING_USER_ID_2"
  },
  "skill": "HELPER"
}
```

Save the returned `id` as `EMPLOYEE_2_ID`

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
  "startedAt": "2025-11-29T08:00:00Z",
  "finishedAt": "2025-11-29T17:00:00Z",
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
  "startedAt": "2025-11-29T09:00:00Z",
  "finishedAt": "2025-11-29T17:00:00Z",
  "steps": []
}
```

Save the returned `id` as `TOURNEE_2_ID`

---

### Step 3: Create 2 Vehicles (for assignments)

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

Save as `VEHICLE_1_ID`

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

Save as `VEHICLE_2_ID`

---

### Step 4: Test getAvailableEmployeeForTournee (Before Assignments)

**POST** `http://localhost:8080/api/v1/employees/available-for-tournee`

**Request Body (use Tournee 1):**
```json
{
  "id": "TOURNEE_1_ID",
  "tourneeType": "PLASTIC",
  "status": "PLANNED",
  "plannedKm": 45.5,
  "plannedCO2": 12.3,
  "startedAt": "2025-11-29T08:00:00Z",
  "finishedAt": "2025-11-29T17:00:00Z",
  "steps": []
}
```

**Expected Response:** Both employees available (since no assignments yet)
```json
[
  {
    "id": "EMPLOYEE_1_ID",
    "user": {
      "id": "USER_ID_1",
      "email": "employee1@example.com"
    },
    "skill": "DRIVER"
  },
  {
    "id": "EMPLOYEE_2_ID",
    "user": {
      "id": "USER_ID_2",
      "email": "employee2@example.com"
    },
    "skill": "HELPER"
  }
]
```

---

### Step 5: Create Tournee Assignment (Make Employee 1 Busy)

**POST** `http://localhost:8080/api/v1/tournee-assignments`

**Assignment (Employee 1 → Tournee 1):**
```json
{
  "tourneeId": "TOURNEE_1_ID",
  "employeeId": "EMPLOYEE_1_ID",
  "vehicleId": "VEHICLE_1_ID",
  "shiftStart": "2025-11-29T08:00:00Z",
  "shiftEnd": "2025-11-29T17:00:00Z"
}
```

This makes Employee 1 busy from 8 AM to 5 PM

---

### Step 6: Test Again (After Assignment)

**POST** `http://localhost:8080/api/v1/employees/available-for-tournee`

**Request 1 - For Tournee 1 (8 AM - 5 PM):**
```json
{
  "id": "TOURNEE_1_ID",
  "tourneeType": "PLASTIC",
  "status": "PLANNED",
  "plannedKm": 45.5,
  "plannedCO2": 12.3,
  "startedAt": "2025-11-29T08:00:00Z",
  "finishedAt": "2025-11-29T17:00:00Z",
  "steps": []
}
```

**Expected Response:** Only Employee 2 available
```json
[
  {
    "id": "EMPLOYEE_2_ID",
    "user": {
      "id": "USER_ID_2",
      "email": "employee2@example.com"
    },
    "skill": "HELPER"
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
  "startedAt": "2025-11-29T09:00:00Z",
  "finishedAt": "2025-11-29T17:00:00Z",
  "steps": []
}
```

**Expected Response:** Only Employee 2 available (because Employee 1 overlaps 9 AM - 5 PM)
```json
[
  {
    "id": "EMPLOYEE_2_ID",
    "user": {
      "id": "USER_ID_2",
      "email": "employee2@example.com"
    },
    "skill": "HELPER"
  }
]
```

---

## Summary of Endpoints Used

| Method | Endpoint | Requires Auth | Purpose |
|--------|----------|---------------|---------|
| POST | `/api/v1/auth/signin` | ❌ No | Get JWT token (FIRST) |
| POST | `/api/v1/employees` | ✅ ADMIN | Create employees |
| POST | `/api/v1/tournees` | ✅ ADMIN | Create tournees |
| POST | `/api/v1/vehicles` | ✅ ADMIN | Create vehicles |
| POST | `/api/v1/employees/available-for-tournee` | ✅ ADMIN | **Test the availability logic** |
| POST | `/api/v1/tournee-assignments` | ✅ ADMIN | Create assignments to make employees busy |
| GET | `/api/v1/employees` | ❌ No | View all employees |
| GET | `/api/v1/tournees` | ❌ No | View all tournees |

---

## Test Workflow Summary

1. **Sign in** → Get JWT token (`/api/v1/auth/signin`)
2. **Add Authorization header** → Use Bearer token in all following requests
3. **Create 2 employees** → Get IDs
4. **Create 2 tournees** → Get IDs
5. **Create 2 vehicles** → Get IDs (needed for assignments)
6. **Test availability** → Both employees should be available
7. **Create an assignment** → Assign Employee 1 to Tournee 1
8. **Test availability again** → Only Employee 2 should be available for overlapping time slots
9. **Verify the logic works correctly**

---

## Key Points

- The `getAvailableEmployeeForTournee()` method checks for **time overlap** between assignments
- Employee 1 assigned 8 AM - 5 PM will be marked as busy for any tournee overlapping that time
- Non-overlapping tournees (e.g., 6 PM - 7 PM) would still have Employee 1 available
- Employees need a linked User object to be created
- Both employees and vehicles are needed in assignments for complete testing

---

## Testing Tips

1. **Create multiple assignments** to test availability with different combinations
2. **Test non-overlapping times** to verify the overlap logic works correctly
3. **Use same employee for multiple assignments** with different time slots to see filtering in action
4. **Compare with vehicle availability** to see both methods work similarly

