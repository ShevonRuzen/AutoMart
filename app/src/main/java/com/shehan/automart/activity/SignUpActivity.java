package com.shehan.automart.activity;


import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.databinding.ActivitySignUpBinding;
import com.shehan.automart.model.User;


public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.signupSigninLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        binding.signupSignupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUp();
            }
        });
    }

    private void signUp() {
        String name = binding.signupName.getText().toString().trim();
        String email = binding.signupEmail.getText().toString().trim();
        String mobile = binding.signupMobile.getText().toString().trim();
        String password = binding.signupPassword.getText().toString().trim();

        if (name.isEmpty()){
            binding.signupName.setError("Name is required!");
            binding.signupName.requestFocus();
            return;
        }

        if (email.isEmpty()){
            binding.signupEmail.setError("Email is required!");
            binding.signupEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            binding.signupEmail.setError("Email is invalid!");
            binding.signupEmail.requestFocus();
            return;
        }

        if (mobile.isEmpty()){
            binding.signupMobile.setError("Mobile number is required!");
            binding.signupMobile.requestFocus();
            return;
        }

        if (!mobile.matches("^07[01245678][0-9]{7}$")){
            binding.signupMobile.setError("Mobile number is invalid!");
            binding.signupMobile.requestFocus();
            return;
        }

        if (password.isEmpty()){
            binding.signupPassword.setError("Password is required!");
            binding.signupPassword.requestFocus();
            return;
        }

        if (password.length() < 6 ){
            binding.signupPassword.setError("Password must be at least 6 characters!");
            binding.signupPassword.requestFocus();
            return;
        }


        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String uid = task.getResult().getUser().getUid().toString();

                    User user = User.builder()
                            .id(uid)
                            .name(name)
                            .email(email)
                            .phoneNumber(mobile)
                            .build();

                    db.collection("user").document(uid).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            updateUI(currentUser);
                            Toast.makeText(SignUpActivity.this, "Sign Up Successful!", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SignUpActivity.this, "Sign Up Failed!", Toast.LENGTH_SHORT).show();
                        }
                    });

                }else{
                    Toast.makeText(SignUpActivity.this, "Sign Up Failed!"+ task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null){
            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}