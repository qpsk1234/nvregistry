package net.snugplace.nvregistry

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.snugplace.nvregistry.adapter.RegistryAdapter
import net.snugplace.nvregistry.model.RegistryEntry
import net.snugplace.nvregistry.util.ChangeHistoryManager
import net.snugplace.nvregistry.util.DebugConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RegistryAdapter
    private lateinit var searchEditText: EditText
    private lateinit var loadFileButton: Button
    private lateinit var historyButton: Button
    private lateinit var settingsButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var filterSpinner: Spinner
    private lateinit var sortCheckBox: CheckBox

    private var allEntries: List<RegistryEntry> = emptyList()
    private var changedNames: Set<String> = emptySet()
    private var mismatchedNames: Set<String> = emptySet()

    // テーマ状態はSettingsActivityのSharedPrefsから読む
    private val isDarkTheme: Boolean
        get() = getSharedPreferences("nvregistry_prefs", MODE_PRIVATE)
            .getBoolean("is_dark_theme", true)

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { parseJsonFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DebugConfig.init(this)

        recyclerView = findViewById(R.id.recyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        loadFileButton = findViewById(R.id.loadFileButton)
        historyButton = findViewById(R.id.historyButton)
        settingsButton = findViewById(R.id.settingsButton)
        progressBar = findViewById(R.id.progressBar)
        filterSpinner = findViewById(R.id.filterSpinner)
        sortCheckBox = findViewById(R.id.sortCheckBox)

        adapter = RegistryAdapter { entry ->
            RegistryEditDialog(this, entry, isDarkTheme) { }.show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadFileButton.setOnClickListener {
            selectFileLauncher.launch("application/json")
        }

        historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilterAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilterAndSort()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        sortCheckBox.setOnCheckedChangeListener { _, _ ->
            applyFilterAndSort()
        }

        applyTheme()
    }

    override fun onResume() {
        super.onResume()
        // 設定画面・履歴画面から戻ったときに状態を再適用
        applyTheme()
        refreshDiffState()
    }

    private fun refreshDiffState() {
        if (allEntries.isEmpty()) return
        val history = ChangeHistoryManager.loadHistory(this)
        changedNames = ChangeHistoryManager.getChangedNames(history)
        mismatchedNames = ChangeHistoryManager.getMismatchedNames(history, allEntries)
        val latestPayloads = ChangeHistoryManager.getLatestPayloads(history)
        adapter.updateDiffState(changedNames, mismatchedNames, latestPayloads)
        applyFilterAndSort()
    }

    private fun applyTheme() {
        val dark = isDarkTheme
        val rootBg = if (dark) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()
        val textColor = if (dark) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val hintColor = if (dark) 0xFF888888.toInt() else 0xFF666666.toInt()

        window.decorView.setBackgroundColor(rootBg)
        searchEditText.setTextColor(textColor)
        searchEditText.setHintTextColor(hintColor)
        sortCheckBox.setTextColor(textColor)

        val spinnerItems = resources.getStringArray(R.array.filter_options)
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerItems) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                view.setTextColor(textColor)
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as android.widget.TextView
                view.setTextColor(textColor)
                view.setBackgroundColor(rootBg)
                return view
            }
        }
        val currentSelection = filterSpinner.selectedItemPosition
        filterSpinner.adapter = spinnerAdapter
        if (currentSelection != AdapterView.INVALID_POSITION) {
            filterSpinner.setSelection(currentSelection, false)
        }

        adapter.setDarkTheme(dark)
    }

    private fun parseJsonFromUri(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        searchEditText.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val entries = Json.decodeFromStream<List<RegistryEntry>>(inputStream)
                    allEntries = entries
                    withContext(Dispatchers.Main) {
                        adapter.submitList(entries)
                        refreshDiffState()
                        searchEditText.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.msg_loaded, entries.size),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.msg_load_error, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun applyFilterAndSort() {
        val query = searchEditText.text.toString()
        val filterPos = filterSpinner.selectedItemPosition // 0 = 全て, 1 = 変更済み, 2 = JSON不一致
        val sortByDiff = sortCheckBox.isChecked

        var list = allEntries.filter {
            it.RegistryName.contains(query, ignoreCase = true)
        }

        if (filterPos == 1) {
            list = list.filter { it.RegistryName in changedNames || it.RegistryName in mismatchedNames }
        } else if (filterPos == 2) {
            list = list.filter { it.RegistryName in mismatchedNames }
        }

        if (sortByDiff) {
            list = list.sortedByDescending { it.RegistryName in changedNames || it.RegistryName in mismatchedNames }
        }

        adapter.submitList(list)
    }
}
