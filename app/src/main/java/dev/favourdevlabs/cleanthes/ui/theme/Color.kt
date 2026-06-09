package dev.favourdevlabs.cleanthes.ui.theme

import androidx.compose.ui.graphics.Color

// ── Cleanthes Gold ────────────────────────────────────────────────────────────
val GoldPrimary     = Color(0xFFC5A059)   // stoic gold — main accent
val GoldBright      = Color(0xFFFFB300)   // urgent / highlighted
val GoldDim         = Color(0xFF8B6914)   // secondary / subdued
val GoldContainer   = Color(0xFF2A2010)   // tinted dark surface

// ── Surfaces (dark hierarchy) ─────────────────────────────────────────────────
val SurfaceDeep     = Color(0xFF0A0A0A)   // true background
val SurfaceDark     = Color(0xFF111111)   // default surface
val SurfaceElevated = Color(0xFF1A1A1A)   // cards, bottom sheets
val SurfaceModal    = Color(0xFF222222)   // dialogs, menus

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary     = Color(0xFFF0EDE8)   // warm white
val TextSecondary   = Color(0xFFB0A89C)   // muted warm
val TextMuted       = Color(0xFF6B6460)   // de-emphasized

// ── Semantic ──────────────────────────────────────────────────────────────────
val Danger          = Color(0xFFCF6679)   // delete, destructive
val Success         = Color(0xFF4CAF6A)
val OnGold          = Color(0xFF0A0A0A)   // text/icon on gold background
// ── Password strength ─────────────────────────────────────────────────────────
val StrengthVeryWeak    = Color(0xFFCF2929)
val StrengthWeak        = Color(0xFFD4621A)
val StrengthFair        = Color(0xFFCFAA00)
val StrengthStrong      = GoldPrimary       // intentional alias
val StrengthVeryStrong  = Success           // intentional alias
