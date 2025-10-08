package com.example.organizer.ai.models

data class Action(
    val intention: UserIntention,
    val parameters: Map<String, String>,
    val response: String,
    val execute: (() -> Unit)? = null
)