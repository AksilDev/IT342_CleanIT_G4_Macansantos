# 🐳 Docker Setup Guide - CleanIT Backend

## 📋 Prerequisites

### 1. Install Docker Desktop

**Windows**:
1. Download: https://docs.docker.com/desktop/install/windows-install/
2. Install Docker Desktop
3. Restart computer if prompted
4. Verify installation:
   ```cmd
   docker --version
   docker-compose --version
   ```

**Mac**:
1. Download: https://docs.docker.com/desktop/install/mac-install/
2. Install Docker Desktop
3. Verify installation:
   ```bash
   docker --version
   docker-compose --version
   ```

**Linux**:
1. Follow: https://docs.docker.com/desktop/install/linux-install/
2. Verify installation:
   ```bash
   docker --version
   docker-compose --version
   ```

---

## 🚀 Quick Start

### Step 1: Start Backend with Docker

From the project root directory:

```bash
# Build and start the backend
docker-compose up --build
```

**First time**: This will take 3-5 minutes (downloads dependencies and builds)

**Subsequent runs**: Takes ~30 seconds

**Expected output**:
```
cleanit-backend | Started BackendApplication in 15.234 seconds
cleanit-backend | Tomcat started on port(s): 8080 (http)
```

### Step 2: Verify Backend is Running

Open browser and go to:
```
http://localhost:8080/api/v1/auth/login
```

You should see a response (even if it's an error page, it means backend is running).

---

## 📱 Configure Mobile App for Physical Device

### Step 1: Find Your Local IP Address

**Windows**:
```cmd
ipconfig
```
Look for **"IPv4 Address"** under your WiFi adapter.

Example output:
```
Wireless LAN adapter Wi-Fi:
   IPv4 Address. . . . . . . . . . . : 192.168.1.100
```

**Mac**:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**Linux**:
```bash
ip addr show | grep "inet " | grep -v 127.0.0.1
```

**Your IP**: Write it down (e.g., `192.168.1.100`)

### Step 2: Update Mobile App Configuration

**Option A: Hardcode in RetrofitClient (Quick Test)**

Edit `mobile/app/src/main/java/edu/cit/macansantos/cleanit/shared/network/ApiConfig.kt`:

```kotlin
// Change this line:
private const val LOCALHOST_URL = "http://localhost:8080/api/"

// To your IP:
private const val LOCALHOST_URL = "http://192.168.1.100:8080/api/"
```

**Option B: Use SharedPreferences (Recommended)**

Add this code to your `LoginActivity.onCreate()` or create a settings screen:

```kotlin
// Set custom API URL for physical device
if (!ApiConfig.isEmulator() && !ApiConfig.hasCustomUrl(this)) {
    // Prompt user to enter their IP
    val builder = AlertDialog.Builder(this)
    val input = EditText(this)
    input.hint = "e.g., http://192.168.1.100:8080/api/"
    
    builder.setTitle("Configure API URL")
        .setMessage("Enter your computer's IP address:")
        .setView(input)
        .setPositiveButton("Save") { _, _ ->
            val url = input.text.toString()
            ApiConfig.setCustomUrl(this, url)
            RetrofitClient.recreate()
        }
        .show()
}
```

### Step 3: Test Connection

1. **Ensure phone and computer are on the same WiFi network**
2. **Rebuild and install mobile app**:
   ```bash
   cd mobile
   ./gradlew clean assembleDebug installDebug
   ```
3. **Open app on physical device**
4. **Try to sign in**

If it works: ✅ You're done!

If it doesn't work: See troubleshooting below.

---

## 🔧 Docker Commands

### Start Backend
```bash
docker-compose up
```

### Start Backend (rebuild)
```bash
docker-compose up --build
```

### Start Backend (detached mode - runs in background)
```bash
docker-compose up -d
```

### Stop Backend
```bash
docker-compose down
```

### View Logs
```bash
docker-compose logs -f backend
```

### Restart Backend
```bash
docker-compose restart backend
```

### Check Status
```bash
docker-compose ps
```

### Remove Everything (including volumes)
```bash
docker-compose down -v
```

---

## 🐛 Troubleshooting

### Issue 1: Port 8080 Already in Use

**Error**:
```
Error starting userland proxy: listen tcp4 0.0.0.0:8080: bind: address already in use
```

**Solution**:

**Windows**:
```cmd
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

**Mac/Linux**:
```bash
# Find what's using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

Or change the port in `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Use 8081 instead
```

### Issue 2: Mobile App Can't Connect

**Symptoms**:
- Connection timeout
- "Unable to resolve host"
- "Failed to connect"

**Checklist**:
- [ ] Backend is running: `docker-compose ps` shows "Up"
- [ ] Phone and computer on same WiFi
- [ ] Correct IP address in mobile app
- [ ] Firewall not blocking port 8080
- [ ] URL ends with `/` (e.g., `http://192.168.1.100:8080/api/`)

**Test from phone browser**:
Open Chrome on your phone and go to:
```
http://YOUR_IP:8080/api/v1/auth/login
```

If this works, the issue is in your mobile app configuration.

If this doesn't work, the issue is network/firewall.

### Issue 3: Firewall Blocking Connection

**Windows**:
1. Open Windows Defender Firewall
2. Click "Allow an app through firewall"
3. Find "Docker Desktop" and ensure both Private and Public are checked
4. Or temporarily disable firewall to test

**Mac**:
1. System Preferences → Security & Privacy → Firewall
2. Click "Firewall Options"
3. Ensure Docker is allowed

### Issue 4: Backend Won't Start

**Check logs**:
```bash
docker-compose logs backend
```

**Common issues**:
- Missing `.env` file in `backend/` directory
- Invalid environment variables
- Database connection issues

**Solution**:
1. Verify `backend/.env` exists and has all required variables
2. Check logs for specific error
3. Try rebuilding: `docker-compose up --build`

### Issue 5: "Cannot connect to Docker daemon"

**Error**:
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Solution**:
- Ensure Docker Desktop is running
- Restart Docker Desktop
- On Linux, ensure your user is in the `docker` group:
  ```bash
  sudo usermod -aG docker $USER
  ```
  Then log out and log back in.

---

## 📊 Network Configuration

### Emulator vs Physical Device

| Device Type | API URL | Auto-Detected |
|-------------|---------|---------------|
| Android Emulator | `http://10.0.2.2:8080/api/` | ✅ Yes |
| Physical Device (Same WiFi) | `http://192.168.1.X:8080/api/` | ⚠️ Needs configuration |
| Physical Device (Different WiFi) | ❌ Won't work | Use cloud deployment |

### How It Works

```
┌─────────────────────────────────────────────────────────┐
│              YOUR COMPUTER (192.168.1.100)               │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  🐳 Docker Container                                    │
│     ├─ Backend (Spring Boot)                            │
│     ├─ Port: 8080                                       │
│     └─ Accessible: 0.0.0.0:8080                         │
│                                                          │
└─────────────────────────────────────────────────────────┘
         ↑                              ↑
         │                              │
    ✅ Works                        ✅ Works
         │                              │
┌────────┴────────┐          ┌─────────┴────────┐
│  📱 Emulator    │          │  📱 Physical     │
│                 │          │  Device          │
│  10.0.2.2:8080  │          │  192.168.1.100   │
│  (auto-mapped   │          │  :8080           │
│   to host)      │          │  (same WiFi)     │
└─────────────────┘          └──────────────────┘
```

---

## 🎯 Production Deployment

For production, you'll want to:

1. **Use a cloud provider** (AWS, Google Cloud, Azure, DigitalOcean)
2. **Get a domain name** (e.g., `api.cleanit.com`)
3. **Set up HTTPS** (Let's Encrypt)
4. **Use environment-specific configs**

**Quick production setup**:

1. Push Docker image to registry:
   ```bash
   docker build -t cleanit-backend:latest ./backend
   docker tag cleanit-backend:latest your-registry/cleanit-backend:latest
   docker push your-registry/cleanit-backend:latest
   ```

2. Deploy to cloud (example: DigitalOcean):
   - Create a Droplet
   - Install Docker
   - Pull and run your image
   - Configure domain and SSL

3. Update mobile app:
   ```kotlin
   private const val PRODUCTION_URL = "https://api.cleanit.com/api/"
   ```

---

## ✅ Success Checklist

After setup, verify:

- [ ] Docker Desktop is installed and running
- [ ] `docker-compose up` starts backend successfully
- [ ] `http://localhost:8080` is accessible from browser
- [ ] Found your local IP address
- [ ] Mobile app configured with correct IP
- [ ] Phone and computer on same WiFi
- [ ] Mobile app can connect to backend
- [ ] Can sign in from physical device
- [ ] Can create bookings from physical device
- [ ] All features work on physical device

---

## 📖 Additional Resources

- **Docker Documentation**: https://docs.docker.com/
- **Docker Compose**: https://docs.docker.com/compose/
- **Spring Boot with Docker**: https://spring.io/guides/gs/spring-boot-docker/
- **Troubleshooting**: See `.kiro/DOCKER_DEPLOYMENT_PLAN.md`

---

## 🆘 Still Having Issues?

1. **Check logs**: `docker-compose logs -f backend`
2. **Verify network**: `docker network ls`
3. **Test from browser**: `http://YOUR_IP:8080/api/v1/auth/login`
4. **Check firewall**: Temporarily disable to test
5. **Restart everything**:
   ```bash
   docker-compose down
   docker-compose up --build
   ```

---

**Need help? Check the detailed troubleshooting guide in `.kiro/DOCKER_DEPLOYMENT_PLAN.md`**

**Happy coding! 🚀**
