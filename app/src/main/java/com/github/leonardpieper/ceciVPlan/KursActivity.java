package com.github.leonardpieper.ceciVPlan;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class KursActivity extends Activity
        implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private Button mCallApiButton;
    private ImageView mOutputImage;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Drive API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_METADATA };

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        mCallApiButton.setText(BUTTON_TEXT);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + BUTTON_TEXT +"\' button to test the API.");
        activityLayout.addView(mOutputText);

        mOutputImage = new ImageView(this);
        mOutputImage.setLayoutParams(tlp);
        activityLayout.addView(mOutputImage);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Drive API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }



    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
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
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
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

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
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
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
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
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
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
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
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
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private Bitmap bm;
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
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            android.os.Debug.waitForDebugger();
            try {
                downloadFile(mService, getThumbnailFromId());

                List<String> list = new ArrayList<>();
                list.add("Hi");
                return list;
//                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        private File getThumbnailFromId() throws IOException {
            File file = mService.files().get("0B7ppyvhHCrfgZjhlVURjdG5MSUU").execute();

            System.out.println("Title: " + file.getTitle());
            System.out.println("Dl: " + file.getSelfLink());
            System.out.println("Description: " + file.getWebContentLink());
            System.out.println("MIME type: " + file.getMimeType());
            return file;
        }

        /**
         * Download a file's content.
         *
         * @param service Drive API service instance.
         * @param file Drive File instance.
         * @return InputStream containing the file's content if successful,
         *         {@code null} otherwise.
         */
        private void downloadFile(Drive service, File file) {
            if (file.getWebContentLink() != null && file.getWebContentLink().length() > 0) {
                try {
//                    HttpResponse resp =
//                            service.getRequestFactory().buildGetRequest(new GenericUrl(file.getWebContentLink() + "&acknowledgeAbuse=true"))
//                                    .execute();
                    URL u = new URL(file.getWebContentLink());
                    HttpURLConnection ucon = (HttpURLConnection) u.openConnection();
                    if (ucon.getResponseCode() > -1) {
                        Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
                        Matcher m = p.matcher(ucon.getContentType());

                        String charset = m.matches() ? m.group(1) : "UTF-8";

                        InputStream inputStream = ucon.getInputStream();
//                        Reader r = new InputStreamReader(inputStream, charset);
//                        StringBuilder buf = new StringBuilder();
//                        while (true) {
//                            int ch = r.read();
//                            if (ch < 0) {
//                                break;
//                            }
//                            buf.append((char) ch);
//                        }
//                        System.out.println(buf.toString());
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


//        /**
//         * Fetch a list of up to 10 file names and IDs.
//         * @return List of Strings describing files, or an empty list if no files
//         *         found.
//         * @throws IOException
//         */
//        private List<String> getDataFromApi() throws IOException {
//
//            List<String> fileInfo = new ArrayList<String>();
//
//            List<File> result = new ArrayList<File>();
//            Drive.Files.List request = mService.files().list();
//
//            do {
//                try {
//                    FileList files = request.execute();
//
//                    result.addAll(files.getItems());
//                    request.setPageToken(files.getNextPageToken());
//                } catch (IOException e) {
//                    System.out.println("An error occurred: " + e);
//                    request.setPageToken(null);
//                }
//            } while (request.getPageToken() != null &&
//                    request.getPageToken().length() > 0);
//
//            // Get a list of up to 10 files.
////            List<String> fileInfo = new ArrayList<String>();
////            FileList result = mService.files().list()
////                    .setFields("nextPageToken, files(id, name)")
////                    .execute();
//
//            List<File> files = result;
//            if (files != null) {
//                for (File file : files) {
//                    fileInfo.add(String.format("%s (%s)\n",
//                            file.getTitle(), file.getId()));
//                }
//            }
//            return fileInfo;
//        }


            @Override
            protected void onPreExecute() {
                mOutputText.setText("");
                mProgress.show();
            }

            @Override
            protected void onPostExecute(List<String> output) {
                mProgress.hide();
                if (output == null || output.size() == 0) {
                    mOutputText.setText("No results returned.");
                } else {
                    output.add(0, "Data retrieved using the Drive API:");
                    mOutputText.setText(TextUtils.join("\n", output));
                    mOutputImage.setImageBitmap(bm);
                }
            }

            @Override
            protected void onCancelled() {
                mProgress.hide();
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
                        mOutputText.setText("The following error occurred:\n"
                                + mLastError.getMessage());
                    }
                } else {
                    mOutputText.setText("Request cancelled.");
                }
            }
        }
    }
////
////import android.content.Context;
////import android.content.Intent;
////import android.content.IntentSender;
////import android.graphics.Bitmap;
////import android.graphics.BitmapFactory;
////import android.graphics.drawable.Drawable;
////import android.net.Uri;
////import android.os.Environment;
////import android.support.annotation.NonNull;
////import android.support.annotation.Nullable;
////import android.support.design.widget.FloatingActionButton;
////import android.support.v7.app.AppCompatActivity;
////import android.os.Bundle;
////import android.util.Log;
////import android.view.MenuItem;
////import android.view.View;
////import android.widget.EditText;
////import android.widget.ImageView;
////import android.widget.LinearLayout;
////import android.widget.TextView;
////
////import com.google.android.gms.appindexing.Action;
////import com.google.android.gms.appindexing.AppIndex;
////import com.google.android.gms.common.ConnectionResult;
////import com.google.android.gms.common.GooglePlayServicesUtil;
////import com.google.android.gms.common.api.GoogleApiClient;
////import com.google.android.gms.common.api.ResultCallback;
////import com.google.android.gms.drive.Drive;
////import com.google.android.gms.drive.DriveApi;
////import com.google.android.gms.drive.DriveContents;
////import com.google.android.gms.drive.DriveFile;
////import com.google.android.gms.drive.DriveFolder;
////import com.google.android.gms.drive.DriveId;
////import com.google.android.gms.drive.query.Filters;
////import com.google.android.gms.drive.query.Query;
////import com.google.android.gms.drive.query.SearchableField;
////import com.google.android.gms.tasks.OnSuccessListener;
////import com.google.firebase.database.DataSnapshot;
////import com.google.firebase.database.DatabaseError;
////import com.google.firebase.database.DatabaseReference;
////import com.google.firebase.database.FirebaseDatabase;
////import com.google.firebase.database.ValueEventListener;
////import com.google.firebase.storage.FileDownloadTask;
////import com.google.firebase.storage.FirebaseStorage;
////import com.google.firebase.storage.StorageReference;
////
////import java.io.BufferedReader;
////import java.io.ByteArrayOutputStream;
////import java.io.File;
////import java.io.FileOutputStream;
////import java.io.FileWriter;
////import java.io.IOException;
////import java.io.InputStream;
////import java.io.InputStreamReader;
////import java.io.OutputStream;
////import java.net.URI;
////import java.util.HashMap;
////import java.util.Map;
////
////public class KursActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
////        GoogleApiClient.OnConnectionFailedListener {
////    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
//////    private StorageReference mStoRef = FirebaseStorage.getInstance().getReference();
////
////    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
////    private static final int REQUEST_CODE_CREATOR = 2;
////    private static final int REQUEST_CODE_RESOLUTION = 3;
////
////    private static final String TAG = "KursActivity";
////
////
////    private String kursName;
////    private GoogleApiClient mGoogleApiClient;
////
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_kurs);
////
////        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//////        getKursData();
//////        drive();
//////        getKursMedia();
////        mGoogleApiClient.connect();
////        getKursData();
////
////        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.kurs_fabSend);
////        fab.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                sendKursMessage();
////            }
////        });
////    }
////
////    private void getKursData() {
////        Intent intent = getIntent();
////
////        String name = intent.getStringExtra("name");
////        kursName = name;
////
////        setTitle(name);
////        getKursMessage(name);
//////        getKursMedia();
////    }
////
////    @Override
////    protected void onStart() {
////
////        super.onStart();
////    }
////
////    private void getKursMessage(String name) {
////        final LinearLayout llMessages = (LinearLayout) findViewById(R.id.kurs_llMessage);
////        DatabaseReference mKursRef = mRootRef
////                .child("Kurse")
////                .child(name)
////                .child("messages");
////        mKursRef.addValueEventListener(new ValueEventListener() {
////            @Override
////            public void onDataChange(DataSnapshot dataSnapshot) {
////                llMessages.removeAllViews();
////                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
////                    TextView tvMessage = new TextView(KursActivity.this);
////                    String sender = childSnapshot.child("sender").getValue(String.class);
////                    String message = childSnapshot.child("message").getValue(String.class);
////                    tvMessage.setText(sender + " " + message);
////                    llMessages.addView(tvMessage);
////                }
////            }
////
////            @Override
////            public void onCancelled(DatabaseError databaseError) {
////
////            }
////        });
////    }
////
////    private void sendKursMessage() {
////        EditText etMessage = (EditText)findViewById(R.id.kurs_message);
////        String message = etMessage.getText().toString();
////        DatabaseReference mKursRef = mRootRef
////                .child("Kurse")
////                .child(kursName)
////                .child("messages");
////
////        String key = mKursRef.push().getKey();
////        Map<String, Object> newMessage = new HashMap<>();
////        newMessage.put(key + "/sender/", "g@g.co");
////        newMessage.put(key + "/message/", message);
////
////        mKursRef.updateChildren(newMessage);
////    }
////
////    private void drive(){
////        mGoogleApiClient = new GoogleApiClient.Builder(this)
////                .addApi(Drive.API)
////                .addScope(Drive.SCOPE_FILE)
////                .addConnectionCallbacks(this)
////                .addOnConnectionFailedListener(this)
////                .build();
//////        mGoogleApiClient.connect();
////    }
////
////    @Override
////    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
////        System.out.println(requestCode);
////
////        switch (requestCode) {
////            case REQUEST_CODE_CAPTURE_IMAGE:
////                if (resultCode == RESULT_OK) {
////                    mGoogleApiClient.connect();
////                }
////                break;
////        }
////    }
////
////    private void getKursMedia() {
////        Drive.DriveApi.fetchDriveId(mGoogleApiClient, "0B7ppyvhHCrfgN2N5ck81M3hBamM")
////                .setResultCallback(idCallback);
////    }
////
////    final private ResultCallback<DriveApi.DriveIdResult> idCallback = new ResultCallback<DriveApi.DriveIdResult>() {
////        @Override
////        public void onResult(DriveApi.DriveIdResult result) {
////            new RetrieveDriveFileContentsAsyncTask(
////                    KursActivity.this).execute(result.getDriveId());
////        }
////    };
////
////    final private class RetrieveDriveFileContentsAsyncTask
////            extends ApiClientAsyncTask<DriveId, Boolean, String>{
////
////        public RetrieveDriveFileContentsAsyncTask(Context context) {
////            super(context);
////        }
////
////        @Override
////        protected String doInBackgroundConnected(DriveId... params) {
////            String contents = null;
////            DriveFile file = params[0].asDriveFile();
////            DriveApi.DriveContentsResult driveContentsResult =
////                    file.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null).await();
////            if (!driveContentsResult.getStatus().isSuccess()) {
////                return null;
////            }
////            DriveContents driveContents = driveContentsResult.getDriveContents();
////            BufferedReader reader = new BufferedReader(
////                    new InputStreamReader(driveContents.getInputStream()));
////            StringBuilder builder = new StringBuilder();
////            String line;
////            try {
////                while ((line = reader.readLine()) != null) {
////                    builder.append(line);
////                }
////                contents = builder.toString();
////            } catch (IOException e) {
////                Log.e(TAG, "IOException while reading from the stream", e);
////            }
////
////            driveContents.discard(getGoogleApiClient());
////            return contents;
////        }
////        @Override
////        protected void onPostExecute(String result) {
////            super.onPostExecute(result);
////            if (result == null) {
////                System.out.println("Error while reading from the file");
////                return;
////            }
////            System.out.println("File contents: " + result);
////        }
////    }
////
////
////
////    @Override
////    public boolean onOptionsItemSelected(MenuItem item) {
////        switch (item.getItemId()) {
////            case android.R.id.home:
////                this.finish();
////                break;
////        }
////        return super.onOptionsItemSelected(item);
////    }
////
////    @Override
////    public void onConnected(@Nullable Bundle bundle) {
////        getKursMedia();
////    }
////
////    @Override
////    public void onConnectionSuspended(int i) {
////
////    }
////
////    @Override
////    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
////        if (connectionResult.hasResolution()) {
////            try {
////                connectionResult.startResolutionForResult(this,REQUEST_CODE_RESOLUTION);
////            } catch (IntentSender.SendIntentException e) {
////                // Unable to resolve, message user appropriately
////            }
////        } else {
////            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
////        }
////    }
////}
