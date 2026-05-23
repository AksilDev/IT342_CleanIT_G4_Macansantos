# CleanIT

A full-stack application for managing cleaning service bookings with role-based access control. Available on Web and Android mobile platforms.

## 🚀 Quick Links

- **Docker Setup**: See [DOCKER_SETUP.md](DOCKER_SETUP.md)
- **Physical Device Setup**: See [PHYSICAL_DEVICE_SETUP.md](PHYSICAL_DEVICE_SETUP.md)
- **Project Status**: ~95% complete (backend + web + mobile)

## Repository Structure

- `backend/` - Spring Boot REST API with JWT authentication
- `web/` - React (Vite) frontend with TypeScript
- `mobile/` - Android mobile app with Kotlin
- `docker-compose.yml` - Docker orchestration for backend
- `DOCKER_SETUP.md` - Complete Docker setup guide
- `PHYSICAL_DEVICE_SETUP.md` - Quick guide for physical devices

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
- Google Play Services Auth 20.7.0
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- **Dynamic API Configuration**: Auto-detects emulator vs physical device

**Architecture:**
- Feature-based modular structure
- MVVM-inspired pattern with Activities
- Repository pattern via Retrofit
- Shared utilities (navigation, session, network)
- Material Design 3 components
- Dynamic API URL configuration with `ApiConfig.kt`

**Recent Enhancements:**
- ✅ Google Account Picker: Always shows account selection
- ✅ Physical Device Support: Dynamic IP configuration
- ✅ Docker Support: Backend containerization for easy deployment

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
│  ├─ Dockerfile                # Docker image definition
│  ├─ .dockerignore            # Docker build exclusions
│  ├─ pom.xml                  # Maven dependencies
│  └─ src/main/java/com/G4/backend/
│     ├─ features/             # Feature-based vertical slices
│     │  ├─ auth/              # Authentication & authorization
│     │  │  ├─ AuthController.java
│     │  │  ├─ AuthService.java
│     │  │  ├─ AuthenticationContext.java
│     │  │  ├─ AuthenticationStrategy.java
│     │  │  ├─ EmailPasswordAuthStrategy.java
│     │  │  ├─ UserFactory.java
│     │  │  ├─ UserEventPublisher.java
│     │  │  ├─ UserEventObserver.java
│     │  │  ├─ EmailNotificationObserver.java
│     │  │  ├─ RegistrationValidator.java
│     │  │  ├─ BaseRegistrationValidator.java
│     │  │  ├─ ValidatorDecorator.java
│     │  │  ├─ EmailValidationDecorator.java
│     │  │  ├─ PasswordValidationDecorator.java
│     │  │  ├─ PasswordResetToken.java
│     │  │  ├─ PasswordResetTokenRepository.java
│     │  │  ├─ LoginRequest.java
│     │  │  ├─ LoginResponse.java
│     │  │  ├─ RegisterRequest.java
│     │  │  ├─ OAuthCompleteRequest.java
│     │  │  ├─ ForgotPasswordRequest.java
│     │  │  ├─ ResetPasswordRequest.java
│     │  │  └─ ChangePasswordRequest.java
│     │  ├─ booking/           # Booking management
│     │  │  ├─ BookingController.java
│     │  │  ├─ TechnicianBookingController.java
│     │  │  ├─ BookingService.java
│     │  │  ├─ BookingNotificationService.java
│     │  │  ├─ Booking.java
│     │  │  ├─ BookingRepository.java
│     │  │  ├─ BookingStatus.java
│     │  │  ├─ BookingException.java
│     │  │  ├─ BookingAddon.java
│     │  │  ├─ BookingAddonRepository.java
│     │  │  ├─ BookingChecklist.java
│     │  │  ├─ BookingChecklistRepository.java
│     │  │  ├─ ChecklistItem.java
│     │  │  ├─ ChecklistItemRepository.java
│     │  │  ├─ BookingPhoto.java
│     │  │  ├─ BookingPhotoRepository.java
│     │  │  ├─ PhotoType.java
│     │  │  ├─ BookingStatusUpdateRequest.java
│     │  │  └─ RescheduleBookingRequest.java
│     │  ├─ catalog/           # Services, add-ons, technicians
│     │  │  ├─ ServiceController.java
│     │  │  ├─ Service.java
│     │  │  ├─ ServiceRepository.java
│     │  │  ├─ AddOn.java
│     │  │  ├─ AddOnRepository.java
│     │  │  ├─ ServiceAllowedAddon.java
│     │  │  └─ ServiceAllowedAddonRepository.java
│     │  └─ users/             # User management
│     │     ├─ UserController.java
│     │     ├─ AdminController.java
│     │     ├─ User.java
│     │     ├─ UserRepository.java
│     │     ├─ TechnicianSettings.java
│     │     └─ TechnicianSettingsRepository.java
│     ├─ shared/               # Shared infrastructure
│     │  ├─ config/            # Configuration classes
│     │  │  ├─ SecurityConfig.java
│     │  │  ├─ JwtService.java
│     │  │  ├─ JwtAuthenticationFilter.java
│     │  │  ├─ OAuth2LoginSuccessHandler.java
│     │  │  ├─ WebConfig.java
│     │  │  ├─ StaticResourceConfig.java
│     │  │  ├─ DataInitializer.java
│     │  │  ├─ DatabaseSchemaFix.java
│     │  │  └─ AdminConfig.java
│     │  ├─ exception/         # Exception handling
│     │  │  └─ GlobalExceptionHandler.java
│     │  └─ storage/           # File storage
│     │     └─ SupabaseStorageService.java
│     └─ BackendApplication.java
├─ web/
│  ├─ package.json             # Dependencies
│  ├─ vite.config.ts           # Vite configuration
│  ├─ tailwind.config.js       # Tailwind CSS config
│  └─ src/
│     ├─ features/             # Feature modules
│     │  ├─ auth/              # Authentication pages
│     │  │  ├─ Login.tsx
│     │  │  ├─ Register.tsx
│     │  │  ├─ ResetPassword.tsx
│     │  │  ├─ RoleSelection.tsx
│     │  │  └─ AuthCallback.tsx
│     │  ├─ booking/           # Booking pages
│     │  │  ├─ Booking.tsx
│     │  │  └─ BookingDetailsModal.tsx
│     │  └─ dashboard/         # Dashboard pages
│     │     ├─ Dashboard.tsx   # Client dashboard
│     │     ├─ Tdashboard.tsx  # Technician dashboard
│     │     └─ Adashboard.tsx  # Admin dashboard
│     ├─ shared/               # Shared utilities
│     │  └─ api/               # API client configuration
│     │     ├─ axios.ts        # Axios instance with interceptors
│     │     └─ supabaseClient.ts # Supabase client for OAuth
│     ├─ App.tsx               # Main app component with routing
│     ├─ main.tsx              # Entry point
│     └─ index.css             # Global styles (Tailwind)
├─ mobile/
│  └─ app/
│     ├─ build.gradle.kts # App-level Gradle configuration
│     └─ src/
│        ├─ main/
│        │  ├─ java/edu/cit/macansantos/cleanit/
│        │  │  ├─ features/           # Feature modules
│        │  │  │  ├─ auth/            # Authentication
│        │  │  │  │  ├─ LoginActivity.kt
│        │  │  │  │  ├─ RegisterActivity.kt
│        │  │  │  │  ├─ OAuthCompleteActivity.kt
│        │  │  │  │  ├─ ResetPasswordActivity.kt
│        │  │  │  │  └─ AuthModels.kt
│        │  │  │  ├─ booking/         # Booking management
│        │  │  │  │  ├─ BookingsActivity.kt
│        │  │  │  │  ├─ BookingDetailActivity.kt
│        │  │  │  │  ├─ CreateBookingActivity.kt
│        │  │  │  │  ├─ BookingsAdapter.kt
│        │  │  │  │  ├─ PhotosAdapter.kt
│        │  │  │  │  └─ BookingRequests.kt
│        │  │  │  ├─ catalog/         # Services & add-ons
│        │  │  │  │  ├─ ServicesActivity.kt
│        │  │  │  │  ├─ ServicesAdapter.kt
│        │  │  │  │  ├─ AddOnsAdapter.kt
│        │  │  │  │  ├─ TechniciansAdapter.kt
│        │  │  │  │  ├─ Service.kt
│        │  │  │  │  ├─ AddOn.kt
│        │  │  │  │  └─ Technician.kt
│        │  │  │  ├─ dashboard/       # Role-specific dashboards
│        │  │  │  │  ├─ AdminDashboardActivity.kt
│        │  │  │  │  ├─ AdminBookingsActivity.kt
│        │  │  │  │  ├─ TechnicianDashboardActivity.kt
│        │  │  │  │  ├─ AdminBookingModels.kt
│        │  │  │  │  └─ DashboardModels.kt
│        │  │  │  ├─ home/            # Client home & profile
│        │  │  │  │  ├─ HomeActivity.kt
│        │  │  │  │  └─ ProfileActivity.kt
│        │  │  │  └─ users/           # User models
│        │  │  │     └─ UserProfile.kt
│        │  │  ├─ shared/             # Shared utilities
│        │  │  │  ├─ navigation/      # Navigation helpers
│        │  │  │  │  └─ RoleNavigator.kt
│        │  │  │  ├─ network/         # API client
│        │  │  │  │  ├─ ApiConfig.kt  # Dynamic URL configuration
│        │  │  │  │  ├─ ApiService.kt
│        │  │  │  │  └─ RetrofitClient.kt
│        │  │  │  ├─ session/         # Session management
│        │  │  │  │  └─ SessionManager.kt
│        │  │  │  └─ util/            # Utilities
│        │  │  │     └─ ImageUploadHelper.kt
│        │  │  ├─ CleanITApplication.kt  # Application class
│        │  │  └─ MainActivity.kt         # Entry point
│        │  ├─ res/
│        │  │  ├─ layout/             # XML layouts (21 files)
│        │  │  │  ├─ activity_login.xml
│        │  │  │  ├─ activity_register.xml
│        │  │  │  ├─ activity_home.xml
│        │  │  │  ├─ activity_bookings.xml
│        │  │  │  ├─ activity_booking_detail.xml
│        │  │  │  ├─ activity_create_booking.xml
│        │  │  │  ├─ activity_services.xml
│        │  │  │  ├─ activity_admin_dashboard.xml
│        │  │  │  ├─ activity_admin_bookings.xml
│        │  │  │  ├─ activity_technician_dashboard.xml
│        │  │  │  ├─ activity_profile.xml
│        │  │  │  ├─ activity_oauth_complete.xml
│        │  │  │  ├─ activity_reset_password.xml
│        │  │  │  ├─ dialog_reschedule.xml
│        │  │  │  ├─ item_booking.xml
│        │  │  │  ├─ item_service.xml
│        │  │  │  ├─ item_addon.xml
│        │  │  │  ├─ item_technician.xml
│        │  │  │  └─ item_photo.xml
│        │  │  ├─ drawable/           # UI resources (15 files)
│        │  │  │  ├─ ic_google.xml
│        │  │  │  ├─ ic_eye.xml
│        │  │  │  ├─ ic_eye_off.xml
│        │  │  │  ├─ ic_service_placeholder.xml
│        │  │  │  ├─ badge_verified.xml
│        │  │  │  ├─ badge_unverified.xml
│        │  │  │  ├─ input_background.xml
│        │  │  │  ├─ input_background_light.xml
│        │  │  │  ├─ bg_avatar.xml
│        │  │  │  ├─ bg_empty_state.xml
│        │  │  │  ├─ bg_error_message.xml
│        │  │  │  ├─ bg_success_message.xml
│        │  │  │  └─ bg_warning_message.xml
│        │  │  ├─ values/
│        │  │  │  ├─ strings.xml      # App strings & Google Client ID
│        │  │  │  ├─ colors.xml       # Color palette
│        │  │  │  └─ themes.xml       # App themes
│        │  │  ├─ mipmap-*/           # App icons (all densities)
│        │  │  └─ xml/
│        │  │     └─ network_security_config.xml
│        │  └─ AndroidManifest.xml    # App manifest
│        ├─ androidTest/              # Instrumented tests
│        └─ test/                     # Unit tests
├─ docker-compose.yml          # Docker orchestration
├─ DOCKER_SETUP.md            # Docker setup guide
└─ PHYSICAL_DEVICE_SETUP.md   # Physical device guide
```

## Environment Variables

### Web Frontend (`web/.env`)

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your_google_client_id
```

### Mobile (Android)

No environment variables needed. API base URL is dynamically configured in:
- `mobile/app/src/main/java/edu/cit/macansantos/cleanit/shared/network/ApiConfig.kt`
- **Emulator**: Automatically uses `http://10.0.2.2:8080/api/`
- **Physical Device**: Configure your machine's IP address (e.g., `http://192.168.1.5:8080/api/`)

**Google Sign-In Configuration:**
- Web Client ID configured in `mobile/app/src/main/res/values/strings.xml`
- Android OAuth client must be created in Google Cloud Console with SHA-1 fingerprint

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
- PostgreSQL 14+ (or use Docker)
- **Docker Desktop** (recommended for backend)
- Android Studio (for mobile development)
  - Android SDK 34
  - Kotlin plugin
  - Android Emulator or physical device

### Option 1: Docker (Recommended)

**Start Backend with Docker:**
```bash
# From project root
docker-compose up --build
```

Backend will be available at: `http://localhost:8080`

**Advantages:**
- No PostgreSQL installation needed
- Consistent environment
- Easy deployment
- Works on all platforms

**See [DOCKER_SETUP.md](DOCKER_SETUP.md) for complete guide**

### Option 2: Traditional Setup

#### Database Setup

1. Create database:
   ```sql
   CREATE DATABASE cleanit;
   ```

2. Update `backend/.env` with credentials

3. Schema and seed data initialize automatically on first run

#### Start Backend

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

#### Option 1: Android Studio (Recommended)
1. Open `mobile/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Select device/emulator from dropdown
4. Click Run (▶️) button

#### Option 2: Command Line
```bash
cd mobile

# Build APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install in one command
./gradlew clean assembleDebug installDebug
```

#### Network Configuration

**For Android Emulator:**
- API Base URL: `http://10.0.2.2:8080/api/`
- **Automatically configured** - no changes needed
- `ApiConfig.kt` auto-detects emulator

**For Physical Device:**

**Quick Setup (5 minutes):**
1. Find your machine's IP address:
   - Windows: `ipconfig` (look for IPv4)
   - Mac/Linux: `ifconfig` or `ip addr`
   - Example: `192.168.1.5`

2. Update `ApiConfig.kt`:
   ```kotlin
   private const val LOCALHOST_URL = "http://YOUR_IP:8080/api/"
   // Example: "http://192.168.1.5:8080/api/"
   ```

3. Ensure device and computer are on the same WiFi network

4. Rebuild the app:
   ```bash
   cd mobile
   ./gradlew clean assembleDebug installDebug
   ```

**See [PHYSICAL_DEVICE_SETUP.md](PHYSICAL_DEVICE_SETUP.md) for detailed guide**

#### Google Sign-In Setup (Mobile)

**Important:** Google Sign-In requires Android OAuth client configuration.

1. Get SHA-1 fingerprint:
   ```bash
   cd mobile
   ./gradlew signingReport
   ```
   Copy the SHA-1 from **debug keystore**

2. Add to Google Cloud Console:
   - Go to: https://console.cloud.google.com/
   - Navigate to: APIs & Services > Credentials
   - Click: **+ CREATE CREDENTIALS** > **OAuth client ID**
   - Select: **Android**
   - Fill in:
     - **Package name**: `edu.cit.macansantos.cleanit`
     - **SHA-1**: Your fingerprint from step 1
   - Click: **Create**

3. Wait 10 minutes for Google propagation

4. Rebuild and test:
   ```bash
   cd mobile
   ./gradlew clean assembleDebug installDebug
   ```

**Features:**
- ✅ Account picker appears every time (can switch accounts)
- ✅ Works with existing web accounts
- ✅ Seamless OAuth flow

**Note:** For physical device, also update the API base URL in `ApiConfig.kt` to your machine's IP address.

## API Endpoints

Base URL: `http://localhost:8080/api` (web proxy + mobile Retrofit)

### Authentication
| Endpoint | Web | Mobile | Notes |
|----------|-----|--------|-------|
| `POST /v1/auth/login` | Yes | Yes | Email/password → JWT |
| `POST /v1/auth/register` | Yes | Yes | Includes `imageUrl` after upload |
| `POST /v1/auth/upload-image` | Yes | Yes | Multipart ID document |
| `POST /v1/auth/oauth-check` | Yes | Yes | Google / OAuth existing user |
| `POST /v1/auth/oauth-complete` | Yes | Yes | New Google user profile completion |
| `POST /v1/auth/google` | — | Yes | Legacy mobile fallback |
| `POST /v1/auth/forgot-password` | Yes | Yes | Returns `resetToken` when account exists (dev flow) |
| `POST /v1/auth/reset-password` | Yes | Yes | Token + new password |
| `POST /v1/user/change-password` | Yes | Yes | Authenticated password change |
| Browser `GET /oauth2/authorization/google` | Yes | — | Web Supabase + Spring OAuth redirect |

### User & catalog
| Endpoint | Web | Mobile |
|----------|-----|--------|
| `GET /v1/user/profile/{email}` | Yes | Yes |
| `GET /v1/user/technicians/verified` | Yes | Yes |
| `GET /v1/services` | Yes | Yes |
| `GET /v1/services/{id}` | — | Yes |
| `GET /v1/services/{id}/addons` | Yes | Yes |

### Client bookings
| Endpoint | Web | Mobile |
|----------|-----|--------|
| `GET /v1/bookings/client/{clientId}` | Yes | Yes |
| `GET /v1/bookings/{id}` | — | Yes | Booking detail screen |
| `POST /v1/bookings/create` | Yes | Yes |
| `POST /v1/bookings/{id}/cancel` | Yes | Yes |
| `POST /v1/bookings/{id}/reschedule` | Yes | Yes |

### Technician
| Endpoint | Web | Mobile |
|----------|-----|--------|
| `GET /v1/technician/bookings/pending` | Yes | Yes |
| `GET /v1/technician/{id}/bookings` | Yes | Yes |
| `GET /v1/technician/{id}/availability` | Yes | Yes |
| `POST /v1/technician/{id}/availability` | Yes | Yes |
| `GET /v1/technician/{id}/statistics` | — | Yes |
| `POST /v1/technician/bookings/{id}/accept` | Yes | Yes |
| `POST /v1/technician/bookings/{id}/status` | Yes | Yes |
| `GET/POST .../checklist` | Yes | Yes |
| `GET/POST .../photos` | Yes | Yes |
| `GET .../validate-checklist` | Yes | Yes |
| `GET .../validate-photos` | Yes | Yes |

### Admin
| Endpoint | Web | Mobile |
|----------|-----|--------|
| `GET /v1/admin/dashboard/statistics` | Yes | Yes |
| `GET /v1/admin/dashboard/recent-bookings` | Yes | Yes |
| `GET /v1/admin/pending-verifications` | Yes | Yes |
| `POST /v1/admin/verify-user/{id}` | Yes | Yes |
| `GET /v1/admin/bookings/by-status` | Yes | Yes |
| `POST /v1/admin/bookings/{id}/void` | Yes | Yes |

## Web ↔ Mobile feature parity (README features)

| README feature | Web | Mobile |
|----------------|-----|--------|
| Browse services + add-ons + book | Yes | Yes |
| Booking history & status | Yes | Yes |
| Cancel / reschedule | Yes | Yes |
| Before/after photos (client view) | Yes | Yes |
| Technician accept / status / checklist / photos | Yes | Yes |
| Availability toggle | Yes | Yes |
| Admin statistics | Yes | Yes |
| Admin booking drill-down + void | Yes | Yes (search; web adds sort) |
| User verification | Yes | Yes |
| Email/password auth | Yes | Yes |
| Google sign-in + new user onboarding | Yes (Supabase) | Yes (`oauth-check` / `oauth-complete`) |
| Registration with ID image | Yes | Yes |
| Password reset (forgot + reset) | Yes | Yes |
| Change password (logged in) | Yes | Yes |

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
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")
implementation("androidx.constraintlayout:constraintlayout:2.1.4")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Async & Lifecycle
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

// Image Loading
implementation("io.coil-kt:coil:2.5.0")

// Google Sign-In
implementation("com.google.android.gms:play-services-auth:20.7.0")

// UI Components
implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
implementation("androidx.recyclerview:recyclerview:1.3.2")
implementation("androidx.cardview:cardview:1.0.0")

// Testing
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
```

**Build Configuration:**
- Kotlin: 1.9.0
- Gradle: 8.4
- Min SDK: 24 (Android 7.0 Nougat)
- Target SDK: 34 (Android 14)
- Compile SDK: 34

## Deployment

### Backend Options
- **Docker** (recommended) - See [DOCKER_SETUP.md](DOCKER_SETUP.md)
- Heroku (Java buildpack + PostgreSQL)
- AWS Elastic Beanstalk
- Railway
- Render
- DigitalOcean App Platform

### Web Frontend Options
- Vercel
- Netlify
- AWS S3 + CloudFront
- GitHub Pages

### Mobile (Android) Options
- Google Play Store (production)
- Firebase App Distribution (beta testing)
- APK direct distribution (internal testing)

### Database Options
- Supabase (currently used)
- AWS RDS
- Heroku Postgres
- Railway
- DigitalOcean Managed Databases

## Troubleshooting

### Backend won't start
- Check PostgreSQL is running (or use Docker: `docker-compose up`)
- Verify database credentials in `.env`
- Ensure Java 17 is installed: `java -version`
- Check port 8080 is not in use: `netstat -ano | findstr :8080` (Windows)

### Web Frontend won't start
- Clear node_modules: `rm -rf node_modules && npm install`
- Check Node.js version: `node -v` (should be 18+)
- Verify `.env` file exists with correct API URL

### Mobile app won't build
- Sync Gradle files in Android Studio
- Check Android SDK is installed (SDK 34)
- Verify Java 17 is configured in Android Studio
- Clean and rebuild: `./gradlew clean assembleDebug`
- Check internet connection (Gradle downloads dependencies)

### Mobile app can't connect to backend
- **Emulator**: Should use `http://10.0.2.2:8080/api/` (auto-configured)
- **Physical device**: 
  - Update `ApiConfig.kt` with your machine's IP
  - Ensure device and computer on same WiFi
  - Check firewall settings (allow port 8080)
  - Test in browser: `http://YOUR_IP:8080/api/v1/auth/login`
- Ensure backend is running: `docker-compose ps` or check `localhost:8080`

### Google Sign-In not working (Mobile)
- Verify Android OAuth client created in Google Cloud Console
- Check SHA-1 fingerprint matches your debug keystore
- Wait 10 minutes after creating OAuth client (propagation time)
- Verify package name: `edu.cit.macansantos.cleanit`
- Check `strings.xml` has correct Web Client ID
- Rebuild app after configuration changes

### Docker issues
- Ensure Docker Desktop is running
- Check port 8080 is available
- Verify `.env` file exists in `backend/` directory
- View logs: `docker-compose logs -f backend`
- Rebuild: `docker-compose up --build`
- See [DOCKER_SETUP.md](DOCKER_SETUP.md) for detailed troubleshooting

### Database connection errors
- Verify PostgreSQL service is running
- Check database exists: `psql -l`
- Verify credentials in `backend/.env`
- Check database URL format in `application.properties`

---

## 📊 Project Status

**Overall Completion**: ~95%

### Backend
- ✅ 100% - All features implemented
- ✅ JWT authentication
- ✅ Google OAuth
- ✅ Role-based access control
- ✅ Supabase integration
- ✅ Docker support

### Web Frontend
- ✅ 100% - All features implemented
- ✅ React + TypeScript
- ✅ Tailwind CSS
- ✅ Role-specific dashboards
- ✅ Google OAuth (Supabase)

### Mobile (Android)
- ✅ 95% - Core features complete
- ✅ All backend features implemented
- ✅ Google Sign-In with account picker
- ✅ Physical device support
- ✅ Dynamic API configuration
- ⏳ UI enhancements planned (modern design)

### Recent Updates
- ✅ Google Account Picker: Always shows account selection
- ✅ Physical Device Support: Dynamic IP configuration
- ✅ Docker Support: Backend containerization
- ✅ Booking Details: Enhanced display with photos
- ✅ ScrollView Fix: Resolved crash issues

---

## Platform parity status

**Web and mobile implement the same backend capabilities** for client, technician, and admin roles (per feature list above). Users can register on one platform and sign in on the other with email/password or Google.

**Shared gaps (both platforms):** email delivery for reset tokens (tokens returned in API / server logs for local testing).

**Web-only:** Supabase browser OAuth redirect (`/oauth2/authorization/google`).

**Mobile-only extras:** technician statistics card, dedicated booking detail API call (same data as web list view).

---

**Last Updated**: May 2026
