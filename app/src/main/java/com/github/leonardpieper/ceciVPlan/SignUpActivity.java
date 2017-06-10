package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private final int LOGIN_REQUEST_CODE = 6001;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    private Button nexButton;
    private Button cancelBtn;

    private Button ceciLoginBtn;

    private TextView slctCeciTv;
    private TextView slctHGTv;

    private FirebaseAuth mAuth;

    private EditText etPhone;
    private EditText etSMSCode;
    private EditText etEmail;
    private EditText etPwd;

    private TextView tvDatenschutz;

    private Button phoneSignUp;
    private Button emailSignUp;
    private Button btnVerify;
    private Button signUpBtn;


    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        etPhone = (EditText) findViewById(R.id.signUp_et_phone);
        etSMSCode = (EditText) findViewById(R.id.signUp_et_smsCode);
        etEmail = (EditText) findViewById(R.id.signUp_et_email);
        etPwd = (EditText) findViewById(R.id.signUp_et_pwd);

        btnVerify = (Button) findViewById(R.id.signUp_btn_Verify);

        phoneSignUp = (Button) findViewById(R.id.signUp_btn_phone);
        emailSignUp = (Button) findViewById(R.id.signUp_btn_email);
        signUpBtn = (Button) findViewById(R.id.signUp_btn_signUp);

        tvDatenschutz = (TextView) findViewById(R.id.signUp_tv_dataProt);
        tvDatenschutz.setMovementMethod(LinkMovementMethod.getInstance());

        phoneSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPhone.setVisibility(View.VISIBLE);
                signUpBtn.setVisibility(View.VISIBLE);
                tvDatenschutz.setVisibility(View.VISIBLE);
                etEmail.setVisibility(View.GONE);
                etPwd.setVisibility(View.GONE);

                phoneSignUp.setVisibility(View.GONE);
                emailSignUp.setVisibility(View.GONE);
            }
        });
        emailSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPhone.setVisibility(View.GONE);
                signUpBtn.setVisibility(View.VISIBLE);
                tvDatenschutz.setVisibility(View.VISIBLE);
                etEmail.setVisibility(View.VISIBLE);
                etPwd.setVisibility(View.VISIBLE);

                phoneSignUp.setVisibility(View.GONE);
                emailSignUp.setVisibility(View.GONE);
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etPhone.isShown() && (!etEmail.isShown() || !etPwd.isShown())) {
                    String phoneNumber = etPhone.getText().toString();

                    etPhone.setVisibility(View.GONE);
                    etSMSCode.setVisibility(View.VISIBLE);
                    btnVerify.setVisibility(View.VISIBLE);
                    signUpBtn.setVisibility(View.GONE);

                    loginWithPhone(phoneNumber);

                } else if (etEmail.isShown() && etPwd.isShown() && !etPhone.isShown()) {
                    String email = etEmail.getText().toString();
                    String pwd = etPwd.getText().toString();

                    loginWithEmail(email, pwd);
                }
            }
        });

    }

    private void loginWithPhone(String phoneNumber) {
        if(phoneNumber!=null&&!phoneNumber.isEmpty()) {
            mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted:" + phoneAuthCredential);

                    signInWithPhoneAuthCredential(phoneAuthCredential);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    Log.w(TAG, "onVerificationFailed", e);
                }

                @Override
                public void onCodeSent(final String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    Log.d(TAG, "onCodeSent:" + verificationId);


                    btnVerify.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String smsCode = etSMSCode.getText().toString();
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, smsCode);
                            signInWithPhoneAuthCredential(credential);
                        }
                    });
                }
            };

            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    mCallbacks
            );
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithPhone:success");

                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithPhone:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }

    private void loginWithEmail(final String email, final String password){
        if(email!=null&&password!=null&&!email.isEmpty()&&!password.isEmpty()) {
            final ProgressBar progressBar = (ProgressBar)findViewById(R.id.signUp_progress_progBar);
            progressBar.setVisibility(View.VISIBLE);

            mAuth.fetchProvidersForEmail(email).addOnCompleteListener(this, new OnCompleteListener<ProviderQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                    if (task.isSuccessful()) {
                        //Überprüft, ob ein Nutzer mit der Email bereits existiert.
                        if (task.getResult().getProviders() != null && task.getResult().getProviders().size() >= 1) {
                            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "signInWithEmail:success");
                                        SignUpActivity.this.finish();
                                    } else {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthInvalidUserException e) {
                                            etEmail.setError("Ungültige E-Mail Adresse");
                                            etEmail.requestFocus();
                                        } catch (FirebaseAuthInvalidCredentialsException e) {
                                            etPwd.setError("Falsches Passwort");
                                            etPwd.requestFocus();
                                        } catch (Exception e) {
                                            Log.e(TAG, e.getMessage());
                                        }
                                    }
                                }
                            });
                        } else {
                            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    progressBar.setVisibility(View.GONE);
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "createUserWithEmail:success");
                                        SignUpActivity.this.finish();
                                    } else {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthWeakPasswordException e) {
                                            etPwd.setError("Passwort zu schwach (mind. 6 Zeichen)");
                                            etPwd.requestFocus();
                                        } catch (FirebaseAuthInvalidCredentialsException e) {
                                            etEmail.setError("Ungültige E-Mail Adresse");
                                            etEmail.requestFocus();
                                        } catch (FirebaseAuthUserCollisionException e) {
                                            etEmail.setError("Dieser Nutzer existiert bereits");
                                            etEmail.requestFocus();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            etEmail.setError("Ungültige E-Mail Adresse");
                            etEmail.requestFocus();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if(signUpBtn.isShown()){
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.signUp_progress_progBar);
            progressBar.setVisibility(View.GONE);

            etPhone.setVisibility(View.GONE);
            signUpBtn.setVisibility(View.GONE);
            tvDatenschutz.setVisibility(View.GONE);
            etEmail.setVisibility(View.GONE);
            etPwd.setVisibility(View.GONE);

            phoneSignUp.setVisibility(View.VISIBLE);
            emailSignUp.setVisibility(View.VISIBLE);
        }else if(btnVerify.isShown()){
            etPhone.setVisibility(View.VISIBLE);
            etSMSCode.setVisibility(View.GONE);
            btnVerify.setVisibility(View.GONE);
            signUpBtn.setVisibility(View.VISIBLE);
        }else{
            super.onBackPressed();
        }

    }
}
