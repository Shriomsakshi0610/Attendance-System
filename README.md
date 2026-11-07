# 🏛️ Renaissance — Smart GPS-Based Attendance Management System

**Renaissance** is a modern, production-ready, full-stack college classroom attendance management web application powered by **strict 40-meter GPS geofencing** using the Haversine formula and backed by **MongoDB**.

Built specifically to eliminate proxy attendance in university lecture halls, Renaissance pairs ephemeral random attendance codes with cryptographic backend coordinate distance calculations, ensuring attendance can only be registered if the student is physically present inside the classroom.

---

## ⚡ Technology Stack

### **Frontend**
- **HTML5** (Semantic structure)
- **CSS3** (Custom responsive SaaS design system: Deep Purple `#2e1065`, Indigo `#4338ca`, Electric Violet `#8b5cf6`)
- **Vanilla JavaScript (ES6+)** (No React, Angular, Vue, or frontend frameworks)
- **Fetch API** (Asynchronous REST communication with session cookie propagation)
- **Chart.js** (Interactive weekly turnout and subject attendance analytics)

### **Backend**
- **Java 21 / 27**
- **Spring Boot 3.3.5**
  - **Spring Web** (REST API controllers)
  - **Spring Security** (BCrypt hashing, role-based protection, HTTP-only cookie sessions)
  - **Spring Data MongoDB** (Document mapping, compound indexes, references, custom MongoTemplate aggregation)
  - **MongoDB Java Driver 5.x**
  - **Spring WebSocket with STOMP** (Real-time live attendance counters and feeds)
  - **Apache Commons CSV** (RFC 4180 compliant CSV export and bulk roster import)
- **Maven** (Build and dependency management)

### **Database**
- **MongoDB Atlas / Local MongoDB** (Document database with compound unique indexes and audit logs)

---

## 📐 System Architecture

Renaissance is deployed as a single self-contained Spring Boot web application where static assets are served directly from `src/main/resources/static/`:

```
c:\Users\Shrio\Attendance-System\
├── pom.xml
├── Dockerfile
├── .env.example
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/renaissance/
│   │   │   ├── RenaissanceApplication.java
│   │   │   ├── config/             # SecurityConfig, WebSocketConfig, WebConfig, DataInitializer, DatabaseConfigValidator
│   │   │   ├── controller/         # Auth, Student, Teacher, Class, Subject, Attendance, Session, Report, Health
│   │   │   ├── dto/                # Request/Response data transfer objects
│   │   │   ├── entity/             # 9 MongoDB documents (Teacher, Student, CollegeClass, AttendanceRecord, etc.)
│   │   │   ├── exception/          # GlobalExceptionHandler, AttendanceValidationException
│   │   │   ├── repository/         # 9 Spring Data MongoRepository interfaces & MongoTemplate custom impls
│   │   │   ├── security/           # CustomUserDetailsService, SecurityUser
│   │   │   ├── service/            # AttendanceService, LocationVerificationService, SessionService, etc.
│   │   │   └── util/               # GeoLocationUtils (Haversine), AttendanceCodeGenerator
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-production.properties
│   │       ├── application-local.properties
│   │       └── static/
│   │           ├── index.html              # Main welcome & portal selector
│   │           ├── student-attendance.html # Mobile-first student attendance page
│   │           ├── teacher-login.html      # Secure faculty authentication
│   │           ├── dashboard.html          # Teacher analytics & stats dashboard
│   │           ├── students.html           # Student management & bulk CSV import
│   │           ├── attendance.html         # Records, <75% shortage alert & student profile modal
│   │           ├── sessions.html           # Session launcher with GPS capture & live feed
│   │           ├── reports.html            # Institutional analytics & CSV export
│   │           ├── css/                    # style.css, components.css, student.css, dashboard.css, responsive.css
│   │           └── js/                     # app.js, api.js, auth.js, location.js, attendance.js, etc.
│   └── test/
│       ├── java/com/renaissance/           # GeoLocationUtilsTest, AttendanceServiceTest, AppTests, MongoTestConfig
│       └── resources/
│           └── application-test.properties # In-memory MongoDB wire test harness
```

---

## 🛰️ Core GPS Geofencing Logic (Haversine Formula)

When a faculty member starts an attendance session:
1. The teacher's browser captures exact GPS coordinates $(\text{lat}_1, \text{lon}_1)$ via `navigator.geolocation.getCurrentPosition`.
2. Spring Boot creates an `AttendanceSession` with `geofence_radius_meters = 40.0`.
3. An ephemeral 6-character random alphanumeric code is generated (e.g. `RN7A82`).

When a student submits attendance:
1. Student's coordinates $(\text{lat}_2, \text{lon}_2)$ and GPS accuracy radius (in meters) are captured.
2. The server independently calculates the great-circle distance using the Haversine equation:
   $$\Delta \text{lat} = \text{radians}(\text{lat}_2 - \text{lat}_1)$$
   $$\Delta \text{lon} = \text{radians}(\text{lon}_2 - \text{lon}_1)$$
   $$a = \sin^2\left(\frac{\Delta \text{lat}}{2}\right) + \cos(\text{radians}(\text{lat}_1)) \cdot \cos(\text{radians}(\text{lat}_2)) \cdot \sin^2\left(\frac{\Delta \text{lon}}{2}\right)$$
   $$c = 2 \cdot \text{atan2}(\sqrt{a}, \sqrt{1 - a})$$
   $$d = R \cdot c \quad \text{where } R = 6,371,000 \text{ meters}$$
3. **Boundary Verification**:
   - If $d \le 40.0 \text{ meters}$, attendance is recorded as `PRESENT`.
   - If $d > 40.0 \text{ meters}$, attendance is rejected with error: *"You are outside the 40-meter attendance radius."*
   - Every attempt (success or failure) is logged in `attendance_attempt_logs` with distance, IP address, and client User-Agent for auditing.

---

## 🍃 MongoDB Collections & Schema Design

The application models data into 9 MongoDB collections:
1. `teachers`: Faculty accounts, BCrypt passwords, unique email index, department reference.
2. `departments`: Academic branches (`CSE`, `ISE`, `ECE`, `MECH`) with unique code index.
3. `classes`: Specific semesters and sections with compound unique index on `{department: 1, semester: 1, section: 1}`.
4. `subjects`: Courses mapped to department and semester with unique code index.
5. `students`: Unique indexed `usn` (e.g., `1MS21CS001`), name, email, department & class references.
6. `enrollments`: Enrolled students mapped to subjects with compound unique index on `{student: 1, subject: 1}`.
7. `attendance_sessions`: Live teacher coordinates, active code index, start/expiration time, status.
8. `attendance_records`: Marked attendance records with compound unique index on `{session: 1, student: 1}` (strictly preventing duplicate submissions).
9. `attendance_attempt_logs`: Complete audit trail of all attendance attempts including GPS coordinates, accuracy, and failure reasons.

---

## 🚀 Setup & Installation Guide

### Prerequisites
1. **Java JDK 21+** (JDK 21 or JDK 27)
   ```bash
   java -version
   ```
2. **Apache Maven 3.9+** (or use included `mvnw.cmd`)
   ```bash
   .\mvnw.cmd -version
   ```
3. **MongoDB Atlas or Local MongoDB**

---

### Step 1: Database Setup (MongoDB Atlas)

1. Create a free cluster on [MongoDB Atlas](https://cloud.mongodb.com).
2. Create a database user and allow your IP address (or `0.0.0.0/0` for cloud deployment).
3. Copy your connection string:
   ```text
   mongodb+srv://<username>:<password>@<cluster>.mongodb.net/renaissance_db?retryWrites=true&w=majority
   ```

---

### Step 2: Configure Database Credentials

Configure `MONGODB_URI` via environment variable or in `application.properties`:

```bash
set MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/renaissance_db?retryWrites=true&w=majority
```

Spring Data MongoDB will automatically initialize collections and unique compound indexes on first startup.

---

### Step 3: Build & Test the Application

Run from the project root directory (`c:\Users\Shrio\Attendance-System`):

```bash
.\mvnw.cmd clean test
```

All 12 unit and integration tests will execute against an in-memory MongoDB wire server (`mongo-java-server`), validating:
- The 40-meter boundary condition
- Outside-radius rejection
- Duplicate attendance submission prevention
- Expired session protection
- MongoDB entity and repository mappings

---

### Step 4: Run the Application

#### Option A: Run via Maven Wrapper
```bash
.\mvnw.cmd spring-boot:run
```

#### Option B: Build and run the standalone JAR
```bash
.\mvnw.cmd clean package -DskipTests
java -jar target/renaissance-attendance-system-1.0.0.jar
```

The application will start on **http://localhost:8080**.

---

## 🔑 Default Provisioned Credentials & Sample Data

Upon initial startup, `DataInitializer` automatically provisions:
- **Default Faculty Administrator:**
  - **Email:** `teacher@renaissance.edu`
  - **Password:** `Admin@1234`
  - **Name:** Prof. Charles Babbage
- **Sample Departments:** `CSE`, `ISE`, `ECE`, `MECH`
- **Sample Classes:** `CSE - Sem 5 (A)`, `CSE - Sem 5 (B)`, `ISE - Sem 5 (A)`, `ECE - Sem 3 (A)`
- **Sample Subjects:** `21CS51` (Database Management Systems), `21CS52` (Computer Networks), `21CS53` (Software Engineering), `21CS54` (Automata Theory)
- **Pre-Enrolled Students:**
  - `1MS21CS001` — Aarav Sharma (CSE Sem 5A)
  - `1MS21CS002` — Ananya Rao (CSE Sem 5A)
  - `1MS21CS003` — Bhavya Patel (CSE Sem 5A)
  - `1MS21CS004` — Chetan Kumar (CSE Sem 5A)
  - `1MS21CS005` — Divya Nair (CSE Sem 5A)
  - `1MS21CS006` — Eshan Gupta (CSE Sem 5A)
  - `1MS21CS050` — Farhan Akhtar (CSE Sem 5B)

---

## 🌐 Application Navigation & URLs

| Page | URL | Description |
|---|---|---|
| **Portal Hub** | `http://localhost:8080/` | Landing page for choosing Student or Faculty portal |
| **Student Attendance** | `http://localhost:8080/student-attendance.html` | Mobile-first GPS verified attendance marking |
| **Faculty Login** | `http://localhost:8080/teacher-login.html` | Secure faculty login (BCrypt + HTTP-only cookies) |
| **Teacher Dashboard** | `http://localhost:8080/dashboard.html` | Analytics cards, Chart.js trends & active sessions |
| **Class Sessions** | `http://localhost:8080/sessions.html` | Launch sessions with GPS capture & live WebSocket counter |
| **Student Roster** | `http://localhost:8080/students.html` | Roster management, student search, and CSV bulk import |
| **Attendance Records** | `http://localhost:8080/attendance.html` | Table with <75% shortage alert & student profile modal |
| **Reports & CSV Export**| `http://localhost:8080/reports.html` | Filtered attendance reports & CSV download |
| **Health Check** | `http://localhost:8080/health` | Deployment health check for Render monitoring |

---

## 🧪 Testing GPS Geofencing Locally

When testing on desktop browsers without physical movement:
1. Open Chrome DevTools (`F12`).
2. Press `Ctrl + Shift + P` and search for **"Show Sensors"**.
3. Under the **Sensors** tab, override the Geolocation:
   - **Inside 40m radius:** Use the exact coordinates or add $\pm 0.00020^{\circ}$ latitude (~22 meters).
   - **Outside 40m radius:** Add $+0.00045^{\circ}$ latitude (~50 meters) to test the strict rejection error: *"You are outside the 40-meter attendance radius."*

---

## 📄 License & Attribution
Developed for college classroom attendance management by the Renaissance Project Team.
