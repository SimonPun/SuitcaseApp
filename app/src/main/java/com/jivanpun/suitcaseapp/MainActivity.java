package com.jivanpun.suitcaseapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {
    // Declare UI elements
    private TextInputEditText editTextEmail, editTextPassword;
    private Button signIn;
    private TextView signUp, forgotPassword;
    private ImageView googleSignup;

    // Firebase Authentication instance
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        signIn = findViewById(R.id.sign_in);
        signUp = findViewById(R.id.sign_up);
        googleSignup = findViewById(R.id.google_btn);
        forgotPassword = findViewById(R.id.forgot_password);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Check if a user is already logged in
        if (firebaseAuth.getCurrentUser() != null) {
            // User is already logged in, redirect to the home page
            Intent intent = new Intent(MainActivity.this, HomePage.class);
            startActivity(intent);
            finish(); // Finish MainActivity (login page)
        }

        // Set click listener for Sign Up text
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterPage.class);
                startActivity(intent);
                finish();
            }
        });

        // Set click listener for Forgot Password text
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showForgotPasswordDialog();
            }
        });

        // Initialize Google Sign-In options
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("315387309180-6fm29ae42qijsj84bn9kknadu5j6tpoh.apps.googleusercontent.com") // Add your client ID here
                .requestEmail()
                .build();

        // Initialize Google Sign-In client
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        // Set click listener for Google Sign-Up button
        googleSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Initialize sign-in intent
                Intent intent = googleSignInClient.getSignInIntent();
                // Start activity for result
                startActivityForResult(intent, 100);
            }
        });

        // Set click listener for Sign In button
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get user input
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();

                // Validate email and password
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, "Enter Email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sign in with Firebase Authentication
                firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Login successful, navigate to the home page
                                    Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(MainActivity.this, HomePage.class);
                                    intent.putExtra("loggedIn", true); // Set a flag indicating successful login
                                    startActivity(intent);
                                    finish(); // Finish MainActivity (login page)
                                } else {
                                    // Login failed, show error message
                                    if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                        // Invalid email
                                        Toast.makeText(MainActivity.this, "Invalid Email", Toast.LENGTH_SHORT).show();
                                    } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                        // Invalid password
                                        Toast.makeText(MainActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Other authentication error
                                        Toast.makeText(MainActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check request code
        if (requestCode == 100) {
            // When request code is 100, initialize task
            Task<GoogleSignInAccount> signInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
            // Check if the task is successful
            if (signInAccountTask.isSuccessful()) {
                // Google sign-in successful, initialize string
                String successMessage = "Google sign-in successful";
                // Display a Toast message
                displayToast(successMessage);
                try {
                    // Initialize the sign-in account
                    GoogleSignInAccount googleSignInAccount = signInAccountTask.getResult(ApiException.class);
                    if (googleSignInAccount != null) {
                        // When sign-in account is not null, initialize auth credential
                        AuthCredential authCredential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);
                        // Sign in with Firebase using the credential
                        firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Firebase authentication successful, redirect to profile activity
                                    startActivity(new Intent(getApplicationContext(), HomePage.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                    displayToast("Login successful");
                                } else {
                                    // Firebase authentication failed, display error message
                                    displayToast("Authentication Failed: " + task.getException().getMessage());
                                }
                            }
                        });
                    }
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Helper method to display a Toast message
    private void displayToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Method to show the forgot password dialog
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        View view = LayoutInflater.from(this).inflate(R.layout.forgotpassword, null);
        builder.setView(view);

        TextInputEditText emailEditText = view.findViewById(R.id.forgotPasswordEmail);
        Button resetButton = view.findViewById(R.id.resetPasswordButton);

        AlertDialog dialog = builder.create();

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = emailEditText.getText().toString().trim();
                if (TextUtils.isEmpty(email)) {
                    emailEditText.setError("Enter your email");
                    return;
                }

                firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(getApplicationContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
    }
}