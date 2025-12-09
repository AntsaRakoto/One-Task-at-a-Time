package com.example.personalapp

import android.content.Intent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskTimerActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var timerText: TextView
    private lateinit var subTitle: TextView
    private lateinit var btnStartPause: Button
    private var countDownTimer: CountDownTimer? = null
    private var remainingMillis: Long = 0L
    private var isRunning = false

    private var taskId: Long = -1L
    private var taskDurationMin: Int = 0
    private var taskName: String? = null

    private var projectId: Long = -1L


    private lateinit var recycler: RecyclerView
    private lateinit var adapter: TacheAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_timer) // ton layout minuteur

        timerText = findViewById(R.id.timerText)
        subTitle = findViewById(R.id.subTitle)
        btnStartPause = findViewById(R.id.btnStartPause)

        val backBtn: ImageView? = findViewById(R.id.backBtn)
        backBtn?.setOnClickListener { finish() }

        recycler = findViewById(R.id.tasksRecycler)

        adapter = TacheAdapter(
            items = emptyList(),
            onCheckedChange = { tache, completed ->
                updateTacheCompleted(tache, completed)
            },
            onItemClick = { /* rien dans cette activity */ }
        )

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)


        // DB
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).fallbackToDestructiveMigration(false).build()

        // récupère depuis l'intent (valeurs passées)
        taskId = intent.getLongExtra("TASK_ID", -1L)
        taskName = intent.getStringExtra("TASK_NAME")
        taskDurationMin = intent.getIntExtra("TASK_DURATION", 0)
        projectId = intent.getLongExtra("PROJECT_ID", -1L)

        subTitle.text = taskName ?: "Tâche"

        // si tu veux charger la tâche depuis la BDD (par sécurité) :
        if (taskId != -1L) {
            lifecycleScope.launch {
                val t: Tache? = withContext(Dispatchers.IO) {
                    db.tacheDao().getById(taskId)
                }
                t?.let {
                    // si tu veux remplacer la durée
                    taskDurationMin = it.durationMinutes
                    subTitle.text = it.name
                }
                // init minuteur avec la durée (en ms)
                remainingMillis = taskDurationMin * 60L * 1000L
                updateTimerText(remainingMillis)
            }
        } else {
            // pas d'id : initialise depuis la durée passée
            remainingMillis = taskDurationMin * 60L * 1000L
            updateTimerText(remainingMillis)
        }

        btnStartPause.setOnClickListener {
            if (isRunning) pauseTimer() else startOrResumeTimer()
        }
    }

    private fun startOrResumeTimer() {
        if (remainingMillis <= 0L) {
            // Si tu veux redémarrer avec la durée initiale
            // remainingMillis = taskDurationMin * 60L * 1000L
        }
        startCountDown(remainingMillis)
    }

    private fun startCountDown(millis: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(ms: Long) {
                remainingMillis = ms
                updateTimerText(ms)
            }
            override fun onFinish() {
                remainingMillis = 0L
                updateTimerText(0L)
                isRunning = false
                btnStartPause.text = "Start"
                btnStartPause.isEnabled = false
                btnStartPause.alpha = 0.5f

                lifecycleScope.launch(Dispatchers.IO) {
                    if (taskId != -1L) {
                        val t = db.tacheDao().getById(taskId)
                        t?.let {
                            db.tacheDao().update(it.copy(completed = true))
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TaskTimerActivity, "La tâche est terminée.", Toast.LENGTH_LONG).show()
                    }
                }
            }


        }.start()
        isRunning = true
        btnStartPause.text = "Pause"
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        btnStartPause.text = "Reprendre"
    }

    private fun updateTimerText(ms: Long) {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        timerText.text = String.format("%02d : %02d", min, sec)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
    private fun updateTacheCompleted(t: Tache, completed: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.tacheDao().updateCompleted(t.id, completed)

            withContext(Dispatchers.Main) {
                loadTaches()
            }
        }
    }
    private fun loadTaches() {

        lifecycleScope.launch(Dispatchers.IO) {
            val list = db.tacheDao().getTachesForProject(projectId) // tu as ce projectId déjà

            withContext(Dispatchers.Main) {
                adapter.updateList(list)
            }
        }
    }


}
