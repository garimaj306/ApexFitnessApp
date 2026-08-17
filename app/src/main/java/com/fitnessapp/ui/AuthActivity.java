package com.fitnessapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fitnessapp.R;
import com.fitnessapp.data.firebase.FirebaseManager;
import com.fitnessapp.data.firebase.FirestoreCollections;
import com.fitnessapp.data.models.DailySummary;
import com.fitnessapp.data.models.UserProfile;
import com.fitnessapp.data.repository.FitnessRepository;
import com.fitnessapp.utils.DateTimeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

/**
 * Authentication Activity: High-Performance, Instant Sign In, Sign Up, and Guest Access.
 */
public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "AuthActivity";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    private TabLayout tabLayoutAuth;
    private TextInputLayout tilName;
    private TextInputEditText etName;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;
    private TextView tvForgotPassword;
    private MaterialButton btnPrimaryAuth;
    private ProgressBar progressAuth;

    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnGuestAccess;

    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseManager.getInstance(this).getAuth();
        firestore = FirebaseManager.getInstance(this).getFirestore();

        initViews();
        setupTabSwitcher();
        setupClickListeners();
    }

    private void initViews() {
        tabLayoutAuth = findViewById(R.id.tab_layout_auth);
        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        btnPrimaryAuth = findViewById(R.id.btn_primary_auth);
        progressAuth = findViewById(R.id.progress_auth);

        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
        btnGuestAccess = findViewById(R.id.btn_guest_access);
    }

    private void setupTabSwitcher() {
        tabLayoutAuth.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isSignUpMode = (tab.getPosition() == 1);
                updateFormUI();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateFormUI() {
        if (isSignUpMode) {
            tilName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            tvForgotPassword.setVisibility(View.GONE);
            btnPrimaryAuth.setText("CREATE ATHLETE ACCOUNT");
        } else {
            tilName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            tvForgotPassword.setVisibility(View.VISIBLE);
            btnPrimaryAuth.setText("SIGN IN");
        }
        clearErrors();
    }

    private void setupClickListeners() {
        btnPrimaryAuth.setOnClickListener(v -> {
            if (isSignUpMode) {
                handleSignUp();
            } else {
                handleSignIn();
            }
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        btnGuestAccess.setOnClickListener(v -> handleGuestLogin());
        btnGoogleSignIn.setOnClickListener(v -> handleGuestLogin());
    }

    private void handleSignIn() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (!validateSignIn(email, password)) return;

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String uid = user != null ? user.getUid() : "athlete";
                    String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Athlete";
                    String em = user != null ? user.getEmail() : email;

                    // Immediately update in-memory state and route to dashboard
                    FitnessRepository.getInstance(AuthActivity.this).resetToFreshUser(uid, name, em);
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Sign In Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void handleSignUp() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (!validateSignUp(name, email, password, confirmPass)) return;

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    String uid = firebaseUser != null ? firebaseUser.getUid() : "new_athlete";

                    // 1. Immediately reset fresh zero-state and route directly to Main Dashboard (no lag)
                    FitnessRepository.getInstance(AuthActivity.this).resetToFreshUser(uid, name, email);
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Welcome to Apex Fit, " + name + "!", Toast.LENGTH_SHORT).show();
                    navigateToMain();

                    // 2. Background async provisioning of User and Daily Summary documents
                    provisionNewUserFirestoreAsync(uid, email, name);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AuthActivity.this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void provisionNewUserFirestoreAsync(String userId, String email, String displayName) {
        UserProfile newProfile = new UserProfile(userId, email, displayName);
        newProfile.setTargetStepGoal(10000);
        newProfile.setCreatedAt(Timestamp.now());

        DailySummary todaySummary = new DailySummary(DateTimeUtils.getTodayDateKey());
        todaySummary.setCurrentStreakDays(1);
        todaySummary.setStepsCount(0);
        todaySummary.setDistanceTravelledKm(0.0);
        todaySummary.setCaloriesBurntKcal(0.0);
        todaySummary.setActiveDurationMinutes(0);
        todaySummary.setWaterIntakeMl(0);
        todaySummary.setLastUpdatedAt(Timestamp.now());

        try {
            firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .set(newProfile, SetOptions.merge());

            firestore.collection(FirestoreCollections.USERS)
                    .document(userId)
                    .collection(FirestoreCollections.DAILY_SUMMARIES)
                    .document(todaySummary.getDate())
                    .set(todaySummary, SetOptions.merge());
        } catch (Exception ex) {
            Log.e(TAG, "Background provisioning warning: " + ex.getMessage());
        }
    }

    private void handleGuestLogin() {
        setLoading(true);
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    FirebaseUser user = auth.getCurrentUser();
                    String uid = user != null ? user.getUid() : "demo_athlete_apex";
                    FitnessRepository.getInstance(AuthActivity.this).resetToFreshUser(uid, "Demo Athlete", "guest@apexfit.com");
                    Toast.makeText(AuthActivity.this, "Welcome, Demo Athlete!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
    }

    private void handleForgotPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter your email address first");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> Toast.makeText(AuthActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(AuthActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean validateSignIn(String email, String password) {
        clearErrors();
        boolean valid = true;
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Valid email is required");
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            valid = false;
        }
        return valid;
    }

    private boolean validateSignUp(String name, String email, String password, String confirmPassword) {
        clearErrors();
        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            tilName.setError("Name is required");
            valid = false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Valid email is required");
            valid = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void setLoading(boolean isLoading) {
        progressAuth.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnPrimaryAuth.setEnabled(!isLoading);
        btnGoogleSignIn.setEnabled(!isLoading);
        btnGuestAccess.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
