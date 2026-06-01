package com.example.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.DesignElement
import com.example.data.UserCard
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

object PDFExporter {

    private data class ExportLayer(
        val zIndex: Int,
        val draw: (Canvas, Float, Float) -> Unit
    )

    private fun getDimensions(quality: String): Pair<Int, Int> {
        return when (quality.uppercase()) {
            "HD" -> Pair(1600, 960)
            "ULTRA HD" -> Pair(3200, 1920)
            else -> Pair(800, 480) // Standard Quality
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String?): Bitmap? {
        if (base64Str.isNullOrEmpty()) return null
        return try {
            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // Main draw routine to render a UserCard to an Android Bitmap
    fun drawCardToBitmap(context: Context, card: UserCard, quality: String): Bitmap {
        val (width, height) = getDimensions(quality)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Scale factors for translating coordinates from original design scale (400x240 units)
        val scaleX = width / 400f
        val scaleY = height / 240f

        // 1. Draw Background
        val bgPaint = Paint().apply { isAntiAlias = true }
        if (card.backgroundType == "GRADIENT") {
            val startColor = try { Color.parseColor(card.backgroundColor) } catch (e: Exception) { Color.parseColor("#10121A") }
            val endColor = try { Color.parseColor(card.gradientEndColor) } catch (e: Exception) { Color.parseColor("#1E2130") }
            bgPaint.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                startColor, endColor, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        } else {
            val sColor = try { Color.parseColor(card.backgroundColor) } catch (e: Exception) { Color.parseColor("#10121A") }
            canvas.drawColor(sColor)
        }

        // 2. Draw Borders and Branding Decoration
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f * scaleX
            color = try { Color.parseColor(card.qrCodeColor) } catch (e: Exception) { Color.parseColor("#D4AF37") }
        }
        
        when (card.borderStyle) {
            "MODERN_DOUBLE" -> {
                canvas.drawRect(12f * scaleX, 12f * scaleY, (width - 12f * scaleX), (height - 12f * scaleY), borderPaint)
                borderPaint.strokeWidth = 1f * scaleX
                canvas.drawRect(18f * scaleX, 18f * scaleY, (width - 18f * scaleX), (height - 18f * scaleY), borderPaint)
            }
            "MINIMAL_GOLD" -> {
                canvas.drawRect(15f * scaleX, 15f * scaleY, (width - 15f * scaleX), (height - 15f * scaleY), borderPaint)
            }
            "CYBER_SLATE" -> {
                val linePaint = Paint().apply {
                    isAntiAlias = true
                    color = Color.parseColor("#00FFCC")
                    strokeWidth = 4f * scaleX
                }
                canvas.drawLine(0f, 0f, 60f * scaleX, 0f, linePaint)
                canvas.drawLine(0f, 0f, 0f, 60f * scaleY, linePaint)
                canvas.drawLine(width.toFloat(), height.toFloat(), width - 60f * scaleX, height.toFloat(), linePaint)
                canvas.drawLine(width.toFloat(), height.toFloat(), width.toFloat(), height - 60f * scaleY, linePaint)
            }
        }

        // 3. Render Formatted Contact Fields dynamically based on Visibility
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }

        val visibleFields = try {
            val list = mutableListOf<String>()
            val array = JSONArray(card.visibleFieldsJson)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website")
        }

        val primaryColorInt = try { Color.parseColor(card.qrCodeColor) } catch (e: Exception) { Color.parseColor("#D4AF37") }

        // Compile layers to draw in strict Z-Index order
        val layers = mutableListOf<ExportLayer>()

        if (visibleFields.contains("fullName")) {
            layers.add(ExportLayer(card.fullNameZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = Color.WHITE
                    textSize = card.fullNameSize * sY * card.fullNameScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val x = card.fullNameX * sX
                val y = card.fullNameY * sY
                canv.save()
                canv.rotate(card.fullNameRotation, x, y)
                canv.drawText(card.fullName, x, y, textPaint)
                canv.restore()
            })
        }

        if (visibleFields.contains("jobTitle")) {
            layers.add(ExportLayer(card.jobTitleZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = primaryColorInt
                    textSize = card.jobTitleSize * sY * card.jobTitleScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val x = card.jobTitleX * sX
                val y = card.jobTitleY * sY
                canv.save()
                canv.rotate(card.jobTitleRotation, x, y)
                canv.drawText(card.jobTitle.uppercase(), x, y, textPaint)
                canv.restore()
            })
        }

        if (visibleFields.contains("companyName")) {
            layers.add(ExportLayer(card.companyNameZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = Color.LTGRAY
                    textSize = card.companyNameSize * sY * card.companyNameScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val x = card.companyNameX * sX
                val y = card.companyNameY * sY
                canv.save()
                canv.rotate(card.companyNameRotation, x, y)
                canv.drawText(card.companyName, x, y, textPaint)
                canv.restore()
            })
        }

        if (visibleFields.contains("mobileNumber")) {
            layers.add(ExportLayer(card.mobileNumberZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = Color.WHITE
                    textSize = card.mobileNumberSize * sY * card.mobileNumberScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val x = card.mobileNumberX * sX
                val y = card.mobileNumberY * sY
                canv.save()
                canv.rotate(card.mobileNumberRotation, x, y)
                canv.drawText("📞 " + card.mobileNumber, x, y, textPaint)
                canv.restore()
            })
        }

        if (visibleFields.contains("email")) {
            layers.add(ExportLayer(card.emailZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = Color.WHITE
                    textSize = card.emailSize * sY * card.emailScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val x = card.emailX * sX
                val y = card.emailY * sY
                canv.save()
                canv.rotate(card.emailRotation, x, y)
                canv.drawText("✉️ " + card.email, x, y, textPaint)
                canv.restore()
            })
        }

        if (visibleFields.contains("website")) {
            layers.add(ExportLayer(card.websiteZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = Color.WHITE
                    textSize = card.websiteSize * sY * card.websiteScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val x = card.websiteX * sX
                val y = card.websiteY * sY
                canv.save()
                canv.rotate(card.websiteRotation, x, y)
                canv.drawText("🌐 " + card.website, x, y, textPaint)
                canv.restore()
            })
        }

        if (visibleFields.contains("address")) {
            layers.add(ExportLayer(card.addressZIndex) { canv, sX, sY ->
                textPaint.apply {
                    color = Color.WHITE
                    textSize = card.addressSize * sY * card.addressScale
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val x = card.addressX * sX
                val y = card.addressY * sY
                canv.save()
                canv.rotate(card.addressRotation, x, y)
                canv.drawText("📍 " + card.address, x, y, textPaint)
                canv.restore()
            })
        }

        // Render QR Code Layer
        if (card.qrCodeVisible) {
            layers.add(ExportLayer(card.qrCodeZIndex) { canv, sX, sY ->
                val qrX = card.qrCodeX * sX
                val qrY = card.qrCodeY * sY
                val qrSize = card.qrCodeSize * sX
                canv.save()
                canv.rotate(card.qrCodeRotation, qrX + qrSize / 2f, qrY + qrSize / 2f)
                
                if (card.qrCodeType == "UPLOADED" && !card.qrCodeBase64Image.isNullOrEmpty()) {
                    val uploadBmp = decodeBase64ToBitmap(card.qrCodeBase64Image)
                    if (uploadBmp != null) {
                        val destRect = RectF(qrX, qrY, qrX + qrSize, qrY + qrSize)
                        val qrPaint = Paint().apply { isAntiAlias = true }
                        canv.drawBitmap(uploadBmp, null, destRect, qrPaint)
                    }
                } else {
                    drawQRCodeToCanvas(
                        canv,
                        card.qrCodeData,
                        qrX,
                        qrY,
                        qrSize,
                        primaryColorInt,
                        card.qrCodeShape
                    )
                }
                canv.restore()
            })
        }

        // Custom elements parsing and execution
        try {
            val elements = parseDesignElements(card.designElementsJson)
            for (elem in elements) {
                layers.add(ExportLayer(elem.zIndex) { canv, sX, sY ->
                    val elemPaint = Paint().apply { isAntiAlias = true }
                    if (elem.type == "TEXT") {
                        elemPaint.apply {
                            color = Color.parseColor(elem.color)
                            textSize = elem.fontSize * sY * elem.scale
                            typeface = if (elem.isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                        }
                        val x = elem.x * sX
                        val y = elem.y * sY
                        canv.save()
                        canv.rotate(elem.rotation, x, y)
                        canv.drawText(elem.content, x, y, elemPaint)
                        canv.restore()
                    } else if (elem.type == "SHAPE") {
                        elemPaint.apply {
                            color = Color.parseColor(elem.color)
                            style = Paint.Style.FILL
                        }
                        val x = elem.x * sX
                        val y = elem.y * sY
                        val eWidth = 40f * elem.scale * sX
                        val eHeight = 40f * elem.scale * sY
                        canv.save()
                        canv.rotate(elem.rotation, x + eWidth / 2f, y + eHeight / 2f)
                        canv.drawRect(x, y, x + eWidth, y + eHeight, elemPaint)
                        canv.restore()
                    } else if (elem.type == "UPLOADED_IMAGE_QR") {
                        val qrX = elem.x * sX
                        val qrY = elem.y * sY
                        val qrSize = 50f * elem.scale * sX
                        canv.save()
                        canv.rotate(elem.rotation, qrX + qrSize / 2f, qrY + qrSize / 2f)
                        val uploadBmp = decodeBase64ToBitmap(elem.content)
                        if (uploadBmp != null) {
                            val destRect = RectF(qrX, qrY, qrX + qrSize, qrY + qrSize)
                            canv.drawBitmap(uploadBmp, null, destRect, elemPaint)
                        }
                        canv.restore()
                    } else if (elem.type == "QR_CODE") {
                        val qrX = elem.x * sX
                        val qrY = elem.y * sY
                        val qrSize = 50f * elem.scale * sX
                        canv.save()
                        canv.rotate(elem.rotation, qrX + qrSize / 2f, qrY + qrSize / 2f)
                        drawQRCodeToCanvas(
                            canv,
                            elem.content,
                            qrX,
                            qrY,
                            qrSize,
                            Color.parseColor(elem.color),
                            "ROUNDED"
                        )
                        canv.restore()
                    }
                })
            }
        } catch (ignored: Exception) {}

        // Draw background decoration outline layer
        layers.sortBy { it.zIndex }
        for (layer in layers) {
            layer.draw(canvas, scaleX, scaleY)
        }

        // 6. Draw Watermark if account type is free
        if (!card.isPremium) {
            val waterPaint = Paint().apply {
                isAntiAlias = true
                color = Color.argb(80, 255, 255, 255)
                textSize = 7f * scaleY
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            canvas.drawText("Made by Pillai\'Play Card Maker", 25f * scaleX, height - 15f * scaleY, waterPaint)
        }

        return bitmap
    }

    private fun drawQRCodeToCanvas(
        canvas: Canvas,
        data: String,
        startX: Float,
        startY: Float,
        size: Float,
        colorInt: Int,
        shape: String
    ) {
        val matrix = QRGenerator.generateQRMatrix(data)
        val paint = Paint().apply {
            isAntiAlias = true
            color = colorInt
            style = Paint.Style.FILL
        }

        val cell = size / 21f
        val gap = cell * 0.95f

        for (r in 0 until 21) {
            for (c in 0 until 21) {
                if (matrix[r][c]) {
                    val px = startX + c * cell
                    val py = startY + r * cell

                    when (shape) {
                        "CIRCLE" -> {
                            canvas.drawCircle(px + cell / 2f, py + cell / 2f, gap / 2f, paint)
                        }
                        "ROUNDED" -> {
                            val rRect = RectF(px, py, px + gap, py + gap)
                            val isFinder = (r < 7 && c < 7) || (r < 7 && c > 13) || (r > 13 && c < 7)
                            val rad = if (isFinder) cell * 0.2f else cell * 0.35f
                            canvas.drawRoundRect(rRect, rad, rad, paint)
                        }
                        else -> { // SQUARE
                            canvas.drawRect(px, py, px + gap, py + gap, paint)
                        }
                    }
                }
            }
        }
    }

    private fun parseDesignElements(jsonStr: String): List<DesignElement> {
        val list = mutableListOf<DesignElement>()
        if (jsonStr.isEmpty() || jsonStr == "[]") return list
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DesignElement(
                        id = obj.getString("id"),
                        type = obj.getString("type"),
                        name = obj.optString("name", ""),
                        content = obj.getString("content"),
                        x = obj.getDouble("x").toFloat(),
                        y = obj.getDouble("y").toFloat(),
                        color = obj.optString("color", "#FFFFFF"),
                        fontSize = obj.optString("fontSize", "14").toFloat(),
                        isBold = obj.optBoolean("isBold", false),
                        isItalic = obj.optBoolean("isItalic", false),
                        isUnderline = obj.optBoolean("isUnderline", false),
                        rotation = obj.optDouble("rotation", 0.0).toFloat(),
                        scale = obj.optDouble("scale", 1.0).toFloat(),
                        zIndex = obj.optInt("zIndex", 0)
                    )
                )
            }
        } catch (ignored: Exception) {}
        return list
    }

    private fun serializeDesignElements(list: List<DesignElement>): String {
        val array = JSONArray()
        for (elem in list) {
            val obj = org.json.JSONObject().apply {
                put("id", elem.id)
                put("type", elem.type)
                put("name", elem.name)
                put("content", elem.content)
                put("x", elem.x)
                put("y", elem.y)
                put("color", elem.color)
                put("fontSize", elem.fontSize)
                put("isBold", elem.isBold)
                put("isItalic", elem.isItalic)
                put("isUnderline", elem.isUnderline)
                put("rotation", elem.rotation)
                put("scale", elem.scale)
                put("zIndex", elem.zIndex)
            }
            array.put(obj)
        }
        return array.toString()
    }

    // MAIN FILE WRITER
    fun exportCardToFile(
        context: Context,
        card: UserCard,
        format: String, // "PNG", "JPG", "PDF"
        quality: String // "STANDARD", "HD", "ULTRA HD"
    ): File? {
        val bitmap = drawCardToBitmap(context, card, quality)
        val timestamp = System.currentTimeMillis()
        val sanitName = card.cardName.replace(" ", "_").lowercase()
        
        val directory = context.cacheDir
        
        return when (format.uppercase()) {
            "PDF" -> {
                val file = File(directory, "${sanitName}_$timestamp.pdf")
                try {
                    val pdfDocument = PdfDocument()
                    val (w, h) = getDimensions(quality)
                    val pageInfo = PdfDocument.PageInfo.Builder(w, h, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)

                    FileOutputStream(file).use { out ->
                        pdfDocument.writeTo(out)
                    }
                    pdfDocument.close()
                    file
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            "JPG" -> {
                val file = File(directory, "${sanitName}_$timestamp.jpg")
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    file
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            else -> { // PNG
                val file = File(directory, "${sanitName}_$timestamp.png")
                try {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    file
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }
}
