package com.github.leonardpieper.ceciVPlan;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.tools.LocalUser;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

//    GoogleAccountCredential mCredential;

    private TextView tvLoggedInUser;

    private CardView cvTeacher;
    private EditText etTeacherShortc;
    private String year = "";
    private Button btnSave;

//    private Button btnJahrgangslct;
    private Button btnLogin;
    private Button btnAdvanced;
    private Button btnLogout;
//    private Button btnSignUp;
    private Button btnDriveLink;

    private EditText etVPlanU;
    private EditText etVPlanPwd;
    private Button btnSetVPlan;
    private CardView cvVPlan;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private DatabaseReference mRootRef = MyDatabaseUtil.getDatabase().getReference();

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private int easterEggCounter;

    private static final String PREF_ACCOUNT_NAME = "accountName";
//    private static final String[] SCOPES = {DriveScopes.DRIVE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvLoggedInUser = (TextView)findViewById(R.id.tvLogInUser);

        etTeacherShortc = (EditText)findViewById(R.id.etLehrerkrzl);
        btnSave = (Button)findViewById(R.id.btnSave);
        cvTeacher = (CardView)findViewById(R.id.cvTeacher);

//        btnLogin = (Button)findViewById(R.id.btnSpinnerJahrgang);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        btnAdvanced = (Button)findViewById(R.id.btnAdvanced);
        btnLogout = (Button)findViewById(R.id.btnLogout);
//        btnSignUp = (Button)findViewById(R.id.btnSignUpNew);
//        btnDriveLink = (Button)findViewById(R.id.btnDriveLink);

//        btnJahrgangslct = (Button)findViewById(R.id.btnSpinnerJahrgang);

        cvVPlan = (CardView)findViewById(R.id.cvVPlanCred);
        etVPlanU = (EditText)findViewById(R.id.set_vplanU);
        etVPlanPwd = (EditText)findViewById(R.id.set_vplanPwd);
        btnSetVPlan = (Button)findViewById(R.id.set_btnSetVPlan);

//        mCredential = GoogleAccountCredential.usingOAuth2(
//                getApplicationContext(), Arrays.asList(SCOPES))
//                .setBackOff(new ExponentialBackOff());

        mAuth = FirebaseAuth.getInstance();

        isfbLoggedIn();


//        btnJahrgangslct.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final String[] stufen = new String[]{"EF", "Q1", "Q2"};
//
//                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
//                builder.setTitle("Wähle deine Jahrgangsstufe");
//                builder.setItems(stufen, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
//                        SharedPreferences.Editor editor = preferences.edit();
//                        editor.putString("jahrgang", stufen[which]);
//                        editor.putInt("jahrgangNumber", convertJahrgang(stufen[which]));
//                        editor.commit();
//                        year = stufen[which];
//
//                        FirebaseAnalytics.getInstance(SettingsActivity.this).setUserProperty("jahrgangsstufe", String.valueOf(convertJahrgang(stufen[which])));
//
//                        btnJahrgangslct.setText(stufen[which]);
//                    }
//                });
//                builder.show();
//
//                easterEggCounter++;
//                if(easterEggCounter>=5){
//                    EasterEgg easterEgg = new EasterEgg(SettingsActivity.this);
//                    try {
//                        easterEgg.addEmoji("\uD83C\uDF6A ", "den Cookie");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    easterEggCounter = 0;
//                }
//            }
//        });

//        btnLogin.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String uname = etUname.getText().toString();
////                String email = uname + "@example.com";
//                String pwd  =etPwd.getText().toString();
//
//                if(!uname.isEmpty()&&!pwd.isEmpty()&&year!=""){
//                    mAuth.signInWithEmailAndPassword(uname, pwd).addOnCompleteListener(SettingsActivity.this, new OnCompleteListener<AuthResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<AuthResult> task) {
//                            if(task.isSuccessful()){
//                                Toast.makeText(SettingsActivity.this, "Anmeldung erfolgreich", Toast.LENGTH_LONG).show();
//                                isfbLoggedIn();
//                            }else{
//                                FirebaseAuthException e = (FirebaseAuthException) task.getException();
//                                switch (e.getErrorCode()) {
//                                    case "ERROR_USER_NOT_FOUND":
//                                        etUname.setError("Benutzer nicht gefunden");
//                                        etUname.requestFocus();
//                                        break;
//                                    case "ERROR_INVALID_EMAIL":
//                                        etUname.setError("Ungültige E-Mail Adresse");
//                                        etUname.requestFocus();
//                                        break;
//                                    case "ERROR_WRONG_PASSWORD":
//                                        etPwd.setError("Falsches Passwort");
//                                        etPwd.requestFocus();
//                                        break;
//                                    case "ERROR_USER_DISABLED":
//                                        etUname.setError("Benutzerkonto deaktiviert");
//                                        etUname.requestFocus();
//                                        break;
//                                    default:
//                                        Log.d(TAG, e.getErrorCode());
//
//                                }
////                                Toast.makeText(SettingsActivity.this, "Anmeldung fehlgeschlagen", Toast.LENGTH_LONG).show();
//                            }
//                        }
//                    });
//                }else{
//                    if(uname.isEmpty()){
//                        Toast.makeText(SettingsActivity.this, "Bitte E-Mail eingeben", Toast.LENGTH_SHORT).show();
//                    }else if(pwd.isEmpty()){
//                        Toast.makeText(SettingsActivity.this, "Bitte Passwort eingeben", Toast.LENGTH_SHORT).show();
//                    }else if(year==""){
//                        Toast.makeText(SettingsActivity.this, "Bitte Jahrgangsstufe auswählen", Toast.LENGTH_SHORT).show();
//                    }
//                }
//
//
//            }
//        });

        btnAdvanced.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);

                LayoutInflater factory = LayoutInflater.from(SettingsActivity.this);
                final View optionsView = factory.inflate(R.layout.dialog_advanced_login, null);

                final Switch teacherSwitch = (Switch) optionsView.findViewById(R.id.swtchTeacher);
                final EditText etTeacherPwd = (EditText) optionsView.findViewById(R.id.etTeacherPwd);

                builder.setView(optionsView)
                        .setTitle("Lehrer-Optionen")
                        // Add action buttons
                        .setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                LocalUser localUser = new LocalUser(SettingsActivity.this);
                                localUser.setTeacherStatus(teacherSwitch.isChecked());
                                localUser.setTeacherName(etTeacherPwd.getText().toString());

                                if(etTeacherPwd.getText().toString().length()>0) {
                                    if (mAuth.getCurrentUser() != null) {
                                        DatabaseReference leherPwdRef = mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("lehrerPwd");
                                        leherPwdRef.setValue(etTeacherPwd.getText().toString());
                                    }
                                }
                            }
                        })
                        .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                builder.show();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Snackbar.make(v, "Abgemeldet", Snackbar.LENGTH_LONG).show();
                isfbLoggedOut();

                LocalUser localUser = new LocalUser(SettingsActivity.this);
                localUser.resetAll();
            }
        });

//        btnSignUp.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent signAIntent = new Intent(SettingsActivity.this, SignUpActivity.class);
//                startActivity(signAIntent);
//            }
//        });

//        btnDriveLink.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                linkGDrive();
//            }
//        });

        btnSetVPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateVPlanCred();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference db = mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("abk");
                db.setValue(etTeacherShortc.getText().toString());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(SettingsActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("lehrer-abk", etTeacherShortc.getText().toString());
                editor.putBoolean("isTeacher", true);
                editor.commit();
            }
        });
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        isfbLoggedIn();
//    }

    public void isfbLoggedIn(){
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            if(!user.isAnonymous()) {
                tvLoggedInUser.setText("Hallo, " + user.getEmail());

                tvLoggedInUser.setVisibility(View.VISIBLE);
                btnLogout.setVisibility(View.VISIBLE);
                btnAdvanced.setVisibility(View.VISIBLE);
//                btnDriveLink.setVisibility(View.VISIBLE);
//                etTeacherShortc.setVisibility(View.VISIBLE);
//                btnSave.setVisibility(View.VISIBLE);
                cvVPlan.setVisibility(View.VISIBLE);

//                etUname.setVisibility(View.GONE);
//                etPwd.setVisibility(View.GONE);
//                btnJahrgangslct.setVisibility(View.GONE);
                btnLogin.setVisibility(View.GONE);
//                btnSignUp.setVisibility(View.GONE);

                LocalUser localUser = new LocalUser(SettingsActivity.this);
                if(localUser.getTeacherStatus()){
                    cvTeacher.setVisibility(View.VISIBLE);
                }
            }
        }else {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            overridePendingTransition(0,0);
        }
    }

    public void isfbLoggedOut(){
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null){
//            String uName = user.getEmail().replace("@example.com", "");
//            tvLoggedInUser.setText("Hallo " + uName);

            tvLoggedInUser.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
//            btnDriveLink.setVisibility(View.GONE);
            btnAdvanced.setVisibility(View.GONE);
//            etTeacherShortc.setVisibility(View.GONE);
//            btnSave.setVisibility(View.GONE);
            cvTeacher.setVisibility(View.GONE);
            cvVPlan.setVisibility(View.GONE);

//            etUname.setVisibility(View.VISIBLE);
//            etPwd.setVisibility(View.VISIBLE);
//            btnJahrgangslct.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);
//            btnSignUp.setVisibility(View.VISIBLE);
        }
    }

    private void updateVPlanCred(){
        String uname = etVPlanU.getText().toString();
        String pwd = etVPlanPwd.getText().toString();
        if(!uname.isEmpty()&&!pwd.isEmpty()){
            if(mAuth.getCurrentUser()!=null) {
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("uname", uname);
                hm.put("pwd", pwd);
                mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("vPlan").setValue(hm);
                Toast t = Toast.makeText(SettingsActivity.this, "Daten erfolgreich geändert", Toast.LENGTH_SHORT);
                t.show();
            }else {
                Toast t = Toast.makeText(SettingsActivity.this, "Kein Nutzer angemeldet", Toast.LENGTH_LONG);
                t.show();
            }
        }else {
            Toast t = Toast.makeText(SettingsActivity.this, "Bitte Benutzernamen und Passwort angeben", Toast.LENGTH_LONG);
            t.show();
        }
    }

//    private void linkGDrive(){
//        getResultsFromApi();
//    }
//
//    /**
//     * Attempt to call the API, after verifying that all the preconditions are
//     * satisfied. The preconditions are: Google Play Services installed, an
//     * account was selected and the device currently has online access. If any
//     * of the preconditions are not satisfied, the app will prompt the user as
//     * appropriate.
//     */
//    private void getResultsFromApi() {
//        if (!isGooglePlayServicesAvailable()) {
//            acquireGooglePlayServices();
//        } else if (mCredential.getSelectedAccountName() == null) {
//            chooseAccount();
//        } else if (!isDeviceOnline()) {
//            //FIXME
////            mOutputText.setText("No network connection available.");
//        } else {
//            Toast t = Toast.makeText(SettingsActivity.this, "Erfolgreich bei Google Drive angemeldet", Toast.LENGTH_LONG);
//            t.show();
//        }
//    }
//
//    /**
//     * Check that Google Play services APK is installed and up to date.
//     *
//     * @return true if Google Play Services is available and up to
//     * date on this device; false otherwise.
//     */
//    private boolean isGooglePlayServicesAvailable() {
//        GoogleApiAvailability apiAvailability =
//                GoogleApiAvailability.getInstance();
//        final int connectionStatusCode =
//                apiAvailability.isGooglePlayServicesAvailable(this);
//        return connectionStatusCode == ConnectionResult.SUCCESS;
//    }
//
//    /**
//     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
//     * Play Services installation via a user dialog, if possible.
//     */
//    private void acquireGooglePlayServices() {
//        GoogleApiAvailability apiAvailability =
//                GoogleApiAvailability.getInstance();
//        final int connectionStatusCode =
//                apiAvailability.isGooglePlayServicesAvailable(this);
//        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
//            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
//        }
//    }
//
//    /**
//     * Display an error dialog showing that Google Play Services is missing
//     * or out of date.
//     *
//     * @param connectionStatusCode code describing the presence (or lack of)
//     *                             Google Play Services on this device.
//     */
//    void showGooglePlayServicesAvailabilityErrorDialog(
//            final int connectionStatusCode) {
//        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
//        Dialog dialog = apiAvailability.getErrorDialog(
//                SettingsActivity.this,
//                connectionStatusCode,
//                REQUEST_GOOGLE_PLAY_SERVICES);
//        dialog.show();
//    }
//
//    /**
//     * Attempts to set the account used with the API credentials. If an account
//     * name was previously saved it will use that one; otherwise an account
//     * picker dialog will be shown to the user. Note that the setting the
//     * account to use with the credentials object requires the app to have the
//     * GET_ACCOUNTS permission, which is requested here if it is not already
//     * present. The AfterPermissionGranted annotation indicates that this
//     * function will be rerun automatically whenever the GET_ACCOUNTS permission
//     * is granted.
//     */
//    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
//    private void chooseAccount() {
//        if (EasyPermissions.hasPermissions(
//                this, android.Manifest.permission.GET_ACCOUNTS)) {
//            String accountName = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
//                    .getString(PREF_ACCOUNT_NAME, null);
//
//            if (accountName != null) {
//                mCredential.setSelectedAccountName(accountName);
//                getResultsFromApi();
//            } else {
//                // Start a dialog from which the user can choose an account
//                startActivityForResult(
//                        mCredential.newChooseAccountIntent(),
//                        REQUEST_ACCOUNT_PICKER);
//            }
//        } else {
//            // Request the GET_ACCOUNTS permission via a user dialog
//            EasyPermissions.requestPermissions(
//                    this,
//                    "This app needs to access your Google account (via Contacts).",
//                    REQUEST_PERMISSION_GET_ACCOUNTS,
//                    Manifest.permission.GET_ACCOUNTS);
//        }
//    }
//
//    /**
//     * Called when an activity launched here (specifically, AccountPicker
//     * and authorization) exits, giving you the requestCode you started it with,
//     * the resultCode it returned, and any additional data from it.
//     *
//     * @param requestCode code indicating which activity result is incoming.
//     * @param resultCode  code indicating the result of the incoming
//     *                    activity result.
//     * @param data        Intent (containing result data) returned by incoming
//     *                    activity result.
//     */
//    @Override
//    protected void onActivityResult(
//            int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        switch (requestCode) {
//            case REQUEST_GOOGLE_PLAY_SERVICES:
//                if (resultCode != RESULT_OK) {
//                    //FIXME: Error
////                    mOutputText.setText(
////                            "This app requires Google Play Services. Please install " +
////                                    "Google Play Services on your device and relaunch this app.");
//                } else {
//                    getResultsFromApi();
//                }
//                break;
//            case REQUEST_ACCOUNT_PICKER:
//                if (resultCode == RESULT_OK && data != null &&
//                        data.getExtras() != null) {
//                    String accountName =
//                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
//                    if (accountName != null) {
//                        SharedPreferences settings =
//                                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = settings.edit();
//                        editor.putString(PREF_ACCOUNT_NAME, accountName);
//                        editor.apply();
//                        mCredential.setSelectedAccountName(accountName);
//                        getResultsFromApi();
//                    }
//                }
//                break;
//            case REQUEST_AUTHORIZATION:
//                if (resultCode == RESULT_OK) {
//                    getResultsFromApi();
//                }
//                break;
//        }
//    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
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

    private int convertJahrgang(String jahrgang){
        switch (jahrgang.toLowerCase()){
            case "q2":
                return 12;
            case "q1":
                return 11;
            case "ef":
                return 10;
            default:
                return Integer.parseInt(jahrgang);
        }
    }
}
