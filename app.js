/**
 * PathPass & GR (ZAS) - Main Application Logic & Interactivity
 * Developed for 1618101 Cpl (Tech) Md Zakaria Alom, 3 Signal Battalion
 */

document.addEventListener('DOMContentLoaded', () => {
    // Application State
    const state = {
        theme: localStorage.getItem('theme') || 'dark',
        activeTab: 'calcView',
        activeUnit: 'km',
        grMode: 'forward',
        mapMode: 'google_earth',
        radarRangeKm: 'auto',
        tilt3D: 0,
        rot3D: 0,
        leafletMap: null,
        markers: [],
        losPolyline: null,
        fresnelCircle: null,
        radarAnimationId: null,
        radarAngle: 0,
        savedWaypoints: JSON.parse(localStorage.getItem('saved_waypoints') || '[]')
    };

    // Apply saved theme
    document.documentElement.setAttribute('data-theme', state.theme);

    // =========================================================================
    // UI TOAST NOTIFICATION HELPER
    // =========================================================================
    function showToast(message, type = 'info') {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        let iconHtml = '<i class="fa-solid fa-circle-check text-emerald-400"></i>';
        if (type === 'error') iconHtml = '<i class="fa-solid fa-circle-exclamation text-rose-400"></i>';
        else if (type === 'info') iconHtml = '<i class="fa-solid fa-circle-info text-cyan-400"></i>';

        toast.innerHTML = `${iconHtml}<span>${message}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3200);
    }

    // Copy to clipboard helper with custom toast
    async function copyToClipboard(text, customMessage = null) {
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
            } else {
                const ta = document.createElement('textarea');
                ta.value = text;
                ta.style.position = 'fixed';
                ta.style.opacity = '0';
                document.body.appendChild(ta);
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
            }
            showToast(customMessage || `${text} copied to clipboard!`, 'success');
        } catch (err) {
            showToast("Failed to copy to clipboard", "error");
        }
    }

    // =========================================================================
    // THEME TOGGLE HANDLER
    // =========================================================================
    const themeToggleBtn = document.getElementById('themeToggleBtn');
    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', () => {
            state.theme = state.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', state.theme);
            localStorage.setItem('theme', state.theme);
            showToast(`Theme changed to ${state.theme.toUpperCase()} mode`, 'info');
        });
    }

    // Info Modal
    const infoModal = document.getElementById('infoModal');
    document.getElementById('btnInfoModal')?.addEventListener('click', () => {
        infoModal.classList.remove('hidden');
    });
    document.getElementById('btnCloseInfoModal')?.addEventListener('click', () => {
        infoModal.classList.add('hidden');
    });

    // =========================================================================
    // NAVIGATION & TAB SWITCHING
    // =========================================================================
    function switchTab(targetId) {
        state.activeTab = targetId;

        // Update Desktop Tabs
        document.querySelectorAll('.nav-tab-btn').forEach(btn => {
            const isTarget = btn.getAttribute('data-target') === targetId;
            btn.classList.toggle('active', isTarget);
            btn.classList.toggle('border-cyan-400', isTarget);
            btn.classList.toggle('text-cyan-400', isTarget);
            btn.classList.toggle('border-transparent', !isTarget);
            btn.classList.toggle('text-slate-400', !isTarget);
        });

        // Update Mobile Tabs
        document.querySelectorAll('.mobile-nav-btn').forEach(btn => {
            const isTarget = btn.getAttribute('data-target') === targetId;
            btn.classList.toggle('active', isTarget);
            btn.classList.toggle('text-cyan-400', isTarget);
            btn.classList.toggle('text-slate-400', !isTarget);
        });

        // Hide all panes, show targeted pane
        document.querySelectorAll('.tab-pane').forEach(pane => {
            pane.classList.add('hidden');
        });
        const activePane = document.getElementById(targetId);
        if (activePane) {
            activePane.classList.remove('hidden');
        }

        // Trigger resize / canvas updates if needed
        if (targetId === 'radarMapView') {
            initOrUpdateMap();
            if (state.mapMode === 'modern_radar') {
                startRadarAnimation();
            }
        } else if (targetId === 'rfLinkView') {
            drawPathProfile();
        }
    }

    document.querySelectorAll('.nav-tab-btn, .mobile-nav-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-target');
            if (target) switchTab(target);
        });
    });

    // Unit Selector Group
    document.querySelectorAll('.unit-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.unit-chip').forEach(b => {
                b.classList.remove('bg-cyan-600', 'text-white');
                b.classList.add('text-slate-400');
            });
            btn.classList.add('bg-cyan-600', 'text-white');
            btn.classList.remove('text-slate-400');
            state.activeUnit = btn.getAttribute('data-unit');
            calculateCoordinates();
        });
    });

    // =========================================================================
    // TAB 1: COORDINATE CALCULATOR
    // =========================================================================
    function getCalcInputs() {
        return {
            p1Name: document.getElementById('p1Name')?.value || 'Site Alpha',
            p1Lat: parseFloat(document.getElementById('p1Lat')?.value || '23.8103'),
            p1Lon: parseFloat(document.getElementById('p1Lon')?.value || '90.4125'),
            p1Elev: parseFloat(document.getElementById('p1Elev')?.value || '15.9'),
            p2Name: document.getElementById('p2Name')?.value || 'Site Bravo',
            p2Lat: parseFloat(document.getElementById('p2Lat')?.value || '24.582254'),
            p2Lon: parseFloat(document.getElementById('p2Lon')?.value || '90.141237'),
            p2Elev: parseFloat(document.getElementById('p2Elev')?.value || '22.9')
        };
    }

    function calculateCoordinates() {
        const { p1Lat, p1Lon, p1Elev, p2Lat, p2Lon, p2Elev } = getCalcInputs();

        if (isNaN(p1Lat) || isNaN(p1Lon) || isNaN(p2Lat) || isNaN(p2Lon)) {
            showToast("Invalid coordinates entered", "error");
            return;
        }

        const distMeters = calculateVincentyDistance(p1Lat, p1Lon, p2Lat, p2Lon);
        const distKm = distMeters / 1000.0;
        const bearing = calculateInitialBearing(p1Lat, p1Lon, p2Lat, p2Lon);
        const backBearing = calculateBackAzimuth(bearing);
        const cardinal = getCardinalDirection(bearing);
        const backCardinal = getCardinalDirection(backBearing);
        const elevDiff = (p2Elev || 0) - (p1Elev || 0);

        // Format distance based on active unit
        const resDistEl = document.getElementById('resDistance');
        const resDistAltEl = document.getElementById('resDistanceAlt');
        if (resDistEl) {
            if (state.activeUnit === 'm') {
                resDistEl.innerText = `${Math.round(distMeters).toLocaleString()} m`;
                resDistAltEl.innerText = `${distKm.toFixed(3)} km`;
            } else if (state.activeUnit === 'mi') {
                const miles = distMeters * 0.000621371;
                resDistEl.innerText = `${miles.toFixed(2)} mi`;
                resDistAltEl.innerText = `${distKm.toFixed(3)} km`;
            } else if (state.activeUnit === 'nm') {
                const nm = distMeters * 0.000539957;
                resDistEl.innerText = `${nm.toFixed(2)} NM`;
                resDistAltEl.innerText = `${distKm.toFixed(3)} km`;
            } else {
                resDistEl.innerText = `${distKm.toFixed(2)} km`;
                resDistAltEl.innerText = `${Math.round(distMeters).toLocaleString()} m`;
            }
        }

        // Bearings
        document.getElementById('resBearing').innerText = `${Math.round(bearing)}° ${cardinal}`;
        document.getElementById('resBearingDeg').innerText = `${bearing.toFixed(2)}° True`;
        document.getElementById('resBackBearing').innerText = `${Math.round(backBearing)}° ${backCardinal}`;
        document.getElementById('resBackBearingDeg').innerText = `${backBearing.toFixed(2)}° Back`;

        // Elevation Diff
        const prefix = elevDiff > 0 ? '+' : '';
        document.getElementById('resElevDiff').innerText = `${prefix}${elevDiff.toFixed(1)} m`;

        // Update secondary views
        updateRfCalculations();
        if (state.activeTab === 'radarMapView') {
            initOrUpdateMap();
        }
    }

    document.getElementById('btnCalculateDistance')?.addEventListener('click', () => {
        calculateCoordinates();
        showToast("Distance and bearing calculated successfully!", "success");
    });

    // Auto-update elevation on coordinate change
    ['p1Lat', 'p1Lon'].forEach(id => {
        document.getElementById(id)?.addEventListener('change', () => {
            const lat = parseFloat(document.getElementById('p1Lat').value);
            const lon = parseFloat(document.getElementById('p1Lon').value);
            if (!isNaN(lat) && !isNaN(lon)) {
                const elev = calculateBangladeshPhysiographicElevation(lat, lon);
                document.getElementById('p1Elev').value = elev.toFixed(1);
            }
        });
    });

    ['p2Lat', 'p2Lon'].forEach(id => {
        document.getElementById(id)?.addEventListener('change', () => {
            const lat = parseFloat(document.getElementById('p2Lat').value);
            const lon = parseFloat(document.getElementById('p2Lon').value);
            if (!isNaN(lat) && !isNaN(lon)) {
                const elev = calculateBangladeshPhysiographicElevation(lat, lon);
                document.getElementById('p2Elev').value = elev.toFixed(1);
            }
        });
    });

    // Swap Points
    document.getElementById('btnSwapPoints')?.addEventListener('click', () => {
        const p1NameEl = document.getElementById('p1Name');
        const p1LatEl = document.getElementById('p1Lat');
        const p1LonEl = document.getElementById('p1Lon');
        const p1ElevEl = document.getElementById('p1Elev');

        const p2NameEl = document.getElementById('p2Name');
        const p2LatEl = document.getElementById('p2Lat');
        const p2LonEl = document.getElementById('p2Lon');
        const p2ElevEl = document.getElementById('p2Elev');

        const temp = {
            name: p1NameEl.value,
            lat: p1LatEl.value,
            lon: p1LonEl.value,
            elev: p1ElevEl.value
        };

        p1NameEl.value = p2NameEl.value;
        p1LatEl.value = p2LatEl.value;
        p1LonEl.value = p2LonEl.value;
        p1ElevEl.value = p2ElevEl.value;

        p2NameEl.value = temp.name;
        p2LatEl.value = temp.lat;
        p2LonEl.value = temp.lon;
        p2ElevEl.value = temp.elev;

        calculateCoordinates();
        showToast("পয়েন্ট ১ ও পয়েন্ট ২ অদলবদল করা হয়েছে", "info");
    });

    // Copy Calculation Summary
    document.getElementById('btnCopyCalcSummary')?.addEventListener('click', () => {
        const { p1Name, p1Lat, p1Lon, p2Name, p2Lat, p2Lon } = getCalcInputs();
        const distKm = (calculateVincentyDistance(p1Lat, p1Lon, p2Lat, p2Lon) / 1000.0).toFixed(2);
        const bearing = calculateInitialBearing(p1Lat, p1Lon, p2Lat, p2Lon).toFixed(1);
        const text = `Point 1: ${p1Name} (${p1Lat}, ${p1Lon})\nPoint 2: ${p2Name} (${p2Lat}, ${p2Lon})\nDistance: ${distKm} km | Bearing: ${bearing}°`;
        copyToClipboard(text, "Calculation summary copied!");
    });

    // Open External Google Maps Route
    document.getElementById('btnOpenGoogleMapsRoute')?.addEventListener('click', () => {
        const { p1Lat, p1Lon, p2Lat, p2Lon } = getCalcInputs();
        const url = `https://www.google.com/maps/dir/?api=1&origin=${p1Lat},${p1Lon}&destination=${p2Lat},${p2Lon}&travelmode=driving`;
        window.open(url, '_blank');
    });

    // GPS Acquisition Helpers
    function requestLiveLocation(onSuccess) {
        if (!navigator.geolocation) {
            showToast("Geolocation is not supported by your browser", "error");
            return;
        }
        showToast("Acquiring high-accuracy GPS fix...", "info");
        navigator.geolocation.getCurrentPosition(
            pos => {
                const lat = pos.coords.latitude;
                const lon = pos.coords.longitude;
                const elev = pos.coords.altitude || calculateBangladeshPhysiographicElevation(lat, lon);
                onSuccess(lat, lon, elev);
                showToast("GPS location acquired!", "success");
            },
            err => {
                showToast(`GPS Error: ${err.message}`, "error");
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    }

    document.getElementById('btnP1Gps')?.addEventListener('click', () => {
        requestLiveLocation((lat, lon, elev) => {
            document.getElementById('p1Lat').value = lat.toFixed(6);
            document.getElementById('p1Lon').value = lon.toFixed(6);
            document.getElementById('p1Elev').value = elev.toFixed(1);
            calculateCoordinates();
        });
    });

    document.getElementById('btnP2Gps')?.addEventListener('click', () => {
        requestLiveLocation((lat, lon, elev) => {
            document.getElementById('p2Lat').value = lat.toFixed(6);
            document.getElementById('p2Lon').value = lon.toFixed(6);
            document.getElementById('p2Elev').value = elev.toFixed(1);
            calculateCoordinates();
        });
    });

    // Share GPS Location
    document.getElementById('btnShareGps')?.addEventListener('click', () => {
        requestLiveLocation((lat, lon) => {
            const url = `https://maps.google.com/?q=${lat.toFixed(6)},${lon.toFixed(6)}`;
            if (navigator.share) {
                navigator.share({
                    title: 'Live GPS Location',
                    text: `My GPS Coordinates: ${lat.toFixed(6)}, ${lon.toFixed(6)}`,
                    url: url
                }).catch(() => {});
            } else {
                copyToClipboard(url, "GPS Google Maps Link copied!");
            }
        });
    });

    // =========================================================================
    // TAB 2: ARMY GR (MILITARY GRID REFERENCE) SYSTEM
    // =========================================================================
    // Setup Presets Selector
    const presetSelector = document.getElementById('presetPointSelector');
    if (presetSelector) {
        USER_REFERENCE_POINTS.forEach(pt => {
            const opt = document.createElement('option');
            opt.value = pt.id;
            opt.innerText = `${pt.name} (${pt.expectedSheet} - GR: ${pt.expectedGr})`;
            presetSelector.appendChild(opt);
        });

        presetSelector.addEventListener('change', e => {
            const found = USER_REFERENCE_POINTS.find(p => p.id === e.target.value);
            if (found) {
                document.getElementById('grInputLat').value = found.lat;
                document.getElementById('grInputLon').value = found.lon;
                document.getElementById('revMapSheetInput').value = found.expectedSheet;
                document.getElementById('revGrInput').value = found.expectedGr;
                runForwardGr();
                showToast(`Loaded ${found.name}`, 'info');
            }
        });
    }

    // GR Mode Toggle (Forward vs Reverse)
    document.querySelectorAll('.gr-mode-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.gr-mode-btn').forEach(b => {
                b.classList.remove('bg-emerald-600', 'text-white');
                b.classList.add('text-slate-400');
            });
            btn.classList.add('bg-emerald-600', 'text-white');
            btn.classList.remove('text-slate-400');
            state.grMode = btn.getAttribute('data-mode');

            if (state.grMode === 'forward') {
                document.getElementById('grForwardForm').classList.remove('hidden');
                document.getElementById('grReverseForm').classList.add('hidden');
            } else {
                document.getElementById('grForwardForm').classList.add('hidden');
                document.getElementById('grReverseForm').classList.remove('hidden');
            }
        });
    });

    function runForwardGr() {
        const lat = parseFloat(document.getElementById('grInputLat').value);
        const lon = parseFloat(document.getElementById('grInputLon').value);
        if (isNaN(lat) || isNaN(lon)) {
            showToast("Invalid Latitude or Longitude", "error");
            return;
        }

        const res = convertLatLonToMilitaryGr(lat, lon);

        document.getElementById('resGr6').innerText = `${res.gr6.substring(0, 3)} ${res.gr6.substring(3)}`;
        document.getElementById('resGr8').innerText = `${res.gr8.substring(0, 4)} ${res.gr8.substring(4)}`;
        document.getElementById('resGr10').innerText = `${res.gr10.substring(0, 5)} ${res.gr10.substring(5)}`;

        document.getElementById('resMapSheet').innerText = res.mapSheet50k;
        document.getElementById('resDegreeSheet').innerText = res.degreeSheet250k;
        document.getElementById('resGrElevation').innerText = `${res.elevationMeters.toFixed(2)} m (${res.elevationFeet.toFixed(1)} ft)`;
        document.getElementById('resBtmCoords').innerText = `E: ${Math.round(res.eastingMeters)} N: ${Math.round(res.northingMeters)}`;
        document.getElementById('resWgsLatLonDisplay').innerText = `${res.lat.toFixed(6)}, ${res.lon.toFixed(6)}`;
        document.getElementById('resWgsDmsDisplay').innerText = `${res.latDms}, ${res.lonDms}`;
    }

    function runReverseGr() {
        const sheet = document.getElementById('revMapSheetInput').value.trim();
        const gr = document.getElementById('revGrInput').value.trim();

        try {
            const res = convertMilitaryGrToLatLon(sheet, gr);
            document.getElementById('resWgsLatLonDisplay').innerText = `${res.lat.toFixed(6)}, ${res.lon.toFixed(6)}`;
            document.getElementById('resWgsDmsDisplay').innerText = `${res.latDms}, ${res.lonDms}`;
            document.getElementById('resMapSheet').innerText = res.mapSheet;
            document.getElementById('resGrElevation').innerText = `${res.elevationMeters.toFixed(2)} m (${res.elevationFeet.toFixed(1)} ft)`;
            document.getElementById('resBtmCoords').innerText = `E: ${Math.round(res.eastingMeters)} N: ${Math.round(res.northingMeters)}`;

            // Populate Forward form inputs with reverse result
            document.getElementById('grInputLat').value = res.lat.toFixed(6);
            document.getElementById('grInputLon').value = res.lon.toFixed(6);
            runForwardGr();
            showToast("Coordinates found from Map Sheet & Army GR!", "success");
        } catch (err) {
            showToast(err.message, "error");
        }
    }

    document.getElementById('btnConvertLatLonToGr')?.addEventListener('click', () => {
        runForwardGr();
        showToast("Army GR and 1:50,000 Map Sheet calculated!", "success");
    });

    document.getElementById('btnConvertGrToLatLon')?.addEventListener('click', runReverseGr);

    document.getElementById('btnGrUseGps')?.addEventListener('click', () => {
        requestLiveLocation((lat, lon) => {
            document.getElementById('grInputLat').value = lat.toFixed(6);
            document.getElementById('grInputLon').value = lon.toFixed(6);
            runForwardGr();
        });
    });

    // CR 6: Unified One-Click COPY LAT/LON Button
    document.getElementById('btnCopyLatLonUnified')?.addEventListener('click', () => {
        const latLonText = document.getElementById('resWgsLatLonDisplay').innerText.trim();
        copyToClipboard(latLonText, "Latitude & Longitude Copied Successfully!");
    });

    // Copy Individual GR Buttons
    document.querySelectorAll('.btn-copy-gr').forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-copy-target');
            const targetEl = document.getElementById(targetId);
            if (targetEl) {
                const text = targetEl.innerText.replace(/\s+/g, '');
                copyToClipboard(text, `Army GR ${text} copied!`);
            }
        });
    });

    // =========================================================================
    // TAB 3: NETWORK INTERFACE & RF LINK BUDGET ENGINE
    // =========================================================================
    function getRfInputs() {
        const { p1Elev, p2Elev, p1Lat, p1Lon, p2Lat, p2Lon } = getCalcInputs();
        const distKm = calculateVincentyDistance(p1Lat, p1Lon, p2Lat, p2Lon) / 1000.0;

        return {
            distanceKm: distKm,
            p1Elevation: p1Elev,
            p1AntennaHeight: parseFloat(document.getElementById('rfP1Mast')?.value || '24'),
            p2Elevation: p2Elev,
            p2AntennaHeight: parseFloat(document.getElementById('rfP2Mast')?.value || '24'),
            frequencyMhz: parseFloat(document.getElementById('rfFreq1')?.value || '1350'),
            txPowerDbm: parseFloat(document.getElementById('rfTxPower')?.value || '30'),
            antennaGainDbi: parseFloat(document.getElementById('rfAntennaGain')?.value || '15'),
            rxSensitivityDbm: parseFloat(document.getElementById('rfRxSens')?.value || '-87')
        };
    }

    function updateRfCalculations() {
        const inputs = getRfInputs();
        const budget = calculateRfLinkBudget(inputs);

        // Update Status Badge
        const statusBadge = document.getElementById('rfLinkStatusBadge');
        if (statusBadge) {
            statusBadge.innerText = budget.linkStatusText.toUpperCase();
            statusBadge.style.color = budget.linkStatusColor;
            statusBadge.style.borderColor = budget.linkStatusColor;
        }

        // Update Bulge & Fresnel labels
        document.getElementById('lblBulge').innerText = `${budget.midEarthBulgeMeters.toFixed(1)}m`;
        document.getElementById('lblFresnel').innerText = `${budget.fresnelRadiusMidMeters.toFixed(1)}m`;

        // Populate 13 Parameters Grid
        const paramsGrid = document.getElementById('rfParamsGrid');
        if (paramsGrid) {
            const params = [
                { label: "1. Geodetic Distance", value: `${budget.distanceKm.toFixed(2)} km` },
                { label: "2. Operating Frequency", value: `${budget.frequencyMhz.toFixed(0)} MHz` },
                { label: "3. Power Output (Tx)", value: `${budget.txPowerDbm.toFixed(1)} dBm (${Math.pow(10, (budget.txPowerDbm-30)/10).toFixed(2)} W)` },
                { label: "4. Antenna Gain (Tx/Rx)", value: `${budget.antennaGainDbi.toFixed(1)} dBi (Custom)` },
                { label: "5. Free Space Path Loss (FSPL)", value: `${budget.freeSpacePathLossDb.toFixed(1)} dB` },
                { label: "6. Obstacle / Bulge Loss", value: `${budget.obstacleLossDb.toFixed(1)} dB` },
                { label: "7. Total Path Loss", value: `${budget.totalPathLossDb.toFixed(1)} dB` },
                { label: "8. Received Signal Level (RSL)", value: `${budget.receivedSignalLevelDbm.toFixed(1)} dBm`, highlight: budget.receivedSignalLevelDbm >= inputs.rxSensitivityDbm },
                { label: "9. Fade Margin", value: `${budget.fadeMarginDb >= 0 ? '+' : ''}${budget.fadeMarginDb.toFixed(1)} dB`, highlight: budget.fadeMarginDb >= 15 },
                { label: "10. Electric Field Strength", value: `${budget.eFieldDbuVm.toFixed(1)} dBµV/m` },
                { label: "11. Rx Voltage (50Ω)", value: `${budget.rxVoltageUv.toFixed(1)} µV` },
                { label: "12. Midpoint Clearance", value: `${budget.clearanceMidMeters.toFixed(1)} m (F1: ${budget.fresnelRadiusMidMeters.toFixed(1)}m)` },
                { label: "13. Antenna Elevation Tilt", value: `${budget.tiltAngleDeg.toFixed(2)}°` }
            ];

            paramsGrid.innerHTML = params.map(p => `
                <div class="p-2.5 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-between">
                    <span class="text-slate-400 text-[11px]">${p.label}:</span>
                    <span class="font-bold ${p.highlight !== undefined ? (p.highlight ? 'text-emerald-400' : 'text-rose-400') : 'text-slate-200'}">${p.value}</span>
                </div>
            `).join('');
        }

        drawPathProfile();
    }

    document.getElementById('btnRecalculateRf')?.addEventListener('click', () => {
        updateRfCalculations();
        showToast("RF Link Budget & Path Profile updated!", "success");
    });

    // 2D Canvas Path Profile Drawing
    function drawPathProfile() {
        const canvas = document.getElementById('losProfileCanvas');
        if (!canvas) return;

        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * window.devicePixelRatio;
        canvas.height = rect.height * window.devicePixelRatio;
        const ctx = canvas.getContext('2d');
        ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

        const w = rect.width;
        const h = rect.height;
        ctx.clearRect(0, 0, w, h);

        const { p1Name, p2Name, p1Elev, p2Elev, p1Lat, p1Lon, p2Lat, p2Lon } = getCalcInputs();
        const inputs = getRfInputs();
        const distKm = Math.max(0.1, inputs.distanceKm);

        // Update Legends
        document.getElementById('legendSiteA').innerText = `${p1Name} (0.0 km)`;
        document.getElementById('legendSiteB').innerText = `${p2Name} (${distKm.toFixed(1)} km)`;

        const h1 = inputs.p1AntennaHeight;
        const h2 = inputs.p2AntennaHeight;

        // Sample terrain along path
        const samples = 35;
        const terrainPoints = [];
        for (let i = 0; i <= samples; i++) {
            const frac = i / samples;
            const curLat = p1Lat + (p2Lat - p1Lat) * frac;
            const curLon = p1Lon + (p2Lon - p1Lon) * frac;
            let rawGround = frac === 0 ? p1Elev : (frac === 1 ? p2Elev : calculateBangladeshPhysiographicElevation(curLat, curLon));
            
            // Earth curvature bulge
            const d1 = distKm * frac;
            const d2 = distKm * (1.0 - frac);
            const bulge = (d1 * d2) / 12.75;
            const totalGround = rawGround + bulge;
            const rayMsl = (p1Elev + h1) + ((p2Elev + h2) - (p1Elev + h1)) * frac;

            terrainPoints.push({
                frac,
                ground: totalGround,
                ray: rayMsl,
                fresnel: 17.32 * Math.sqrt((d1 * d2) / ((inputs.frequencyMhz / 1000.0) * distKm || 1))
            });
        }

        const maxMsl = Math.max(...terrainPoints.map(p => Math.max(p.ground, p.ray + p.fresnel))) + 15;
        const minMsl = Math.max(0, Math.min(...terrainPoints.map(p => p.ground)) - 10);
        const rangeMsl = maxMsl - minMsl;

        const padX = 45;
        const padY = 25;
        const plotW = w - padX * 2;
        const plotH = h - padY * 2;

        const getX = frac => padX + frac * plotW;
        const getY = msl => padY + (1.0 - (msl - minMsl) / rangeMsl) * plotH;

        // 1. Draw Grid lines
        ctx.strokeStyle = '#1E293B';
        ctx.lineWidth = 1;
        for (let i = 0; i <= 4; i++) {
            const y = padY + (i / 4) * plotH;
            ctx.beginPath();
            ctx.moveTo(padX, y);
            ctx.lineTo(w - padX, y);
            ctx.stroke();

            const mslVal = maxMsl - (i / 4) * rangeMsl;
            ctx.fillStyle = '#64748B';
            ctx.font = '10px monospace';
            ctx.textAlign = 'right';
            ctx.fillText(`${Math.round(mslVal)}m`, padX - 6, y + 3);
        }

        // 2. Draw 1st Fresnel Zone Ellipse Envelope
        ctx.beginPath();
        terrainPoints.forEach((p, idx) => {
            const x = getX(p.frac);
            const y = getY(p.ray + p.fresnel);
            if (idx === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        for (let i = terrainPoints.length - 1; i >= 0; i--) {
            const p = terrainPoints[i];
            const x = getX(p.frac);
            const y = getY(p.ray - p.fresnel);
            ctx.lineTo(x, y);
        }
        ctx.closePath();
        ctx.fillStyle = 'rgba(0, 229, 255, 0.12)';
        ctx.fill();
        ctx.strokeStyle = 'rgba(0, 229, 255, 0.4)';
        ctx.setLineDash([4, 4]);
        ctx.stroke();
        ctx.setLineDash([]);

        // 3. Draw Undulating Ground Elevation Contour
        ctx.beginPath();
        ctx.moveTo(getX(0), h - padY);
        terrainPoints.forEach(p => {
            ctx.lineTo(getX(p.frac), getY(p.ground));
        });
        ctx.lineTo(getX(1), h - padY);
        ctx.closePath();
        const grad = ctx.createLinearGradient(0, padY, 0, h - padY);
        grad.addColorStop(0, '#334155');
        grad.addColorStop(0.5, '#1E293B');
        grad.addColorStop(1, '#0F172A');
        ctx.fillStyle = grad;
        ctx.fill();
        ctx.strokeStyle = '#94A3B8';
        ctx.lineWidth = 2;
        ctx.stroke();

        // 4. Draw Line of Sight Straight Ray
        const startRay = Offset => getY(p1Elev + h1);
        const endRay = Offset => getY(p2Elev + h2);
        ctx.beginPath();
        ctx.moveTo(getX(0), getY(p1Elev + h1));
        ctx.lineTo(getX(1), getY(p2Elev + h2));
        ctx.strokeStyle = '#FFD600';
        ctx.lineWidth = 3;
        ctx.stroke();

        // 5. Draw Tower Masts at Site 1 & Site 2
        // Tower 1
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(getX(0), getY(p1Elev));
        ctx.lineTo(getX(0), getY(p1Elev + h1));
        ctx.stroke();
        ctx.fillStyle = '#10B981';
        ctx.beginPath();
        ctx.arc(getX(0), getY(p1Elev + h1), 5, 0, Math.PI * 2);
        ctx.fill();

        // Tower 2
        ctx.beginPath();
        ctx.moveTo(getX(1), getY(p2Elev));
        ctx.lineTo(getX(1), getY(p2Elev + h2));
        ctx.stroke();
        ctx.fillStyle = '#EF4444';
        ctx.beginPath();
        ctx.arc(getX(1), getY(p2Elev + h2), 5, 0, Math.PI * 2);
        ctx.fill();
    }

    // =========================================================================
    // TAB 4: TACTICAL RADAR & 3D MAP VIEW
    // =========================================================================
    function initOrUpdateMap() {
        const { p1Lat, p1Lon, p2Lat, p2Lon, p1Name, p2Name } = getCalcInputs();

        // Container switching
        const mapContainer = document.getElementById('interactiveMapContainer');
        const radarContainer = document.getElementById('radarScopeContainer');
        const offlineContainer = document.getElementById('offlineMapContainer');

        const radarRangeBar = document.getElementById('radarRangeBar');
        const arcgisBar = document.getElementById('arcgisControlsBar');

        if (state.mapMode === 'modern_radar') {
            mapContainer.classList.add('hidden');
            offlineContainer.classList.add('hidden');
            radarContainer.classList.remove('hidden');
            radarRangeBar?.classList.remove('hidden');
            arcgisBar?.classList.add('hidden');
            startRadarAnimation();
            return;
        } else if (state.mapMode === 'offline_map') {
            mapContainer.classList.add('hidden');
            radarContainer.classList.add('hidden');
            offlineContainer.classList.remove('hidden');
            radarRangeBar?.classList.add('hidden');
            arcgisBar?.classList.add('hidden');
            stopRadarAnimation();
            drawOfflineTopoCanvas();
            return;
        } else {
            radarContainer.classList.add('hidden');
            offlineContainer.classList.add('hidden');
            mapContainer.classList.remove('hidden');
            radarRangeBar?.classList.add('hidden');
            arcgisBar?.classList.toggle('hidden', state.mapMode !== 'arcgis_earth');
            stopRadarAnimation();
        }

        // Initialize Leaflet Map if not created
        if (!state.leafletMap) {
            state.leafletMap = L.map('leafletMap', {
                zoomControl: true,
                attributionControl: false
            });
        }

        // Remove previous layers
        state.leafletMap.eachLayer(layer => {
            state.leafletMap.removeLayer(layer);
        });

        // Add Base Imagery according to mode
        if (state.mapMode === 'arcgis_earth') {
            L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 19
            }).addTo(state.leafletMap);
            L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', {
                maxZoom: 19
            }).addTo(state.leafletMap);
        } else {
            // Google Earth Photorealistic Satellite Base
            L.tileLayer('https://mt1.google.com/vt/lyrs=y&x={x}&y={y}&z={z}', {
                maxZoom: 20
            }).addTo(state.leafletMap);
        }

        // Markers
        const m1 = L.marker([p1Lat, p1Lon]).addTo(state.leafletMap)
            .bindPopup(`<b>Site A: ${p1Name}</b><br>Lat: ${p1Lat.toFixed(5)}<br>Lon: ${p1Lon.toFixed(5)}`);
        const m2 = L.marker([p2Lat, p2Lon]).addTo(state.leafletMap)
            .bindPopup(`<b>Site B: ${p2Name}</b><br>Lat: ${p2Lat.toFixed(5)}<br>Lon: ${p2Lon.toFixed(5)}`);

        // LOS Polyline
        L.polyline([[p1Lat, p1Lon], [p2Lat, p2Lon]], {
            color: '#FFD600',
            weight: 3.5,
            opacity: 0.95,
            dashArray: '8, 4'
        }).addTo(state.leafletMap);

        // Fit bounds
        state.leafletMap.fitBounds([[p1Lat, p1Lon], [p2Lat, p2Lon]], { padding: [40, 40] });

        // Update 3D transforms
        update3DTransform();
    }

    // View Mode Switcher
    document.querySelectorAll('.map-mode-chip').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.map-mode-chip').forEach(b => {
                b.classList.remove('bg-teal-600', 'bg-purple-600', 'bg-cyan-600', 'bg-emerald-600', 'text-white');
                b.classList.add('text-slate-400');
            });
            btn.classList.add('bg-cyan-600', 'text-white');
            btn.classList.remove('text-slate-400');
            state.mapMode = btn.getAttribute('data-mode');
            initOrUpdateMap();
        });
    });

    // 3D Gesture & Controls for ArcGIS Earth (CR 2.2)
    function update3DTransform() {
        const inner = document.querySelector('.map-3d-inner');
        if (inner) {
            inner.style.transform = `rotateX(${state.tilt3D}deg) rotateZ(${state.rot3D}deg)`;
        }
        const hud = document.getElementById('hud3dLabel');
        if (hud) {
            hud.innerText = `PITCH: ${Math.round(state.tilt3D)}° | ROT: ${Math.round(state.rot3D)}°`;
        }
    }

    document.getElementById('btnTilt45')?.addEventListener('click', () => {
        state.tilt3D = 45;
        update3DTransform();
    });
    document.getElementById('btnRotateLeft')?.addEventListener('click', () => {
        state.rot3D = (state.rot3D - 45 + 360) % 360;
        update3DTransform();
    });
    document.getElementById('btnRotateRight')?.addEventListener('click', () => {
        state.rot3D = (state.rot3D + 45) % 360;
        update3DTransform();
    });
    document.getElementById('btnReset3D')?.addEventListener('click', () => {
        state.tilt3D = 0;
        state.rot3D = 0;
        update3DTransform();
    });

    // Modern Tactical PPI Radar Scope Canvas Animation
    function startRadarAnimation() {
        if (state.radarAnimationId) cancelAnimationFrame(state.radarAnimationId);

        const canvas = document.getElementById('radarScopeCanvas');
        if (!canvas) return;

        const ctx = canvas.getContext('2d');
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * window.devicePixelRatio;
        canvas.height = rect.height * window.devicePixelRatio;

        function renderRadar() {
            const w = rect.width;
            const h = rect.height;
            ctx.save();
            ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
            ctx.clearRect(0, 0, w, h);

            const cx = w / 2;
            const cy = h / 2;
            const radius = Math.min(cx, cy) - 15;

            // Background Bezel
            ctx.fillStyle = '#020610';
            ctx.beginPath();
            ctx.arc(cx, cy, radius, 0, Math.PI * 2);
            ctx.fill();

            // Concentric Range Rings
            ctx.strokeStyle = 'rgba(0, 229, 255, 0.25)';
            ctx.lineWidth = 1;
            for (let r = 0.25; r <= 1.0; r += 0.25) {
                ctx.beginPath();
                ctx.arc(cx, cy, radius * r, 0, Math.PI * 2);
                ctx.stroke();
            }

            // Crosshair Axes
            ctx.beginPath();
            ctx.moveTo(cx - radius, cy);
            ctx.lineTo(cx + radius, cy);
            ctx.moveTo(cx, cy - radius);
            ctx.lineTo(cx, cy + radius);
            ctx.stroke();

            // Outer Azimuth Bezel Degrees
            ctx.fillStyle = '#64748B';
            ctx.font = '9px monospace';
            ctx.textAlign = 'center';
            for (let deg = 0; deg < 360; deg += 30) {
                const rad = ((deg - 90) * Math.PI) / 180;
                const tx = cx + Math.cos(rad) * (radius - 10);
                const ty = cy + Math.sin(rad) * (radius - 10);
                ctx.fillText(`${String(deg).padStart(3, '0')}°`, tx, ty + 3);
            }

            // Radar Sweep Beam
            state.radarAngle = (state.radarAngle + 1.2) % 360;
            const sweepRad = ((state.radarAngle - 90) * Math.PI) / 180;

            const sweepGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);
            sweepGrad.addColorStop(0, 'rgba(0, 229, 255, 0.4)');
            sweepGrad.addColorStop(1, 'rgba(0, 229, 255, 0.02)');

            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.arc(cx, cy, radius, sweepRad - 0.45, sweepRad);
            ctx.closePath();
            ctx.fillStyle = sweepGrad;
            ctx.fill();

            // Direct line of beam
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(cx + Math.cos(sweepRad) * radius, cy + Math.sin(sweepRad) * radius);
            ctx.strokeStyle = '#00E5FF';
            ctx.lineWidth = 2;
            ctx.stroke();

            // Target Blip Indicator
            const { p1Lat, p1Lon, p2Lat, p2Lon } = getCalcInputs();
            const bearing = calculateInitialBearing(p1Lat, p1Lon, p2Lat, p2Lon);
            const targetRad = ((bearing - 90) * Math.PI) / 180;
            const blipR = radius * 0.65;
            const bx = cx + Math.cos(targetRad) * blipR;
            const by = cy + Math.sin(targetRad) * blipR;

            ctx.fillStyle = '#EF4444';
            ctx.beginPath();
            ctx.arc(bx, by, 5, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = '#FFD600';
            ctx.lineWidth = 1.5;
            ctx.beginPath();
            ctx.arc(bx, by, 9, 0, Math.PI * 2);
            ctx.stroke();

            ctx.restore();
            state.radarAnimationId = requestAnimationFrame(renderRadar);
        }

        renderRadar();
    }

    function stopRadarAnimation() {
        if (state.radarAnimationId) {
            cancelAnimationFrame(state.radarAnimationId);
            state.radarAnimationId = null;
        }
    }

    // Offline Topo Map Canvas
    function drawOfflineTopoCanvas() {
        const canvas = document.getElementById('offlineTopoCanvas');
        if (!canvas) return;

        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * window.devicePixelRatio;
        canvas.height = rect.height * window.devicePixelRatio;
        const ctx = canvas.getContext('2d');
        ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

        const w = rect.width;
        const h = rect.height;
        ctx.fillStyle = '#070D18';
        ctx.fillRect(0, 0, w, h);

        // Tactical Grid
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.2)';
        ctx.lineWidth = 1;
        for (let x = 0; x < w; x += 60) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, h);
            ctx.stroke();
        }
        for (let y = 0; y < h; y += 60) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(w, y);
            ctx.stroke();
        }

        // Draw Center Point & 25km Coverage Circle
        const cx = w / 2;
        const cy = h / 2;
        ctx.strokeStyle = '#059669';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(cx, cy, Math.min(cx, cy) * 0.75, 0, Math.PI * 2);
        ctx.stroke();

        ctx.fillStyle = '#00E5FF';
        ctx.font = 'bold 12px monospace';
        ctx.fillText("⚡ 25KM OFFLINE MAP ACTIVE", 20, 30);
    }

    // =========================================================================
    // TAB 5: SAVED INTEL & LOCAL STORAGE
    // =========================================================================
    function renderSavedPoints() {
        const container = document.getElementById('savedPointsList');
        if (!container) return;

        if (state.savedWaypoints.length === 0) {
            container.innerHTML = `
                <div class="tactical-card p-8 text-center text-slate-500 font-semibold text-xs">
                    <i class="fa-solid fa-bookmark text-2xl text-slate-600 mb-2 block"></i>
                    কোনো সংরক্ষিত পয়েন্ট পাওয়া যায়নি। বর্তমান ক্যালকুলেশন সংরক্ষণ করুন।
                </div>
            `;
            return;
        }

        container.innerHTML = state.savedWaypoints.map((pt, idx) => `
            <div class="tactical-card p-3.5 flex items-center justify-between flex-wrap gap-2 border-l-4 border-l-cyan-500">
                <div>
                    <h4 class="font-bold text-sm text-cyan-400">${pt.title || `Waypoint #${idx + 1}`}</h4>
                    <p class="text-xs text-slate-400 font-mono mt-0.5">P1: ${pt.p1Lat}, ${pt.p1Lon} ➔ P2: ${pt.p2Lat}, ${pt.p2Lon}</p>
                    <p class="text-[11px] text-slate-500 font-mono">Distance: ${pt.distKm} km | Date: ${new Date(pt.timestamp).toLocaleDateString()}</p>
                </div>
                <div class="flex items-center gap-2">
                    <button class="btn-load-wp px-3 py-1.5 rounded bg-cyan-900/60 text-cyan-300 border border-cyan-500/40 text-xs font-bold hover:bg-cyan-800" data-idx="${idx}">
                        <i class="fa-solid fa-arrow-up-right-from-square"></i> Load
                    </button>
                    <button class="btn-delete-wp text-rose-400 hover:text-rose-300 p-2 text-xs" data-idx="${idx}">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </div>
        `).join('');

        // Attach listeners
        document.querySelectorAll('.btn-load-wp').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.getAttribute('data-idx'));
                const pt = state.savedWaypoints[idx];
                if (pt) {
                    document.getElementById('p1Name').value = pt.p1Name;
                    document.getElementById('p1Lat').value = pt.p1Lat;
                    document.getElementById('p1Lon').value = pt.p1Lon;
                    document.getElementById('p2Name').value = pt.p2Name;
                    document.getElementById('p2Lat').value = pt.p2Lat;
                    document.getElementById('p2Lon').value = pt.p2Lon;
                    calculateCoordinates();
                    switchTab('calcView');
                    showToast(`Loaded "${pt.title}"!`, 'success');
                }
            });
        });

        document.querySelectorAll('.btn-delete-wp').forEach(btn => {
            btn.addEventListener('click', () => {
                const idx = parseInt(btn.getAttribute('data-idx'));
                state.savedWaypoints.splice(idx, 1);
                localStorage.setItem('saved_waypoints', JSON.stringify(state.savedWaypoints));
                renderSavedPoints();
                showToast("Waypoint deleted", "info");
            });
        });
    }

    document.getElementById('btnSaveCurrentPoint')?.addEventListener('click', () => {
        const title = document.getElementById('savePointTitle').value.trim() || 'New Waypoint';
        const inputs = getCalcInputs();
        const distKm = (calculateVincentyDistance(inputs.p1Lat, inputs.p1Lon, inputs.p2Lat, inputs.p2Lon) / 1000.0).toFixed(2);

        state.savedWaypoints.unshift({
            title,
            ...inputs,
            distKm,
            timestamp: Date.now()
        });

        localStorage.setItem('saved_waypoints', JSON.stringify(state.savedWaypoints));
        document.getElementById('savePointTitle').value = '';
        renderSavedPoints();
        showToast(`Saved "${title}" to device storage!`, 'success');
    });

    document.getElementById('btnClearAllSaved')?.addEventListener('click', () => {
        if (confirm("Are you sure you want to clear all saved waypoints?")) {
            state.savedWaypoints = [];
            localStorage.removeItem('saved_waypoints');
            renderSavedPoints();
            showToast("All saved waypoints cleared", "info");
        }
    });

    document.getElementById('btnExportJson')?.addEventListener('click', () => {
        const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(state.savedWaypoints, null, 2));
        const a = document.createElement('a');
        a.setAttribute("href", dataStr);
        a.setAttribute("download", `pathpass_intel_${Date.now()}.json`);
        document.body.appendChild(a);
        a.click();
        a.remove();
        showToast("Waypoints exported as JSON", "success");
    });

    // =========================================================================
    // INITIALIZATION
    // =========================================================================
    calculateCoordinates();
    runForwardGr();
    renderSavedPoints();
});
