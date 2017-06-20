package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.tools.LocalUser;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
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
    private CountryCodePicker ccpCode;

    private TextView tvDatenschutz;

    private Button phoneSignUp;
    private Button emailSignUp;
    private Button btnVerify;
    private Button signUpBtn;
    private Button signUpHelp;

    private RelativeLayout signUpHelpRl;


    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        setTitle("Anmelden");

        mAuth = FirebaseAuth.getInstance();

        etPhone = (EditText) findViewById(R.id.signUp_et_phone);
        etSMSCode = (EditText) findViewById(R.id.signUp_et_smsCode);
        etEmail = (EditText) findViewById(R.id.signUp_et_email);
        etPwd = (EditText) findViewById(R.id.signUp_et_pwd);

        ccpCode = (CountryCodePicker)findViewById(R.id.signUp_ccp_code);
        ccpCode.registerCarrierNumberEditText(etPhone);

        btnVerify = (Button) findViewById(R.id.signUp_btn_Verify);

        phoneSignUp = (Button) findViewById(R.id.signUp_btn_phone);
        emailSignUp = (Button) findViewById(R.id.signUp_btn_email);
        signUpBtn = (Button) findViewById(R.id.signUp_btn_signUp);
        signUpHelp = (Button) findViewById(R.id.signUp_btn_help);

        signUpHelpRl = (RelativeLayout) findViewById(R.id.signUp_rl_help);

        tvDatenschutz = (TextView) findViewById(R.id.signUp_tv_dataProt);
        tvDatenschutz.setMovementMethod(LinkMovementMethod.getInstance());

        phoneSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPhone.setVisibility(View.VISIBLE);
                ccpCode.setVisibility(View.VISIBLE);
                signUpBtn.setVisibility(View.VISIBLE);
                tvDatenschutz.setVisibility(View.VISIBLE);
                etEmail.setVisibility(View.GONE);
                etPwd.setVisibility(View.GONE);

                phoneSignUp.setVisibility(View.GONE);
                emailSignUp.setVisibility(View.GONE);
                signUpHelpRl.setVisibility(View.GONE);
            }
        });
        emailSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etPhone.setVisibility(View.GONE);
                ccpCode.setVisibility(View.GONE);
                signUpBtn.setVisibility(View.VISIBLE);
                tvDatenschutz.setVisibility(View.VISIBLE);
                etEmail.setVisibility(View.VISIBLE);
                etPwd.setVisibility(View.VISIBLE);

                phoneSignUp.setVisibility(View.GONE);
                emailSignUp.setVisibility(View.GONE);
                signUpHelpRl.setVisibility(View.GONE);
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etPhone.isShown() && (!etEmail.isShown() || !etPwd.isShown())) {
                    String phoneNumber = ccpCode.getFullNumberWithPlus();

                    etPhone.setVisibility(View.GONE);
                    ccpCode.setVisibility(View.GONE);
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

        signUpHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                builder.setTitle("Informationen zur Anmeldung")
                        .setMessage(Html.fromHtml(new StringBuilder()
                                .append("Ceciplan benötigt Ihre Anmeldedaten in Form von Telefonnummer oder E-Mail-Adresse um Missbrauch des Service zu vermindern.<br /><br />")
                                .append("Da es möglich ist nutzergenerierte Inhalte innerhalb der App zu erstellen, wird zum Schutz aller Benutzer eine Kontaktmöglichkeit vorausgesetzt. Die App soll <b>keine Möglichkeit für anonymes Mobbing</b> geben. Ferner soll sichergestellt werden, dass gesetzeswidrige Medien oder Schriften nicht über diese Plattform verbreitet werden.<br /><br />")
                                .append("Sollten Sie Datenschutzbedenken haben lesen Sie sich bitte die <a href=“http://leonardpieper.github.io/ceciplan/content/datenschutz.html“>Datenschutzbestimmungen</a> durch")
                                .toString()))
                        .setPositiveButton("Verstanden", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                builder.show();
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
        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.signUp_progress_progBar);
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithPhone:success");
                            FirebaseUser user = task.getResult().getUser();

                            vpCredentialsExist(user, progressBar);

                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithPhone:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast t = Toast.makeText(SignUpActivity.this, "Ungültiger Code", Toast.LENGTH_SHORT);
                                t.show();
                                progressBar.setVisibility(View.GONE);
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
                                        FirebaseUser user = task.getResult().getUser();

                                        vpCredentialsExist(user, progressBar);
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
                                        loadVPlanlayout();
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

    private void vpCredentialsExist(FirebaseUser user, final ProgressBar progressBar){
        mRootRef.child("Users").child(user.getUid()).child("vPlan").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                if(dataSnapshot.hasChild("uname")){
                    SignUpActivity.this.finish();
                }else {
                    loadVPlanlayout();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                loadVPlanlayout();
            }
        });
    }

    private void loadVPlanlayout(){
        Button finishBtn = (Button)findViewById(R.id.signUp_btn_finish);
        final Button yearBtn = (Button)findViewById(R.id.signUp_btn_year);
        final TextView vplanUname = (TextView)findViewById(R.id.signUp_et_vplan_uname);
        final TextView vplanPwd = (TextView)findViewById(R.id.signUp_et_vplan_pwd);

        signUpBtn.setVisibility(View.GONE);
        tvDatenschutz.setVisibility(View.GONE);
        etEmail.setVisibility(View.GONE);
        etPwd.setVisibility(View.GONE);
        etSMSCode.setVisibility(View.GONE);
        btnVerify.setVisibility(View.GONE);

        finishBtn.setVisibility(View.VISIBLE);
        yearBtn.setVisibility(View.VISIBLE);
        vplanUname.setVisibility(View.VISIBLE);
        vplanPwd.setVisibility(View.VISIBLE);

        yearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(SignUpActivity.this, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        yearBtn.setText(item.getTitle());
                        LocalUser user = new LocalUser(SignUpActivity.this);
                        user.setJahrgangText(item.getTitle().toString());
                        return true;
                    }
                });
                popupMenu.inflate(R.menu.jahrgang_popup_menu);
                popupMenu.show();
            }
        });

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uname = vplanUname.getText().toString();
                String pwd = vplanPwd.getText().toString();
                if(!uname.isEmpty()&&!pwd.isEmpty()&&mAuth.getCurrentUser()!=null){
                    HashMap<String, Object> hm = new HashMap<>();
                    hm.put("uname", uname);
                    hm.put("pwd", pwd);
                    mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("vPlan").setValue(hm);

                    SignUpActivity.this.finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
//        View finishBtn = findViewById(R.id.signUp_vplan_btn_finish);
        if(signUpBtn!=null&&signUpBtn.isShown()){
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.signUp_progress_progBar);
            progressBar.setVisibility(View.GONE);

            etPhone.setVisibility(View.GONE);
            ccpCode.setVisibility(View.GONE);
            signUpBtn.setVisibility(View.GONE);
            tvDatenschutz.setVisibility(View.GONE);
            etEmail.setVisibility(View.GONE);
            etPwd.setVisibility(View.GONE);

            phoneSignUp.setVisibility(View.VISIBLE);
            emailSignUp.setVisibility(View.VISIBLE);
            signUpHelpRl.setVisibility(View.VISIBLE);
        }else if(btnVerify!=null && btnVerify.isShown()){
            etPhone.setVisibility(View.VISIBLE);
            ccpCode.setVisibility(View.VISIBLE);
            etSMSCode.setVisibility(View.GONE);
            btnVerify.setVisibility(View.GONE);
            signUpBtn.setVisibility(View.VISIBLE);
        }else{
            super.onBackPressed();
        }

    }
}
