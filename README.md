# CleanIT (IT342)

CleanIT is a full-stack web application for managing cleaning service bookings with role-based access for clients, technicians, and administrators.

This repository contains:

- `backend/`: Spring Boot REST API with JWT authentication
- `web/`: React (Vite) frontend with TypeScript

## Features

### Authentication & Authorization

- User registration with email/password and OAuth2 (Google)
- JWT-based authentication with secure token management
- Role-based access control (Client, Technician, Admin)
- Protected routes with server-side security

### Client Features

- Browse available cleaning services
- Create and manage bookings with add-ons
- View booking history and status
- Reschedule or cancel bookings
- Upload photos for service requests

### Technician Features

- View assigned bookings
- Update booking status (In Progress, Completed)
- Upload before/after photos
- Manage service checklists
- Configure availability settings

### Admin Features

- Manage users, services, and bookings
- View system-wide analytics
- Configure service offerings and add-ons
- Monitor booking workflow

## Tech Stack

### Frontend

- React 18 with TypeScript
- Tailwind CSS for styling
- Axios for API communication
- React Router for navigation
- Lucide React for icons

### Backend

- Java 17
- Spring Boot 3.x (Web, Security, Data JPA, Validation, OAuth2)
- PostgreSQL database
- JWT authentication (JJWT library)
- Maven for dependency management

### Design Patterns

- Factory Pattern for user creation
- Decorator Pattern for validation
- Observer Pattern for notifications
- Strategy Pattern for authentication methods

## Project Structure

```
.
├─ backend/
│  ├─ src/main/java/com/G4/backend/
│  │  ├─ config/          # Security, JWT, OAuth2 configuration
│  │  ├─ controller/      # REST API endpoints
│  │  ├─ dto/             # Data transfer objects
│  │  ├─ entity/          # JPA entities
│  │  ├─ enums/           # Enumerations (BookingStatus, PhotoType, etc.)
│  │  ├─ exception/       # Custom exceptions and handlers
│  │  ├─ repository/      # Data access layer
│  │  └─ service/         # Business logic
│  │     ├─ decorator/    # Validation decorators
│  │     ├─ factory/      # User factory
│  │     ├─ observer/     # Event observers
│  │     └─ strategy/     # Authentication strategies
│  └─ src/main/resources/
│     └─ application.properties
└─ web/
   └─ src/
      ├─ components/      # Reusable UI components
      ├─ pages/           # Page components
      │  ├─ login/
      │  ├─ register/
      │  └─ dashboard/
      ├─ services/        # API service layer
      └─ types/           # TypeScript type definitions
```

## Environment Variables

### Frontend (Vite)

Create `web/.env` (gitignored). Template provided at `web/.env.example`.

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_CLIENT_ID=your_google_client_id
```

Note: Vite exposes environment variables to the browser, so do not store server secrets here.

### Backend

Create `backend/.env` (gitignored). Template provided at `backend/.env.example`.

```env
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/cleanit
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400000

# OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Admin Account
ADMIN_EMAIL=admin@cleanit.com
ADMIN_PASSWORD=your_admin_password
```

## Running the Project Locally

### Prerequisites

- Node.js 18+ (LTS recommended)
- Java 17
- Maven 3.6+
- PostgreSQL 14+

### Database Setup

1. Create a PostgreSQL database named `cleanit`
2. Update `backend/.env` with your database credentials
3. The application will automatically initialize the schema on first run

### 1) Start the Backend

From the `backend/` folder:

```bash
mvn spring-boot:run
```

Backend runs at: `http://localhost:8080`

Key API endpoints:

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `GET /api/auth/oauth2/google` - Google OAuth2 login
- `GET /api/services` - List available services
- `POST /api/bookings` - Create booking
- `GET /api/bookings` - Get user bookings

### 2) Start the Frontend

From the `web/` folder:

```bash
npm install
npm run dev
```

Frontend runs at: `http://localhost:5174`

Default routes:

- `/` - Redirects to login
- `/login` - Login page
- `/register` - Registration page
- `/dashboard` - Client dashboard
- `/technician` - Technician dashboard
- `/admin` - Admin dashboard

## API Documentation

The backend exposes RESTful endpoints organized by domain:

- `/api/auth/*` - Authentication and authorization
- `/api/services/*` - Service management
- `/api/bookings/*` - Booking operations
- `/api/users/*` - User management
- `/api/admin/*` - Admin operations
- `/api/technician/*` - Technician operations

## Testing

### Backend Tests

```bash
cd backend
mvn test
```

### Frontend Tests

```bash
cd web
npm test
```

## Deployment

The application can be deployed to various platforms:

- Backend: Heroku, AWS Elastic Beanstalk, or any Java hosting service
- Frontend: Vercel, Netlify, or any static hosting service
- Database: AWS RDS, Heroku Postgres, or managed PostgreSQL service

## Contributing

1. Create a feature branch from `main`
2. Make your changes
3. Test thoroughly
4. Submit a pull request

## License

This project is developed for IT342 coursework.

## Notes

- JWT tokens are stored in localStorage and included in API requests via Authorization header
- File uploads are stored in `backend/uploads/` directory
- The application uses role-based access control with three roles: CLIENT, TECHNICIAN, ADMIN
- OAuth2 integration supports Google authentication
- Design patterns are implemented throughout the codebase for maintainability