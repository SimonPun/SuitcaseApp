package com.jivanpun.suitcaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterPage extends AppCompatActivity {
    // Declare UI elements
    private TextInputEditText editTextEmail, editTextPassword;
    private Button signUp;
    private TextView signIn;

    // Firebase Authentication instance
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);

        // Initialize UI elements
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        signUp = findViewById(R.id.sign_up);
        signIn = findViewById(R.id.sign_in);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Set click listener for Sign Up button
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retrieve user input
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();

                // Validate email and password
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(RegisterPage.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterPage.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isPasswordValid(password)) {
                    Toast.makeText(RegisterPage.this, "Password must meet complexity requirements.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Create user with Firebase Authentication
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Registration successful, navigate to main activity
                                    Toast.makeText(RegisterPage.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterPage.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Registration failed, show error message
                                    Toast.makeText(RegisterPage.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        // Set click listener for Sign In text
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to the main activity
                Intent intent = new Intent(RegisterPage.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    // Override the back button press behavior
    @Override
    public void onBackPressed() {
        // Navigate to the main activity
        Intent intent = new Intent(RegisterPage.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Check if the password meets complexity requirements
    private boolean isPasswordValid(String password) {
        // Define password complexity requirements using a regular expression
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}