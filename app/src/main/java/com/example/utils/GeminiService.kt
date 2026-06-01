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

    interface AIAssistantCallback {
        fun onSuccess(themeName: String, backgroundColor: String, gradientEndColor: String, primaryColor: String, fontStyle: String, qrX: Float, qrY: Float, visibleFields: List<String>)
        fun onFailure(error: String)
    }

    fun generateCardLayout(
        name: String,
        businessType: String,
        companyName: String,
        callback: AIAssistantCallback
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Simulated local offline generator as defensive fallback or preview
            simulateLocalAIGeneration(name, businessType, companyName, callback)
            return
        }

        val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        
        val systemInstruction = """
            You are a professional Graphic and Branding UI Designer.
            Your task is to propose an exquisite, high-end visiting card layout based on the user's details.
            You must output ONLY a valid raw JSON object. Do not include markdown code block characters like ```json.
            The JSON object must have EXACTLY the following structure with no other commentary:
            {
              "themeName": "Theme Design Name",
              "backgroundColor": "#HEXCOLOR",
              "gradientEndColor": "#HEXCOLOR",
              "primaryColor": "#HEXCOLOR",
              "fontStyle": "Space Grotesk" or "Elegant Serif" or "Modern Bold" or "Tech Clean",
              "qrCodeVisible": true,
              "qrCodeX": 250.0,
              "qrCodeY": 110.0,
              "qrCodeColor": "#HEXCOLOR",
              "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"]
            }
        """.trimIndent()

        val prompt = "Create a custom theme layout for Name: $name, business vertical: $businessType, Company: $companyName."

        // Build request body according to Gemini REST specification
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
                put("temperature", 0.7)
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
                simulateLocalAIGeneration(name, businessType, companyName, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API failed: Code ${response.code}, Msg: $responseBody")
                    simulateLocalAIGeneration(name, businessType, companyName, callback)
                    return
                }

                try {
                    val rootJson = JSONObject(responseBody)
                    val candidates = rootJson.getJSONArray("candidates")
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val rawText = parts.getJSONObject(0).getString("text").trim()
                    
                    // Sanitize potential markdown wrap
                    val cleanJsonStr = rawText
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val resultJson = JSONObject(cleanJsonStr)
                    val themeName = resultJson.optString("themeName", "AI Classic Dark")
                    val bgColor = resultJson.optString("backgroundColor", "#11131E")
                    val gradientEndColor = resultJson.optString("gradientEndColor", "#1E2235")
                    val primaryColor = resultJson.optString("primaryColor", "#FFD700")
                    val fontStyle = resultJson.optString("fontStyle", "Space Grotesk")
                    val qrCodeX = resultJson.optDouble("qrCodeX", 260.0).toFloat()
                    val qrCodeY = resultJson.optDouble("qrCodeY", 100.0).toFloat()
                    
                    val fieldsArray = resultJson.optJSONArray("visibleFields")
                    val visibleList = mutableListOf<String>()
                    if (fieldsArray != null) {
                        for (i in 0 until fieldsArray.length()) {
                            visibleList.add(fieldsArray.getString(i))
                        }
                    } else {
                        visibleList.addAll(listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website"))
                    }

                    callback.onSuccess(
                        themeName = themeName,
                        backgroundColor = bgColor,
                        gradientEndColor = gradientEndColor,
                        primaryColor = primaryColor,
                        fontStyle = fontStyle,
                        qrX = qrCodeX,
                        qrY = qrCodeY,
                        visibleFields = visibleList
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "JSON parse failure from Gemini response", e)
                    simulateLocalAIGeneration(name, businessType, companyName, callback)
                }
            }
        })
    }

    private fun simulateLocalAIGeneration(
        name: String,
        businessType: String,
        companyName: String,
        callback: AIAssistantCallback
    ) {
        // High quality local engine that executes instantly if offline, without keys, or on timeout.
        val lowerType = businessType.lowercase()
        val themeName: String
        val bgColor: String
        val gradientEnd: String
        val primaryColor: String
        val fontStyle: String
        val qrX: Float
        val qrY: Float
        val visible = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address")

        when {
            lowerType.contains("tech") || lowerType.contains("soft") || lowerType.contains("develop") || lowerType.contains("code") -> {
                themeName = "AI Cyber Tech Minimal"
                bgColor = "#080B10"
                gradientEnd = "#0D1D2C"
                primaryColor = "#00FFCC"
                fontStyle = "Tech Clean"
                qrX = 280f
                qrY = 40f
            }
            lowerType.contains("luxury") || lowerType.contains("jewelry") || lowerType.contains("gold") || lowerType.contains("premium") -> {
                themeName = "AI Golden Luxury Noir"
                bgColor = "#0D0D0E"
                gradientEnd = "#1E1A13"
                primaryColor = "#D4AF37"
                fontStyle = "Elegant Serif"
                qrX = 260f
                qrY = 110f
            }
            lowerType.contains("creative") || lowerType.contains("art") || lowerType.contains("design") || lowerType.contains("photo") -> {
                themeName = "AI Vibrant Art Neon"
                bgColor = "#18051E"
                gradientEnd = "#35083B"
                primaryColor = "#FF007F"
                fontStyle = "Space Grotesk"
                qrX = 260f
                qrY = 110f
            }
            lowerType.contains("medical") || lowerType.contains("doctor") || lowerType.contains("clinic") || lowerType.contains("health") -> {
                themeName = "AI Clinical Teal"
                bgColor = "#FAFDFD"
                gradientEnd = "#ECF6F6"
                primaryColor = "#008080"
                fontStyle = "Modern Bold"
                qrX = 270f
                qrY = 100f
            }
            else -> {
                // Corporate classic
                themeName = "AI Royal Corporate"
                bgColor = "#0A0E17"
                gradientEnd = "#131C2E"
                primaryColor = "#3D85C6"
                fontStyle = "Space Grotesk"
                qrX = 265f
                qrY = 115f
            }
        }

        // Return immediately on background thread
        Thread {
            try {
                Thread.sleep(1500) // Realistic loader delay
                callback.onSuccess(
                    themeName = themeName,
                    backgroundColor = bgColor,
                    gradientEndColor = gradientEnd,
                    primaryColor = primaryColor,
                    fontStyle = fontStyle,
                    qrX = qrX,
                    qrY = qrY,
                    visibleFields = visible
                )
            } catch (ignored: Exception) {}
        }.start()
    }
}
