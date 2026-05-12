package com.nilpo.contenttracker.core.model

data class Ownership(
    val isOwned: Boolean,
    val type: OwnershipType = OwnershipType.None,
)

enum class OwnershipType {
    None,
    Physical,
    Digital,
    Subscription,
    Borrowed,
}
