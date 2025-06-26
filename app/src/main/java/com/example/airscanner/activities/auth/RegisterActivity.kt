package com.example.airscanner.activities.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.airscanner.R

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

        registerBtn.setOnClickListener {
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            if (password != confirm) {
                messageText.text = "Passwords do not match"
                return@setOnClickListener
            }

            if (!password.any { it.isUpperCase() } || !password.any { it.isDigit() }) {
                messageText.text = "Password must contain at least one capital letter and one number"
                return@setOnClickListener
            }

            messageText.text = "Registration successful!"
        }

        val goLoginBtn = findViewById<Button>(R.id.btn_go_login)
        goLoginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
