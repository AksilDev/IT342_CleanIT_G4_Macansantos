# CleanIT

A full-stack application for managing cleaning service bookings with role-based access control. Available on Web and Android mobile platforms.

## Repository Structure

- `backend/` - Spring Boot REST API with JWT authentication
- `web/` - React (Vite) frontend with TypeScript
- `mobile/` - Android mobile app with Kotlin

## Features

### Client
- Browse services and create bookings with add-ons
- View booking history and status updates
- Reschedule or cancel bookings
- View before/after service photos

### Technician
- View and accept assigned bookings
- Update booking status and complete checklists
- Upload before/after photos
- Manage availability settings

### Admin
- Real-time booking statistics dashboard
- Interactive booking drill-down with search and sort
- Void/terminate bookings
- User verification and management
- System-wide analytics

## Tech Stack

### Web Frontend
- React 18 with TypeScript
- Tailwind CSS
- Axios for API communication
- React Router for navigation
- Vite for development and building

### Mobile (Android)
- Kotlin
- XML Layouts (Material Design)
- Retrofit 2.9.0 for API calls
- Coil 2.5.0 for image loading
- Coroutines for async operations
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

### Backend
- Java 17
- Spring Boot 3.x (Web, Security, Data JPA, OAuth2)
- PostgreSQL database
- JWT authentication (JJWT)
- Maven for dependency management

### Design Patterns
- Factory Pattern (user creation)
- Decorator Pattern (validation)
- Observer Pattern (notifications)
- Strategy Pattern (authentication)
- Vertical Slice Architecture (feature-based organization)

## Project Structure

```
.
├─ backend/
│  └─ src/main/java/com/G4/backend/
│     ├─ config/          # Security, JWT, OAuth2, initialization
│     ├─ controller/      # REST API endpoints
│     ├─ dto/             # Data transfer objects
│     ├─ entity/          # JPA entities
│     ├─ enums/           # Enumerations
│     ├─ exception/       # Custom exceptions
│     ├─ repository/      # Data access layer
│     └─ service/         # Business logic
│        ├─ decorator/    # Validation decorators
│        ├─ factory/      # User factory
│        └─ observer/     # Event observers
├─ web/
│  └─ src/
│     ├─ api/             # Axios configuration
│     ├─ components/      # Reusable components
│     ├─ pages/           # Page components
│     │  ├─ login/
│     │  ├─ register/
│     │  ├─ booking/
│     │  └─ dashboard/    # Role-specific dashboards
│     └─ types/           # TypeScript definitions
└─ mobile/
   └─ app/src/main/
      ├─ java/edu/cit/macansantos/cleanit/
      │  ├─ adapter/      # RecyclerView adapters
      │  ├─ model/        # Data models
      │  └─ network/      # Retrofit API service
      └─ res/
         ├─ layout/       # XML layouts
         └─ drawable/     # UI resources
```

## Environment Variables

### Web Frontend (`web/.env`)

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your_google_client_id
```

### Mobile (Android)

No environment variables needed. API base URL is configured in:
- `mobile/app/src/main/java/edu/cit/macansantos/cleanit/network/RetrofitClient.kt`
- Default: `http://10.0.2.2:8080/api/` (Android emulator)
- For physical device: Update to your machine's IP address

### Backend (`backend/.env`)

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/cleanit
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_EXPIRATION=86400000

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Admin Account
ADMIN_EMAIL=admin@cleanit.com
ADMIN_PASSWORD=your_secure_admin_password
ADMIN_NAME=Super Admin
```

**Security Notes:**
- Never commit `.env` files
- Use strong passwords in production
- Rotate JWT secrets regularly

## Running Locally

### Prerequisites
- Node.js 18+
- Java 17
- Maven 3.6+
- PostgreSQL 14+
- Android Studio (for mobile development)

### Database Setup

1. Create database:
   ```sql
   CREATE DATABASE cleanit;
   ```

2. Update `backend/.env` with credentials

3. Schema and seed data initialize automatically on first run

### Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend: `http://localhost:8080`

### Start Web Frontend

```bash
cd web
npm install
npm run dev
```

Frontend: `http://localhost:5173`

### Run Mobile App

1. Open `mobile/` folder in Android Studio
2. Sync Gradle files
3. Run on emulator or physical device

**Note:** For physical device, update the API base URL in `RetrofitClient.kt` to your machine's IP address (e.g., `http://192.168.1.100:8080/api/`)

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login (returns JWT)
- `GET /api/auth/oauth2/google` - Google OAuth2 login

### Services
- `GET /api/v1/services` - List all services
- `GET /api/v1/services/{id}` - Get service details
- `GET /api/v1/services/{id}/addons` - Get compatible add-ons

### Bookings
- `POST /api/v1/bookings/create` - Create booking
- `GET /api/v1/bookings/client/{clientId}` - Get client bookings
- `POST /api/v1/bookings/{bookingId}/cancel` - Cancel booking
- `POST /api/v1/bookings/{bookingId}/reschedule` - Reschedule booking

### Technician
- `GET /api/v1/technician/bookings/pending` - Get pending bookings
- `POST /api/v1/technician/bookings/{id}/accept` - Accept booking
- `POST /api/v1/technician/bookings/{id}/status` - Update status
- `POST /api/v1/technician/bookings/{id}/photos` - Upload photos

### Admin
- `GET /api/v1/admin/dashboard/statistics` - Get statistics
- `GET /api/v1/admin/bookings/by-status` - Get bookings by status
- `POST /api/v1/admin/bookings/{id}/void` - Void/terminate booking
- `GET /api/v1/admin/pending-verifications` - Get pending users
- `POST /api/v1/admin/verify-user/{id}` - Approve/reject user

## Service Configuration

### Available Services
1. **Standard External Cleaning** - ₱200, 90 min
2. **Deep Internal Cleaning** - ₱1250, 150 min
3. **GPU Deep Cleaning** - ₱600, 60 min
4. **PSU Cleaning** - ₱450, 45 min

### Pre-Service Checklist
1. Verify location is valid and searchable
2. Inspect tools are clean and working
3. Client available and gives consent
4. Inspect unit for physical damages
5. Take photo of unit before service starts

## Dependencies

### Backend
```xml
<!-- Spring Boot -->
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-oauth2-client

<!-- Database -->
postgresql

<!-- JWT -->
jjwt-api (0.11.5)
```

### Web Frontend
```json
{
  "react": "^18.2.0",
  "react-router-dom": "^6.x",
  "axios": "^1.x",
  "tailwindcss": "^3.x",
  "typescript": "^5.x",
  "vite": "^5.x"
}
```

### Mobile (Android)
```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Async
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Image Loading
implementation("io.coil-kt:coil:2.5.0")

// UI
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
implementation("com.google.android.material:material:1.11.0")
```

## Deployment

### Backend Options
- Heroku (Java buildpack + PostgreSQL)
- AWS Elastic Beanstalk
- Railway
- Render

### Web Frontend Options
- Vercel
- Netlify
- AWS S3 + CloudFront

### Mobile (Android) Options
- Google Play Store (production)
- Firebase App Distribution (beta testing)
- APK direct distribution (internal testing)

### Database Options
- AWS RDS
- Heroku Postgres
- Supabase
- Railway

## Troubleshooting

### Backend won't start
- Check PostgreSQL is running
- Verify database credentials in `.env`
- Ensure Java 17 is installed: `java -version`

### Web Frontend won't start
- Clear node_modules: `rm -rf node_modules && npm install`
- Check Node.js version: `node -v` (should be 18+)
- Verify `.env` file exists

### Mobile app won't build
- Sync Gradle files in Android Studio
- Check Android SDK is installed (SDK 34)
- Verify Java 17 is configured in Android Studio
- Clean and rebuild: `./gradlew clean assembleDebug`

### Mobile app can't connect to backend
- Emulator: Use `http://10.0.2.2:8080/api/`
- Physical device: Use your machine's IP (e.g., `http://192.168.1.100:8080/api/`)
- Ensure backend is running and accessible
- Check firewall settings

### Database connection errors
- Verify PostgreSQL service is running
- Check database exists: `psql -l`

---

## 📱 Mobile App Status

**Completion:** 90% ✅  
**Build Status:** Successful (0 errors)  
**Testing Status:** Ready for production testing

### Mobile Features
- ✅ Email/password authentication
- ✅ User registration with auto-login
- ✅ Home dashboard with pull-to-refresh
- ✅ Service browsing with images (Coil)
- ✅ Search services functionality
- ✅ Complete booking creation flow
- ✅ Booking management with status filter
- ✅ Cancel/reschedule bookings
- ✅ Before/after photos display
- ✅ No-show notification handling
- ✅ Profile management

**See `MOBILE_APP_STATUS.md` for detailed implementation report.**

---

**Last Updated**: May 2026
