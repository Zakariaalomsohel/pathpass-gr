# PathPass & GR (ZAS) - Military Tactical Intelligence & RF Suite

**Developer / Maintainer**: 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion

---

## 🌐 Web Application Overview

**PathPass & GR (ZAS)** is now available as a responsive, modern, military-tactical Web Application (HTML5, CSS3, JavaScript, Tailwind CSS, Leaflet). You can run it on any browser, mobile phone, tablet, or desktop, and deploy it for free to **GitHub Pages**, **Netlify**, or **Vercel**.

### Key Features
1. **Geodetic Calculations (CALS)**:
   - High-precision WGS-84 ellipsoidal distance (Vincenty & Haversine equations).
   - Forward Azimuth, Back Azimuth, Elevation difference, and 1-click Point Swap.
   - Independent GPS lock for Point 1 (Observer) and Point 2 (Target).
2. **Army GR (Military Grid Reference)**:
   - **LAT/LON → GR**: Computes 6-figure, 8-figure, 10-figure GR, Survey of Bangladesh 1:50k Topo Sheet name/number, BTM (Bangladesh Transverse Mercator, EPSG:3106), and high-resolution ground elevation.
   - **GR & Sheet → LAT/LON**: 2D Newton-Raphson inverse BTM solver with 1-click **"COPY LAT/LON"** button (`Latitude, Longitude` format with instant toast confirmation).
   - Real-time GPS tracking with auto-updating coordinates.
3. **Network Interface & 13-Parameter RF Link Budget**:
   - Radio Mobile standards: Frequency, Tx/Rx Power, Antenna Gains (dBi), Cable/Connector Losses, Line-of-Sight status, Free Space Path Loss (FSPL), Obstacle diffraction loss, Received Signal Level (RSL in dBm), Rx Voltage in 50Ω (μV), Fade Margin, Electric Field Strength (dBμV/m), and Antenna Tilt Angle.
4. **2D Digital Path Profile Visualizer (Radio Mobile Style)**:
   - HTML5 Canvas terrain cross-section with Earth curvature bulge ($d_1 d_2 / 12.75$), straight LOS ray, 1st Fresnel zone clearance envelope, Tx/Rx mast towers, and critical clearance point calculation.
5. **Tactical Radar & 3D Map View**:
   - **Modern Radar**: 360° Pulse-Doppler PPI Plan Position Indicator with animated sweep, azimuth degree bezel, range rings, and range scale selector (AUTO, 10km, 25km, 50km, 100km).
   - **Google Earth**: Leaflet photorealistic satellite imagery with live tactical overlays, markers, and distance scale.
   - **ArcGIS Earth**: 3D perspective viewer with two-finger tilt/rotate simulation.
   - **25km Offline Topo Map**: Device-local canvas renderer for offline environments.
6. **Intel Storage & Tactical Presets**:
   - Save, load, and export tactical waypoints to JSON.
   - Instant 1-click presets for benchmark military locations (Mymensingh, Tangail, Jamuna Bridge, Dhaka Cantonment, Bogura, Sylhet, Chattogram).
7. **Tactical Theme Engine**:
   - Neon Emerald (`#10B981`), Electric Cyan (`#00E5FF`), Cyber Gold (`#FFD600`), and Tactical Dark/Light mode toggle with `localStorage` memory.

---

## 🚀 Free Web Hosting & Deployment Guide

### Option 1: GitHub Pages (Recommended)
1. Push this repository to your GitHub account:
   ```bash
   git init
   git add .
   git commit -m "Deploy PathPass & GR Web Application"
   git branch -M main
   git remote add origin https://github.com/<your-username>/pathpass-zas.git
   git push -u origin main
   ```
2. On GitHub, navigate to **Settings** > **Pages**.
3. Under **Build and deployment**, set Source to **Deploy from a branch**, select `main` branch, and folder `/ (root)`.
4. Click **Save**. Your web app will be live at `https://<your-username>.github.io/pathpass-zas/`.

### Option 2: Netlify (1-Click Drag & Drop or Git)
1. Go to [Netlify](https://app.netlify.com).
2. Either drag-and-drop the project folder directly into Netlify Drop, or connect your GitHub repository.
3. `netlify.toml` is pre-configured with `publish = "."`. It will deploy automatically.

### Option 3: Vercel
1. Go to [Vercel](https://vercel.com) and import the repository.
2. `vercel.json` is pre-configured with zero build configuration needed. Click **Deploy**.

### Option 4: Local Computer / Offline Use
Simply open `index.html` in Chrome, Firefox, Safari, or Edge, or run:
```bash
# Python 3
python3 -m http.server 8080

# Or Node.js
npx serve
```
Then visit `http://localhost:8080` in your browser.

---

## 📱 Android Application

The original Android Jetpack Compose application source code is maintained inside `/app`:
1. Open this repository in Android Studio.
2. Sync Gradle and run on device or emulator.
3. Target SDK: 35, Min SDK: 26.
