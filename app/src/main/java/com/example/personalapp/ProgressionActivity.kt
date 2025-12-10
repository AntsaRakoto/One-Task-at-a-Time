package com.example.personalapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.databinding.ActivityProgressionBinding
import com.example.personalapp.utils.setupProfileLogout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProgressionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressionBinding
    private var projectId: Long = -1L
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupUiActions()

        // Récupérer l'ID du projet depuis l'intent
        projectId = intent.getLongExtra("PROJECT_ID", -1L)
        if (projectId == -1L) finish() // ID invalide, ferme l'activité

        loadProgress()
    }

    private fun loadProgress() {
        lifecycleScope.launch {
            val taches = withContext(Dispatchers.IO) {
                db.tacheDao().getTachesForProject(projectId)
            }
            val projet = withContext(Dispatchers.IO) {
                db.projetDao().getProjetById(projectId)
            } ?: return@launch

            val totalMinutes = projet.durationMinutes
            val completedMinutes = taches.filter { it.completed }.sumOf { it.durationMinutes }

            val progressPercent = if (totalMinutes > 0) {
                ((completedMinutes.toFloat() / totalMinutes) * 100).toInt().coerceIn(0, 100)
            } else 0

            binding.progressCircular.progress = progressPercent
            binding.descriProgres.text = "${projet.name}\neffectuée à $progressPercent%"
        }
    }

    private fun setupUiActions() {
        binding.topToolbar.findViewById<View>(R.id.backBtn)?.setOnClickListener {
            finish()
        }

        binding.topToolbar.findViewById<View>(R.id.profileBtn)?.setOnClickListener {
            val profileBtn: ImageView = findViewById(R.id.profileBtn)
            setupProfileLogout(this, profileBtn)
        }

        binding.btnProjet.setOnClickListener {
            val intent = Intent(this, ProjetListActivity::class.java)
            startActivity(intent)
        }
    }
}
