package com.example.organizer.ai.models

data class ParsedCommand(
    val intention: UserIntention,
    val confidence: Float,
    val parameters: Map<String, String> = emptyMap(),
    val rawText: String
)