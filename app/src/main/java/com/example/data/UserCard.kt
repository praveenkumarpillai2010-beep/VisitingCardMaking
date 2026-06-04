package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_cards")
data class UserCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardName: String,
    val templateId: String = "vibe_modern_gold",
    val themeName: String = "Gold Luxury",
    val isPremium: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    
    // User editable details
    val fullName: String = "Pillai Play",
    val jobTitle: String = "Chief Executive Officer",
    val companyName: String = "Pillai\'Play Entertainment",
    val mobileNumber: String = "+91 98765 43210",
    val whatsAppNumber: String = "+91 98765 43210",
    val email: String = "hello@pillaiplay.com",
    val website: String = "www.pillaiplay.com",
    val address: String = "Navi Mumbai, Maharashtra, India",
    
    // Social links
    val facebook: String = "pillaiplay",
    val instagram: String = "pillaiplay",
    val linkedIn: String = "pillaiplay",
    val youtube: String = "pillaiplay",
    val telegram: String = "pillaiplay",
    
    // Visibility structure (JSON formatted list of visible keys)
    val visibleFieldsJson: String = "[\"fullName\",\"jobTitle\",\"companyName\",\"mobileNumber\",\"email\",\"website\",\"address\"]",
    
    // Primary QR Code parameters
    var qrCodeType: String = "WEBSITE", // WEBSITE, WHATSAPP, EMAIL, PHONE, SOCIAL, UPLOADED
    var qrCodeData: String = "https://pillaiplay.com",
    var qrCodeColor: String = "#D4AF37", // Gold
    var qrCodeShape: String = "ROUNDED", // SQUARE, ROUNDED, CIRCLE
    var qrCodeSize: Float = 80f,
    var qrCodeX: Float = 240f,
    var qrCodeY: Float = 110f,
    var qrCodeVisible: Boolean = true,
    var qrCodeRotation: Float = 0f,
    var qrCodeZIndex: Int = 8,
    var qrCodeBase64Image: String? = null, // Holds cropped uploaded barcode image if CUSTOM QR
    
    // Background info
    val backgroundColor: String = "#10121A",
    val gradientEndColor: String = "#1D2130",
    val backgroundType: String = "GRADIENT", // SOLID, GRADIENT, PATTERN
    val backgroundImage: String = "gradient_golden",
    val fontStyle: String = "Space Grotesk",
    val borderStyle: String = "MINIMAL_GOLD",
    
    // Custom elements (serialized JSON list of DesignElement)
    val designElementsJson: String = "[]",

    // Snap to grid setting
    val snapToGrid: Boolean = false,

    // Coordinates, scales, rotations, and zIndex values of ALL native layers
    val fullNameX: Float = 25f,
    val fullNameY: Float = 25f,
    val fullNameScale: Float = 1.0f,
    val fullNameRotation: Float = 0f,
    val fullNameSize: Float = 20f,
    val fullNameZIndex: Int = 1,

    val jobTitleX: Float = 25f,
    val jobTitleY: Float = 55f,
    val jobTitleScale: Float = 1.0f,
    val jobTitleRotation: Float = 0f,
    val jobTitleSize: Float = 10f,
    val jobTitleZIndex: Int = 2,

    val companyNameX: Float = 25f,
    val companyNameY: Float = 80f,
    val companyNameScale: Float = 1.0f,
    val companyNameRotation: Float = 0f,
    val companyNameSize: Float = 12f,
    val companyNameZIndex: Int = 3,

    val mobileNumberX: Float = 25f,
    val mobileNumberY: Float = 120f,
    val mobileNumberScale: Float = 1.0f,
    val mobileNumberRotation: Float = 0f,
    val mobileNumberSize: Float = 9f,
    val mobileNumberZIndex: Int = 4,

    val emailX: Float = 25f,
    val emailY: Float = 140f,
    val emailScale: Float = 1.0f,
    val emailRotation: Float = 0f,
    val emailSize: Float = 9f,
    val emailZIndex: Int = 5,

    val websiteX: Float = 25f,
    val websiteY: Float = 160f,
    val websiteScale: Float = 1.0f,
    val websiteRotation: Float = 0f,
    val websiteSize: Float = 9f,
    val websiteZIndex: Int = 6,

    val addressX: Float = 25f,
    val addressY: Float = 180f,
    val addressScale: Float = 1.0f,
    val addressRotation: Float = 0f,
    val addressSize: Float = 9f,
    val addressZIndex: Int = 7,
    val lockedElements: String = ""
)

data class DesignElement(
    val id: String,
    val type: String, // "TEXT", "ICON", "STICKER", "SHAPE", "QR_CODE"
    val name: String, // e.g. "Phone Icon", "Star Sticker", "Square Shape", "Dynamic QR"
    val content: String, // actual text to display, or icon identifier, or design data/base64
    val x: Float, // relative position X (0..400)
    val y: Float, // relative position Y (0..240)
    val color: String = "#FFFFFF",
    val fontSize: Float = 14f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0
)
