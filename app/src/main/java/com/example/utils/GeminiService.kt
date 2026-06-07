package com.example.utils

import android.util.Log
import com.example.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class CardDesignOption(
        val themeName: String,
        val backgroundColor: String,
        val gradientEndColor: String,
        val primaryColor: String,
        val fontStyle: String,
        val qrCodeVisible: Boolean = true,
        val qrX: Float = 240f,
        val qrY: Float = 110f,
        val visibleFields: List<String> = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"),
        val cardShape: String = "ROUNDED_RECTANGLE",
        val layoutArrangement: String = "CLASSIC_REAR_QR",
        val fullNameX: Float? = null,
        val fullNameY: Float? = null,
        val fullNameSize: Float? = null,
        val jobTitleX: Float? = null,
        val jobTitleY: Float? = null,
        val jobTitleSize: Float? = null,
        val companyNameX: Float? = null,
        val companyNameY: Float? = null,
        val companyNameSize: Float? = null,
        val mobileX: Float? = null,
        val mobileY: Float? = null,
        val emailX: Float? = null,
        val emailY: Float? = null,
        val websiteX: Float? = null,
        val websiteY: Float? = null,
        val addressX: Float? = null,
        val addressY: Float? = null
    )

    interface AIAssistantCallback {
        fun onSuccess(options: List<CardDesignOption>)
        fun onFailure(error: String)
    }

    fun generateCardLayout(
        name: String,
        companyName: String,
        jobTitle: String,
        phoneNumber: String,
        email: String,
        website: String,
        address: String,
        category: String,
        preferredColor: String,
        preferredStyle: String,
        brandDescription: String,
        logoUri: String?,
        photoUri: String?,
        callback: AIAssistantCallback
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, brandDescription, callback)
            return
        }

        val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        
        val systemInstruction = """
            You are a professional Graphic and Branding UI Designer with elite aesthetic sense.
            Your task is to generate exactly 3 beautiful, distinct, high-end visiting card layout design proposals (as Option 1, Option 2, Option 3) matching the user's requirements and brand description.
            The user provides a brief description of their brand: "$brandDescription". You MUST integrate details, vibes, and themes implied by this brand description into the generated options. Design professional card layouts, color schemes, and layouts centered on this brand brief.

            Design 1 MUST match the user requested design style ($preferredStyle) and preferred color tone ($preferredColor) while incorporating elements of the brand description ($brandDescription).
            Design 2 MUST represent elite prestige branding based on the brand description ($brandDescription). Make this custom tailored to their brand vibe with high-end palettes (e.g., if brand is tea/nature, use forest greens or earthy clay; if it's fintech/wealth, use deep blue with cyber gold; if it's sweet treats, use pastel lavender & pink; if it's clean wellness, use soothing mint or teal, etc.).
            Design 3 MUST be a futuristic high-tech neon accented minimal design or custom theme inspired uniquely by the brand description ($brandDescription).

            The fontStyle field must be exactly one of: "Space Grotesk", "Elegant Serif", "Modern Bold", or "Tech Clean".
            The cardShape field must be exactly one of: "ROUNDED_RECTANGLE", "LEAF_CUT", "HEXAGON", or "RECTANGLE".
            The layoutArrangement field must be exactly one of: "CLASSIC_REAR_QR", "CENTER_MINIMALIST", "MODERN_SPLIT", or "HORIZONTAL_DENSITY".

            Specify coordinate placements (values in Float from 0.0 to 360.0 for X, and 0.0 to 220.0 for Y) and sizes (e.g. fullNameSize: 16.0 to 22.0, jobTitleSize: 9.0 to 12.0) that fit the chosen arrangement and prevent overlapping.
            Ensure standard layout arrangements match these definitions if custom coordinates are not suggested, but you should provide exact custom values for perfect balance.
            Card boundaries: Width is 360.0 (X range 0.0 to 360.0), Height is 220.0 (Y range 0.0 to 220.0).

            You must output ONLY a valid raw JSON object. Do not include markdown code block characters like ```json.
            The JSON object must have EXACTLY the following structure with no other commentary or wrap:
            {
              "options": [
                {
                  "themeName": "Theme Option 1 Name",
                  "backgroundColor": "#HEXCOLOR",
                  "gradientEndColor": "#HEXCOLOR",
                  "primaryColor": "#HEXCOLOR",
                  "fontStyle": "font style name",
                  "cardShape": "ROUNDED_RECTANGLE",
                  "layoutArrangement": "CLASSIC_REAR_QR",
                  "qrCodeVisible": true,
                  "qrCodeX": 260.0,
                  "qrCodeY": 70.0,
                  "qrCodeColor": "#HEXCOLOR",
                  "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"],
                  "fullNameX": 20.0,
                  "fullNameY": 25.0,
                  "fullNameSize": 19.0,
                  "jobTitleX": 20.0,
                  "jobTitleY": 50.0,
                  "jobTitleSize": 10.0,
                  "companyNameX": 20.0,
                  "companyNameY": 75.0,
                  "companyNameSize": 12.0,
                  "mobileX": 20.0,
                  "mobileY": 115.0,
                  "emailX": 20.0,
                  "emailY": 135.0,
                  "websiteX": 20.0,
                  "websiteY": 155.0,
                  "addressX": 20.0,
                  "addressY": 175.0
                },
                {
                  "themeName": "Theme Option 2 Name",
                  "backgroundColor": "#HEXCOLOR",
                  "gradientEndColor": "#HEXCOLOR",
                  "primaryColor": "#HEXCOLOR",
                  "fontStyle": "font style name",
                  "cardShape": "LEAF_CUT",
                  "layoutArrangement": "CENTER_MINIMALIST",
                  "qrCodeVisible": true,
                  "qrCodeX": 140.0,
                  "qrCodeY": 110.0,
                  "qrCodeColor": "#HEXCOLOR",
                  "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"],
                  "fullNameX": 90.0,
                  "fullNameY": 65.0,
                  "fullNameSize": 20.0,
                  "jobTitleX": 110.0,
                  "jobTitleY": 90.0,
                  "jobTitleSize": 10.0,
                  "companyNameX": 105.0,
                  "companyNameY": 35.0,
                  "companyNameSize": 12.0,
                  "mobileX": 35.0,
                  "mobileY": 185.0,
                  "emailX": 130.0,
                  "emailY": 185.0,
                  "websiteX": 225.0,
                  "websiteY": 185.0,
                  "addressX": 105.0,
                  "addressY": 205.0
                },
                {
                  "themeName": "Theme Option 3 Name",
                  "backgroundColor": "#HEXCOLOR",
                  "gradientEndColor": "#HEXCOLOR",
                  "primaryColor": "#HEXCOLOR",
                  "fontStyle": "font style name",
                  "cardShape": "HEXAGON",
                  "layoutArrangement": "MODERN_SPLIT",
                  "qrCodeVisible": true,
                  "qrCodeX": 35.0,
                  "qrCodeY": 65.0,
                  "qrCodeColor": "#HEXCOLOR",
                  "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website"],
                  "fullNameX": 160.0,
                  "fullNameY": 50.0,
                  "fullNameSize": 19.0,
                  "jobTitleX": 160.0,
                  "jobTitleY": 72.0,
                  "jobTitleSize": 10.0,
                  "companyNameX": 160.0,
                  "companyNameY": 25.0,
                  "companyNameSize": 12.0,
                  "mobileX": 160.0,
                  "mobileY": 115.0,
                  "emailX": 160.0,
                  "emailY": 135.0,
                  "websiteX": 160.0,
                  "websiteY": 155.0,
                  "addressX": 160.0,
                  "addressY": 175.0
                }
              ]
            }
        """.trimIndent()

        val prompt = """
            Generate 3 visiting card layouts for:
            - Full Name: $name
            - Company: $companyName
            - Job Title: $jobTitle
            - Category: $category
            - Brand Description: $brandDescription
            - Preferred Color: $preferredColor
            - Preferred Style: $preferredStyle
            - Has Custom Logo: ${if (logoUri != null) "Yes" else "No"}
            - Has Custom Profile Photo: ${if (photoUri != null) "Yes" else "No"}
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.75)
            })
        }

        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network failure: ${e.message}", e)
                simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, brandDescription, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API failed: Code ${response.code}, Msg: $responseBody")
                    simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, brandDescription, callback)
                    return
                }

                try {
                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val rawText = parts.getJSONObject(0).getString("text").trim()
                    
                    val cleanJsonStr = rawText
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val resultJson = JSONObject(cleanJsonStr)
                    val jsonOptions = resultJson.getJSONArray("options")
                    val optionsList = mutableListOf<CardDesignOption>()

                    for (i in 0 until jsonOptions.length()) {
                        val obj = jsonOptions.getJSONObject(i)
                        val themeName = obj.optString("themeName", "AI Option ${i + 1}")
                        val bgColor = obj.optString("backgroundColor", "#11131E")
                        val gradColor = obj.optString("gradientEndColor", "#1E2235")
                        val primColor = obj.optString("primaryColor", "#FFD700")
                        val fontStyle = obj.optString("fontStyle", "Space Grotesk")
                        val qrCodeVisible = obj.optBoolean("qrCodeVisible", true)
                        val qrCodeX = obj.optDouble("qrCodeX", 240.0).toFloat()
                        val qrCodeY = obj.optDouble("qrCodeY", 110.0).toFloat()

                        val fieldsArr = obj.optJSONArray("visibleFields")
                        val visibleList = mutableListOf<String>()
                        if (fieldsArr != null) {
                            for (j in 0 until fieldsArr.length()) {
                                visibleList.add(fieldsArr.getString(j))
                            }
                        } else {
                            visibleList.addAll(listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website"))
                        }

                        val cardShape = obj.optString("cardShape", "ROUNDED_RECTANGLE")
                        val layoutArrangement = obj.optString("layoutArrangement", "CLASSIC_REAR_QR")
                        
                        val fullNameX = if (obj.has("fullNameX")) obj.optDouble("fullNameX").toFloat() else null
                        val fullNameY = if (obj.has("fullNameY")) obj.optDouble("fullNameY").toFloat() else null
                        val fullNameSize = if (obj.has("fullNameSize")) obj.optDouble("fullNameSize").toFloat() else null
                        val jobTitleX = if (obj.has("jobTitleX")) obj.optDouble("jobTitleX").toFloat() else null
                        val jobTitleY = if (obj.has("jobTitleY")) obj.optDouble("jobTitleY").toFloat() else null
                        val jobTitleSize = if (obj.has("jobTitleSize")) obj.optDouble("jobTitleSize").toFloat() else null
                        val companyNameX = if (obj.has("companyNameX")) obj.optDouble("companyNameX").toFloat() else null
                        val companyNameY = if (obj.has("companyNameY")) obj.optDouble("companyNameY").toFloat() else null
                        val companyNameSize = if (obj.has("companyNameSize")) obj.optDouble("companyNameSize").toFloat() else null
                        val mobileX = if (obj.has("mobileX")) obj.optDouble("mobileX").toFloat() else null
                        val mobileY = if (obj.has("mobileY")) obj.optDouble("mobileY").toFloat() else null
                        val emailX = if (obj.has("emailX")) obj.optDouble("emailX").toFloat() else null
                        val emailY = if (obj.has("emailY")) obj.optDouble("emailY").toFloat() else null
                        val websiteX = if (obj.has("websiteX")) obj.optDouble("websiteX").toFloat() else null
                        val websiteY = if (obj.has("websiteY")) obj.optDouble("websiteY").toFloat() else null
                        val addressX = if (obj.has("addressX")) obj.optDouble("addressX").toFloat() else null
                        val addressY = if (obj.has("addressY")) obj.optDouble("addressY").toFloat() else null

                        optionsList.add(CardDesignOption(
                            themeName = themeName,
                            backgroundColor = bgColor,
                            gradientEndColor = gradColor,
                            primaryColor = primColor,
                            fontStyle = fontStyle,
                            qrCodeVisible = qrCodeVisible,
                            qrX = qrCodeX,
                            qrY = qrCodeY,
                            visibleFields = visibleList,
                            cardShape = cardShape,
                            layoutArrangement = layoutArrangement,
                            fullNameX = fullNameX,
                            fullNameY = fullNameY,
                            fullNameSize = fullNameSize,
                            jobTitleX = jobTitleX,
                            jobTitleY = jobTitleY,
                            jobTitleSize = jobTitleSize,
                            companyNameX = companyNameX,
                            companyNameY = companyNameY,
                            companyNameSize = companyNameSize,
                            mobileX = mobileX,
                            mobileY = mobileY,
                            emailX = emailX,
                            emailY = emailY,
                            websiteX = websiteX,
                            websiteY = websiteY,
                            addressX = addressX,
                            addressY = addressY
                        ))
                    }

                    if (optionsList.isNotEmpty()) {
                        callback.onSuccess(optionsList)
                    } else {
                        simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, brandDescription, callback)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "JSON parsing error from Gemini response", e)
                    simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, brandDescription, callback)
                }
            }
        })
    }

    private fun simulateLocalAIGeneration(
        name: String,
        companyName: String,
        jobTitle: String,
        category: String,
        preferredColor: String,
        preferredStyle: String,
        brandDescription: String,
        callback: AIAssistantCallback
    ) {
        val userColor = if (preferredColor.startsWith("#") && preferredColor.length == 7) {
            preferredColor
        } else {
            when (preferredColor.lowercase()) {
                "red" -> "#E53935"
                "blue" -> "#1E88E5"
                "green" -> "#43A047"
                "gold" -> "#D4AF37"
                "purple" -> "#8E24AA"
                "cyan" -> "#00ACC1"
                "orange" -> "#F4511E"
                "silver" -> "#B0BEC5"
                else -> "#D4AF37" // default Gold accent
            }
        }

        // Generate 3 elegant, customized falling options locally
        val list = mutableListOf<CardDesignOption>()

        // Option 1: Style / category match
        val fontOption1 = when (preferredStyle.lowercase()) {
            "luxury", "elegant" -> "Elegant Serif"
            "minimal" -> "Space Grotesk"
            "technology" -> "Tech Clean"
            "professional", "corporate" -> "Modern Bold"
            else -> "Space Grotesk"
        }

        var localColor1 = userColor
        var localBg1 = "#0F121F"
        var localBgEnd1 = "#1F2943"
        var localThemeName1 = "AI Preferred $preferredStyle Theme"
        var localFont1 = fontOption1

        var localColor2 = "#D4AF37"
        var localBg2 = "#0A0A0C"
        var localBgEnd2 = "#1B170B"
        var localThemeName2 = "AI Golden Luxury Elite"
        var localFont2 = "Elegant Serif"

        var localColor3 = "#00FFCC"
        var localBg3 = "#03060C"
        var localBgEnd3 = "#0B2135"
        var localThemeName3 = "AI Cyber Tech Minimalist"
        var localFont3 = "Tech Clean"

        val descLower = brandDescription.lowercase()
        if (descLower.contains("organic") || descLower.contains("eco") || descLower.contains("nature") || descLower.contains("green") || descLower.contains("tea") || descLower.contains("coffee") || descLower.contains("garden")) {
            localColor1 = "#2ECC71" // Emerald
            localBg1 = "#0E1A14"
            localBgEnd1 = "#1B3327"
            localThemeName1 = "Dynamic Organic Garden"
            localFont1 = "Space Grotesk"

            localColor2 = "#8D6E63" // Coffee/Earthy Brown
            localBg2 = "#15100E"
            localBgEnd2 = "#2E1F1A"
            localThemeName2 = "Warm Roasted Prestige"
            localFont2 = "Elegant Serif"
        } else if (descLower.contains("cyber") || descLower.contains("tech") || descLower.contains("ai") || descLower.contains("crypt") || descLower.contains("software") || descLower.contains("digital") || descLower.contains("app")) {
            localColor1 = "#00E5FF" // Cool Cyan
            localBg1 = "#060A13"
            localBgEnd1 = "#0D182E"
            localThemeName1 = "AI Cognitive Matrix"
            localFont1 = "Tech Clean"

            localColor2 = "#D4AF37"
            localBg2 = "#110D05"
            localBgEnd2 = "#261D0A"
            localThemeName2 = "Elite FinTech Premium"
            localFont2 = "Elegant Serif"
        } else if (descLower.contains("health") || descLower.contains("doctor") || descLower.contains("clinic") || descLower.contains("medical") || descLower.contains("dentist") || descLower.contains("physio") || descLower.contains("care")) {
            localColor1 = "#00B8D4" // Clean Teal
            localBg1 = "#061313"
            localBgEnd1 = "#0E2B2B"
            localThemeName1 = "Clinical WellCare"
            localFont1 = "Modern Bold"
        } else if (descLower.contains("art") || descLower.contains("design") || descLower.contains("fashion") || descLower.contains("creative") || descLower.contains("music") || descLower.contains("beauty") || descLower.contains("styl")) {
            localColor1 = "#FF4081" // Vibrant Pink
            localBg1 = "#1A0510"
            localBgEnd1 = "#3D0C26"
            localThemeName1 = "Avant-Garde Studio"
            localFont1 = "Space Grotesk"

            localColor3 = "#E040FB" // Neon Purple
            localBg3 = "#100015"
            localBgEnd3 = "#260033"
            localThemeName3 = "Synthwave Neon Edge"
            localFont3 = "Tech Clean"
        }

        // Option 1
        list.add(CardDesignOption(
            themeName = localThemeName1,
            backgroundColor = localBg1,
            gradientEndColor = localBgEnd1,
            primaryColor = localColor1,
            fontStyle = localFont1,
            qrCodeVisible = true,
            qrX = 260f,
            qrY = 70f,
            visibleFields = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"),
            cardShape = "ROUNDED_RECTANGLE",
            layoutArrangement = "CLASSIC_REAR_QR",
            fullNameX = 20f, fullNameY = 25f, fullNameSize = 19f,
            jobTitleX = 20f, jobTitleY = 50f, jobTitleSize = 10f,
            companyNameX = 20f, companyNameY = 75f, companyNameSize = 12f,
            mobileX = 20f, mobileY = 115f,
            emailX = 20f, emailY = 135f,
            websiteX = 20f, websiteY = 155f,
            addressX = 20f, addressY = 175f
        ))

        // Option 2
        list.add(CardDesignOption(
            themeName = localThemeName2,
            backgroundColor = localBg2,
            gradientEndColor = localBgEnd2,
            primaryColor = localColor2,
            fontStyle = localFont2,
            qrCodeVisible = true,
            qrX = 140f,
            qrY = 110f,
            visibleFields = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"),
            cardShape = "LEAF_CUT",
            layoutArrangement = "CENTER_MINIMALIST",
            fullNameX = 90f, fullNameY = 65f, fullNameSize = 20f,
            jobTitleX = 110f, jobTitleY = 90f, jobTitleSize = 10f,
            companyNameX = 105f, companyNameY = 35f, companyNameSize = 12f,
            mobileX = 35f, mobileY = 185f,
            emailX = 130f, emailY = 185f,
            websiteX = 225f, websiteY = 185f,
            addressX = 105f, addressY = 205f
        ))

        // Option 3
        list.add(CardDesignOption(
            themeName = localThemeName3,
            backgroundColor = localBg3,
            gradientEndColor = localBgEnd3,
            primaryColor = localColor3,
            fontStyle = localFont3,
            qrCodeVisible = true,
            qrX = 35f,
            qrY = 65f,
            visibleFields = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website"),
            cardShape = "HEXAGON",
            layoutArrangement = "MODERN_SPLIT",
            fullNameX = 160f, fullNameY = 50f, fullNameSize = 19f,
            jobTitleX = 160f, jobTitleY = 72f, jobTitleSize = 10f,
            companyNameX = 160f, companyNameY = 25f, companyNameSize = 12f,
            mobileX = 160f, mobileY = 115f,
            emailX = 160f, emailY = 135f,
            websiteX = 160f, websiteY = 155f,
            addressX = 160f, addressY = 175f
        ))

        Thread {
            try {
                Thread.sleep(1800) // realistic delay to feel premium
                callback.onSuccess(list)
            } catch (ignored: Exception) {}
        }.start()
    }
}
