package com.example.personalapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.personalapp.ui.WeatherViewModel
import com.example.personalapp.ui.WeatherViewModelFactory
import com.example.personalapp.databinding.ActivityWeatherBinding
import com.example.personalapp.network.RetrofitClient
import com.example.personalapp.network.WeatherService
import com.example.personalapp.repository.WeatherRepository
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.utils.setupProfileLogout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeatherBinding

    private val viewModel: WeatherViewModel by viewModels {
        val service = RetrofitClient.retrofit.create(WeatherService::class.java)
        val repo = WeatherRepository(service, BuildConfig.OPENWEATHER_API_KEY)
        WeatherViewModelFactory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeatherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUiActions()
        observeViewModel()

        viewModel.loadCity("Paris,FR")

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentUserId = prefs.getLong("current_user_id", -1L) // -1 = pas connecté

        if (currentUserId == -1L) {
            // Pas d'utilisateur connecté → retourner au LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            lifecycleScope.launch {
                val user = AppDatabase.getDatabase(this@WeatherActivity).userDao().getUserById(currentUserId)
            }
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
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getLong("current_user_id", -1L)
            if (currentUserId == -1L) {
                // Pas connecté
                val intent = Intent(this@WeatherActivity, LoginActivity::class.java)
                startActivity(intent)
                return@setOnClickListener
            }

            // Vérifier en base s'il existe un projet actif pour l'utilisateur
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(this@WeatherActivity)
                val activeProject = withContext(Dispatchers.IO) {
                    db.projetDao().getActiveProjectForUser(currentUserId)
                }

                if (activeProject != null) {
                    // Un projet actif existe
                    val intent = Intent(this@WeatherActivity, ProjetListActivity::class.java)
                    intent.putExtra("PROJECT_ID", activeProject.id)
                    startActivity(intent)
                } else {
                    // Pas de projet actif -> message
                    Toast.makeText(this@WeatherActivity, "Aucun projet actif", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { s ->
                // Loading
                binding.progressBar.visibility = if (s.loading) View.VISIBLE else View.GONE

                // Temperature -> on adapte le titre principal pour afficher la température
                val tempText = s.temp?.let { String.format("%.0f°C", it) } ?: "--°C"
                // Construire un titre principal : garde ton style mais intègre la température
                binding.titleMain.text = when {
                    s.temp == null -> getString(R.string.loading_title)
                    s.temp <= 5 -> "Brrr... une journée fraîche s’annonce !\n$tempText"
                    s.temp <= 15 -> "Il fait frais aujourd'hui\n$tempText"
                    s.temp <= 25 -> "Journée agréable\n$tempText"
                    else -> "Il fait chaud aujourd'hui\n$tempText"
                }

                // Phrase / subtext : description ou conseil selon la météo
                binding.subtext.text = when {
                    s.temp == null -> "Chargement des informations météo..."
                    s.temp <= 5 -> "Un bon moment pour te concentrer sur tes objectifs et organiser ton espace."
                    s.temp <= 15 -> "C'est l'heure de bouger, fais tes appels et travaille depuis un lieu inspirant, n'oublie pas d'apporter un petit pull."
                    s.temp <= 25 -> "Journée agréable, idéal pour sortir et travailler dehors."
                    else -> "Il fait chaud aujourd'hui, n'oublie pas de t'hydrater, reste à l'ombre et fais des tâches légères!"
                }

                if (!s.icon.isNullOrEmpty()) {
                    val iconUrl = "https://openweathermap.org/img/wn/${s.icon}@4x.png"
                    Glide.with(this@WeatherActivity)
                        .load(iconUrl)
                        .placeholder(R.drawable.weather_froid)
                        .into(binding.imageThermometer)
                } else {
                    binding.imageThermometer.setImageResource(R.drawable.weather_froid)
                }
                if (!s.error.isNullOrEmpty()) {
                    Toast.makeText(this@WeatherActivity, "Erreur: ${s.error}", Toast.LENGTH_SHORT).show()
                }

                supportActionBar?.title = s.city ?: getString(R.string.app_name)
            }
        }
    }
}
