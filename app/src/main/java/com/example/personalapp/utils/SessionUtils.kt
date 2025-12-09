package com.example.personalapp.utils

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import android.widget.PopupMenu
import com.example.personalapp.LoginActivity

fun setupProfileLogout(context: Context, profileBtn: ImageView) {
    profileBtn.setOnClickListener { view ->
        val popup = PopupMenu(context, view)
        popup.menu.add("Se déconnecter")
        popup.setOnMenuItemClickListener { item ->
            if (item.title == "Se déconnecter") {
                // Supprimer l'utilisateur connecté
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().remove("current_user_id").apply()

                // Retour au LoginActivity
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
            true
        }
        popup.show()
    }
}
