package com.shehan.automart.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shehan.automart.R;
import com.shehan.automart.databinding.ActivityLoginBinding;
import com.shehan.automart.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    private CredentialManager credentialManager;
    private ExecutorService executorService;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        executorService = Executors.newSingleThreadExecutor();

        db = FirebaseFirestore.getInstance();

        binding.signinSignupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });

        binding.signinSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.signinEmail.getText().toString().trim();
                String password = binding.signinPassword.getText().toString().trim();

                if (email.isEmpty()) {
                    binding.signinEmail.setError("Email is required!");
                    binding.signinEmail.requestFocus();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.signinEmail.setError("Email is invalid!");
                    binding.signinEmail.requestFocus();
                    return;
                }

                if (password.isEmpty()) {
                    binding.signinPassword.setError("Password is required!");
                    binding.signinPassword.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    binding.signinPassword.setError("Password must be at least 6 characters!");
                    binding.signinPassword.requestFocus();
                    return;
                }


                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Toast.makeText(LoginActivity.this, "Authentication successful.", Toast.LENGTH_SHORT).show();


                                    FirebaseMessaging.getInstance().getToken()
                                            .addOnCompleteListener(task2 -> {
                                                if (!task2.isSuccessful()) return;

                                                String token = task2.getResult();

                                                FirebaseFirestore.getInstance()
                                                        .collection("user")
                                                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                        .update("fcm_token", token);
                                            });


                                    updateUI(user);
                                } else {
                                    Toast.makeText(LoginActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


            }
        });

        binding.signinGsigninBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CancellationSignal cancellationSignal = new CancellationSignal();

        credentialManager.getCredentialAsync(
                LoginActivity.this,
                request,
                cancellationSignal,
                executorService,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        handleGoogleSignIn(credential);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    private void handleGoogleSignIn(Credential credential) {
        if (credential instanceof CustomCredential){
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())){
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(customCredential.getData());

                String idToken = googleIdTokenCredential.getIdToken();
                firebaseAuthWithGoogle(idToken);
            }else {
                Toast.makeText(LoginActivity.this,"Unexpected credential type",Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(LoginActivity.this,"Invalid credential",Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(firebaseCredential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        User user = User.builder()
                                .id(currentUser.getUid())
                                .name(currentUser.getDisplayName())
                                .email(currentUser.getEmail())
                                .phoneNumber(currentUser.getPhoneNumber() == null ? "" : currentUser.getPhoneNumber())
                                .profileImage(currentUser.getPhotoUrl() == null ? "" : currentUser.getPhotoUrl().toString())
                                .build();

                        db.collection("user").document(currentUser.getUid()).set(user)
                                .addOnSuccessListener(unused -> {

                                    FirebaseMessaging.getInstance().getToken()
                                            .addOnCompleteListener(task3 -> {
                                                if (!task3.isSuccessful()) {
                                                    updateUI(currentUser);
                                                    return;
                                                }

                                                String token = task3.getResult();

                                                FirebaseFirestore.getInstance()
                                                        .collection("user")
                                                        .document(currentUser.getUid())
                                                        .update("fcm_token", token)
                                                        .addOnCompleteListener(task2 -> {
                                                            updateUI(currentUser);
                                                        });
                                            });

                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, "Sign Up Failed!", Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void updateUI(FirebaseUser user) {
        if (user != null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (mAuth.getCurrentUser() != null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}