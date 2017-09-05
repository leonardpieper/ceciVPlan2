package com.github.leonardpieper.ceciVPlan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.PopupMenu;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.tools.LocalUser;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.concurrent.Executor;

public class SignUpAnonActivity extends AppCompatActivity {
    private static final String TAG = "SignUpAnonActivity";

    FirebaseAuth mAuth;
    DatabaseReference mRootRef = MyDatabaseUtil.getDatabase().getReference();

    private EditText etVPlanUname;
    private EditText etVPlanPwd;
    private Button yearBtn;
    private ProgressBar progressBarProgress;

    private Button btnSignUp;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_anon);

        mAuth = FirebaseAuth.getInstance();

        etVPlanUname = (EditText) findViewById(R.id.signUpAnon_et_vplan_uname);
        etVPlanPwd = (EditText) findViewById(R.id.signUpAnon_et_vplan_pwd);
        yearBtn = (Button) findViewById(R.id.signUpAnon_btn_year);
        progressBarProgress = (ProgressBar) findViewById(R.id.signUpAnon_progress_progBar);
        btnSignUp = (Button) findViewById(R.id.signUpAnon_btn_signUp);
        btnLogin = (Button) findViewById(R.id.signUpAnon_btn_login);

        TextView tvDataProt = (TextView)findViewById(R.id.signUpAnon_tv_dataProt);
        tvDataProt.setMovementMethod(LinkMovementMethod.getInstance());

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!etVPlanUname.getText().toString().isEmpty() && !etVPlanPwd.getText().toString().isEmpty()) {
                    progressBarProgress.setVisibility(View.VISIBLE);
                    signUpAnon(etVPlanUname.getText().toString(), etVPlanPwd.getText().toString());
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpAnonActivity.this, SignUpActivity.class);
                startActivity(intent);
                SignUpAnonActivity.this.finish();
            }
        });

        yearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(SignUpAnonActivity.this, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        yearBtn.setText(item.getTitle());
                        LocalUser user = new LocalUser(SignUpAnonActivity.this);
                        user.setJahrgangText(item.getTitle().toString());

                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SignUpAnonActivity.this);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("jahrgang", item.getTitle().toString());
                        editor.commit();
                        return true;
                    }
                });
                popupMenu.inflate(R.menu.jahrgang_popup_menu);
                popupMenu.show();
            }
        });


    }

    private void signUpAnon(final String vplanUName, final String vplanPwd) {
        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    HashMap<String, Object> hm = new HashMap<>();
                    hm.put("uname", vplanUName);
                    hm.put("pwd", vplanPwd);
                    mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("vPlan").setValue(hm);

                    progressBarProgress.setVisibility(View.GONE);

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SignUpAnonActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("pref_vplan_etpref_user",vplanUName);
                    editor.putString("pref_vplan_etpref_pwd", vplanPwd);
                    editor.commit();


                    SignUpAnonActivity.this.finish();
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText(SignUpAnonActivity.this, "Anmeldung fehlgeschlagen",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        //Do Nothing!
    }
}
