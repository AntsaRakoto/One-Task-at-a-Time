package com.example.personalapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.data.Projet
import com.example.personalapp.utils.setupProfileLogout

import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InputProjetActivity : AppCompatActivity() {

    private lateinit var etProjetName: TextInputEditText
    private lateinit var npHours: NumberPicker
    private lateinit var npMinutes: NumberPicker
    private lateinit var btnValider: Button
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projet)

        // Views
        etProjetName = findViewById(R.id.etProjetName)
        npHours = findViewById(R.id.npHours)
        npMinutes = findViewById(R.id.npMinutes)
        btnValider = findViewById(R.id.btn_projet)
        toolbar = findViewById(R.id.top_toolbar)

        val profileBtn: ImageView = findViewById(R.id.profileBtn)
        setupProfileLogout(this, profileBtn)


        val backBtn: ImageView = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // NumberPickers config
        npHours.minValue = 0
        npHours.maxValue = 23
        npMinutes.minValue = 0
        npMinutes.maxValue = 59

        // Click du bouton Valider
        btnValider.setOnClickListener {
            val name = etProjetName.text?.toString()?.trim() ?: ""
            if (name.isEmpty()) {
                etProjetName.error = "Le nom du projet est requis"
                return@setOnClickListener
            }

            val totalMinutes = npHours.value * 60 + npMinutes.value

            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getLong("current_user_id", -1)
            if (currentUserId.toInt() == -1) {
                Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val projet = Projet(name = name, durationMinutes = totalMinutes, userId = currentUserId)

            // Insert dans Room dans une coroutine
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val rowId = withContext(Dispatchers.IO) {
                        db.projetDao().insert(projet)
                    }

                    Log.d("InputProjetActivity", "rowId = $rowId")

                    if (rowId > 0) {
                        Toast.makeText(this@InputProjetActivity, "Projet créé", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@InputProjetActivity, ProjetListActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@InputProjetActivity, "Erreur lors de la création", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@InputProjetActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Gérer le clic sur l'icône back du toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
