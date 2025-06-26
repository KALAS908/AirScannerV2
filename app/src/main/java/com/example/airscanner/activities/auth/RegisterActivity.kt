package com.example.airscanner.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.airscanner.R
import com.example.airscanner.services.AuthRequest
import com.example.airscanner.services.AuthService
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.*
data class ErrorResponse(
    val title: String,
    val status: Int,
    val errors: Map<String, List<String>>
)


class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)

        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val confirmInput = findViewById<EditText>(R.id.password_input_confirm)
        val registerBtn = findViewById<Button>(R.id.btn_register)

        val messageText = findViewById<TextView>(R.id.tv_message)

        val retrofit = Retrofit.Builder()
            //.baseUrl("https://airscanner-h5d0bhehefe9h3cu.northeurope-01.azurewebsites.net/")
            .baseUrl("http://10.0.2.2:5181/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val authService = retrofit.create(AuthService::class.java)

        registerBtn.setOnClickListener {
            val email = emailInput.text.toString();
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            if (password != confirm) {
                messageText.text = "Passwords do not match"
                return@setOnClickListener
            }

            if (password.length < 6) {
                messageText.text = "Password must be at least 6 characters long"
                return@setOnClickListener
            }
            if (!password.any { it.isUpperCase() }) {
                messageText.text = "Password must contain at least one uppercase letter"
                return@setOnClickListener
            }
            if (!password.any { it.isLowerCase() }) {
                messageText.text = "Password must contain at least one lowercase letter"
                return@setOnClickListener
            }
            if (!password.any { it.isDigit() }) {
                messageText.text = "Password must contain at least one digit"
                return@setOnClickListener
            }
            if (!password.any { !it.isLetterOrDigit() }) {
                messageText.text = "Password must contain at least one special character"
                return@setOnClickListener
            }

            val authRequest = AuthRequest (email,password,"test","test");



            retrofit.create(AuthService::class.java)
                .register(authRequest)
                .enqueue(object : Callback<Void> {
                override fun onResponse(call: retrofit2.Call<Void>, response: retrofit2.Response<Void>) {
                    if (response.isSuccessful) {
                        messageText.text = "Registration successful!"

                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else if (response.code() == 400) {
                        try {
                            val errorBody = response.errorBody()?.string()
                            val gson = com.google.gson.Gson()
                            val errorResponse = gson.fromJson(errorBody, ErrorResponse::class.java)

                            val errorMessages = mutableListOf<String>()
                            errorResponse.errors.forEach { (key, messages) ->
                                when (key) {
                                    "DuplicateUserName" -> errorMessages.add("This email is already registered")
                                    "Password" -> errorMessages.addAll(messages)
                                    else -> errorMessages.addAll(messages)
                                }
                            }

                            messageText.text = errorMessages.joinToString("\n")
                        } catch (e: Exception) {
                            messageText.text = "Registration failed. Please try again."
                        }
                    } else {
                        messageText.text = "Registration failed. Please try again."
                    }
                }

                override fun onFailure(call: retrofit2.Call<Void>, t: Throwable) {
                    messageText.text = "Network error. Please check your connection."
                }
            })


                messageText.text = "Registration successful!"
        }

        val goLoginBtn = findViewById<Button>(R.id.btn_go_login)
        goLoginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
