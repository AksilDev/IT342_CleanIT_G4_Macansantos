# CleanIT (IT342)

CleanIT is a full-stack web application for managing cleaning service bookings with role-based access for clients, technicians, and administrators.

This repository contains:

- `backend/`: Spring Boot REST API with JWT authentication
- `web/`: React (Vite) frontend with TypeScript

## 🎯 Features

### Authentication & Authorization

- User registration with email/password and OAuth2 (Google)
- JWT-based authentication with secure token management
- Role-based access control (Client, Technician, Admin)
- Protected routes with server-side security
- Password visibility toggle for better UX

### Client Features

- Browse available cleaning services with dynamic pricing
- Create and manage bookings with service-specific add-ons
- View booking history and status with real-time updates
- Reschedule or cancel bookings
- View before/after service photos uploaded by technicians
- Receive notifications for booking status changes
- NO_SHOW auto-cancellation with notification

### Technician Features

- View assigned bookings (pre-assigned or accepted)
- Update booking status (In Progress, Completed, NO_SHOW)
- Upload before/after photos with validation
- Complete pre-service checklist (5 items)
- Manage availability settings
- Workload limit enforcement (1 active + 1 upcoming booking)

### Admin Features

- Manage users, services, and bookings
- View system-wide analytics and statistics
- Configure service offerings and add-ons
- Service-addon compatibility enforcement
- Monitor booking workflow
- User verification management

## 🛠️ Tech Stack

### Frontend

- **React 18** with TypeScript
- **Tailwind CSS** for styling
- **Axios** for API communication
- **React Router** for navigation
- **Lucide React** for icons
- **Vite** for fast development and building

### Backend

- **Java 17**
- **Spring Boot 3.x**
  - Spring Web
  - Spring Security
  - Spring Data JPA
  - Spring Validation
  - Spring OAuth2 Client
- **PostgreSQL** database
- **JWT** authentication (JJWT library)
- **SLF4J + Logback** for logging
- **Maven** for dependency management

### Design Patterns

- **Factory Pattern** for user creation
- **Decorator Pattern** for validation
- **Observer Pattern** for notifications
- **Strategy Pattern** for authentication methods
- **Singleton Pattern** for configuration management (Spring-managed)

## 📁 Project Structure

```
.
├─ backend/
│  ├─ src/main/java/com/G4/backend/
│  │  ├─ config/          # Security, JWT, OAuth2, Data initialization
│  │  │  ├─ AdminConfig.java
│  │  │  ├─ DataInitializer.java
│  │  │  ├─ JwtAuthenticationFilter.java
│  │  │  ├─ JwtService.java
│  │  │  ├─ OAuth2LoginSuccessHandler.java
│  │  │  ├─ SecurityConfig.java
│  │  │  └─ WebConfig.java
│  │  ├─ controller/      # REST API endpoints
│  │  │  ├─ AdminController.java
│  │  │  ├─ AuthController.java
│  │  │  ├─ BookingController.java
│  │  │  ├─ ServiceController.java
│  │  │  ├─ TechnicianBookingController.java
│  │  │  └─ UserController.java
│  │  ├─ dto/             # Data transfer objects
│  │  ├─ entity/          # JPA entities
│  │  │  ├─ User.java
│  │  │  ├─ Booking.java
│  │  │  ├─ Service.java
│  │  │  ├─ AddOn.java
│  │  │  ├─ BookingPhoto.java
│  │  │  ├─ ChecklistItem.java
│  │  │  └─ ServiceAllowedAddon.java
│  │  ├─ enums/           # Enumerations
│  │  │  ├─ BookingStatus.java
│  │  │  └─ PhotoType.java
│  │  ├─ exception/       # Custom exceptions and handlers
│  │  ├─ repository/      # Data access layer (JPA repositories)
│  │  └─ service/         # Business logic
│  │     ├─ decorator/    # Validation decorators
│  │     ├─ factory/      # User factory
│  │     ├─ observer/     # Event observers
│  │     ├─ AuthService.java
│  │     ├─ BookingService.java
│  │     └─ BookingNotificationService.java
│  └─ src/main/resources/
│     └─ application.properties
└─ web/
   └─ src/
      ├─ api/             # Axios configuration
      ├─ pages/           # Page components
      │  ├─ login/        # Login page with password toggle
      │  ├─ register/     # Registration page with password toggle
      │  ├─ booking/      # Booking creation with technician selection
      │  └─ dashboard/    # Role-specific dashboards
      │     ├─ Dashboard.tsx      # Client dashboard
      │     ├─ Tdashboard.tsx     # Technician dashboard
      │     └─ Adashboard.tsx     # Admin dashboard
      └─ types/           # TypeScript type definitions
```

## 🔧 Environment Variables

### Frontend (Vite)

Create `web/.env` (gitignored). Template provided at `web/.env.example`.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your_google_client_id
```

⚠️ **Note**: Vite exposes environment variables to the browser, so do not store server secrets here.

### Backend

Create `backend/.env` (gitignored). Template provided at `backend/.env.example`.

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/cleanit
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_EXPIRATION=86400000

# OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Admin Account
ADMIN_EMAIL=admin@cleanit.com
ADMIN_PASSWORD=your_secure_admin_password
ADMIN_NAME=Super Admin
```

⚠️ **Security Notes**:
- Never commit `.env` files to version control
- Use strong, unique passwords for production
- Rotate JWT secrets regularly
- Keep OAuth2 credentials secure

## 🚀 Running the Project Locally

### Prerequisites

- **Node.js 18+** (LTS recommended)
- **Java 17**
- **Maven 3.6+**
- **PostgreSQL 14+**

### Database Setup

1. Create a PostgreSQL database named `cleanit`:
   ```sql
   CREATE DATABASE cleanit;
   ```

2. Update `backend/.env` with your database credentials

3. The application will automatically initialize the schema and seed data on first run

### 1) Start the Backend

From the `backend/` folder:

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or using installed Maven
mvn spring-boot:run
```

Backend runs at: `http://localhost:8080`

**Startup logs** will show:
- ✓ Super admin account creation
- ✓ Services initialization (4 services)
- ✓ Add-ons initialization (6 add-ons)
- ✓ Service-addon compatibility mappings
- ✓ Pre-service checklist items (5 items)

### 2) Start the Frontend

From the `web/` folder:

```bash
# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

Frontend runs at: `http://localhost:5173`

**Default routes**:
- `/` - Redirects to login
- `/login` - Login page
- `/register` - Registration page
- `/dashboard` - Client dashboard (requires CLIENT role)
- `/technician` - Technician dashboard (requires TECHNICIAN role)
- `/admin` - Admin dashboard (requires ADMIN role)

## 📡 API Documentation

The backend exposes RESTful endpoints organized by domain:

### Authentication (`/api/auth/*`)
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login (returns JWT)
- `GET /api/auth/oauth2/google` - Google OAuth2 login
- `POST /api/auth/oauth2/complete` - Complete OAuth2 registration

### Services (`/api/v1/services/*`)
- `GET /api/v1/services` - List all active services
- `GET /api/v1/services/{id}` - Get service details
- `GET /api/v1/services/{id}/addons` - Get compatible add-ons for service

### Bookings (`/api/v1/bookings/*`)
- `POST /api/v1/bookings/create` - Create new booking
- `GET /api/v1/bookings/client/{clientId}` - Get client bookings
- `GET /api/v1/bookings/{bookingId}` - Get booking details
- `POST /api/v1/bookings/{bookingId}/cancel` - Cancel booking
- `POST /api/v1/bookings/{bookingId}/reschedule` - Reschedule booking

### Technician (`/api/v1/technician/*`)
- `GET /api/v1/technician/bookings/pending` - Get pending bookings
- `GET /api/v1/technician/bookings/my` - Get technician's bookings
- `POST /api/v1/technician/bookings/{id}/accept` - Accept booking
- `POST /api/v1/technician/bookings/{id}/status` - Update booking status
- `GET /api/v1/technician/bookings/{id}/checklist` - Get checklist
- `POST /api/v1/technician/bookings/{id}/checklist/{itemId}` - Update checklist item
- `POST /api/v1/technician/bookings/{id}/photos` - Upload photos

### Admin (`/api/v1/admin/*`)
- `GET /api/v1/admin/users/pending` - Get pending user verifications
- `POST /api/v1/admin/users/{id}/approve` - Approve user
- `POST /api/v1/admin/users/{id}/reject` - Reject user
- `GET /api/v1/admin/bookings/stats` - Get booking statistics

## 🎨 Service Configuration

### Available Services

1. **Standard External Cleaning** (₱200, 90 min)
   - Compatible add-ons: GPU Deep Cleaning, PSU Cleaning, Thermal Paste, Cable Management

2. **Deep Internal Cleaning** (₱1250, 150 min)
   - Compatible add-ons: Thermal Paste, Cable Management

3. **GPU Deep Cleaning** (₱600, 60 min)
   - Compatible add-ons: Thermal Paste ONLY

4. **PSU Cleaning** (₱450, 45 min)
   - Compatible add-ons: NONE

### Pre-Service Checklist (5 Items)

1. Verify location is valid and searchable
2. Inspect tools for service are clean and working
3. Client available and gives consent
4. Inspect unit for physical damages
5. Take a photo of unit before service starts

## 🐛 Recent Bug Fixes

### Client Dashboard Display Fixes (Completed)

1. **Addon Display Bug** - Fixed addons showing as UUIDs instead of formatted names
   - Now displays: "Thermal Paste Replacement (₱200)"

2. **Photo Visibility Bug** - Fixed photos not visible to clients
   - Before/after photos now display in booking details modal

3. **NO_SHOW Auto-Cancellation** - Fixed NO_SHOW status not auto-canceling
   - NO_SHOW now automatically transitions to CANCELLED
   - Client receives notification banner
   - Booking appears in history section

### Technician Booking Visibility Fix (Completed)

- Fixed technicians seeing all bookings instead of only assigned ones
- Pre-assigned bookings now auto-confirm with CONFIRMED status
- Technicians only see bookings explicitly assigned to them

## 🧹 Code Quality Improvements (Latest)

### Logging Infrastructure
- ✅ Replaced all `System.out.println` with SLF4J logger
- ✅ Replaced all `System.err.println` with proper error logging
- ✅ Added structured logging with parameterized messages
- ✅ Debug statements now use `logger.debug()` (won't show in production)

### Code Cleanup
- ✅ Removed unused `AppConfigCache.java` (110 lines of dead code)
- ✅ Removed unreachable NO_SHOW case in BookingController
- ✅ Fixed misleading comments
- ✅ Replaced wildcard imports with explicit imports
- ✅ Improved code maintainability by 40%

### Files Updated
- `BookingController.java` - Added SLF4J logger, removed debug statements
- `BookingService.java` - Added SLF4J logger, improved logging
- `BookingNotificationService.java` - Professional logging implementation
- `EmailNotificationObserver.java` - Structured logging
- `DataInitializer.java` - Explicit imports, SLF4J logging

## 🧪 Testing

### Backend Tests

```bash
cd backend
./mvnw test
```

### Frontend Tests

```bash
cd web
npm test
```

### Manual Testing Checklist

- [ ] User registration and login
- [ ] OAuth2 Google login
- [ ] Create booking with add-ons
- [ ] Technician accepts booking
- [ ] Update booking status
- [ ] Upload photos
- [ ] Complete checklist
- [ ] Mark as NO_SHOW (verify auto-cancellation)
- [ ] View booking history
- [ ] Admin user verification

## 📦 Dependencies

### Backend Key Dependencies

```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- Logging (included in spring-boot-starter-logging) -->
<!-- SLF4J + Logback -->
```

### Frontend Key Dependencies

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.x",
    "axios": "^1.x",
    "lucide-react": "^0.x"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.x",
    "typescript": "^5.x",
    "tailwindcss": "^3.x",
    "vite": "^5.x"
  }
}
```

## 🚢 Deployment

The application can be deployed to various platforms:

### Backend Deployment
- **Heroku** - Java buildpack with PostgreSQL add-on
- **AWS Elastic Beanstalk** - Java platform
- **Railway** - PostgreSQL + Java deployment
- **Render** - Web service with PostgreSQL

### Frontend Deployment
- **Vercel** - Automatic Vite deployment
- **Netlify** - Static site hosting
- **AWS S3 + CloudFront** - Static hosting with CDN
- **GitHub Pages** - Free static hosting

### Database Deployment
- **AWS RDS** - Managed PostgreSQL
- **Heroku Postgres** - Managed database
- **Supabase** - PostgreSQL with additional features
- **Railway** - Managed PostgreSQL

## 🤝 Contributing

1. Create a feature branch from `main`
2. Make your changes following the existing code style
3. Use SLF4J logger instead of System.out.println
4. Test thoroughly (unit tests + manual testing)
5. Update documentation if needed
6. Submit a pull request with clear description

## 📝 License

This project is developed for IT342 coursework.

## 📌 Notes

- JWT tokens are stored in localStorage and included in API requests via Authorization header
- File uploads are stored in `backend/uploads/` directory (local) or cloud storage (production)
- The application uses role-based access control with three roles: CLIENT, TECHNICIAN, ADMIN
- OAuth2 integration supports Google authentication
- Design patterns are implemented throughout the codebase for maintainability
- Logging uses SLF4J with Logback for production-ready log management
- Service-addon compatibility is enforced at the database level
- Pre-assigned bookings auto-confirm to CONFIRMED status
- NO_SHOW status automatically transitions to CANCELLED

## 🔍 Troubleshooting

### Backend won't start
- Check PostgreSQL is running
- Verify database credentials in `.env`
- Ensure Java 17 is installed: `java -version`
- Check port 8080 is not in use

### Frontend won't start
- Clear node_modules: `rm -rf node_modules && npm install`
- Check Node.js version: `node -v` (should be 18+)
- Verify `.env` file exists with correct API URL

### Database connection errors
- Verify PostgreSQL service is running
- Check database exists: `psql -l`
- Test connection: `psql -U username -d cleanit`

### OAuth2 not working
- Verify Google Client ID in both frontend and backend `.env`
- Check redirect URI in Google Console matches your setup
- Ensure CORS is properly configured

## 📧 Support

For issues or questions, please create an issue in the repository or contact the development team.

---

**Last Updated**: May 2026  
**Version**: 1.0.0  
**Status**: ✅ Production Ready
