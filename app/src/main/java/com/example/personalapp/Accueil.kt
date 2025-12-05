package com.example.personalapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.UserDatabase
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

        btnActivityTemps.setOnClickListener {
            val intent = Intent(this@Accueil, WeatherActivity::class.java)
            startActivity(intent)
        }
        btnActivityProjet.setOnClickListener {
            val intent = Intent(this@Accueil, LoginActivity::class.java)
            startActivity(intent)
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
            val db = UserDatabase.getDatabase(this@Accueil)

            val lastUser = withContext(Dispatchers.IO){
                db.userDao().getLastUser()
            }
            userNameTextView.text = lastUser?.userName ?: "Aucun nom trouvé"
        }
    }
}