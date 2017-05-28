package com.github.leonardpieper.ceciVPlan;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.tools.LocalUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;

    private EditText etEmail;
    private EditText etPwd;
    private Spinner spinYear;
    private Button btnLogin;

    private String selectedYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();

        etEmail = (EditText)findViewById(R.id.etEmail_reAcc);
        etPwd = (EditText)findViewById(R.id.etPwd_reAcc);
        spinYear = (Spinner)findViewById(R.id.spinYear_reAcc);
        btnLogin = (Button)findViewById(R.id.btnLogin_reAcc);

        spinYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        selectedYear = "Q2";
                        break;
                    case 1:
                        selectedYear = "Q1";
                        break;
                    case 2:
                        selectedYear = "EF";
                        break;
                    case 3:
                        selectedYear = "Lehrer";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String pwd = etPwd.getText().toString();

                if (!email.isEmpty() && !pwd.isEmpty()) {
                    mAuth.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                            if(task.isSuccessful()) {
                                LocalUser user = new LocalUser(LoginActivity.this);
                                user.setJahrgangText(selectedYear);
                                //Login succeeded --> Activity closes itself
                                setResult(RESULT_OK, null);
                                LoginActivity.this.finish();
                            }


                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "signInWithEmail:failed", task.getException());

                                FirebaseAuthException e = (FirebaseAuthException) task.getException();
                                switch (e.getErrorCode()) {
                                    case "ERROR_USER_NOT_FOUND":
                                        etEmail.setError("Benutzer nicht gefunden");
                                        etEmail.requestFocus();
                                        break;
                                    case "ERROR_INVALID_EMAIL":
                                        etEmail.setError("Ungültige E-Mail Adresse");
                                        etEmail.requestFocus();
                                        break;
                                    case "ERROR_WRONG_PASSWORD":
                                        etPwd.setError("Falsches Passwort");
                                        etPwd.requestFocus();
                                        break;
                                    case "ERROR_USER_DISABLED":
                                        etEmail.setError("Benutzerkonto deaktiviert");
                                        etEmail.requestFocus();
                                        break;
                                    default:
                                        Log.d(TAG, e.getErrorCode());

                                }
                            }
                        }
                    });

                }
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
