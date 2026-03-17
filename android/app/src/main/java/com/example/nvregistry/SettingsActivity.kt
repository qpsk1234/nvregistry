package com.example.nvregistry

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.nvregistry.util.DebugConfig

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = getString(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        DebugConfig.init(this)

        // テーマ切替
        val themeButton = findViewById<Button>(R.id.settingsThemeButton)
        updateThemeButton(themeButton)
        themeButton.setOnClickListener {
            val sharedPref = getSharedPreferences("nvregistry_prefs", MODE_PRIVATE)
            val dark = sharedPref.getBoolean("is_dark_theme", true)
            sharedPref.edit().putBoolean("is_dark_theme", !dark).apply()
            updateThemeButton(themeButton)
        }

        // 言語切替
        val langButton = findViewById<Button>(R.id.settingsLangButton)
        updateLangButton(langButton)
        langButton.setOnClickListener {
            val current = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "ja"
            val newLang = if (current == "ja") "en" else "ja"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
            updateLangButton(langButton)
        }

        // デバッグログ切替
        val debugButton = findViewById<Button>(R.id.settingsDebugButton)
        updateDebugButton(debugButton)
        debugButton.setOnClickListener {
            val enabled = DebugConfig.toggle(this)
            updateDebugButton(debugButton)
            Toast.makeText(this, "Debug log: ${if (enabled) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateThemeButton(btn: Button) {
        val dark = getSharedPreferences("nvregistry_prefs", MODE_PRIVATE)
            .getBoolean("is_dark_theme", true)
        btn.text = if (dark) getString(R.string.btn_theme_light) else getString(R.string.btn_theme_dark)
    }

    private fun updateLangButton(btn: Button) {
        val lang = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "ja"
        btn.text = if (lang == "ja") "Switch to English" else "日本語に切替"
    }

    private fun updateDebugButton(btn: Button) {
        btn.text = if (DebugConfig.isEnabled) "Debug Log: ON ●" else "Debug Log: OFF ○"
        btn.setTextColor(
            if (DebugConfig.isEnabled) 0xFFFF5722.toInt() else getColor(android.R.color.darker_gray)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
