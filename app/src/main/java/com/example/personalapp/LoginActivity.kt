package com.example.personalapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.personalapp.data.User
import com.example.personalapp.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var userInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialise le database
        database = AppDatabase.getDatabase(this)

        // Prendre le Views par l'id
        userInput = findViewById(R.id.username)
        passwordInput = findViewById(R.id.password)
        submitButton = findViewById(R.id.login)

        // Click du bouton submit
        submitButton.setOnClickListener setOnclickListener@{
            val userName = userInput.text.toString().trim()
            val passwd = passwordInput.text.toString()

            if (userName.isEmpty() || passwd.isEmpty()) {
                Toast.makeText(this, "Tous les champs doivent être remplis", Toast.LENGTH_SHORT).show()
                return@setOnclickListener
            }

            lifecycleScope.launch {
                Log.d("LoginActivity", "userName: $userName, passwd: $passwd")

                val resultat = loginOrAddUser(userName, passwd)

                Log.d("LoginActivity", "userDao.getUserByUsername: $resultat")

                when (resultat) {
                    is LoginResult.Success -> {
                        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        prefs.edit { putLong("current_user_id", resultat.user.id ?: 0L) }

                        val intent = Intent(this@LoginActivity, Accueil::class.java)
                        intent.putExtra("username", resultat.user.userName)
                        startActivity(intent)
                        finish()
                    }
                    is LoginResult.WrongPassword -> {
                        Toast.makeText(this@LoginActivity, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                    }
                    is LoginResult.Error -> {
                        Toast.makeText(this@LoginActivity, resultat.message, Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
    }

    private sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        object WrongPassword : LoginResult()
        data class Error(val message: String) : LoginResult()
    }

    private suspend fun loginOrAddUser(userName: String, passwd: String): LoginResult {
          return try {
              withContext(Dispatchers.IO) {
                  val existing = database.userDao().getUserByUsername(userName)
                  when {
                      existing == null -> {
                          val nouveauUser = User(userName = userName, password = passwd)
                          val nouveauId = database.userDao().addUser(nouveauUser)
                          val insertedUser = database.userDao().getUserById(nouveauId)
                              ?: return@withContext LoginResult.Error("Erreur lors de l'ajout de l'utilisateur")
                          LoginResult.Success(insertedUser)
                      }
                      existing.password == passwd -> {
                          LoginResult.Success(existing)
                      }
                      else -> {
                          LoginResult.WrongPassword
                      }
                  }
              }
          }
          catch (e: Exception) {
              e.printStackTrace()
              LoginResult.Error(e.localizedMessage)
          }
    }


}