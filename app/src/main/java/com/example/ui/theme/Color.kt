package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// FinFlow Brand Colors - Emerald, Teal & Slate
val EmeraldPrimaryLight = Color(0xFF047857)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val EmeraldOnContainerLight = Color(0xFF064E3B)

val TealSecondaryLight = Color(0xFF0F766E)
val TealOnSecondaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFFCCFBF1)
val TealOnContainerLight = Color(0xFF134E4A)

val AmberAccentLight = Color(0xFFD97706)
val AmberContainerLight = Color(0xFFFEF3C7)

val SlateBackgroundLight = Color(0xFFF8FAFC)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceVariantLight = Color(0xFFF1F5F9)
val SlateOutlineLight = Color(0xFFCBD5E1)

// Dark Theme Colors
val EmeraldPrimaryDark = Color(0xFF34D399)
val EmeraldOnPrimaryDark = Color(0xFF064E3B)
val EmeraldContainerDark = Color(0xFF065F46)
val EmeraldOnContainerDark = Color(0xFFA7F3D0)

val TealSecondaryDark = Color(0xFF2DD4BF)
val TealOnSecondaryDark = Color(0xFF134E4A)
val TealContainerDark = Color(0xFF115E59)
val TealOnContainerDark = Color(0xFF99F6E4)

val AmberAccentDark = Color(0xFFFBBF24)
val AmberContainerDark = Color(0xFF78350F)

val SlateBackgroundDark = Color(0xFF0B1315)
val SlateSurfaceDark = Color(0xFF131F24)
val SlateSurfaceVariantDark = Color(0xFF1C2C33)
val SlateOutlineDark = Color(0xFF334B54)

// Category Semantic Colors
val ColorHousing = Color(0xFF3B82F6)
val ColorFood = Color(0xFF10B981)
val ColorTransport = Color(0xFFF59E0B)
val ColorUtilities = Color(0xFF8B5CF6)
val ColorEntertainment = Color(0xFFEC4899)
val ColorHealthcare = Color(0xFFEF4444)
val ColorShopping = Color(0xFF06B6D4)
val ColorPersonalCare = Color(0xFF14B8A6)
val ColorEducation = Color(0xFF6366F1)
val ColorOther = Color(0xFF64748B)

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Housing" -> ColorHousing
        "Food & Dining" -> ColorFood
        "Transportation" -> ColorTransport
        "Utilities" -> ColorUtilities
        "Entertainment" -> ColorEntertainment
        "Healthcare" -> ColorHealthcare
        "Shopping" -> ColorShopping
        "Personal Care" -> ColorPersonalCare
        "Education" -> ColorEducation
        else -> ColorOther
    }
}
