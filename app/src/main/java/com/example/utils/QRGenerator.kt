package com.example.utils

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlin.math.abs

object QRGenerator {
    
    // Tries to decode a QR Code from a Bitmap. Returns the text of the QR code if successful.
    fun decodeQRCodeFromBitmap(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        
        try {
            val result = MultiFormatReader().decode(binaryBitmap)
            return result.text ?: ""
        } catch (e: Exception) {
            throw Exception("Unreadable QR barcode or image format: " + e.localizedMessage)
        }
    }
    
    // Generates a deterministic 21x21 QR Matrix for any input string
    fun generateQRMatrix(data: String): Array<BooleanArray> {
        val size = 21
        val matrix = Array(size) { BooleanArray(size) }
        val hash = data.hashCode()
        
        // 1. Draw Position Finder Patterns at three corners
        // Top-Left corner (0..6, 0..6)
        drawFinderPattern(matrix, 0, 0)
        // Top-Right corner (14..20, 0..6)
        drawFinderPattern(matrix, 14, 0)
        // Bottom-Left corner (0..6, 14..20)
        drawFinderPattern(matrix, 0, 14)

        // 2. Populate the rest with a deterministic pseudo-random hash algorithm
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip positional corners
                if ((r < 8 && c < 8) || (r < 8 && c > 12) || (r > 12 && c < 8)) {
                    continue
                }
                
                // Timing patterns (Row 6 and Column 6 dots alternating)
                if (r == 6 || c == 6) {
                    matrix[r][c] = (r + c) % 2 == 0
                    continue
                }
                
                // Deterministic fill based on hash & coordinates
                val coordSeed = r * 31 + c * 17
                val value = abs(hash xor coordSeed)
                matrix[r][c] = value % 3 == 0 || value % 7 == 0
            }
        }
        
        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, rowOffset: Int, colOffset: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                // 1. Solid outer 7x7 square
                val isOuterBorder = r == 0 || r == 6 || c == 0 || c == 6
                // 2. Solid inner 3x3 square
                val isInnerSquare = r >= 2 && r <= 4 && c >= 2 && c <= 4
                
                matrix[rowOffset + r][colOffset + c] = isOuterBorder || isInnerSquare
            }
        }
    }
}

@Composable
fun QRCodeComposable(
    data: String,
    sizeDp: Dp,
    colorHex: String,
    shape: String, // "SQUARE", "ROUNDED", "CIRCLE"
    modifier: Modifier = Modifier
) {
    val matrix = QRGenerator.generateQRMatrix(data)
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFFD4AF37) // Default Gold
    }

    Canvas(modifier = modifier.size(sizeDp)) {
        val totalSize = size.width
        val cellSize = totalSize / 21f

        for (r in 0 until 21) {
            for (c in 0 until 21) {
                if (matrix[r][c]) {
                    val px = c * cellSize
                    val py = r * cellSize
                    
                    // Fine padding adjustment for gapless appearance
                    val adjust = cellSize * 0.95f
                    
                    when (shape) {
                        "CIRCLE" -> {
                            drawCircle(
                                color = color,
                                radius = adjust / 2f,
                                center = Offset(px + cellSize / 2f, py + cellSize / 2f)
                            )
                        }
                        "ROUNDED" -> {
                            val isFinder = (r < 7 && c < 7) || (r < 7 && c > 13) || (r > 13 && c < 7)
                            val radius = if (isFinder) cellSize * 0.2f else cellSize * 0.35f
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(px, py),
                                size = Size(adjust, adjust),
                                cornerRadius = CornerRadius(radius, radius)
                            )
                        }
                        else -> { // "SQUARE"
                            drawRect(
                                color = color,
                                topLeft = Offset(px, py),
                                size = Size(adjust, adjust)
                            )
                        }
                    }
                }
            }
        }
    }
}
