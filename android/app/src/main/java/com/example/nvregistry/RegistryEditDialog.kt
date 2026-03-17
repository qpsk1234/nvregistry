package com.example.nvregistry

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nvregistry.adapter.NvValueAdapter
import com.example.nvregistry.model.ChangeRecord
import com.example.nvregistry.model.RegistryEntry
import com.example.nvregistry.util.ChangeHistoryManager
import com.example.nvregistry.util.DebugConfig
import com.example.nvregistry.util.PayloadParser
import com.example.nvregistry.util.ShellUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistryEditDialog(
    context: Context,
    private val entry: RegistryEntry,
    private val isDarkTheme: Boolean,
    private val onSave: (String) -> Unit
) : Dialog(context) {

    companion object {
        private const val TAG = "RegistryEditDialog"
        private fun logD(msg: String) { if (DebugConfig.isEnabled) Log.d(TAG, msg) }
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var logTextView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var nvRecyclerView: RecyclerView
    private lateinit var nvAdapter: NvValueAdapter

    // JSON初期値（リセット用に保持）
    private val jsonElements = PayloadParser.parse(entry.Payload, entry.TypeName, entry.Size, entry.Count)
    private var parsedElements = jsonElements.toMutableList().toList()

    private val bgColor = if (isDarkTheme) 0xFF1E1E1E.toInt() else 0xFFFFFFFF.toInt()
    private val textColor = if (isDarkTheme) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_edit_registry)
        window?.decorView?.setBackgroundColor(bgColor)
        window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        findViewById<TextView>(R.id.dialogTitle).apply {
            text = "${entry.RegistryName}  [${entry.TypeName}×${entry.Count}]"
            setTextColor(textColor)
        }
        findViewById<TextView>(R.id.jsonValueText).apply {
            text = "JSON: ${PayloadParser.summarize(jsonElements, entry.TypeName, entry.Count)}"
            setTextColor(0xFFFF8C00.toInt())
        }

        progressBar = findViewById(R.id.dialogProgressBar)
        logTextView = findViewById(R.id.resultTextView)
        logScroll = findViewById(R.id.resultScrollView)

        nvRecyclerView = findViewById(R.id.nvValuesRecyclerView)
        nvAdapter = NvValueAdapter(
            elements = parsedElements,
            jsonElements = jsonElements,
            isDarkTheme = isDarkTheme
        ) { index, hexInput ->
            executeSetAtIndex(index, hexInput)
        }
        nvRecyclerView.layoutManager = LinearLayoutManager(context)
        nvRecyclerView.adapter = nvAdapter
        nvRecyclerView.isNestedScrollingEnabled = true

        // GETボタン
        findViewById<Button>(R.id.getButton).apply {
            text = context.getString(R.string.btn_get)
            setOnClickListener { executeGet() }
        }

        // 全行JSON値リセットボタン
        findViewById<Button>(R.id.resetAllJsonButton).apply {
            text = context.getString(R.string.btn_reset_all_json)
            setOnClickListener {
                parsedElements = jsonElements.toList()
                nvAdapter.updateElements(parsedElements)
                appendLog(context.getString(R.string.msg_reset_success))
            }
        }

        // 閉じるボタン
        findViewById<Button>(R.id.closeButton).apply {
            text = context.getString(R.string.btn_close)
            setOnClickListener { dismiss() }
        }
    }

    private fun appendLog(msg: String) {
        logTextView.append("$msg\n")
        logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun parseGoogGetNvResponse(result: String): List<PayloadParser.ParsedElement>? {
        val lineRegex = """\+GOOGGETNV:\s*"[^"]+",\s*(\d+),\s*"([^"]*)"""".toRegex()
        val matches = lineRegex.findAll(result).toList()

        logD("parseResponse: found ${matches.size} GOOGGETNV match(es)")
        matches.forEachIndexed { i, m ->
            logD("  match[$i]: idx=${m.groupValues[1]} val=${m.groupValues[2]}")
        }

        if (matches.isEmpty()) {
            appendLog(context.getString(R.string.msg_parse_error))
            return null
        }

        if (matches.size == 1) {
            val rawPayload = matches[0].groupValues[2]
            val normalized = rawPayload.trim()
            logD("Single match payload: $normalized")
            val elements = PayloadParser.parse(normalized, entry.TypeName, entry.Size, entry.Count)
            if (elements.isNotEmpty()) {
                appendLog(context.getString(R.string.msg_updated, elements.size))
                return elements
            }
            appendLog(context.getString(R.string.msg_parse_failed, rawPayload))
            return null
        }

        val sortedValues = matches
            .map { Pair(it.groupValues[1].toIntOrNull() ?: 0, it.groupValues[2]) }
            .sortedBy { it.first }
        val fullPayload = sortedValues
            .flatMap { (_, v) -> v.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
            .joinToString(",")
        logD("Multi-line reconstructed payload: $fullPayload")

        val elements = PayloadParser.parse(fullPayload, entry.TypeName, entry.Size, entry.Count)
        appendLog(context.getString(R.string.msg_updated, elements.size))
        return elements
    }

    private fun executeGet() {
        progressBar.visibility = View.VISIBLE
        logTextView.text = ""
        appendLog(context.getString(R.string.msg_get_executing))

        CoroutineScope(Dispatchers.IO).launch {
            val result = ShellUtils.executeGetNv(entry.RegistryName)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                appendLog("--- RAW (${result.length} chars) ---")
                appendLog(result)
                appendLog("--- END ---")

                val newElements = parseGoogGetNvResponse(result)
                if (newElements != null && newElements.isNotEmpty()) {
                    parsedElements = newElements
                    nvAdapter.updateElements(newElements)
                }
            }
        }
    }

    private fun executeSetAtIndex(index: Int, hexInput: String) {
        progressBar.visibility = View.VISIBLE
        val byteStr = PayloadParser.hexInputToByteStr(hexInput, entry.Size)
        appendLog("SET [idx=$index] $hexInput → $byteStr")
        val valueBefore = parsedElements.getOrNull(index)?.hexDisplay ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val (setResult, getResult) = ShellUtils.executeSetNvAtIndex(entry.RegistryName, index, byteStr)
            val success = setResult.contains("OK", ignoreCase = true) ||
                          getResult.contains(byteStr, ignoreCase = true)

            // 変更履歴記録（typeName/size/count/indexを保存）
            ChangeHistoryManager.addRecord(context, ChangeRecord(
                timestamp = System.currentTimeMillis(),
                registryName = "${entry.RegistryName}[$index]",
                jsonPayload = entry.Payload,
                valueBeforeChange = valueBefore,
                newValue = hexInput,
                postSetGetResult = getResult,
                success = success,
                typeName = entry.TypeName,
                size = entry.Size,
                count = entry.Count,
                index = index
            ))

            val newElements = parseGoogGetNvResponseInternal(getResult)

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                appendLog("【SET】$setResult")
                appendLog("【自動GET】$getResult")
                if (newElements != null) {
                    parsedElements = newElements
                    nvAdapter.updateElements(newElements)
                }
                appendLog(if (success) context.getString(R.string.msg_success) else context.getString(R.string.msg_warning))
                onSave(hexInput)
            }
        }
    }

    private fun parseGoogGetNvResponseInternal(result: String): List<PayloadParser.ParsedElement>? {
        val lineRegex = """\+GOOGGETNV:\s*"[^"]+",\s*(\d+),\s*"([^"]*)"""".toRegex()
        val matches = lineRegex.findAll(result).toList()
        if (matches.isEmpty()) return null
        if (matches.size == 1) {
            return PayloadParser.parse(matches[0].groupValues[2], entry.TypeName, entry.Size, entry.Count)
                .takeIf { it.isNotEmpty() }
        }
        val fullPayload = matches
            .map { Pair(it.groupValues[1].toIntOrNull() ?: 0, it.groupValues[2]) }
            .sortedBy { it.first }
            .flatMap { (_, v) -> v.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
            .joinToString(",")
        return PayloadParser.parse(fullPayload, entry.TypeName, entry.Size, entry.Count).takeIf { it.isNotEmpty() }
    }
}
