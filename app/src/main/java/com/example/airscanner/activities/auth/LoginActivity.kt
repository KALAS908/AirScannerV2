package com.example.airscanner.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.*
import com.example.airscanner.R
import com.example.airscanner.activities.MainActivity
import com.example.airscanner.models.TokenManager
import com.example.airscanner.services.AuthRequest
import com.example.airscanner.services.AuthResponse
import com.example.airscanner.services.AuthService
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        // Initialize TokenManager
        TokenManager.init(this)

        // Check if already logged in
        if (TokenManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val loginBtn = findViewById<Button>(R.id.btn_login)
        val messageText = findViewById<TextView>(R.id.tv_message)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5181/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val authService = retrofit.create(AuthService::class.java)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                messageText.text = "Please fill in all fields"
                return@setOnClickListener
            }

            // Create auth request (empty strings for 2FA fields since they're not used)
            val authRequest = AuthRequest(email, password, "", "")

            authService.login(authRequest).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        if (authResponse != null) {
                            // Save tokens globally
                            TokenManager.saveTokens(
                                authResponse.accessToken,
                                authResponse.refreshToken,
                                authResponse.tokenType
                            )

                            messageText.text = "Login successful!"

                            // Navigate to main activity
                            navigateToMain()
                        }
                    } else {
                        messageText.text = "Invalid email or password"
                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    messageText.text = "Network error. Please try again."
                }
            })
        }

        val goRegisterBtn = findViewById<Button>(R.id.btn_go_register)
        goRegisterBtn.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
