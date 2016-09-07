package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvLoggedInUser;
    private EditText etUname;
    private EditText etPwd;
    private Button btnJahrgangslct;
    private Button btnLogin;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvLoggedInUser = (TextView)findViewById(R.id.tvLogInUser);
        etUname = (EditText)findViewById(R.id.etUname);
        etPwd = (EditText)findViewById(R.id.etPwd);
        btnLogin = (Button)findViewById(R.id.btnSpinnerJahrgang);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        btnLogout = (Button)findViewById(R.id.btnLogout);

        btnJahrgangslct = (Button)findViewById(R.id.btnSpinnerJahrgang);

        mAuth = FirebaseAuth.getInstance();

        isfbLoggedIn();


        btnJahrgangslct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] stufen = new String[]{"EF", "Q1", "Q2"};

                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle("Wähle deine Jahrgangsstufe");
                builder.setItems(stufen, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("jahrgang", stufen[which]);
                        editor.commit();

                        btnJahrgangslct.setText(stufen[which]);
                    }
                });
                builder.show();
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uname = etUname.getText().toString();
                String email = uname + "@example.com";
                String pwd  =etPwd.getText().toString();

                mAuth.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(SettingsActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(SettingsActivity.this, "Anmeldung erfolgreich", Toast.LENGTH_LONG).show();
                            isfbLoggedIn();
                        }else{
                            Toast.makeText(SettingsActivity.this, "Anmeldung fehlgeschlagen", Toast.LENGTH_LONG).show();
                        }
                    }
                });


            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Snackbar.make(v, "Abgemeldet", Snackbar.LENGTH_LONG).show();
                isfbLoggedOut();
            }
        });
    }

    public void isfbLoggedIn(){
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            String uName = user.getEmail().replace("@example.com", "");
            tvLoggedInUser.setText("Hallo " + uName);

            tvLoggedInUser.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);

            etUname.setVisibility(View.GONE);
            etPwd.setVisibility(View.GONE);
            btnJahrgangslct.setVisibility(View.GONE);
            btnLogin.setVisibility(View.GONE);
        }
    }

    public void isfbLoggedOut(){
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null){
//            String uName = user.getEmail().replace("@example.com", "");
//            tvLoggedInUser.setText("Hallo " + uName);

            tvLoggedInUser.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);

            etUname.setVisibility(View.VISIBLE);
            etPwd.setVisibility(View.VISIBLE);
            btnJahrgangslct.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);
        }
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
