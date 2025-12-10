package com.example.personalapp

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.data.Tache
import com.example.personalapp.models.TacheAdapter
import com.example.personalapp.utils.setupProfileLogout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskTimerActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var timerText: TextView
    private lateinit var subTitle: TextView
    private lateinit var btnStart: Button
    private lateinit var btn5: Button
    private lateinit var btn10: Button
    private lateinit var btn15: Button

    private var taskCountDownTimer: CountDownTimer? = null
    private var breakCountDownTimer: CountDownTimer? = null

    // task timing state
    private var taskId: Long = -1L
    private var taskDurationMin: Int = 0
    private var taskName: String? = null
    private var projectId: Long = -1L

    private var taskRemainingMillis: Long = 0L
    private var hasTaskStarted: Boolean = false
    private var isBreakRunning: Boolean = false

    // RecyclerView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TacheAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_timer)

        timerText = findViewById(R.id.timerText)
        subTitle = findViewById(R.id.subTitle)
        btnStart = findViewById(R.id.btnStartPause)
        btn5 = findViewById(R.id.btn5)
        btn10 = findViewById(R.id.btn10)
        btn15 = findViewById(R.id.btn15)

        val backBtn: ImageView? = findViewById(R.id.backBtn)
        backBtn?.setOnClickListener { finish() }
        val profileBtn: ImageView = findViewById(R.id.profileBtn)
        setupProfileLogout(this, profileBtn)

        recycler = findViewById(R.id.tasksRecycler)
        adapter = TacheAdapter(
            items = emptyList(),
            onCheckedChange = { t, completed -> updateTacheCompleted(t, completed) },
            onItemClick = { /* Rien ici */ }
        )
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).fallbackToDestructiveMigration(false).build()

        // Récupérer l'intent
        taskId = intent.getLongExtra("TASK_ID", -1L)
        taskName = intent.getStringExtra("TASK_NAME")
        taskDurationMin = intent.getIntExtra("TASK_DURATION", 0)
        projectId = intent.getLongExtra("PROJECT_ID", -1L)

        subTitle.text = taskName ?: "Tâche"

        // Charger la tâche depuis la BDD (sécurisé) et initialiser remaining
        lifecycleScope.launch {
            val t: Tache? = withContext(Dispatchers.IO) {
                if (taskId != -1L) db.tacheDao().getById(taskId) else null
            }
            t?.let {
                taskDurationMin = it.durationMinutes
                subTitle.text = it.name
            }
            // initialisation du remaining (en ms)
            taskRemainingMillis = taskDurationMin * 60L * 1000L
            updateTimerText(taskRemainingMillis)
            // charger la liste des taches du projet si tu veux l'afficher
            loadTaches()
        }

        // Start : l'utilisateur appuie 1x pour démarrer la tâche
        btnStart.setOnClickListener {
            if (!hasTaskStarted && taskRemainingMillis > 0L) {
                // Lance le minuteur de la tâche
                startTaskTimer(taskRemainingMillis)
                hasTaskStarted = true
                // après le premier démarrage, on rend le bouton "inactif" car la pause doit se faire
                // via les boutons 5/10/15. On change le texte pour indiquer l'état.
                btnStart.isEnabled = false
                btnStart.text = "En cours"
            }
        }

        // Break buttons : si la tâche est en cours, déclenchent un break
        btn5.setOnClickListener { startBreakMinutes(5) }
        btn10.setOnClickListener { startBreakMinutes(10) }
        btn15.setOnClickListener { startBreakMinutes(15) }
    }

    private fun startTaskTimer(millis: Long) {
        // si un break tourne, ne démarrer pas
        if (isBreakRunning) return

        taskCountDownTimer?.cancel()
        taskCountDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(ms: Long) {
                taskRemainingMillis = ms
                updateTimerText(ms)
            }

            override fun onFinish() {
                taskRemainingMillis = 0L
                updateTimerText(0L)
                // tâche terminée -> Marquer completed en BDD
                lifecycleScope.launch(Dispatchers.IO) {
                    if (taskId != -1L) {
                        val t = db.tacheDao().getById(taskId)
                        t?.let {
                            db.tacheDao().update(it.copy(completed = true))
                        }
                    }
                    // Toast sur Main
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TaskTimerActivity, "La tâche est terminée.", Toast.LENGTH_LONG).show()
                        // état UI : tâche finie -> réinitialiser bouton start (désactivé)
                        btnStart.isEnabled = false
                        btnStart.text = "Terminée"
                        // rendre inactifs les boutons de break
                        setBreakButtonsEnabled(false)
                    }
                }
            }
        }.start()
    }

    private fun startBreakMinutes(minutes: Int) {
        // Break possible uniquement si :
        // - la tâche a démarré au moins une fois (hasTaskStarted)
        // - il reste du temps de tâche (taskRemainingMillis > 0)
        // - aucun break n'est en cours
        if (!hasTaskStarted) {
            Toast.makeText(this, "Démarre d'abord la tâche (Start).", Toast.LENGTH_SHORT).show()
            return
        }
        if (taskRemainingMillis <= 0L) {
            Toast.makeText(this, "La tâche est déjà terminée.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isBreakRunning) return

        // on met en pause le task timer
        taskCountDownTimer?.cancel()

        // état break
        isBreakRunning = true
        setBreakButtonsEnabled(false)
        // affichage indicatif
        btnStart.text = "Pause (${minutes}m)"

        val breakMillis = minutes * 60L * 1000L
        breakCountDownTimer?.cancel()
        breakCountDownTimer = object : CountDownTimer(breakMillis, 1000) {
            override fun onTick(ms: Long) {
                // afficher countdown du break (on peut distinguer ds timerText)
                updateTimerTextForBreak(ms)
            }

            override fun onFinish() {
                isBreakRunning = false
                // reprendre le minuteur de la tâche automatiquement
                updateTimerText(taskRemainingMillis)
                startTaskTimer(taskRemainingMillis)
                // reapparition des boutons de break
                setBreakButtonsEnabled(true)
                // rétablir bouton start en tant qu'indicateur "En cours"
                btnStart.text = "En cours"
            }
        }.start()
    }

    private fun setBreakButtonsEnabled(enabled: Boolean) {
        btn5.isEnabled = enabled
        btn10.isEnabled = enabled
        btn15.isEnabled = enabled
    }

    private fun updateTimerText(ms: Long) {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        timerText.text = String.format("%02d:%02d", min, sec)
    }

    private fun updateTimerTextForBreak(ms: Long) {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        // optionnel : ajouter "(pause)" pour distinguer
        timerText.text = String.format("Pause %02d:%02d", min, sec)
    }

    private fun updateTacheCompleted(t: Tache, completed: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.tacheDao().updateCompleted(t.id, completed)
            withContext(Dispatchers.Main) { loadTaches() }
        }
    }

    private fun loadTaches() {
        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.tacheDao().getTachesForProject(projectId)
            withContext(Dispatchers.Main) { adapter.updateList(list) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        taskCountDownTimer?.cancel()
        breakCountDownTimer?.cancel()
    }
}
