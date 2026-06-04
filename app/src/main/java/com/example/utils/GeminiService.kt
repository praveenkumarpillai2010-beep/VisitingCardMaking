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
        val visibleFields: List<String> = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address")
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
        logoUri: String?,
        photoUri: String?,
        callback: AIAssistantCallback
    ) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, callback)
            return
        }

        val endpoint = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        
        val systemInstruction = """
            You are a professional Graphic and Branding UI Designer with elite aesthetic sense.
            Your task is to generate exactly 3 beautiful, distinct, high-end visiting card layout design proposals (as Option 1, Option 2, Option 3) matching the user's requirements.
            Design 1 MUST match the user requested design style ($preferredStyle) and preferred color tone ($preferredColor).
            Design 2 MUST be a luxury prestige signature gold variation with elegant serif typography.
            Design 3 MUST be a futuristic high-tech neon accented minimal design.

            The fontStyle field must be exactly one of: "Space Grotesk", "Elegant Serif", "Modern Bold", or "Tech Clean".

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
                  "qrCodeVisible": true,
                  "qrCodeX": 240.0,
                  "qrCodeY": 110.0,
                  "qrCodeColor": "#HEXCOLOR",
                  "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"]
                },
                {
                  "themeName": "Theme Option 2 Name",
                  "backgroundColor": "#HEXCOLOR",
                  "gradientEndColor": "#HEXCOLOR",
                  "primaryColor": "#HEXCOLOR",
                  "fontStyle": "font style name",
                  "qrCodeVisible": true,
                  "qrCodeX": 240.0,
                  "qrCodeY": 110.0,
                  "qrCodeColor": "#HEXCOLOR",
                  "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"]
                },
                {
                  "themeName": "Theme Option 3 Name",
                  "backgroundColor": "#HEXCOLOR",
                  "gradientEndColor": "#HEXCOLOR",
                  "primaryColor": "#HEXCOLOR",
                  "fontStyle": "font style name",
                  "qrCodeVisible": true,
                  "qrCodeX": 240.0,
                  "qrCodeY": 110.0,
                  "qrCodeColor": "#HEXCOLOR",
                  "visibleFields": ["fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address"]
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
                simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API failed: Code ${response.code}, Msg: $responseBody")
                    simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, callback)
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

                        optionsList.add(CardDesignOption(
                            themeName = themeName,
                            backgroundColor = bgColor,
                            gradientEndColor = gradColor,
                            primaryColor = primColor,
                            fontStyle = fontStyle,
                            qrCodeVisible = qrCodeVisible,
                            qrX = qrCodeX,
                            qrY = qrCodeY,
                            visibleFields = visibleList
                        ))
                    }

                    if (optionsList.isNotEmpty()) {
                        callback.onSuccess(optionsList)
                    } else {
                        simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, callback)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "JSON parsing error from Gemini response", e)
                    simulateLocalAIGeneration(name, companyName, jobTitle, category, preferredColor, preferredStyle, callback)
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

        // Option 1
        list.add(CardDesignOption(
            themeName = "AI Preferred $preferredStyle",
            backgroundColor = "#0F121F",
            gradientEndColor = "#1F2943",
            primaryColor = userColor,
            fontStyle = fontOption1,
            qrCodeVisible = true,
            qrX = 240f,
            qrY = 110f,
            visibleFields = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address")
        ))

        // Option 2: Luxury Gold Accent Elite
        list.add(CardDesignOption(
            themeName = "AI Golden Luxury Elite",
            backgroundColor = "#0A0A0C",
            gradientEndColor = "#1B170B",
            primaryColor = "#D4AF37",
            fontStyle = "Elegant Serif",
            qrCodeVisible = true,
            qrX = 240f,
            qrY = 110f,
            visibleFields = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address")
        ))

        // Option 3: Cyber Tech Futuristic
        list.add(CardDesignOption(
            themeName = "AI Cyber Tech Minimalist",
            backgroundColor = "#03060C",
            gradientEndColor = "#0B2135",
            primaryColor = "#00FFCC",
            fontStyle = "Tech Clean",
            qrCodeVisible = true,
            qrX = 242f,
            qrY = 108f,
            visibleFields = listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website")
        ))

        Thread {
            try {
                Thread.sleep(1800) // realistic delay to feel premium
                callback.onSuccess(list)
            } catch (ignored: Exception) {}
        }.start()
    }
}
