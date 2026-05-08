# 🔐 Mobile App Google OAuth Setup Guide

This guide will help you set up Google OAuth authentication for the CleanIT Android mobile app.

## 📋 Prerequisites

- Google Cloud Console account
- Android Studio installed
- Backend server running
- SHA-1 certificate fingerprint for your app

---

## 🚀 Step 1: Get SHA-1 Certificate Fingerprint

### For Debug Build:

```bash
cd mobile
./gradlew signingReport
```

Look for the **SHA-1** fingerprint under `Variant: debug` → `Config: debug`.

Example output:
```
SHA-1: A1:B2:C3:D4:E5:F6:G7:H8:I9:J0:K1:L2:M3:N4:O5:P6:Q7:R8:S9:T0
```

### For Release Build:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

---

## 🌐 Step 2: Configure Google Cloud Console

### 1. Create/Select Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Name it: `CleanIT Mobile`

### 2. Enable Google Sign-In API

1. Navigate to **APIs & Services** → **Library**
2. Search for "Google Sign-In API"
3. Click **Enable**

### 3. Configure OAuth Consent Screen

1. Go to **APIs & Services** → **OAuth consent screen**
2. Select **External** user type
3. Fill in required fields:
   - **App name**: CleanIT
   - **User support email**: your-email@example.com
   - **Developer contact**: your-email@example.com
4. Add scopes:
   - `email`
   - `profile`
   - `openid`
5. Click **Save and Continue**

### 4. Create OAuth 2.0 Credentials

#### A. Create Web Client ID (for backend)

1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth 2.0 Client ID**
3. Select **Web application**
4. Name: `CleanIT Web Client`
5. Add **Authorized redirect URIs**:
   ```
   http://localhost:8080/login/oauth2/code/google
   http://localhost:5173/auth/callback
   ```
6. Click **Create**
7. **Copy the Client ID** - you'll need this for mobile app

#### B. Create Android Client ID

1. Click **Create Credentials** → **OAuth 2.0 Client ID**
2. Select **Android**
3. Name: `CleanIT Android`
4. **Package name**: `edu.cit.macansantos.cleanit`
5. **SHA-1 certificate fingerprint**: Paste the SHA-1 from Step 1
6. Click **Create**

---

## 📱 Step 3: Configure Mobile App

### 1. Update `strings.xml`

Open `mobile/app/src/main/res/values/strings.xml` and replace the placeholder:

```xml
<resources>
    <string name="app_name">CleanIT</string>
    <!-- Replace with your Web Client ID from Step 2.4.A -->
    <string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com</string>
</resources>
```

**⚠️ IMPORTANT**: Use the **Web Client ID**, NOT the Android Client ID!

### 2. Sync Gradle

In Android Studio:
1. Click **File** → **Sync Project with Gradle Files**
2. Wait for sync to complete

---

## 🖥️ Step 4: Configure Backend

### 1. Update `backend/.env`

Add your Google OAuth credentials:

```env
# Google OAuth2
GOOGLE_CLIENT_ID=your_web_client_id_here.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_client_secret_here
```

### 2. Restart Backend Server

```bash
cd backend
./mvnw spring-boot:run
```

---

## 🧪 Step 5: Test Google Sign-In

### 1. Run Mobile App

1. Open Android Studio
2. Select emulator or physical device
3. Click **Run** (green play button)

### 2. Test Login Flow

1. Open the app
2. Click **"Sign in with Google"** button
3. Select your Google account
4. Grant permissions
5. You should be redirected to the home screen

### 3. Verify Backend

Check backend logs for:
```
Google OAuth authentication successful for: user@example.com
```

---

## 🐛 Troubleshooting

### Issue: "Developer Error" or "Error 10"

**Cause**: SHA-1 fingerprint mismatch

**Solution**:
1. Verify SHA-1 in Google Cloud Console matches your app
2. Make sure you're using the correct keystore (debug vs release)
3. Wait 5-10 minutes after updating SHA-1 in Google Cloud Console

### Issue: "Sign in failed: 12500"

**Cause**: Missing or incorrect Web Client ID

**Solution**:
1. Verify `default_web_client_id` in `strings.xml` is the **Web Client ID**
2. Make sure it ends with `.apps.googleusercontent.com`
3. Sync Gradle files

### Issue: "Connection error" after Google sign-in

**Cause**: Backend not configured or not running

**Solution**:
1. Verify backend is running on `http://localhost:8080`
2. Check `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in backend `.env`
3. Restart backend server

### Issue: "Google authentication not fully implemented"

**Cause**: Backend Google token verification not implemented

**Solution**:
This is expected. The backend needs Google token verification library. For now:
1. Use email/password login
2. Or implement full Google token verification (see Step 6)

---

## 🔧 Step 6: Implement Full Google Token Verification (Optional)

To fully implement Google OAuth, add Google's token verification to the backend:

### 1. Add Dependency to `backend/pom.xml`

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 2. Update `AuthService.java`

Replace the `authenticateWithGoogle` method:

```java
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public LoginResponse authenticateWithGoogle(String idToken) {
    try {
        // Verify token with Google
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), 
                GsonFactory.getDefaultInstance())
            .setAudience(Collections.singletonList(googleClientId))
            .build();

        GoogleIdToken googleIdToken = verifier.verify(idToken);
        if (googleIdToken == null) {
            throw new RuntimeException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // Find or create user
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name);
                newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                newUser.setRole("client"); // Default role
                return userRepository.save(newUser);
            });

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse.Builder()
            .id(user.getId().toString())
            .name(user.getName())
            .email(user.getEmail())
            .role(user.getRole())
            .contactNo(user.getContactNo())
            .token(token)
            .message("Google sign-in successful")
            .build();
    } catch (Exception e) {
        throw new RuntimeException("Google authentication failed: " + e.getMessage());
    }
}
```

---

## 📝 Summary

### What You Need:

1. ✅ **Google Cloud Console**:
   - Web Client ID (for mobile app)
   - Android Client ID (for Google Play Services)
   - Client Secret (for backend)

2. ✅ **Mobile App** (`strings.xml`):
   - `default_web_client_id` = Web Client ID

3. ✅ **Backend** (`.env`):
   - `GOOGLE_CLIENT_ID` = Web Client ID
   - `GOOGLE_CLIENT_SECRET` = Client Secret

4. ✅ **SHA-1 Fingerprint**:
   - Added to Android Client ID in Google Cloud Console

### Testing Checklist:

- [ ] SHA-1 fingerprint added to Google Cloud Console
- [ ] Web Client ID added to `strings.xml`
- [ ] Backend `.env` configured with Google credentials
- [ ] Backend server restarted
- [ ] Mobile app synced and rebuilt
- [ ] Google Sign-In button appears on login screen
- [ ] Clicking button opens Google account picker
- [ ] After selecting account, redirects to home screen

---

## 🎉 Success!

If everything is configured correctly:
1. User clicks "Sign in with Google"
2. Google account picker appears
3. User selects account
4. App receives ID token
5. Backend verifies token
6. User is logged in and redirected to home screen

---

## 📚 Additional Resources

- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android/start)
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [Spring Boot OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)

---

**Last Updated**: May 2026  
**Version**: 1.0
