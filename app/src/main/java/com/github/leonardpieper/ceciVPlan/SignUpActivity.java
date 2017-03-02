package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private final int LOGIN_REQUEST_CODE = 6001;

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    private Button nexButton;
    private Button cancelBtn;
    private Button btnSignLogin;

    private Button ceciLoginBtn;

    private TextView slctCeciTv;
    private TextView slctHGTv;

    private EditText email;
    private  EditText pwd;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        email = (EditText)findViewById(R.id.personalEmail);
        pwd = (EditText)findViewById(R.id.signPwd);

        nexButton = (Button)findViewById(R.id.btnSignUpNext);
        cancelBtn = (Button)findViewById(R.id.signBtnNoSign);
        btnSignLogin = (Button)findViewById(R.id.btnSignLogin);

        nexButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                setContentView(R.layout.activity_sign_up_schoolchooser);
                if(mAuth.getCurrentUser()!=null) {
                    if (mAuth.getCurrentUser().isAnonymous()) {
                        migrateAnonToPwd();
                        SignUpActivity.this.finish();
                    } else {
                        if(!email.getText().toString().isEmpty()&&!pwd.getText().toString().isEmpty()){
                            signUp();
                        }

                    }
                }else {
                    if(!email.getText().toString().isEmpty()&&!pwd.getText().toString().isEmpty()){
                        signUp();
                    }
                }

            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                builder.setTitle("Möchtest du wirklich abbrechen?")
                        .setMessage("Wenn du deine Email nicht hinzufügst kannst du unter umständen nicht alle Funktionen der App nnutzen.")
                        .setPositiveButton("Nein", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setNegativeButton("Ja", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SignUpActivity.this.finish();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        btnSignLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signIntent = new Intent(SignUpActivity.this, LoginActivity.class);
                signIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(signIntent, LOGIN_REQUEST_CODE);
            }
        });

    }

    private void migrateAnonToPwd(){
        AuthCredential credential = EmailAuthProvider.getCredential(email.getText().toString(), pwd.getText().toString());
        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "linkWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signUp(){
        mAuth.createUserWithEmailAndPassword(email.getText().toString(), pwd.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            String s = task.getException().getMessage();
                            Toast.makeText(SignUpActivity.this, s,
                                    Toast.LENGTH_SHORT).show();
                        }else {
                            setContentView(R.layout.activity_sign_up_log_in_ceci);
                            loginCeci();
                        }
                    }
                });
    }

    private void loginCeci(){
        ceciLoginBtn = (Button)findViewById(R.id.ceciLogBtn);
        final EditText ceciUname = (EditText)findViewById(R.id.ceciUnameET);
        final EditText ceciPwd = (EditText)findViewById(R.id.ceciPwdET);

        ceciLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference conditionRef = mRootRef
                        .child("Users")
                        .child(mAuth.getCurrentUser().getUid())
                        .child("vPlan");
                DatabaseReference pwRef = conditionRef.child("pwd");
                DatabaseReference uNameRef = conditionRef.child("uname");

                String uname = ceciUname.getText().toString();
                String pwd = ceciPwd.getText().toString();

                pwRef.setValue(pwd);
                uNameRef.setValue(uname);

                SignUpActivity.this.finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case LOGIN_REQUEST_CODE:
                if(resultCode == RESULT_OK) {
                    this.finish();
                    break;
                }
        }
    }

    //    public void onCreateSchoolChooser(){
//        slctCeciTv = (TextView)findViewById(R.id.tvSlctCeci);
//        slctHGTv = (TextView)findViewById(R.id.tvSlctHG);
//        slctCeciTv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
//        slctHGTv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(SignUpActivity.this, "Noch nicht verfügbar", Toast.LENGTH_LONG).show();
//            }
//        });
//    }
}
