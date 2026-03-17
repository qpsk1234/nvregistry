package net.snugplace.nvregistry

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.snugplace.nvregistry.adapter.HistoryAdapter
import net.snugplace.nvregistry.util.ChangeHistoryManager
import net.snugplace.nvregistry.util.DebugConfig

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.title = getString(R.string.history_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        DebugConfig.init(this)

        val recyclerView = findViewById<RecyclerView>(R.id.historyRecyclerView)
        val emptyText = findViewById<TextView>(R.id.emptyText)
        val clearButton = findViewById<Button>(R.id.clearHistoryButton)

        val records = ChangeHistoryManager.loadHistory(this)

        if (records.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.layoutManager = LinearLayoutManager(this)

            // RegistryNameクリック → RegistryEditDialogを開く
            recyclerView.adapter = HistoryAdapter(records) { record ->
                val entry = ChangeHistoryManager.toRegistryEntry(record)
                val isDark = getSharedPreferences("nvregistry_prefs", MODE_PRIVATE)
                    .getBoolean("is_dark_theme", true)
                RegistryEditDialog(this, entry, isDark) { }.show()
            }
        }

        clearButton.setOnClickListener {
            ChangeHistoryManager.clearHistory(this)
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            clearButton.isEnabled = false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
