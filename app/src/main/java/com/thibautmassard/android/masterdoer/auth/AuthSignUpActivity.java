package com.thibautmassard.android.masterdoer.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.thibautmassard.android.masterdoer.R;
import com.thibautmassard.android.masterdoer.ui.AddProjectActivity;
import com.thibautmassard.android.masterdoer.ui.AddProjectFragment;
import com.thibautmassard.android.masterdoer.ui.MainActivity;

import static android.view.View.GONE;

/**
 * Created by thib146 on 08/04/2017.
 */

public class AuthSignUpActivity extends BaseActivity implements
        View.OnClickListener {

    private static final String TAG = "AnonymousAuth";

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private DatabaseReference mFirebaseDatabaseRef;

    private boolean userCreated = false;

    // [START declare_auth_listener]
    private FirebaseAuth.AuthStateListener mAuthListener;
    // [END declare_auth_listener]

    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    private EditText mNameField;
    private EditText mEmailField;
    private EditText mPasswordField;

    private Switch mAuthSwitch;
    private TextView mSubscribeTextView;
    private TextView mConnectTextView;

    private Button mButtonSignIn;
    private Button mButtonSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_signup);

        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        mFirebaseDatabaseRef = FirebaseDatabase.getInstance().getReference();

        // [START auth_state_listener]
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    Intent mainIntent = new Intent(AuthSignUpActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // [START_EXCLUDE]
                updateUI(user);
                // [END_EXCLUDE]
            }
        };
        // [END auth_state_listener]

        // Fields
        mNameField = (EditText) findViewById(R.id.auth_signup_name);
        mEmailField = (EditText) findViewById(R.id.auth_signup_email);
        mPasswordField = (EditText) findViewById(R.id.auth_signup_password);
        mAuthSwitch = (Switch) findViewById(R.id.auth_switch);
        mSubscribeTextView = (TextView) findViewById(R.id.textview_subscribe);
        mConnectTextView = (TextView) findViewById(R.id.textview_connect);
        mButtonSignIn = (Button) findViewById(R.id.auth_signup_button_signin);
        mButtonSignUp = (Button) findViewById(R.id.auth_signup_button_signup);

        // Click listeners
        mButtonSignIn.setOnClickListener(this);
        mButtonSignUp.setOnClickListener(this);
        mAuthSwitch.setOnClickListener(this);
        mSubscribeTextView.setOnClickListener(this);
        mConnectTextView.setOnClickListener(this);
    }

    // [START on_start_add_listener]
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    // [END on_start_add_listener]

    // [START on_stop_remove_listener]
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
    // [END on_stop_remove_listener]

    private void signInWithEmailAndPassword(String email, String password) {
        showProgressDialog();
        // [START signin_anonymously]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmailAndPassword:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmailAndPassword", task.getException());
                            Toast.makeText(AuthSignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            hideProgressDialog();

                            Intent mainIntent = new Intent(AuthSignUpActivity.this, MainActivity.class);
                            startActivity(mainIntent);
                        }
                    }
                });
        // [END signin_anonymously]
    }

    private void createUserWithEmailAndPassword(final String email, final String password) {
        showProgressDialog();
        // [START signin_anonymously]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmailAndPassword:onComplete:" + task.isSuccessful());
                        userCreated = true;

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "createUserWithEmailAndPassword", task.getException());
                            Toast.makeText(AuthSignUpActivity.this, "User creation failed.",
                                    Toast.LENGTH_SHORT).show();
                            userCreated = false;
                        } else {

                            updateUI(mAuth.getCurrentUser());

                            // [START_EXCLUDE]
                            hideProgressDialog();
                            // [END_EXCLUDE]

                            Intent mainIntent = new Intent(AuthSignUpActivity.this, MainActivity.class);
                            startActivity(mainIntent);
                        }
                    }
                });
        // [END signin_anonymously]
    }

    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private void updateUI(FirebaseUser user) {
        hideProgressDialog();

        //TextView emailView = (TextView) findViewById(R.id.signup_email_check);
        boolean isSignedIn = (user != null);

        // Status text
        //if (isSignedIn) {
        //    emailView.setText(user.getEmail());
        //} else {
        //    emailView.setText(null);
        //}

        if (userCreated && user != null) {
            mFirebaseDatabaseRef.child("users").child(user.getUid()).setValue(user);
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxProjectId").setValue(0);
            mFirebaseDatabaseRef.child("users").child(user.getUid()).child("maxTaskId").setValue(0);

            String userName = mNameField.getText().toString();
            String userEmail = mEmailField.getText().toString();
            if (userName.length() != 0) {
                mFirebaseDatabaseRef.child("users").child(user.getUid()).child("userName").setValue(userName);
            } else {
                int index = userEmail.indexOf('@');
                userEmail = userEmail.substring(0, index);
                mFirebaseDatabaseRef.child("users").child(user.getUid()).child("userName").setValue(userEmail);
            }

            userCreated = false;
        }

        // Button visibility
        findViewById(R.id.auth_signup_button_signup).setEnabled(!isSignedIn);
        findViewById(R.id.auth_signup_button_signin).setEnabled(!isSignedIn);
    }

    private void switchConnexionMode(boolean textClick) {
        if (mAuthSwitch.isChecked()) {
            subscriptionMode();
        } else {
            connexionMode();
        }
    }

    private void connexionMode() {
        if (Build.VERSION.SDK_INT < 23) {
            mSubscribeTextView.setTextAppearance(getApplicationContext(), R.style.boldText);
            mConnectTextView.setTextAppearance(getApplicationContext(), R.style.normalText);
        } else {
            mSubscribeTextView.setTextAppearance(R.style.boldText);
            mConnectTextView.setTextAppearance(R.style.normalText);
        }
        mNameField.setVisibility(View.VISIBLE);
        mButtonSignIn.setVisibility(GONE);
        mButtonSignUp.setVisibility(View.VISIBLE);
    }

    private void subscriptionMode() {
        if (Build.VERSION.SDK_INT < 23) {
            mSubscribeTextView.setTextAppearance(getApplicationContext(), R.style.normalText);
            mConnectTextView.setTextAppearance(getApplicationContext(), R.style.boldText);
        } else {
            mSubscribeTextView.setTextAppearance(R.style.normalText);
            mConnectTextView.setTextAppearance(R.style.boldText);
        }
        mNameField.setVisibility(GONE);
        mButtonSignIn.setVisibility(View.VISIBLE);
        mButtonSignUp.setVisibility(GONE);
    }

    @Override
    public void onClick(View v) {
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();
        int i = v.getId();
        if (i == R.id.auth_signup_button_signup) {
            if (mEmailField.getText().length() != 0 && mPasswordField.getText().length() != 0) {
                createUserWithEmailAndPassword(email, password);
            }
        } else if (i == R.id.auth_signup_button_signin) {
            if (mEmailField.getText().length() != 0 && mPasswordField.getText().length() != 0) {
                signInWithEmailAndPassword(email, password);
            }
        } else if (i == R.id.auth_switch) {
            switchConnexionMode(false);
        } else if (i == R.id.textview_subscribe) {
            mAuthSwitch.setChecked(false);
            switchConnexionMode(false);
        } else if (i == R.id.textview_connect) {
            mAuthSwitch.setChecked(true);
            switchConnexionMode(false);
        }
    }
}