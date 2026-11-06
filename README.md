# 🏛️ Renaissance — Smart GPS-Based Attendance Management System

**Renaissance** is a modern, production-ready, full-stack college classroom attendance management web application powered by **strict 15-meter GPS geofencing** using the Haversine formula.

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
  - **Spring Data JPA & Hibernate** (Relational ORM, entity relationships, custom query methods)
  - **Spring WebSocket with STOMP** (Real-time live attendance counters and feeds)
  - **Apache Commons CSV** (RFC 4180 compliant CSV export and bulk roster import)
- **Maven** (Build and dependency management)

### **Database**
- **MySQL 8.x** (Primary relational persistence with indexes, unique constraints, and audit logs)

---

## 📐 System Architecture

Renaissance is deployed as a single self-contained Spring Boot web application where static assets are served directly from `src/main/resources/static/`:

```
c:\Users\Shrio\Attendance-System\
├── pom.xml
├── .env.example
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/renaissance/
│   │   │   ├── RenaissanceApplication.java
│   │   │   ├── config/             # SecurityConfig, WebSocketConfig, WebConfig, DataInitializer
│   │   │   ├── controller/         # Auth, Student, Teacher, Class, Subject, Attendance, Session, Report
│   │   │   ├── dto/                # Request/Response data transfer objects
│   │   │   ├── entity/             # 9 JPA entities (Teacher, Student, CollegeClass, etc.)
│   │   │   ├── exception/          # GlobalExceptionHandler, AttendanceValidationException
│   │   │   ├── repository/         # 9 Spring Data JPA repositories
│   │   │   ├── security/           # CustomUserDetailsService, SecurityUser
│   │   │   ├── service/            # AttendanceService, LocationVerificationService, SessionService, etc.
│   │   │   └── util/               # GeoLocationUtils (Haversine), AttendanceCodeGenerator
│   │   └── resources/
│   │       ├── application.properties
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
│       ├── java/com/renaissance/           # GeoLocationUtilsTest, AttendanceServiceTest, AppTests
│       └── resources/
│           └── application-test.properties # In-memory test harness
```

---

## 🛰️ Core GPS Geofencing Logic (Haversine Formula)

When a faculty member starts an attendance session:
1. The teacher's browser captures exact GPS coordinates $(\text{lat}_1, \text{lon}_1)$ via `navigator.geolocation.getCurrentPosition`.
2. Spring Boot creates an `AttendanceSession` with `geofence_radius_meters = 15.0`.
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
   - If $d \le 15.0 \text{ meters}$, attendance is recorded as `PRESENT`.
   - If $d > 15.0 \text{ meters}$, attendance is rejected with error: *"You are outside the 15-meter attendance radius."*
   - Every attempt (success or failure) is logged in `attendance_attempt_logs` with distance, IP address, and client User-Agent for auditing.

---

## 🗄️ MySQL Database Schema

The database consists of 9 normalized tables:
1. `teachers`: Faculty accounts, BCrypt passwords, department mapping, and roles.
2. `departments`: Academic branches (CSE, ISE, ECE, MECH).
3. `classes`: Specific semesters and sections with unique constraints on `(department_id, semester, section)`.
4. `subjects`: Courses mapped to department and semester.
5. `students`: Unique indexed `usn` (e.g., `1MS21CS001`), name, email, class association.
6. `enrollments`: Enrolled students mapped to subjects.
7. `attendance_sessions`: Live teacher coordinates, active code, start/expiration time, status.
8. `attendance_records`: Individual marked attendance records with unique constraint on `(session_id, student_id)`.
9. `attendance_attempt_logs`: Complete audit trail of all attendance attempts including GPS coordinates, accuracy, and failure reasons.

---

## 🚀 Setup & Installation Guide

### Prerequisites
1. **Java JDK 21+** (JDK 21 or JDK 27)
   ```bash
   java -version
   ```
2. **Apache Maven 3.9+**
   ```bash
   mvn -version
   ```
3. **MySQL Server 8.x**
   Ensure MySQL server is installed and running on port 3306.

---

### Step 1: Database Setup in MySQL

Log in to your MySQL terminal or MySQL Workbench:
```sql
CREATE DATABASE renaissance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

### Step 2: Configure Database Credentials

Configure your database credentials in `src/main/resources/application.properties` or via environment variables:

```bash
# Optional: Set via environment variables
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/renaissance_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
set SPRING_DATASOURCE_USERNAME=root
set SPRING_DATASOURCE_PASSWORD=your_password
```

Hibernate will automatically generate all 9 tables on startup via `spring.jpa.hibernate.ddl-auto=update`.

---

### Step 3: Build & Package the Application

Run from the project root directory (`c:\Users\Shrio\Attendance-System`):

```bash
mvn clean package
```

All 12 unit tests will execute, validating the 15-meter boundary, duplicate protection, expiration checks, and JPA repository mappings.

---

### Step 4: Run the Application

#### Option A: Run via Maven
```bash
mvn spring-boot:run
```

#### Option B: Run the standalone JAR
```bash
java -jar target/renaissance-attendance-system-1.0.0.jar
```

The application will start on **http://localhost:8080**.

---

## 🔑 Default Provisioned Credentials & Sample Data

Upon initial startup, the `DataInitializer` automatically provisions:
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

---

## 🧪 Testing GPS Geofencing Locally

When testing on desktop browsers without physical movement:
1. Open Chrome DevTools (`F12`).
2. Press `Ctrl + Shift + P` and search for **"Show Sensors"**.
3. Under the **Sensors** tab, override the Geolocation:
   - **Inside 15m radius:** Use the exact coordinates or add $\pm 0.00008^{\circ}$ latitude (~9 meters).
   - **Outside 15m radius:** Add $+0.00045^{\circ}$ latitude (~50 meters) to test the strict rejection error: *"You are outside the 15-meter attendance radius."*

---

## 📄 License & Attribution
Developed for college classroom attendance management by the Renaissance Project Team.
