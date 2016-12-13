package com.github.leonardpieper.ceciVPlan;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpAnonymActivity extends AppCompatActivity {
    private static final String TAG = "SignUpAnonymActivity";

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    private EditText pwdET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_anonym);

        getSupportActionBar().hide();

        pwdET = (EditText)findViewById(R.id.anon_pwd);
        Button anonButton = (Button)findViewById(R.id.anon_button);
        anonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userToFB();
            }
        });

        mAuth = FirebaseAuth.getInstance();
    }

    private void userToFB(){
        if(mAuth.getCurrentUser().getEmail()!=null){
            final String email = mAuth.getCurrentUser().getEmail();

            mAuth.signOut();
            mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                    Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInAnonymously", task.getException());
                            Toast.makeText(SignUpAnonymActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }else{
                            DatabaseReference conditionRef = mRootRef
                                    .child("Users")
                                    .child(mAuth.getCurrentUser().getUid())
                                    .child("vPlan");
                            DatabaseReference pwRef = conditionRef.child("pwd");
                            DatabaseReference uNameRef = conditionRef.child("uname");

                            String uname = email.substring(0, email.indexOf("@"));
                            String pwd = pwdET.getText().toString();

                            pwRef.setValue(pwd);
                            uNameRef.setValue(uname);

                            SignUpAnonymActivity.this.goToNonAnonSignUp();
                        }
                    }
                });
        }
    }

    private void goToNonAnonSignUp(){
        Intent signIntent = new Intent(this, SignUpActivity.class);
        signIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        signIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(signIntent);
        this.finish();
    }
}
