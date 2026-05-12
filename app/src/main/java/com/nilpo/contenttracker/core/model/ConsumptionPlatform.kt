package com.nilpo.contenttracker.core.model

data class ConsumptionPlatform(
    val name: String,
    val type: ConsumptionPlatformType,
)

enum class ConsumptionPlatformType {
    Physical,
    DigitalStore,
    Streaming,
    Ebook,
    Library,
    Other,
}
