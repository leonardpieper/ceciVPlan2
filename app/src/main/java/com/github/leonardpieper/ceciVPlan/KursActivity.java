package com.github.leonardpieper.ceciVPlan;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.File;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.widget.LinearLayout.*;

public class KursActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;

    private DatabaseReference mDatabase;
    private Intent intent;

    private LinearLayout lLayoutl;
    private Button dlBtn;
    private LinearLayout thumbnailRow;
    private int previousRowID = 0;
    private int thumbnailCount = 5000;

    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Drive API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE};

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_kurs);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        intent = getIntent();

        lLayoutl = (LinearLayout) findViewById(R.id.downloadsRl);

        mDatabase = FirebaseDatabase.getInstance().getReference();


        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        getResultsFromApi();
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            //FIXME
//            mOutputText.setText("No network connection available.");
        } else {
            getDownloadableMedia(intent.getStringExtra("name"), 5);
//            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    //FIXME: Error
//                    mOutputText.setText(
//                            "This app requires Google Play Services. Please install " +
//                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    public void getDownloadableMedia(String kurs, int count){
        Query kursStorageRef = mDatabase.child("Kurse").child(kurs).child("storagePath").orderByChild("uploadTime").limitToFirst(count);

        kursStorageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for(DataSnapshot childSnapshot : dataSnapshot.getChildren()){
                    String id = childSnapshot.child("id").getValue(String.class);
                    new MakeRequestTask(mCredential).execute(id);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public java.io.File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        java.io.File file = new java.io.File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
                Log.e("KursActivity", "Directory not created");
        }
        return file;
    }

    public void setDlBtn(Bitmap driveBitmap) {

//        EasyPermissions.requestPermissions(
//                this,
//                "This app needs to access your Google account (via Contacts).",
//                1009,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        java.io.File dir = getAlbumStorageDir("ceciplan");
        System.out.println("Hi");

//        java.io.File root = Environment.getExternalStorageDirectory();
//
//        java.io.File dir = new java.io.File(root.getAbsolutePath() + "/ceciplan");
//        dir.mkdirs();

        java.io.File file = new java.io.File(dir, "Hallo.png");

        try {
            FileOutputStream fs = new FileOutputStream(file);
            driveBitmap.compress(Bitmap.CompressFormat.PNG, 100, fs);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

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

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                KursActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Drive API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<String, Void, String> {
        private ImageView mOutputImage;
        private Bitmap bm;
        private String title;
        private com.google.api.services.drive.Drive mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Drive API Android Quickstar")
                    .build();
        }


        /**
         * Background task to call Drive API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected String doInBackground(String... params) {
//            android.os.Debug.waitForDebugger();
            try {
//                downloadFile(mService, getFileFromId());

                downloadFile(getFileFromId(params[0]));
                return "Hi";
//                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private File getFileFromId(String id) throws IOException {
            File file = mService.files().get(id).execute();

            System.out.println("Title: " + file.getTitle());
            title = file.getTitle();
            System.out.println("Dl: " + file.getWebContentLink());
            System.out.println("Description: " + file.getDescription());
            System.out.println("MIME type: " + file.getMimeType());
            System.out.println("MIME type: " + file.getThumbnailLink());
            return file;
        }

        private void downloadFile(File file) {
            if (file.getThumbnailLink() != null && file.getThumbnailLink().length() > 0) {
                try {
                    URL u = new URL(file.getThumbnailLink());
                    HttpURLConnection ucon = (HttpURLConnection) u.openConnection();
                    if (ucon.getResponseCode() > -1) {
                        Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
                        Matcher m = p.matcher(ucon.getContentType());

                        String charset = m.matches() ? m.group(1) : "UTF-8";

                        InputStream inputStream = ucon.getInputStream();
                        bm = BitmapFactory.decodeStream(inputStream);
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public CardView makeCard(String title, Bitmap image){
            int id = (int)(Math.random()*100);

            CardView cv = new CardView(KursActivity.this);
            RelativeLayout rl = new RelativeLayout(KursActivity.this);

            DisplayMetrics dm = new DisplayMetrics();
            KursActivity.this.getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
            int width = dm.widthPixels ;
            int halfWidth = new Double(width/2.3).intValue();

            float aspectRatio = image.getWidth() /
                    (float) image.getHeight();

            RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            rlParams.setMargins(8,8,8,8);

            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    halfWidth,
                    Math.round(halfWidth / aspectRatio)
            );


            LinearLayout.LayoutParams cvParams = new LinearLayout.LayoutParams(
                    halfWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            cvParams.setMargins(8,8,8,8);

            RelativeLayout.LayoutParams rlTvLayout = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            rlTvLayout.addRule(RelativeLayout.BELOW, id);
            rlTvLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            RelativeLayout.LayoutParams rlBtnLayout = new RelativeLayout.LayoutParams(100, 100);
            rlBtnLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            rlBtnLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);


            cv.setLayoutParams(cvParams);
            rl.setLayoutParams(rlParams);

            // Set CardView corner radius
            cv.setRadius(4);
            // Set cardView content padding
            cv.setContentPadding(15, 15, 15, 15);
            // Set CardView elevation
            cv.setCardElevation(5);

            ImageView ivImage = new ImageView(KursActivity.this);
            ivImage.setLayoutParams(imgParams);
            ivImage.setImageBitmap(image);
            ivImage.setScaleType(ImageView.ScaleType.FIT_START);
            ivImage.setId(id);

            TextView tvTitle = new TextView(KursActivity.this);
            tvTitle.setText(title);
            tvTitle.setLayoutParams(rlTvLayout);

            Button dlBtn = new Button(KursActivity.this);
            dlBtn.setLayoutParams(rlBtnLayout);
            dlBtn.setBackgroundResource(R.drawable.ic_file_download_white_24dp);




//            ll.setOrientation(VERTICAL);
            rl.addView(ivImage);
            rl.addView(tvTitle);
            rl.addView(dlBtn);

            cv.addView(rl);

            return cv;

        }


        @Override
        protected void onPreExecute() {
            mOutputImage = new ImageView(KursActivity.this);
//                mOutputText.setText("");
//                mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
//                mProgress.hide();
            //FIXME: Fix
            if (output == null) {
//                    mOutputText.setText("No results returned.");
            } else {
                CardView imageCard = makeCard(title, bm);
//                mOutputImage.setImageBitmap(bm);
//                mOutputImage.setScaleType(ImageView.ScaleType.FIT_XY);
//                mOutputImage.setAdjustViewBounds(true);



                if(thumbnailCount % 2 ==0){
                    thumbnailRow = new LinearLayout(KursActivity.this);
                    LinearLayout.LayoutParams lLParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    thumbnailRow.setLayoutParams(lLParams);
                    thumbnailRow.setId(thumbnailCount+0);

                    thumbnailRow.addView(imageCard);


//                    if(previousRowID !=5000){
                        lLayoutl.addView(thumbnailRow);
//                    }else{
//                    }

                    previousRowID = thumbnailRow.getId();
                    thumbnailCount++;
                }else{
                    thumbnailRow.addView(imageCard);
                    thumbnailCount++;
                }
//                rl.addView(mOutputImage);
            }


//                setDlBtn(bm);
        }

        @Override
        protected void onCancelled() {
//                mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            KursActivity.REQUEST_AUTHORIZATION);
                } else {
                    //FIXME: fix
//                        mOutputText.setText("The following error occurred:\n"
//                                + mLastError.getMessage());
                }
            } else {
                //FIXME: fix
//                    mOutputText.setText("Request cancelled.");
            }
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed in the action bar.
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            // If there are ancestor activities, they should be added here.
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
