# CleanIT (IT342) 

CleanIT is a full-stack web application for managing cleaning services and client bookings.

This repository contains:

- `backend/`: Spring Boot REST API
- `web/`: React (Vite) frontend

## MVP (Minimum Viable Product)

### Authentication

- Register a user
- Login a user
- Basic protected routes (server-side security)

### Client dashboard (UI)

- Login screen
- Registration screen
- Dashboard screen (basic layout for bookings/services/history)

## Tech Stack

### Frontend

- React 18
- TypeScript
- Tailwind CSS
- Axios
- React Router

### Backend

- Java 17
- Spring Boot (Web, Security, Data JPA, Validation)
- PostgreSQL driver (runtime)
- JJWT (JSON Web Token library)

## Project Structure

```
.
├─ backend/
│  ├─ src/main/java/com/G4/backend
│  └─ src/main/resources
└─ web/
   └─ src/
      └─ pages/
         ├─ login/
         ├─ register/
         └─ dashboard/
```

## Environment Variables

### Frontend (Vite)

Vite exposes environment variables to the browser, so **do not store server secrets** here.

- Create `web/.env` (this file is gitignored)
- Reference variables using `import.meta.env.*`

Template is provided at: `web/.env.example`

Example:

```env
VITE_SUPABASE_URL=
VITE_SUPABASE_ANON_KEY=
```

### Backend

- Create `backend/.env` (this file is gitignored)

Template is provided at: `backend/.env.example`

Example:

```env
SUPABASE_URL=
SUPABASE_ANON_KEY=
SUPABASE_EMAIL=
SUPABASE_PASSWORD=
```

## Running the Project Locally

### Prerequisites

- Node.js (LTS recommended)
- Java 17
- Maven

### 1) Start the Backend

From the `backend/` folder:

```bash
mvn spring-boot:run
```

Default backend base URL:

- `http://localhost:8080`

Auth endpoints:

- `POST http://localhost:8080/api/auth/register`
- `POST http://localhost:8080/api/auth/login`

### 2) Start the Frontend

From the `web/` folder:

```bash
npm install
npm run dev
```

Frontend URL (Vite default):

- `http://localhost:5173`

The root URL `/` redirects to `/login`.

## Notes

- `package-lock.json` is expected and should be committed. It locks dependency versions for consistent installs.
- The backend currently includes basic authentication endpoints and security scaffolding. You can extend this with JWT issuance and request filtering when ready.