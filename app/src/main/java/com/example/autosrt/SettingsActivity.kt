package com.example.autosrt

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var etApiKey: TextInputEditText
    private lateinit var etAccessKey: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSavedConfig()
        setupClickListeners()
    }

    private fun initViews() {
        etApiKey = findViewById(R.id.etApiKey)
        etAccessKey = findViewById(R.id.etAccessKey)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun loadSavedConfig() {
        val sharedPref = getSharedPreferences("api_config", MODE_PRIVATE)
        val apiKey = sharedPref.getString("api_key", "")
        val accessKey = sharedPref.getString("access_key", "")

        etApiKey.setText(apiKey ?: "")
        etAccessKey.setText(accessKey ?: "")
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveConfig()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun saveConfig() {
        val apiKey = etApiKey.text.toString().trim()
        val accessKey = etAccessKey.text.toString().trim()

        if (apiKey.isEmpty() || accessKey.isEmpty()) {
            Toast.makeText(this, "API Key和Access Key不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("api_config", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("api_key", apiKey)
            putString("access_key", accessKey)
            apply()
        }

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
    }
}