package com.example.nvregistry.model

import kotlinx.serialization.Serializable

@Serializable
data class ChangeRecord(
    val timestamp: Long,
    val registryName: String,       // "NAME[idx]" 形式
    val jsonPayload: String,         // SET時点のJSONペイロード全体
    val valueBeforeChange: String,   // GET取得した変更前の値
    val newValue: String,            // SETした値
    val postSetGetResult: String,    // SET後にGETした返り値
    val success: Boolean,
    // RegistryEntry再構築用フィールド（デフォルト値付きで後方互換性を保つ）
    val typeName: String = "u8",
    val size: Int = 1,
    val count: Int = 1,
    val index: Int = 0
)
