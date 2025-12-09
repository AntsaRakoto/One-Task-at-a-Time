package com.example.personalapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.utils.setupProfileLogout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Accueil : AppCompatActivity() {
    private lateinit var btnActivityProgres: Button
    private lateinit var btnActivityProjet: Button
    private lateinit var btnActivityTemps: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil)

        val userNameTextView = findViewById<TextView>(R.id.user_name)
        btnActivityProjet = findViewById<Button>(R.id.btn_organisation)
        btnActivityTemps = findViewById<Button>(R.id.btn_temps_conseil)
        btnActivityProgres = findViewById<Button>(R.id.btn_avancement)

        val profileBtn: ImageView = findViewById(R.id.profileBtn)
        setupProfileLogout(this, profileBtn)


        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getLong("current_user_id", -1L) // -1 = pas connecté

        if (currentUserId == -1L) {
            // Pas d'utilisateur connecté → retourner au LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // L'utilisateur est connecté → tu peux récupérer ses infos
            lifecycleScope.launch {
                val user = AppDatabase.getDatabase(this@Accueil).userDao().getUserById(currentUserId)
                if (user != null) {
                    userNameTextView.text = user.userName
                }
            }
        }

        btnActivityTemps.setOnClickListener {
            val intent = Intent(this@Accueil, WeatherActivity::class.java)
            startActivity(intent)
        }
        btnActivityProjet.setOnClickListener {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getLong("current_user_id", -1L)
            if (currentUserId == -1L) {
                // Pas connecté
                val intent = Intent(this@Accueil, LoginActivity::class.java)
                startActivity(intent)
                return@setOnClickListener
            }

            // Vérifier en base s'il existe un projet actif pour l'utilisateur
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@Accueil)
                val activeProject = withContext(Dispatchers.IO) {
                    db.projetDao().getActiveProjectForUser(currentUserId)
                }

                if (activeProject != null) {
                    // Un projet actif existe
                    val intent = Intent(this@Accueil, ProjetListActivity::class.java)
                    startActivity(intent)
                } else {
                    // Pas de projet actif -> créer un nouveau
                    val intent = Intent(this@Accueil, InputProjetActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        btnActivityProgres.setOnClickListener {
            val intent = Intent(this@Accueil, LoginActivity::class.java)
            startActivity(intent)
        }

        val nameFromIntent = intent.getStringExtra("username")
        if (!nameFromIntent.isNullOrBlank()) {
            userNameTextView.text = nameFromIntent
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@Accueil)

            val lastUser = withContext(Dispatchers.IO){
                db.userDao().getLastUser()
            }
            userNameTextView.text = lastUser?.userName ?: "Aucun nom trouvé"
        }
    }
}