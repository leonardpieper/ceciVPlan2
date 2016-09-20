package com.github.leonardpieper.ceciVPlan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SignUpActivity extends AppCompatActivity {

    private Button nexButton;
    private TextView slctCeciTv;
    private TextView slctHGTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        nexButton = (Button)findViewById(R.id.btnSignUpNext);

        nexButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.activity_sign_up_schoolchooser);
                onCreateSchoolChooser();
            }
        });
    }

    public void onCreateSchoolChooser(){
        slctCeciTv = (TextView)findViewById(R.id.tvSlctCeci);
        slctHGTv = (TextView)findViewById(R.id.tvSlctHG);
        slctCeciTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        slctHGTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SignUpActivity.this, "Noch nicht verfügbar", Toast.LENGTH_LONG).show();
            }
        });
    }
}
