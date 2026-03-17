package com.example.nvregistry.util

import android.content.Context
import com.example.nvregistry.model.ChangeRecord
import com.example.nvregistry.model.RegistryEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ChangeHistoryManager {

    private const val FILE_NAME = "change_history.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun getHistoryFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    fun loadHistory(context: Context): List<ChangeRecord> {
        val file = getHistoryFile(context)
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecord(context: Context, record: ChangeRecord) {
        val current = loadHistory(context).toMutableList()
        current.add(0, record)
        getHistoryFile(context).writeText(json.encodeToString(current))
    }

    fun clearHistory(context: Context) {
        getHistoryFile(context).delete()
    }

    /**
     * SET済みのRegistryName一覧を返す（"[index]" サフィックスを除去）
     */
    fun getChangedNames(history: List<ChangeRecord>): Set<String> {
        return history.map { it.registryName.substringBefore("[") }.toSet()
    }

    /**
     * 直近のSET時点に記録されたjsonPayloadと、現在のJSONエントリのPayloadが
     * 異なる項目のRegistryNameセットを返す（= 新しいJSONとデバイス設定が不一致）
     */
    fun getMismatchedNames(
        history: List<ChangeRecord>,
        currentEntries: List<RegistryEntry>
    ): Set<String> {
        // registryNameごとに最新レコードを取得
        val latestByName = history
            .groupBy { it.registryName.substringBefore("[") }
            .mapValues { (_, records) -> records.first() }

        return currentEntries
            .filter { entry ->
                val latest = latestByName[entry.RegistryName]
                latest != null && latest.jsonPayload != entry.Payload
            }
            .map { it.RegistryName }
            .toSet()
    }

    /**
     * ChangeRecordからRegistryEntryを再構築する（履歴画面→ダイアログ遷移用）
     */
    fun toRegistryEntry(record: ChangeRecord): RegistryEntry {
        val baseName = record.registryName.substringBefore("[")
        return RegistryEntry(
            Index = record.index,
            RegistryName = baseName,
            Size = record.size,
            Count = record.count,
            TypeName = record.typeName,
            Payload = record.jsonPayload
        )
    }
}
