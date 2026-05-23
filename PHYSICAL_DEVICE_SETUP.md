# 📱 Physical Device Setup Guide

## 🎯 Quick Setup (5 Minutes)

### Step 1: Start Backend (1 minute)
```bash
docker-compose up
```

Wait for: `Started BackendApplication`

### Step 2: Find Your IP (1 minute)

**Windows**:
```cmd
ipconfig
```
Look for: `IPv4 Address . . . : 192.168.1.XXX`

**Mac/Linux**:
```bash
ifconfig | grep "inet "
```

**Write it down**: `192.168.1.___`

### Step 3: Configure Mobile App (2 minutes)

**Option A: Quick Edit (Temporary)**

Edit `ApiConfig.kt`:
```kotlin
private const val LOCALHOST_URL = "http://YOUR_IP:8080/api/"
```

**Option B: Runtime Configuration (Better)**

Add to `LoginActivity.kt` in `onCreate()`:
```kotlin
// Check if physical device and no URL configured
if (!ApiConfig.isEmulator() && !ApiConfig.hasCustomUrl(this)) {
    showApiConfigDialog()
}

private fun showApiConfigDialog() {
    val builder = AlertDialog.Builder(this)
    val input = EditText(this)
    input.hint = "http://192.168.1.100:8080/api/"
    input.inputType = InputType.TYPE_TEXT_VARIATION_URI
    
    builder.setTitle("Configure API URL")
        .setMessage("Enter your computer's IP address:\n\nFind it by running 'ipconfig' on Windows or 'ifconfig' on Mac/Linux")
        .setView(input)
        .setPositiveButton("Save") { _, _ ->
            val url = input.text.toString().trim()
            if (url.isNotBlank()) {
                ApiConfig.setCustomUrl(this, url)
                RetrofitClient.recreate()
                Toast.makeText(this, "API URL saved", Toast.LENGTH_SHORT).show()
            }
        }
        .setCancelable(false)
        .show()
}
```

### Step 4: Test (1 minute)

1. Rebuild app: `./gradlew installDebug`
2. Open app on phone
3. Try to sign in
4. ✅ Should work!

---

## 🔍 Troubleshooting

### Can't Connect?

**1. Check WiFi**
- Phone and computer on SAME WiFi? ✅
- Not using mobile data? ✅

**2. Test from Browser**
Open Chrome on phone:
```
http://YOUR_IP:8080/api/v1/auth/login
```

**Works?** → Issue is in app configuration
**Doesn't work?** → Issue is network/firewall

**3. Check Firewall**
- Windows: Allow Docker through firewall
- Mac: System Preferences → Firewall → Allow Docker

**4. Verify Backend Running**
```bash
docker-compose ps
```
Should show: `Up`

**5. Check Logs**
```bash
docker-compose logs -f backend
```
Look for errors

---

## 📊 Network Diagram

```
Your Computer (192.168.1.100)
    ↓
Docker Container (Backend)
    ↓
Port 8080 exposed
    ↓
Accessible from same WiFi
    ↓
Your Phone (192.168.1.XXX)
```

---

## ✅ Success Checklist

- [ ] Backend running (`docker-compose up`)
- [ ] Found IP address
- [ ] Phone on same WiFi as computer
- [ ] Mobile app configured with IP
- [ ] Can access `http://YOUR_IP:8080` from phone browser
- [ ] Mobile app connects successfully

---

## 🎯 Quick Commands

**Start backend**:
```bash
docker-compose up
```

**Find IP (Windows)**:
```cmd
ipconfig | findstr IPv4
```

**Find IP (Mac/Linux)**:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**Test from phone browser**:
```
http://YOUR_IP:8080/api/v1/auth/login
```

**Rebuild mobile app**:
```bash
cd mobile && ./gradlew installDebug
```

---

**That's it! Your physical device should now connect to the backend! 🎉**
