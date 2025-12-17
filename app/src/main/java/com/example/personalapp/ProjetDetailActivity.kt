package com.example.personalapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.data.Tache
import com.example.personalapp.models.TacheAdapter
import com.example.personalapp.utils.setupProfileLogout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ProjetDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TacheAdapter
    private var tacheList = mutableListOf<Tache>()

    private var projectId: Long = -1L
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_projet_detail)

        val profileBtn: ImageView = findViewById(R.id.profileBtn)
        setupProfileLogout(this, profileBtn)

        val backBtn: ImageView = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).fallbackToDestructiveMigration(false).build()

        projectId = intent.getLongExtra("PROJECT_ID", -1L)

        recyclerView = findViewById(R.id.recyclerViewTaches)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TacheAdapter(
            tacheList,
            onCheckedChange = { tache, completed -> updateTache(tache.copy(completed = completed)) },
            onItemClick = { tache ->
                val intent = android.content.Intent(this, TaskTimerActivity::class.java).apply {
                    putExtra("TASK_ID", tache.id)
                    putExtra("TASK_NAME", tache.name)
                    putExtra("TASK_DURATION", tache.durationMinutes)
                    putExtra("PROJECT_ID", projectId)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddTache).setOnClickListener {
            showAddTacheDialog()
        }

        observeTaches()
    }

    private fun observeTaches() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                db.tacheDao().getTachesForProjectFlow(projectId).collect { taches ->
                    adapter.updateList(taches)
                }
            }
        }
    }


    private fun updateTache(tache: Tache) {
        lifecycleScope.launch {
            // debug log
            android.util.Log.d("ProjetDetail", "updateTache id=${tache.id} completed=${tache.completed}")
            val rows = withContext(Dispatchers.IO) {
                db.tacheDao().updateCompleted(tache.id, tache.completed)
            }
            android.util.Log.d("ProjetDetail", "rows affected = $rows")
            withContext(Dispatchers.Main) {
                adapter.updateItem(tache)
            }
        }
    }



    private fun loadTaches() {
        CoroutineScope(Dispatchers.IO).launch {
            val taches = db.tacheDao().getTachesForProject(projectId)
            withContext(Dispatchers.Main) {
                adapter.updateList(taches)
            }
        }
    }


    private fun addTache(tache: Tache) {
        CoroutineScope(Dispatchers.IO).launch {
            db.tacheDao().insert(tache)
            loadTaches()
        }
    }

    private fun showAddTacheDialog() {
        lifecycleScope.launch {
            val (totalMinutes, usedMinutes) = withContext(Dispatchers.IO) {
                // Récupère le projet
                val projet = db.projetDao().getProjetById(projectId)
                val total = projet?.durationMinutes ?: 0

                // Somme des durées des tâches existantes
                val taches = db.tacheDao().getTachesForProject(projectId)
                val used = taches.sumOf { it.durationMinutes }

                total to used
            }

            val remaining = totalMinutes - usedMinutes

            withContext(Dispatchers.Main) {
                if (totalMinutes <= 0) {
                    Toast.makeText(this@ProjetDetailActivity,
                        "Durée totale du projet non définie. Définissez la durée du projet d'abord.",
                        Toast.LENGTH_LONG).show()
                    return@withContext
                }

                if (remaining <= 0) {
                    Toast.makeText(this@ProjetDetailActivity,
                        "Temps total du projet déjà utilisé (0 min restant).",
                        Toast.LENGTH_LONG).show()
                    return@withContext
                }

                val builder = AlertDialog.Builder(this@ProjetDetailActivity)
                builder.setTitle("Ajouter une tâche")

                val view = layoutInflater.inflate(R.layout.dialog_add_tache, null)
                val edtName = view.findViewById<EditText>(R.id.edtTacheName)
                val edtDuration = view.findViewById<EditText>(R.id.edtTacheDuration)

                // Message informatif avec le temps restant
                val infoView = TextView(this@ProjetDetailActivity).apply {
                    text = "Temps restant : $remaining min"
                    setPadding(16, 8, 16, 8)
                }

                val container = LinearLayout(this@ProjetDetailActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 8, 16, 8)
                    addView(infoView)
                    addView(view)
                }

                builder.setView(container)
                builder.setPositiveButton("Ajouter", null)
                builder.setNegativeButton("Annuler", null)

                val dialog = builder.create()
                dialog.setOnShowListener {
                    val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    btn.setOnClickListener {
                        val name = edtName.text.toString().trim()
                        val duration = edtDuration.text.toString().toIntOrNull() ?: 0

                        if (name.isEmpty()) {
                            Toast.makeText(this@ProjetDetailActivity, "Donne un nom à la tâche.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if (duration <= 0) {
                            Toast.makeText(this@ProjetDetailActivity, "La durée doit être > 0.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        if (duration > remaining) {
                            Toast.makeText(this@ProjetDetailActivity,
                                "La durée dépasse le temps restant ($remaining min).", Toast.LENGTH_LONG).show()
                            return@setOnClickListener
                        }
                        lifecycleScope.launch(Dispatchers.IO) {
                            db.tacheDao().insert(
                                Tache(
                                    name = name,
                                    durationMinutes = duration,
                                    completed = false,
                                    projectId = projectId
                                )
                            )
                            val taches = db.tacheDao().getTachesForProject(projectId)
                            withContext(Dispatchers.Main) {
                                adapter.updateList(taches)
                                dialog.dismiss()
                                Toast.makeText(this@ProjetDetailActivity,
                                    "Tâche ajoutée (${duration} min).", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                dialog.show()
            }
        }
    }


}
