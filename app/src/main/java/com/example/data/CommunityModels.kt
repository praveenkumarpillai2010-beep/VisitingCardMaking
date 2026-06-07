package com.example.data

import java.io.Serializable

data class CommunityMessage(
    val id: String,
    val senderId: String, // "self" or the professional's ID
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val visitingCardId: Int? = null,
    val isRead: Boolean = false
) : Serializable

data class CommunityNotification(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val eventType: String // "VIEW", "SAVE", "MESSAGE", "REQUEST", "LIKE", "FOLLOW"
) : Serializable

data class Professional(
    val id: String,
    val name: String,
    val profession: String,
    val company: String,
    val location: String = "Mumbai, India",
    val website: String = "www.profile.link",
    val email: String = "info@professional.com",
    val mobile: String = "+91 99999 88888",
    val bio: String = "Passionate professional in the industry. Let's connect and share details!",
    val facebook: String = "fb_id",
    val instagram: String = "insta_id",
    val linkedin: String = "linkedin_id",
    val category: String, // Business, Technology, Medical, Education, Real Estate, Finance, Creative, Retail
    val directoryRole: String, // Doctors, Teachers, Developers, Designers, Real Estate Agents, Business Owners, Freelancers
    val avatarColorHex: String = "#3949AB",
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val matchingCard: UserCard
) : Serializable

// Data Class for Creator Profile
data class CreatorProfile(
    val id: String,
    val username: String,
    val name: String,
    val bio: String,
    val companyName: String,
    val phone: String,
    val email: String,
    val website: String,
    val instagram: String = "",
    val facebook: String = "",
    val linkedin: String = "",
    val location: String = "San Francisco, CA",
    val profilePhoto: String = "", // Empty indicates fallback avatar initials
    val coverBanner: String = "#004B49", // Solid color hex or theme indicator
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val isPublic: Boolean = true,
    var followersCount: Int = 0,
    var followingCount: Int = 0,
    var likesReceivedCount: Int = 0,
    var totalDesignsCount: Int = 0
) : Serializable

// Data Class for shared business cards in the community
data class CommunitySharedCard(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // Business, Corporate, Creative, Medical, Technology, Real Estate, Restaurant, Premium
    val frontCard: UserCard,
    val backCard: UserCard? = null, // Back card design can be optional
    val creatorId: String,
    val creatorName: String,
    val creatorUsername: String,
    val creatorAvatarColor: String = "#FF5E7E",
    val creatorAvatarUrl: String = "",
    var isVerifiedCreator: Boolean = false,
    var isPremiumCreator: Boolean = false,
    val likesCount: Int = 0,
    val downloadsCount: Int = 0,
    val viewsCount: Int = 0,
    val createdTime: Long = System.currentTimeMillis()
) : Serializable
