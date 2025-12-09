package com.example.personalapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
        // exécution en IO
        val projets = withContext(Dispatchers.IO) {
            db.projetDao().getAllProjetsWithUser()
        }
        adapter.submitList(projets.toList())
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
                // Voir détail du projet
                vb.root.setOnClickListener {
                    val intent = Intent(this@ProjetListActivity, ProjetDetailActivity::class.java)
                    intent.putExtra("PROJECT_ID", p.projetId)
                    startActivity(intent)
                }

            }
        }
    }
}
