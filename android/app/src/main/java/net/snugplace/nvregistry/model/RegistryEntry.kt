package net.snugplace.nvregistry.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistryEntry(
    val Index: Int,
    val RegistryName: String,
    val Size: Int,
    val Count: Int,
    val TypeName: String,
    val Payload: String
)
