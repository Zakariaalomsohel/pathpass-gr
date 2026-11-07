package com.example.calculator

/**
 * Pre-defined reference points from military documentation and user input.
 */
data class ReferencePoint(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val expectedGr: String,
    val expectedSheet: String,
    val notes: String
)

object BangladeshMapData {

    // The user's exact 8 reference points
    val USER_REFERENCE_POINTS = listOf(
        ReferencePoint(
            id = "REF_1",
            name = "Point 1 (78 L/3)",
            lat = 24.4961502,
            lon = 90.0014501,
            expectedGr = "004089",
            expectedSheet = "78 L/3",
            notes = "Tangail border area"
        ),
        ReferencePoint(
            id = "REF_2",
            name = "Point 2 (78 L/2 - Screenshot)",
            lat = 24.582254,
            lon = 90.141237,
            expectedGr = "146184",
            expectedSheet = "78 L/2",
            notes = "Matching screenshot display"
        ),
        ReferencePoint(
            id = "REF_3",
            name = "Point 3 (78 L/2)",
            lat = 24.5903848,
            lon = 90.1392706,
            expectedGr = "143193",
            expectedSheet = "78 L/2",
            notes = "Madhupur / Mymensingh sector"
        ),
        ReferencePoint(
            id = "REF_4",
            name = "Point 4 (78 H/14)",
            lat = 24.6374623,
            lon = 89.9737190,
            expectedGr = "976246",
            expectedSheet = "78 H/14",
            notes = "Jamuna River sector"
        ),
        ReferencePoint(
            id = "REF_5",
            name = "Point 5 (78 L/6)",
            lat = 24.511492,
            lon = 90.3395672,
            expectedGr = "347108",
            expectedSheet = "78 L/6",
            notes = "Trishal / Bhaluka sector"
        ),
        ReferencePoint(
            id = "REF_6",
            name = "Point 6 (78 H/15)",
            lat = 24.3329647,
            lon = 89.8442548,
            expectedGr = "844908",
            expectedSheet = "78 H/15",
            notes = "Sirajganj / Tangail sector"
        ),
        ReferencePoint(
            id = "REF_7",
            name = "Point 7 (78 H/14)",
            lat = 24.6735341,
            lon = 89.9604644,
            expectedGr = "962285",
            expectedSheet = "78 H/14",
            notes = "Dhanbari / Sarishabari sector"
        ),
        ReferencePoint(
            id = "REF_8",
            name = "Point 8 (78 H/13)",
            lat = 24.7963661,
            lon = 89.989117,
            expectedGr = "991421",
            expectedSheet = "78 H/13",
            notes = "Jamalpur sector"
        )
    )

    // Key Divisional & Tactical Headquarters
    val KEY_LOCATIONS = listOf(
        ReferencePoint("DHAKA", "ঢাকা (Dhaka Cantonment)", 23.8103, 90.4125, "", "79 I/1", "National Capital & Army HQ"),
        ReferencePoint("BOGURA", "বগুড়া (Majhira Cantonment)", 24.8465, 89.3777, "", "78 H/9", "Bogura 11 Infantry Div"),
        ReferencePoint("CHITTAGONG", "চট্টগ্রাম (Bhatiary)", 22.3569, 91.7832, "", "79 N/7", "BMA & 24 Infantry Div"),
        ReferencePoint("SYLHET", "সিলেট (Jalalabad Cantonment)", 24.8949, 91.8687, "", "83 D/13", "17 Infantry Div"),
        ReferencePoint("RANGPUR", "রংপুর (Rangpur Cantonment)", 25.7439, 89.2752, "", "78 G/6", "66 Infantry Div"),
        ReferencePoint("COMILLA", "কুমিল্লা (Mainamati Cantonment)", 23.4607, 91.1809, "", "79 M/6", "33 Infantry Div"),
        ReferencePoint("RAJSHAHI", "রাজশাহী (Rajshahi Cantonment)", 24.3745, 88.6042, "", "78 D/14", "Rajshahi sector"),
        ReferencePoint("JASHORE", "যশোর (Jashore Cantonment)", 23.1664, 89.2081, "", "79 F/7", "55 Infantry Div"),
        ReferencePoint("BARISHAL", "বরিশাল (Lebukhali Cantonment)", 22.7010, 90.3535, "", "79 J/11", "7 Infantry Div"),
        ReferencePoint("SAVAR", "সাভার (Savar Cantonment)", 23.8583, 90.2667, "", "79 I/5", "9 Infantry Div"),
        ReferencePoint("GHATAIL", "ঘাটাইল (Shahid Salahuddin Cantt)", 24.5020, 90.0070, "", "78 L/3", "19 Infantry Div (3 Signal Battalion)")
    )

    // Simplified polygon of Bangladesh outline for the tactical overview map
    // (longitude, latitude pairs tracing the international border)
    val BANGLADESH_BORDER_COORDS = listOf(
        Pair(88.02, 24.78),
        Pair(88.35, 25.20),
        Pair(88.05, 25.85),
        Pair(88.55, 26.38),
        Pair(89.00, 26.25),
        Pair(89.70, 26.00),
        Pair(89.85, 25.18),
        Pair(90.65, 25.18),
        Pair(91.60, 25.15),
        Pair(92.48, 25.05),
        Pair(92.50, 24.40),
        Pair(92.20, 23.90),
        Pair(92.35, 23.50),
        Pair(92.65, 22.40),
        Pair(92.35, 21.40),
        Pair(92.25, 20.65), // Teknaf / St Martin
        Pair(91.90, 21.45),
        Pair(91.45, 22.35),
        Pair(90.50, 21.80),
        Pair(89.50, 21.60),
        Pair(89.05, 21.75),
        Pair(88.90, 22.50),
        Pair(88.60, 23.50),
        Pair(88.20, 24.30),
        Pair(88.02, 24.78)
    )

    // 1:250k Degree sheets covering Bangladesh
    data class Sheet250kInfo(
        val sheetCode: String,
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double,
        val label: String
    )

    val BANGLADESH_SHEETS_250K = listOf(
        Sheet250kInfo("78 G", 25.0, 26.0, 89.0, 90.0, "Rangpur"),
        Sheet250kInfo("78 H", 24.0, 25.0, 89.0, 90.0, "Bogura / Tangail W"),
        Sheet250kInfo("78 L", 24.0, 25.0, 90.0, 91.0, "Mymensingh / Tangail E"),
        Sheet250kInfo("78 P", 24.0, 25.0, 91.0, 92.0, "Kishoreganj / Habiganj"),
        Sheet250kInfo("78 K", 25.0, 26.0, 90.0, 91.0, "Netrokona / Garo"),
        Sheet250kInfo("79 B", 22.0, 23.0, 88.0, 89.0, "Sundarbans W"),
        Sheet250kInfo("79 F", 23.0, 24.0, 89.0, 90.0, "Faridpur / Kushtia"),
        Sheet250kInfo("79 G", 22.0, 23.0, 89.0, 90.0, "Khulna / Bagerhat"),
        Sheet250kInfo("79 I", 23.0, 24.0, 90.0, 91.0, "Dhaka / Gazipur / Narayanganj"),
        Sheet250kInfo("79 J", 22.0, 23.0, 90.0, 91.0, "Barishal / Patuakhali"),
        Sheet250kInfo("79 M", 23.0, 24.0, 91.0, 92.0, "Cumilla / Brahmanbaria"),
        Sheet250kInfo("79 N", 22.0, 23.0, 91.0, 92.0, "Chittagong / Noakhali"),
        Sheet250kInfo("79 O", 21.0, 22.0, 91.0, 92.0, "Bay of Bengal Islands"),
        Sheet250kInfo("83 D", 24.0, 25.0, 92.0, 93.0, "Sylhet / Maulvibazar"),
        Sheet250kInfo("84 A", 23.0, 24.0, 92.0, 93.0, "CHT Khagrachari"),
        Sheet250kInfo("84 B", 22.0, 23.0, 92.0, 93.0, "CHT Rangamati / Bandarban"),
        Sheet250kInfo("84 C", 21.0, 22.0, 92.0, 93.0, "Cox's Bazar / Teknaf")
    )
}
