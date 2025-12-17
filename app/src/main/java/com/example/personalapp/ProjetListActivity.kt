package com.example.personalapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.AppDatabase
import com.example.personalapp.data.ProjetWithUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.personalapp.databinding.ActivityProjetListBinding
import com.example.personalapp.databinding.ItemProjetBinding
import com.example.personalapp.utils.setupProfileLogout

class ProjetListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjetListBinding
    private lateinit var adapter: ProjetAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProjetListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar back button
        setSupportActionBar(binding.topToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.backBtn.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Déconnexion
        binding.topToolbar.findViewById<View>(R.id.profileBtn)?.setOnClickListener {
            val profileBtn: ImageView = findViewById(R.id.profileBtn)
            setupProfileLogout(this, profileBtn)
        }
        // RecyclerView setup
        adapter = ProjetAdapter()
        binding.recyclerProjets.layoutManager = LinearLayoutManager(this)
        binding.recyclerProjets.adapter = adapter

        // Database instance
        db = AppDatabase.getDatabase(this)

        // Charger les projets depuis la BDD
        lifecycleScope.launch {
            loadProjets()
        }
    }

    private suspend fun loadProjets() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentUserId = prefs.getLong("current_user_id", -1L)

        val projets = withContext(Dispatchers.IO) {
            db.projetDao().getProjetsWithUserForUser(currentUserId)
        }

        adapter.submitList(projets)
    }

    // Adapter RecyclerView simple
    inner class ProjetAdapter : RecyclerView.Adapter<ProjetAdapter.ProjetVH>() {
        private val items = mutableListOf< ProjetWithUser>()

        fun submitList(list: List<ProjetWithUser>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjetVH {
            val bindingItem = ItemProjetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ProjetVH(bindingItem)
        }

        override fun onBindViewHolder(holder: ProjetVH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ProjetVH(private val vb: ItemProjetBinding) : RecyclerView.ViewHolder(vb.root) {
            fun bind(p: ProjetWithUser) {
                vb.tvProjetName.text = p.projetName
                vb.tvProjetDuration.text = "Durée : ${p.durationMinutes} min"
                vb.tvProjetUser.text = "Par : ${p.userName}"

                // Affichage strike-through si terminé
                if (p.isCompleted) {
                    // Rayé
                    vb.tvProjetName.paintFlags = vb.tvProjetName.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    vb.tvProjetDuration.paintFlags = vb.tvProjetDuration.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    vb.tvProjetUser.paintFlags = vb.tvProjetUser.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    vb.btnComplete.isEnabled = false
                    vb.btnComplete.alpha = 0.5f
                    vb.root.isClickable = false
                } else {
                    // Normal
                    vb.tvProjetName.paintFlags = vb.tvProjetName.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    vb.tvProjetDuration.paintFlags = vb.tvProjetDuration.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    vb.tvProjetUser.paintFlags = vb.tvProjetUser.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    vb.btnComplete.isEnabled = true
                    vb.btnComplete.alpha = 1.0f
                    vb.root.isClickable = true
                }

                // Clic pour voir détail (uniquement si pas terminé)
                vb.root.setOnClickListener {
                    if (!p.isCompleted) {
                        val intent = Intent(this@ProjetListActivity, ProjetDetailActivity::class.java)
                        intent.putExtra("PROJECT_ID", p.projetId)
                        startActivity(intent)
                    }
                }

                // Clic sur "Terminer" -> mettre à jour la BDD
                vb.btnComplete.setOnClickListener {
                    // sécurité : désactiver pour éviter double clic
                    vb.btnComplete.isEnabled = false
                    lifecycleScope.launch {
                        // Construire l'entité Projet pour update
                        val updatedProjet = com.example.personalapp.data.Projet(
                            id = p.projetId,
                            name = p.projetName,
                            durationMinutes = p.durationMinutes,
                            userId = p.userId,
                            isCompleted = true
                        )
                        withContext(Dispatchers.IO) {
                            db.projetDao().update(updatedProjet)
                        }
                        loadProjets()
                    }
                }
            }
        }

    }
}
