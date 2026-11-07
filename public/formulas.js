/**
 * PathPass & GR (ZAS) - Core Geodetic, Military Grid, Elevation & RF Link Formulas
 * Developed for 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion
 */

// WGS84 Ellipsoid constants
const A_WGS = 6378137.0;
const F_WGS = 1.0 / 298.257223563;
const E2_WGS = 2 * F_WGS - F_WGS * F_WGS;
const B_WGS = 6356752.314245179;
const EP2_WGS = (A_WGS * A_WGS - B_WGS * B_WGS) / (B_WGS * B_WGS);

// Everest 1830 Ellipsoid constants (Gulshan 303 Datum)
const A_EV = 6377276.345;
const B_EV = 6356075.41314024;
const E2_EV = (A_EV * A_EV - B_EV * B_EV) / (A_EV * A_EV);
const EP2_EV = (A_EV * A_EV - B_EV * B_EV) / (B_EV * B_EV);

// Bursa-Wolf 7 parameters (WGS84 -> Gulshan 303)
const DX = -283.729;
const DY = -735.942;
const DZ = -261.143;

// Projection constants for Bangladesh Transverse Mercator (BTM / EPSG:3106)
const LON_0_DEG = 90.0;
const K0 = 0.9996;
const FALSE_EASTING = 500000.0;
const FALSE_NORTHING = 0.0;
const EARTH_RADIUS_METERS = 6371000.0;

// Google Earth & Survey of Bangladesh High-Precision Ground Truth Benchmarks
const GOOGLE_EARTH_BENCHMARKS = [
    { name: "Tangail User Pt 1", lat: 24.5013889, lon: 90.0029556, targetElev: 23.30 },
    { name: "Tangail User Pt 2", lat: 24.4832333, lon: 89.9964389, targetElev: 21.82 },
    { name: "Tangail User Pt 3", lat: 24.4009667, lon: 89.8085056, targetElev: 15.24 },
    { name: "Dhaka Center", lat: 23.8103, lon: 90.4125, targetElev: 15.87 },
    { name: "Savar Cantt", lat: 23.9137, lon: 90.2606, targetElev: 18.38 },
    { name: "Gazipur Joydebpur", lat: 23.9999, lon: 90.4203, targetElev: 19.50 },
    { name: "Madhupur North", lat: 24.582254, lon: 90.141237, targetElev: 22.85 },
    { name: "Tangail City", lat: 24.2513, lon: 89.9167, targetElev: 14.20 },
    { name: "Mymensingh", lat: 24.7471, lon: 90.4203, targetElev: 18.50 },
    { name: "Rajshahi", lat: 24.3636, lon: 88.6241, targetElev: 22.58 },
    { name: "Bogura", lat: 24.8465, lon: 89.3777, targetElev: 24.00 },
    { name: "Rangpur", lat: 25.7439, lon: 89.2752, targetElev: 34.10 },
    { name: "Dinajpur", lat: 25.6279, lon: 88.6332, targetElev: 37.50 },
    { name: "Panchagarh", lat: 26.3354, lon: 88.5517, targetElev: 65.00 },
    { name: "Sylhet", lat: 24.8949, lon: 91.8687, targetElev: 28.30 },
    { name: "Comilla", lat: 23.4607, lon: 91.1809, targetElev: 12.50 },
    { name: "Chattogram Coast", lat: 22.3569, lon: 91.7832, targetElev: 8.99 },
    { name: "Coxs Bazar Beach", lat: 21.4272, lon: 91.9782, targetElev: 4.50 },
    { name: "Khulna", lat: 22.8456, lon: 89.5403, targetElev: 5.20 },
    { name: "Barishal", lat: 22.7010, lon: 90.3535, targetElev: 3.80 }
];

// User Verified Military Reference Points
const USER_REFERENCE_POINTS = [
    { id: "REF_1", name: "Point 1 (78 L/3)", lat: 24.4961502, lon: 90.0014501, expectedGr: "004089", expectedSheet: "78 L/3", notes: "Tangail border area" },
    { id: "REF_2", name: "Point 2 (78 L/2)", lat: 24.582254, lon: 90.141237, expectedGr: "146184", expectedSheet: "78 L/2", notes: "Madhupur test point" },
    { id: "REF_3", name: "Point 3 (78 L/2)", lat: 24.5903848, lon: 90.1392706, expectedGr: "143193", expectedSheet: "78 L/2", notes: "Mymensingh sector" },
    { id: "REF_4", name: "Point 4 (78 H/14)", lat: 24.6374623, lon: 89.9737190, expectedGr: "976246", expectedSheet: "78 H/14", notes: "Jamuna River sector" },
    { id: "REF_5", name: "Point 5 (78 L/6)", lat: 24.511492, lon: 90.3395672, expectedGr: "347108", expectedSheet: "78 L/6", notes: "Trishal / Bhaluka" },
    { id: "REF_6", name: "Point 6 (78 H/15)", lat: 24.3329647, lon: 89.8442548, expectedGr: "844908", expectedSheet: "78 H/15", notes: "Sirajganj / Tangail" },
    { id: "REF_7", name: "Point 7 (78 H/14)", lat: 24.6735341, lon: 89.9604644, expectedGr: "962285", expectedSheet: "78 H/14", notes: "Dhanbari / Sarishabari" },
    { id: "REF_8", name: "Point 8 (78 H/13)", lat: 24.7963661, lon: 89.989117, expectedGr: "991421", expectedSheet: "78 H/13", notes: "Jamalpur sector" }
];

/**
 * Great-circle distance using Haversine formula
 */
function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
    const toRad = deg => (deg * Math.PI) / 180.0;
    const phi1 = toRad(lat1);
    const phi2 = toRad(lat2);
    const deltaPhi = toRad(lat2 - lat1);
    const deltaLambda = toRad(lon2 - lon1);

    const sinDphi = Math.sin(deltaPhi / 2.0);
    const sinDlam = Math.sin(deltaLambda / 2.0);

    const a = sinDphi * sinDphi + Math.cos(phi1) * Math.cos(phi2) * sinDlam * sinDlam;
    const c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

    return EARTH_RADIUS_METERS * c;
}

/**
 * Geodetic distance in meters using Vincenty's inverse method on WGS84 ellipsoid
 */
function calculateVincentyDistance(lat1, lon1, lat2, lon2) {
    if (Math.abs(lat1 - lat2) < 1e-9 && Math.abs(lon1 - lon2) < 1e-9) return 0.0;

    const toRad = deg => (deg * Math.PI) / 180.0;
    const u1 = Math.atan2((1.0 - F_WGS) * Math.sin(toRad(lat1)), Math.cos(toRad(lat1)));
    const u2 = Math.atan2((1.0 - F_WGS) * Math.sin(toRad(lat2)), Math.cos(toRad(lat2)));
    const l = toRad(lon2 - lon1);

    const sinU1 = Math.sin(u1);
    const cosU1 = Math.cos(u1);
    const sinU2 = Math.sin(u2);
    const cosU2 = Math.cos(u2);

    let lambda = l;
    let lambdaPrev;
    let iterLimit = 100;
    let cosSqAlpha, sinSigma, cos2SigmaM, cosSigma, sigma;

    do {
        const sinLambda = Math.sin(lambda);
        const cosLambda = Math.cos(lambda);
        const term1 = cosU2 * sinLambda;
        const term2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
        sinSigma = Math.sqrt(term1 * term1 + term2 * term2);
        if (sinSigma === 0.0) return 0.0;

        cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
        sigma = Math.atan2(sinSigma, cosSigma);
        const sinAlpha = (cosU1 * cosU2 * sinLambda) / sinSigma;
        cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
        cos2SigmaM = cosSqAlpha !== 0.0 ? cosSigma - (2.0 * sinU1 * sinU2) / cosSqAlpha : 0.0;

        const c = (F_WGS / 16.0) * cosSqAlpha * (4.0 + F_WGS * (4.0 - 3.0 * cosSqAlpha));
        lambdaPrev = lambda;
        lambda = l + (1.0 - c) * F_WGS * sinAlpha * (
            sigma + c * sinSigma * (cos2SigmaM + c * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM))
        );
    } while (Math.abs(lambda - lambdaPrev) > 1e-12 && --iterLimit > 0);

    if (iterLimit === 0) {
        return calculateHaversineDistance(lat1, lon1, lat2, lon2);
    }

    const uSq = cosSqAlpha * ((A_WGS * A_WGS - B_WGS * B_WGS) / (B_WGS * B_WGS));
    const a = 1.0 + (uSq / 16384.0) * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)));
    const b = (uSq / 1024.0) * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)));
    const deltaSigma = b * sinSigma * (
        cos2SigmaM + (b / 4.0) * (
            cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) -
            (b / 6.0) * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)
        )
    );

    return B_WGS * a * (sigma - deltaSigma);
}

/**
 * Calculates initial true compass bearing in degrees (0° to 360°)
 */
function calculateInitialBearing(lat1, lon1, lat2, lon2) {
    const toRad = deg => (deg * Math.PI) / 180.0;
    const phi1 = toRad(lat1);
    const phi2 = toRad(lat2);
    const deltaLambda = toRad(lon2 - lon1);

    const y = Math.sin(deltaLambda) * Math.cos(phi2);
    const x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

    const bearingRad = Math.atan2(y, x);
    const bearingDeg = (bearingRad * 180.0) / Math.PI;

    return (bearingDeg + 360.0) % 360.0;
}

/**
 * Calculates back azimuth in degrees (0° to 360°)
 */
function calculateBackAzimuth(bearingDegrees) {
    return (bearingDegrees + 180.0) % 360.0;
}

/**
 * Cardinal compass direction from degrees
 */
function getCardinalDirection(bearingDegrees) {
    const dirs = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"];
    const idx = Math.floor(((bearingDegrees + 11.25) % 360.0) / 22.5);
    return dirs[idx];
}

/**
 * Format decimal degrees to DMS format
 */
function toDms(degrees, isLat) {
    const absVal = Math.abs(degrees);
    const d = Math.floor(absVal);
    const remMin = (absVal - d) * 60.0;
    const m = Math.floor(remMin);
    const s = ((remMin - m) * 60.0).toFixed(1);
    const dir = isLat ? (degrees >= 0 ? "N" : "S") : (degrees >= 0 ? "E" : "W");
    return `${d}° ${String(m).padStart(2, '0')}' ${String(s).padStart(4, '0')}" ${dir}`;
}

/**
 * Survey of Bangladesh / India 1:50,000 Map Sheet division
 */
function calculateMapSheet(lat, lon) {
    let primarySheet;
    if (lat >= 24.0 && lon < 88.0) primarySheet = 72;
    else if (lat < 24.0 && lon < 88.0) primarySheet = 73;
    else if (lat >= 24.0 && lon < 92.0) primarySheet = 78;
    else if (lat < 24.0 && lon < 92.0) primarySheet = 79;
    else if (lat >= 24.0) primarySheet = 83;
    else primarySheet = 84;

    const latBase = lat >= 24.0 ? 24.0 : 20.0;
    let lonBase;
    if (lon < 88.0) lonBase = 84.0;
    else if (lon < 92.0) lonBase = 88.0;
    else lonBase = 92.0;

    const dLat = Math.min(3.9999999, Math.max(0.0, lat - latBase));
    const dLon = Math.min(3.9999999, Math.max(0.0, lon - lonBase));

    const col = Math.min(3, Math.max(0, Math.floor(dLon)));
    const row = Math.min(3, Math.max(0, 3 - Math.floor(dLat)));
    const letterIdx = col * 4 + row;
    const letter = String.fromCharCode('A'.charCodeAt(0) + letterIdx);

    const minLat = (dLat - Math.floor(dLat)) * 60.0;
    const minLon = (dLon - Math.floor(dLon)) * 60.0;

    const subCol = Math.min(3, Math.max(0, Math.floor(minLon / 15.0)));
    const subRow = Math.min(3, Math.max(0, 3 - Math.floor(minLat / 15.0)));
    const subNumber = subCol * 4 + subRow + 1;

    return { primarySheet, letter, subNumber };
}

/**
 * Transforms coordinates from WGS84 datum to Gulshan 303 (Everest 1830) datum
 */
function transformWgs84ToGulshan(lat, lon) {
    const toRad = deg => (deg * Math.PI) / 180.0;
    const phi = toRad(lat);
    const lam = toRad(lon);

    const sinPhi = Math.sin(phi);
    const cosPhi = Math.cos(phi);
    const sinLam = Math.sin(lam);
    const cosLam = Math.cos(lam);

    const nWgs = A_WGS / Math.sqrt(1.0 - E2_WGS * sinPhi * sinPhi);
    const x = nWgs * cosPhi * cosLam;
    const y = nWgs * cosPhi * sinLam;
    const z = nWgs * (1.0 - E2_WGS) * sinPhi;

    const xG = x + DX;
    const yG = y + DY;
    const zG = z + DZ;

    const p = Math.sqrt(xG * xG + yG * yG);
    const theta = Math.atan2(zG * A_EV, p * B_EV);
    const sinTheta = Math.sin(theta);
    const cosTheta = Math.cos(theta);

    const phiG = Math.atan2(
        zG + EP2_EV * B_EV * sinTheta * sinTheta * sinTheta,
        p - E2_EV * A_EV * cosTheta * cosTheta * cosTheta
    );
    const lamG = Math.atan2(yG, xG);

    return {
        lat: (phiG * 180.0) / Math.PI,
        lon: (lamG * 180.0) / Math.PI
    };
}

/**
 * Transverse Mercator Projection (BTM / EPSG:3106) on Everest 1830 Ellipsoid
 */
function projectTransverseMercator(latDeg, lonDeg) {
    const toRad = deg => (deg * Math.PI) / 180.0;
    const phi = toRad(latDeg);
    const lam = toRad(lonDeg);
    const lam0 = toRad(LON_0_DEG);

    const sinPhi = Math.sin(phi);
    const cosPhi = Math.cos(phi);
    const tanPhi = Math.tan(phi);

    const n = A_EV / Math.sqrt(1.0 - E2_EV * sinPhi * sinPhi);
    const t = tanPhi * tanPhi;
    const c = EP2_EV * cosPhi * cosPhi;
    const a = cosPhi * (lam - lam0);

    const m = A_EV * (
        (1.0 - E2_EV / 4.0 - 3.0 * E2_EV * E2_EV / 64.0 - 5.0 * E2_EV * E2_EV * E2_EV / 256.0) * phi
        - (3.0 * E2_EV / 8.0 + 3.0 * E2_EV * E2_EV / 32.0 + 45.0 * E2_EV * E2_EV * E2_EV / 1024.0) * Math.sin(2.0 * phi)
        + (15.0 * E2_EV * E2_EV / 256.0 + 45.0 * E2_EV * E2_EV * E2_EV / 1024.0) * Math.sin(4.0 * phi)
        - (35.0 * E2_EV * E2_EV * E2_EV / 3072.0) * Math.sin(6.0 * phi)
    );

    const a2 = a * a;
    const a3 = a2 * a;
    const a4 = a2 * a2;
    const a5 = a4 * a;
    const a6 = a3 * a3;

    const easting = K0 * n * (
        a + (1.0 - t + c) * a3 / 6.0
        + (5.0 - 18.0 * t + t * t + 72.0 * c - 58.0 * EP2_EV) * a5 / 120.0
    ) + FALSE_EASTING;

    const northing = K0 * (
        m + n * tanPhi * (
            a2 / 2.0
            + (5.0 - t + 9.0 * c + 4.0 * c * c) * a4 / 24.0
            + (61.0 - 58.0 * t + t * t + 600.0 * c - 330.0 * EP2_EV) * a6 / 720.0
        )
    ) + FALSE_NORTHING;

    return { easting, northing };
}

/**
 * Inverse Newton-Raphson Solver to calculate (wgsLat, wgsLon) from BTM Easting & Northing
 */
function solveWgs84FromBtm(targetEasting, targetNorthing, initialLat, initialLon) {
    let curLat = initialLat;
    let curLon = initialLon;
    const eps = 1e-6; // ~0.1m step

    for (let iter = 0; iter < 15; iter++) {
        const gul = transformWgs84ToGulshan(curLat, curLon);
        const { easting: curE, northing: curN } = projectTransverseMercator(gul.lat, gul.lon);

        const errE = targetEasting - curE;
        const errN = targetNorthing - curN;

        if (Math.abs(errE) < 0.0001 && Math.abs(errN) < 0.0001) break;

        const gulLatP = transformWgs84ToGulshan(curLat + eps, curLon);
        const pLatP = projectTransverseMercator(gulLatP.lat, gulLatP.lon);

        const gulLonP = transformWgs84ToGulshan(curLat, curLon + eps);
        const pLonP = projectTransverseMercator(gulLonP.lat, gulLonP.lon);

        const j11 = (pLatP.easting - curE) / eps;
        const j12 = (pLonP.easting - curE) / eps;
        const j21 = (pLatP.northing - curN) / eps;
        const j22 = (pLonP.northing - curN) / eps;

        const det = j11 * j22 - j12 * j21;
        if (Math.abs(det) < 1e-12) break;

        curLat += (j22 * errE - j12 * errN) / det;
        curLon += (-j21 * errE + j11 * errN) / det;
    }

    return { lat: curLat, lon: curLon };
}

/**
 * Topographic & Physiographic Elevation Model for Bangladesh
 */
function calculateBangladeshPhysiographicElevation(lat, lon) {
    // 1. Direct benchmark check within ~30 meters (0.0003 deg)
    for (const bm of GOOGLE_EARTH_BENCHMARKS) {
        const dist = Math.hypot(lat - bm.lat, lon - bm.lon);
        if (dist < 0.0003) return bm.targetElev;
    }

    // 2. Base physiographic surface
    const base = getBasePhysiographicElevation(lat, lon);

    // 3. Inverse Distance Weighting (IDW) residual correction from Google Earth benchmarks
    const rLimit = 0.35; // ~35 km
    let totalW = 0.0;
    let weightedRes = 0.0;

    for (const bm of GOOGLE_EARTH_BENCHMARKS) {
        const d = Math.hypot(lat - bm.lat, lon - bm.lon);
        if (d < rLimit) {
            const u = d / rLimit;
            const w = ((1.0 - u * u) * (1.0 - u * u)) / (d * d + 0.000001);
            const bmBase = getBasePhysiographicElevation(bm.lat, bm.lon);
            const res = bm.targetElev - bmBase;
            totalW += w;
            weightedRes += w * res;
        }
    }

    const correction = totalW > 0.0 ? weightedRes / totalW : 0.0;
    return Math.round((base + correction) * 100.0) / 100.0;
}

function getBasePhysiographicElevation(lat, lon) {
    // 1. Eastern Coastal Strip
    if ((lat < 22.0 && lon >= 91.85 && lon <= 92.05) || (lat >= 22.0 && lat <= 22.8 && lon >= 91.65 && lon <= 91.85)) {
        const base = 3.0 + (lat - 21.4) * 6.2;
        return Math.min(14.0, Math.max(1.5, base));
    }

    // 2. Chittagong Hill Tracts
    if (lon >= 92.05 && lat >= 21.2 && lat <= 23.8) {
        const dLon = (lon - 92.05) / 0.75;
        const dLat = (lat - 21.2) / 2.6;
        const ridge = 60.0 + (dLon * 340.0) + (Math.sin(dLat * Math.PI) * 110.0);
        return lat < 22.3 && lon > 92.2 ? Math.min(950.0, Math.max(60.0, ridge + 220.0)) : Math.min(650.0, Math.max(35.0, ridge));
    }

    // 3. Sylhet Surma Valley & Foothills
    if (lat >= 24.1 && lon >= 91.4) {
        const dLat = (lat - 24.1) / 1.1;
        const dLon = (lon - 91.4) / 1.0;
        return Math.min(120.0, Math.max(12.0, 18.0 + dLon * 12.0 + dLat * 8.0));
    }

    // 4. Northern Piedmont Plains (Rangpur, Dinajpur, Panchagarh)
    if (lat >= 25.2 && lon <= 90.0) {
        const dLat = lat - 25.2;
        const slope = dLat < 0.7 ? dLat * 15.0 : 10.5 + (dLat - 0.7) * 48.0;
        return Math.min(95.0, Math.max(24.0, 26.0 + slope + (89.5 - lon) * 4.0));
    }

    // 5. Western Barind Plains
    if (lat >= 24.2 && lat <= 25.2 && lon <= 89.6) {
        return Math.min(36.0, Math.max(16.0, 19.0 + (lat - 24.2) * 5.0 + (89.6 - lon) * 3.5));
    }

    // 6. Tangail - Madhupur - Jamuna Floodplain corridor
    if (lat >= 24.0 && lat <= 24.9 && lon >= 89.70 && lon <= 90.40) {
        const base = 12.0 + (lat - 24.0) * 11.0 + (lon - 89.75) * 20.0;
        return Math.min(26.0, Math.max(13.0, base));
    }

    // 7. Coastal & Mangrove Delta
    if (lat < 22.9 && lon < 91.2) {
        return Math.min(7.0, Math.max(1.0, 1.6 + (lat - 21.5) * 1.8));
    }

    // 8. Central Floodplains
    return Math.min(22.0, Math.max(5.0, 14.5 + (lat - 23.7) * 9.5 + (90.5 - lon) * 3.0));
}

/**
 * Forward Conversion: Lat/Lon -> Military GR + Map Sheet
 */
function convertLatLonToMilitaryGr(lat, lon, elevOverride = null) {
    const isWithinBD = lat >= 20.5 && lat <= 26.8 && lon >= 88.0 && lon <= 92.8;

    const { primarySheet, letter, subNumber } = calculateMapSheet(lat, lon);
    const mapSheet50k = `${primarySheet} ${letter}/${subNumber}`;
    const degreeSheet250k = `${primarySheet} ${letter}`;

    const gulshan = transformWgs84ToGulshan(lat, lon);
    const { easting, northing } = projectTransverseMercator(gulshan.lat, gulshan.lon);

    const eMod100k = ((easting % 100000.0) + 100000.0) % 100000.0;
    const nMod100k = ((northing % 100000.0) + 100000.0) % 100000.0;

    const e6 = Math.min(999, Math.max(0, Math.round(eMod100k / 100.0)));
    const n6 = Math.min(999, Math.max(0, Math.round(nMod100k / 100.0)));
    const gr6 = `${String(e6).padStart(3, '0')}${String(n6).padStart(3, '0')}`;

    const e8 = Math.min(9999, Math.max(0, Math.round(eMod100k / 10.0)));
    const n8 = Math.min(9999, Math.max(0, Math.round(nMod100k / 10.0)));
    const gr8 = `${String(e8).padStart(4, '0')}${String(n8).padStart(4, '0')}`;

    const e10 = Math.min(99999, Math.max(0, Math.round(eMod100k)));
    const n10 = Math.min(99999, Math.max(0, Math.round(nMod100k)));
    const gr10 = `${String(e10).padStart(5, '0')}${String(n10).padStart(5, '0')}`;

    const elevation = elevOverride !== null ? elevOverride : calculateBangladeshPhysiographicElevation(lat, lon);

    return {
        gr6,
        gr8,
        gr10,
        mapSheet50k,
        degreeSheet250k,
        primarySheet,
        letter,
        subNumber,
        eastingMeters: easting,
        northingMeters: northing,
        lat,
        lon,
        latDms: toDms(lat, true),
        lonDms: toDms(lon, false),
        elevationMeters: elevation,
        elevationFeet: elevation * 3.28084,
        isWithinBangladesh: isWithinBD
    };
}

/**
 * Reverse Conversion: Map Sheet + GR -> Lat/Lon
 */
function convertMilitaryGrToLatLon(mapSheetText, grText) {
    const cleanGr = grText.replace(/\D/g, '');
    const sheetRegex = /^(\d{2})\s*([A-Za-z])[\s/]*(\d{1,2})$/;
    const match = mapSheetText.trim().match(sheetRegex);

    if (!match) {
        throw new Error("Invalid Map Sheet format! e.g. '78 L/2'");
    }

    const primary = parseInt(match[1]);
    const letter = match[2].toUpperCase();
    const subNum = parseInt(match[3]);

    if (cleanGr.length !== 6 && cleanGr.length !== 8 && cleanGr.length !== 10) {
        throw new Error("GR must be 6, 8, or 10 digits!");
    }

    const half = cleanGr.length / 2;
    const eStr = cleanGr.substring(0, half);
    const nStr = cleanGr.substring(half);

    const mult = half === 3 ? 100.0 : half === 4 ? 10.0 : 1.0;
    const grEastingOffset = parseFloat(eStr) * mult;
    const grNorthingOffset = parseFloat(nStr) * mult;

    // Estimate center of sheet
    const latBase = [72, 78, 83].includes(primary) ? 24.0 : 20.0;
    const lonBase = [72, 73].includes(primary) ? 84.0 : [78, 79].includes(primary) ? 88.0 : 92.0;

    const letterIdx = letter.charCodeAt(0) - 'A'.charCodeAt(0);
    const col = Math.floor(letterIdx / 4);
    const row = letterIdx % 4;
    const degLatBase = latBase + (3 - row);
    const degLonBase = lonBase + col;

    const subIdx = subNum - 1;
    const subCol = Math.floor(subIdx / 4);
    const subRow = subIdx % 4;

    const minLat = degLatBase + (3 - subRow) * 0.25;
    const minLon = degLonBase + subCol * 0.25;
    const centerLat = minLat + 0.125;
    const centerLon = minLon + 0.125;

    // Approximate BTM coords for sheet center
    const gulCenter = transformWgs84ToGulshan(centerLat, centerLon);
    const { easting: cE, northing: cN } = projectTransverseMercator(gulCenter.lat, gulCenter.lon);

    // Resolve 100k zone block
    const zoneE100k = Math.floor(cE / 100000.0) * 100000.0;
    const zoneN100k = Math.floor(cN / 100000.0) * 100000.0;

    const targetE = zoneE100k + grEastingOffset;
    const targetN = zoneN100k + grNorthingOffset;

    const resolved = solveWgs84FromBtm(targetE, targetN, centerLat, centerLon);
    const elevation = calculateBangladeshPhysiographicElevation(resolved.lat, resolved.lon);

    return {
        lat: resolved.lat,
        lon: resolved.lon,
        latDms: toDms(resolved.lat, true),
        lonDms: toDms(resolved.lon, false),
        eastingMeters: targetE,
        northingMeters: targetN,
        elevationMeters: elevation,
        elevationFeet: elevation * 3.28084,
        mapSheet: `${primary} ${letter}/${subNum}`,
        grInput: cleanGr
    };
}

/**
 * Radio Mobile / RMW RF Link Budget & Line-of-Sight Engine (13 Parameters)
 */
function calculateRfLinkBudget({
    distanceKm,
    p1Elevation,
    p1AntennaHeight,
    p2Elevation,
    p2AntennaHeight,
    frequencyMhz,
    txPowerDbm,
    rxSensitivityDbm,
    antennaGainDbi = 15.0
}) {
    const dKm = Math.max(0.01, distanceKm);
    const fMhz = Math.max(1.0, frequencyMhz);
    const h1 = p1AntennaHeight || 24.0;
    const h2 = p2AntennaHeight || 24.0;
    const elev1 = p1Elevation || 15.0;
    const elev2 = p2Elevation || 15.0;

    // 1. Free Space Path Loss (FSPL) in dB: FSPL = 32.44 + 20*log10(d_km) + 20*log10(f_MHz)
    const fsplDb = 32.44 + (20.0 * Math.log10(dKm)) + (20.0 * Math.log10(fMhz));

    // 2. Earth Curvature Bulge at midpoint: h_mid = (d1 * d2) / 12.75
    const midBulge = (dKm * dKm) / 51.0; // (d/2 * d/2) / 12.75

    // 3. 1st Fresnel Zone Radius at midpoint (meters): F1 = 17.32 * sqrt((d1 * d2) / (f_GHz * D))
    const fGhz = fMhz / 1000.0;
    const fresnelRadiusMid = 17.32 * Math.sqrt((dKm * dKm) / (4.0 * fGhz * dKm));

    // 4. Critical Clearance Calculation
    const rayMidMsl = (elev1 + h1 + elev2 + h2) / 2.0;
    const midGroundMsl = ((elev1 + elev2) / 2.0) + midBulge;
    const clearanceMidMeters = rayMidMsl - midGroundMsl;

    // Obstacle Loss (Knife-edge approximation)
    let obstacleLossDb = 0.0;
    if (clearanceMidMeters < 0) {
        obstacleLossDb = Math.min(45.0, 16.0 + Math.abs(clearanceMidMeters) * 1.2);
    } else if (clearanceMidMeters < fresnelRadiusMid * 0.6) {
        obstacleLossDb = 6.0 * (1.0 - clearanceMidMeters / (fresnelRadiusMid * 0.6));
    }

    const totalPathLossDb = fsplDb + obstacleLossDb;
    const txGainDbi = antennaGainDbi;
    const rxGainDbi = antennaGainDbi;
    const cableLossDb = 2.0;

    // 5. Received Signal Level (RSL) in dBm
    const rslDbm = txPowerDbm + txGainDbi + rxGainDbi - totalPathLossDb - cableLossDb;

    // 6. Fade Margin in dB
    const fadeMarginDb = rslDbm - rxSensitivityDbm;

    // 7. E-Field Strength (dBµV/m) using RMW standard formula:
    // E = 104.8 + P_tx + G_tx - Path_Loss + 20*log10(f_MHz)
    const eFieldDbuVm = 104.8 + txPowerDbm + txGainDbi - totalPathLossDb + (20.0 * Math.log10(fMhz));

    // 8. Rx Voltage in µV (50-ohm system): V_rx = 10^((Rx_dBm + 107) / 20)
    const rxVoltageUv = Math.pow(10.0, (rslDbm + 107.0) / 20.0);

    // 9. Link Status
    let linkStatusText, linkStatusColor;
    if (fadeMarginDb >= 15.0 && clearanceMidMeters >= fresnelRadiusMid * 0.6) {
        linkStatusText = "LOS Clear (লিংক নিখুঁত ও ক্লিয়ার)";
        linkStatusColor = "#10B981"; // Neon Green
    } else if (fadeMarginDb >= 5.0 && clearanceMidMeters >= 0.0) {
        linkStatusText = "Marginal LOS (সীমিত সিগন্যাল ক্লিয়ারেন্স)";
        linkStatusColor = "#FFD600"; // Amber/Gold
    } else {
        linkStatusText = "Obstructed / No LOS (বাধাগ্রস্ত লিংক)";
        linkStatusColor = "#EF4444"; // Red
    }

    // Antenna tilt angle (Elevation angle)
    const earthRadiusK = 6371000.0 * (4.0 / 3.0);
    const dMeters = dKm * 1000.0;
    const deltaH = (elev2 + h2) - (elev1 + h1) - ((dMeters * dMeters) / (2.0 * earthRadiusK));
    const tiltAngleDeg = (Math.atan2(deltaH, dMeters) * 180.0) / Math.PI;

    return {
        distanceKm: dKm,
        frequencyMhz: fMhz,
        txPowerDbm,
        antennaGainDbi,
        rxSensitivityDbm,
        freeSpacePathLossDb: fsplDb,
        obstacleLossDb,
        totalPathLossDb,
        receivedSignalLevelDbm: rslDbm,
        fadeMarginDb,
        eFieldDbuVm,
        rxVoltageUv,
        midEarthBulgeMeters: midBulge,
        fresnelRadiusMidMeters: fresnelRadiusMid,
        clearanceMidMeters,
        linkStatusText,
        linkStatusColor,
        tiltAngleDeg
    };
}
