package com.github.leonardpieper.ceciVPlan;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static android.widget.LinearLayout.FOCUS_DOWN;

public class KursActivity extends AppCompatActivity {
    private final String TAG = "KursActivity";
//    GoogleAccountCredential mCredential;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private StorageReference mStorage;
    private StorageReference mKursStorage;

    private Intent intent;
    private String kursName;
    private String kursNameRef;

    private NestedScrollView scrollView;

    private LinearLayout lLayoutl;
    private Button dlBtn;
    private LinearLayout thumbnailRow;
    private int previousRowID = 0;
    private int thumbnailCount = 5000;

    private int cancelledTimes = 0;

    ProgressDialog mProgress;

    static final int READ_REQUEST_CODE = 42;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 1004;
    static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE_DRIVE = 1005;

    private static final String BUTTON_TEXT = "Call Drive API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
//    private static final String[] SCOPES = {DriveScopes.DRIVE};

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

//        lLayoutl = (LinearLayout) findViewById(R.id.downloadsRl);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance().getReference();

        kursName = intent.getStringExtra("name");
        kursNameRef = kursName.replace(".", "%2E");

        mKursStorage = mStorage.child("kurse").child(kursName);

        setTitle(kursName);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);


//        mCredential = GoogleAccountCredential.usingOAuth2(
//                getApplicationContext(), Arrays.asList(SCOPES))
//                .setBackOff(new ExponentialBackOff());


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.kurs_fabSend);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendKursMessage();
            }
        });

//        Button uploadBtn = (Button) findViewById(R.id.kursMediaUploadBtn);
//        uploadBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                openFileChooser();
//            }
//        });


        getKursMessage(kursNameRef);
        scrollView = (NestedScrollView) findViewById(R.id.childScrollKurs);


//        getResultsFromApi();

    }

    private void getKursMessage(String name) {
        final LinearLayout llMessages = (LinearLayout) findViewById(R.id.kurs_llMessage);
        DatabaseReference mKursRef = mDatabase
                .child("Kurse")
                .child(name)
                .child("messages");
        mKursRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                llMessages.removeAllViews();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    TextView tvMessage = new TextView(KursActivity.this);
                    tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
                    String sender = childSnapshot.child("sender").getValue(String.class);
                    String message = childSnapshot.child("body").getValue(String.class);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tvMessage.setText(Html.fromHtml(sender + " " + message, Html.FROM_HTML_MODE_COMPACT));
                    }else{
                        tvMessage.setText(Html.fromHtml(sender + " " + message));
                    }
                    llMessages.addView(tvMessage);
                }
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(FOCUS_DOWN);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void sendKursMessage() {
        EditText etMessage = (EditText) findViewById(R.id.kurs_message);
        String message = etMessage.getText().toString();

        String htmlMessage = convertlinkToHtml(message);

        DatabaseReference mKursRef = mDatabase
                .child("Kurse")
                .child(kursNameRef)
                .child("messages");

        String key = mKursRef.push().getKey();
        Map<String, Object> newMessage = new HashMap<>();
        newMessage.put(key + "/sender/", mAuth.getCurrentUser().getEmail());
        newMessage.put(key + "/uid/", mAuth.getCurrentUser().getUid());
        newMessage.put(key + "/body/", htmlMessage);

        mKursRef.updateChildren(newMessage);
        etMessage.setText("");
    }

    private String convertlinkToHtml(String message){
        List<String> links = new ArrayList<>();
        Matcher matcher = Patterns.WEB_URL.matcher(message);
        while (matcher.find()) {
            String url = matcher.group();
            Log.d(TAG, "URL extracted: " + url);
            links.add(url);
        }

        for(String link: links){
            message = message.replace(link, "<a href='"+link+"'>"+link+"</a>");
        }
        return message;
    }



//    private void openFileChooser() {
//        Intent fileIntent;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
//            fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
//        } else {
//            fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        }
//        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
//        fileIntent.setType("*/*");
//        startActivityForResult(fileIntent, READ_REQUEST_CODE);
//    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
//    private void getResultsFromApi() {
//        if (!isGooglePlayServicesAvailable()) {
//            acquireGooglePlayServices();
//        } else if (mCredential.getSelectedAccountName() == null) {
//            chooseAccount();
//        } else if (!isDeviceOnline()) {
//            //FIXME
////            mOutputText.setText("No network connection available.");
//        } else {
//            getDownloadableMedia(kursNameRef, 5);
////            new MakeRequestTask(mCredential).execute();
//        }
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
//                this, Manifest.permission.GET_ACCOUNTS) && EasyPermissions.hasPermissions(
//                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
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
//                    "Diese App benötigt zugriff auf deinen Google account (via Contacts) und auf deine SD-Karte.",
//                    REQUEST_PERMISSION_GET_ACCOUNTS,
//                    new String[]{Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_EXTERNAL_STORAGE});
//        }
//    }
//

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param resultData  Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        switch (requestCode) {
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
            case READ_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = null;
                    if (resultData != null) {
                        uri = resultData.getData();
//                        uploadToFirebase(uri);
                    }
//                    new MakeRequestUploadTask(mCredential).execute(resultData);
                }
                break;
//            case REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
//                if (resultCode == RESULT_OK) {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(KursActivity.this);
//
//                    builder.setMessage("Bitte nochmal auf \"download klicken\"")
//                            .setTitle("Info");
//                    builder.create();
//                }
        }
    }

//    private void uploadToFirebase(Uri uri) {
//        StorageReference uploadRef = mKursStorage.child(getFileName(uri));
//        StorageMetadata metadata = new StorageMetadata.Builder()
//                .setCustomMetadata("private", "true").build();
//
//        UploadTask uploadTask = uploadRef.putFile(uri, metadata);
//        uploadTask.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.e(TAG, "Upload Failed");
//            }
//        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Log.d(TAG, "Upload Successful");
//            }
//        });
//    }

//    public String getFileName(Uri uri) {
//        String result = null;
//        if (uri.getScheme().equals("content")) {
//            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
//            try {
//                if (cursor != null && cursor.moveToFirst()) {
//                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
//                }
//            } finally {
//                cursor.close();
//            }
//        }
//        if (result == null) {
//            result = uri.getPath();
//            int cut = result.lastIndexOf('/');
//            if (cut != -1) {
//                result = result.substring(cut + 1);
//            }
//        }
//        return result;
//    }
//
//    public void getDownloadableMedia(String kurs, int count) {
//        Query kursStorageRef = mDatabase.child("Kurse").child(kurs).child("storagePath").orderByChild("date").limitToLast(count);
//
//        kursStorageRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
//                    String id = childSnapshot.child("id").getValue(String.class);
//                    String name = childSnapshot.child("title").getValue(String.class);
//
//
//                    CardView imageCard = getLocalMedia(name, id);
//                    if(imageCard != null) {
//
//                        if (thumbnailCount % 2 == 0) {
//                            thumbnailRow = new LinearLayout(KursActivity.this);
//                            LinearLayout.LayoutParams lLParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//                            thumbnailRow.setLayoutParams(lLParams);
//                            thumbnailRow.setId(thumbnailCount + 0);
//
//                            thumbnailRow.addView(imageCard);
//
//
//                            lLayoutl.addView(thumbnailRow);
//
//                            previousRowID = thumbnailRow.getId();
//                            thumbnailCount++;
//                        } else {
//                            thumbnailRow.addView(imageCard);
//                            thumbnailCount++;
//                        }
//                    }
//
//
//                }
//
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//
//    }
//
//    private CardView getLocalMedia(String title, String id) {
//
//        String root = Environment.getExternalStorageDirectory().toString();
//        java.io.File myDir = new java.io.File(root + java.io.File.separator + "ceciplan" + java.io.File.separator + "downloads");
//        java.io.File f = new java.io.File(myDir, title);
//
//        if (f.getPath() != null) {
//            Bitmap bitmap = null;
//
//            bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
//            if (bitmap != null) {
//                return makeCard(title, bitmap, id);
//            }else {
//                new MakeRequestTask(mCredential).execute(id);
//            }
//
//        } else {
//            new MakeRequestTask(mCredential).execute(id);
//        }
//        return null;
//    }

//    public CardView makeCard(String title, Bitmap image, final String driveId) {
//        int id = (int) (Math.random() * 100);
//
//        if(image==null){
//            image = BitmapFactory.decodeResource(Resources.getSystem(), R.drawable.ic_school_black_24dp );
//        }
//
//        CardView cv = new CardView(KursActivity.this);
//        RelativeLayout rl = new RelativeLayout(KursActivity.this);
//
//        DisplayMetrics dm = new DisplayMetrics();
//        KursActivity.this.getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
//        int width = dm.widthPixels;
//        int height = dm.heightPixels;
//        int halfWidth = new Double(width / 2.3).intValue();
//        int fifthHeight = new Double(height / 5).intValue();
//
//        float aspectRatio = image.getWidth() /
//                (float) image.getHeight();
//
//        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//        );
//        rlParams.setMargins(8, 8, 8, 8);
//
//        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
//                halfWidth,
//                Math.round(halfWidth / aspectRatio)
//        );
//
//
//        LinearLayout.LayoutParams cvParams = new LinearLayout.LayoutParams(
//                fifthHeight,
//                ViewGroup.LayoutParams.MATCH_PARENT
//        );
//        cvParams.setMargins(8, 8, 8, 8);
//
//        RelativeLayout.LayoutParams rlTvLayout = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
//        rlTvLayout.addRule(RelativeLayout.BELOW, id);
//        rlTvLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//
//        RelativeLayout.LayoutParams rlBtnLayout = new RelativeLayout.LayoutParams(100, 100);
//        rlBtnLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//        rlBtnLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//
//
//        cv.setLayoutParams(cvParams);
//        rl.setLayoutParams(rlParams);
//
//        // Set CardView corner radius
//        cv.setRadius(4);
//        // Set cardView content padding
//        cv.setContentPadding(15, 15, 15, 15);
//        // Set CardView elevation
//        cv.setCardElevation(5);
//
//        ImageView ivImage = new ImageView(KursActivity.this);
//        ivImage.setLayoutParams(imgParams);
//        ivImage.setImageBitmap(image);
//        ivImage.setScaleType(ImageView.ScaleType.FIT_START);
//        ivImage.setId(id);
//
//        TextView tvTitle = new TextView(KursActivity.this);
//        tvTitle.setText(title);
//        tvTitle.setLayoutParams(rlTvLayout);
//
//        Button dlBtn = new Button(KursActivity.this);
//        dlBtn.setLayoutParams(rlBtnLayout);
//        dlBtn.setBackgroundResource(R.drawable.ic_file_download_orange_24dp);
//        dlBtn.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(EasyPermissions.hasPermissions(KursActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                    new MakeDownloadTask(mCredential).execute(driveId);
//                }else {
//                    EasyPermissions.requestPermissions(KursActivity.this,
//                            "Zum downloaden benötigt die App zugriff auf den Speicher",
//                            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
//                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                }
//            }
//        });
//
//
////            ll.setOrientation(VERTICAL);
//        rl.addView(ivImage);
//        rl.addView(tvTitle);
//        rl.addView(dlBtn);
//
//        cv.addView(rl);
//
//        return cv;
//
//    }


//    /**
//     * Respond to requests for permissions at runtime for API 23 and above.
//     *
//     * @param requestCode  The request code passed in
//     *                     requestPermissions(android.app.Activity, String, int, String[])
//     * @param permissions  The requested permissions. Never null.
//     * @param grantResults The grant results for the corresponding permissions
//     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        EasyPermissions.onRequestPermissionsResult(
//                requestCode, permissions, grantResults, this);
//    }
//
//    /**
//     * Callback for when a permission is granted using the EasyPermissions
//     * library.
//     *
//     * @param requestCode The request code associated with the requested
//     *                    permission
//     * @param list        The requested permission list. Never null.
//     */
//    @Override
//    public void onPermissionsGranted(int requestCode, List<String> list) {
//        // Do nothing.
//    }
//
//    /**
//     * Callback for when a permission is denied using the EasyPermissions
//     * library.
//     *
//     * @param requestCode The request code associated with the requested
//     *                    permission
//     * @param list        The requested permission list. Never null.
//     */
//    @Override
//    public void onPermissionsDenied(int requestCode, List<String> list) {
//        // Do nothing.
//    }
//
//    /**
//     * Checks whether the device currently has a network connection.
//     *
//     * @return true if the device has a network connection, false otherwise.
//     */
//    private boolean isDeviceOnline() {
//        ConnectivityManager connMgr =
//                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
//        return (networkInfo != null && networkInfo.isConnected());
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
//                KursActivity.this,
//                connectionStatusCode,
//                REQUEST_GOOGLE_PLAY_SERVICES);
//        dialog.show();
//    }
//
//    private class MakeDownloadTask extends AsyncTask<String, Void, Void>{
//
//        private com.google.api.services.drive.Drive mService = null;
//
//        MakeDownloadTask(GoogleAccountCredential credential) {
//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
//            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.drive.Drive.Builder(
//                    transport, jsonFactory, credential)
//                    .setApplicationName("Drive API Android Quickstar")
//                    .build();
//        }
//
//        @Override
//        protected Void doInBackground(String... params) {
//            try {
//                dlFile(getFileFromId(params[0]));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            return null;
//        }
//
//        private void dlFile(File file){
//            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//
//            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(file.getWebContentLink()));
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, java.io.File.separator + "ceciplan" + java.io.File.separator + file.getTitle());
//            long enque = dm.enqueue(request);
//        }
//
//        private File getFileFromId(String id) throws IOException {
//            File file = mService.files().get(id).execute();
//            return file;
//        }
//    }
//
//    /**
//     * An asynchronous task that handles the Drive API call.
//     * Placing the API calls in their own task ensures the UI stays responsive.
//     */
//    private class MakeRequestTask extends AsyncTask<String, Void, String> {
//        private String driveId;
//        private File driveFile;
//
//        private ImageView mOutputImage;
//        private Bitmap bm;
//        private byte[] content;
//        private String title;
//        private String dlUrl;
//        private com.google.api.services.drive.Drive mService = null;
//        private Exception mLastError = null;
//
//        MakeRequestTask(GoogleAccountCredential credential) {
//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
//            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.drive.Drive.Builder(
//                    transport, jsonFactory, credential)
//                    .setApplicationName("Drive API Android Quickstar")
//                    .build();
//        }
//
//
//        /**
//         * Background task to call Drive API.
//         *
//         * @param params no parameters needed for this task.
//         */
//        @Override
//        protected String doInBackground(String... params) {
//            try {
//                driveId = params[0];
//                downloadThumbnail(getFileFromId(params[0]));
//                return "Worked";
//            } catch (Exception e) {
//                mLastError = e;
//                cancel(true);
//                return null;
//            }
//        }
//
//        private File getFileFromId(String id) throws IOException {
//            File file = mService.files().get(id).execute();
//            driveFile = file;
////            System.out.println("Title: " + file.getTitle());
//            title = file.getTitle();
//            dlUrl = file.getWebContentLink();
////            System.out.println("Dl: " + file.getWebContentLink());
////            System.out.println("Description: " + file.getDescription());
////            System.out.println("MIME type: " + file.getMimeType());
////            System.out.println("MIME type: " + file.getThumbnailLink());
//            return file;
//        }
//
//        private void downloadThumbnail(File file) {
//            URL u;
//            if (file.getThumbnailLink() != null && file.getThumbnailLink().length() > 0) {
//                try {
//                    u = new URL(file.getThumbnailLink());
//                    HttpURLConnection ucon = (HttpURLConnection) u.openConnection();
//                    if (ucon.getResponseCode() > -1) {
//                        Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
//                        Matcher m = p.matcher(ucon.getContentType());
//
//                        String charset = m.matches() ? m.group(1) : "UTF-8";
//
//                        InputStream inputStream = ucon.getInputStream();
//
//                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//                        int nRead;
//                        byte[] data = new byte[1024];
//                        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
//                            buffer.write(data, 0, nRead);
//                        }
//                        buffer.flush();
//                        content = buffer.toByteArray();
//
//                        bm = BitmapFactory.decodeByteArray(content, 0, content.length);
//                    }
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        public CardView makeCard(String title, Bitmap image) {
////            if(image!=null&&image.getHeight()!=0) {
//                int id = (int) (Math.random() * 100);
//
//                if(image==null||image.getWidth()<=0){
//                    image = BitmapFactory.decodeResource(KursActivity.this.getResources(), R.drawable.ic_layers_clear_black_24dp );
//                }
//
//                CardView cv = new CardView(KursActivity.this);
//                RelativeLayout rl = new RelativeLayout(KursActivity.this);
//
//                DisplayMetrics dm = new DisplayMetrics();
//                KursActivity.this.getWindow().getWindowManager().getDefaultDisplay().getMetrics(dm);
//                int width = dm.widthPixels;
//                int height = dm.heightPixels;
//                int halfWidth = new Double(width / 2.3).intValue();
//                int fifthHeight = new Double(height / 5).intValue();
//
//                float aspectRatio = image.getWidth() /
//                        (float) image.getHeight();
//
//                RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
//                        ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                );
//                rlParams.setMargins(8, 8, 8, 8);
//
//                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
//                        halfWidth,
//                        Math.round(halfWidth / aspectRatio)
//                );
//
//
//                LinearLayout.LayoutParams cvParams = new LinearLayout.LayoutParams(
//                        fifthHeight,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                );
//                cvParams.setMargins(8, 8, 8, 8);
//
//                RelativeLayout.LayoutParams rlTvLayout = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                        ViewGroup.LayoutParams.WRAP_CONTENT);
//                rlTvLayout.addRule(RelativeLayout.BELOW, id);
//                rlTvLayout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//
//                RelativeLayout.LayoutParams rlBtnLayout = new RelativeLayout.LayoutParams(100, 100);
//                rlBtnLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//                rlBtnLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//
//
//                cv.setLayoutParams(cvParams);
//                rl.setLayoutParams(rlParams);
//
//                // Set CardView corner radius
//                cv.setRadius(4);
//                // Set cardView content padding
//                cv.setContentPadding(15, 15, 15, 15);
//                // Set CardView elevation
//                cv.setCardElevation(5);
//
//                ImageView ivImage = new ImageView(KursActivity.this);
//                ivImage.setLayoutParams(imgParams);
//                ivImage.setImageBitmap(image);
//                ivImage.setScaleType(ImageView.ScaleType.FIT_START);
//                ivImage.setId(id);
//
//                TextView tvTitle = new TextView(KursActivity.this);
//                tvTitle.setText(title);
//                tvTitle.setLayoutParams(rlTvLayout);
//
//                Button dlBtn = new Button(KursActivity.this);
//                dlBtn.setLayoutParams(rlBtnLayout);
//                dlBtn.setBackgroundResource(R.drawable.ic_file_download_orange_24dp);
//                dlBtn.setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (EasyPermissions.hasPermissions(KursActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
//                            dlFile();
//                        } else {
//                            EasyPermissions.requestPermissions(KursActivity.this,
//                                    "Zum downloaden benötigt die App zugriff auf den Speicher",
//                                    REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
//                                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
//                        }
//
//                    }
//                });
//
//
////            ll.setOrientation(VERTICAL);
//                rl.addView(ivImage);
//                rl.addView(tvTitle);
//                rl.addView(dlBtn);
//
//                cv.addView(rl);
//
//                return cv;
////            }else{
////                return new CardView(KursActivity.this);
////            }
//
//        }
//
//        private void dlFile(){
//            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//
//            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(driveFile.getWebContentLink()));
//            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, java.io.File.separator + "ceciplan" + java.io.File.separator + driveFile.getTitle());
//            long enque = dm.enqueue(request);
//
//
//        }
//
//        private void dlFileThumbnail() {
//            if(content!=null) {
//                String root = Environment.getExternalStorageDirectory().toString();
//                java.io.File myDir = new java.io.File(root + java.io.File.separator + "ceciplan" + java.io.File.separator + "downloads");
//                myDir.mkdirs();
//                java.io.File myFile = new java.io.File(myDir, title);
//
//                try {
//                    FileOutputStream fOut = new FileOutputStream(myFile);
//                    fOut.write(content);
//                    fOut.flush();
//                    fOut.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//
//        @Override
//        protected void onPreExecute() {
//            mOutputImage = new ImageView(KursActivity.this);
//        }
//
//        @Override
//        protected void onPostExecute(String output) {
////                mProgress.hide();
//            //FIXME: Fix
//            if (output == null) {
////                    mOutputText.setText("No results returned.");
//            } else {
//                CardView imageCard = makeCard(title, bm);
//
//
//
//                if (thumbnailCount % 2 == 0) {
//                    thumbnailRow = new LinearLayout(KursActivity.this);
//                    LinearLayout.LayoutParams lLParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//                    thumbnailRow.setLayoutParams(lLParams);
//                    thumbnailRow.setId(thumbnailCount + 0);
//
//                    thumbnailRow.addView(imageCard);
//
//
////                    if(previousRowID !=5000){
//                    lLayoutl.addView(thumbnailRow);
////                    }else{
////                    }
//
//                    previousRowID = thumbnailRow.getId();
//                    thumbnailCount++;
//                } else {
//                    thumbnailRow.addView(imageCard);
//                    thumbnailCount++;
//                }
////                rl.addView(mOutputImage);
//            }
//
//            dlFileThumbnail();
//
//
////                setDlBtn(bm);
//        }
//
//        @Override
//        protected void onCancelled() {
////                mProgress.hide();
//            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    if (cancelledTimes < 1) {
//                        startActivityForResult(
//                                ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                                KursActivity.REQUEST_AUTHORIZATION);
//                    }
//                } else {
//                    //FIXME: fix
////                        mOutputText.setText("The following error occurred:\n"
////                                + mLastError.getMessage());
//                }
//            } else {
//                //FIXME: fix
////                    mOutputText.setText("Request cancelled.");
//            }
//            cancelledTimes++;
//        }
//    }
//
//    /**
//     * An asynchronous task that handles the Drive API call.
//     * Placing the API calls in their own task ensures the UI stays responsive.
//     */
//    private class MakeRequestUploadTask extends AsyncTask<Intent, Void, String> {
//        private com.google.api.services.drive.Drive mService = null;
//        private Exception mLastError = null;
//
//        MakeRequestUploadTask(GoogleAccountCredential credential) {
//            HttpTransport transport = AndroidHttp.newCompatibleTransport();
//            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//            mService = new com.google.api.services.drive.Drive.Builder(
//                    transport, jsonFactory, credential)
//                    .setApplicationName("Drive API Android Quickstar")
//                    .build();
//        }
//
//
//        /**
//         * Background task to call Drive API.
//         *
//         * @param params no parameters needed for this task.
//         */
//        @Override
//        protected String doInBackground(Intent... params) {
////            android.os.Debug.waitForDebugger();
//            try {
//                uploadMedia(params[0]);
//                return "Hi";
//            } catch (Exception e) {
//                mLastError = e;
//                cancel(true);
//                return null;
//            }
//        }
//
//        private void uploadMedia(Intent data) throws IOException {
//            Uri uri = null;
//            if (data != null) {
//                uri = data.getData();
//
////                getContentResolver().openInputStream(uri);
//
//                InputStream i = getContentResolver().openInputStream(uri);
//                byte[] buffer = new byte[i.available()];
//                i.read(buffer);
//                String mimeType = URLConnection.guessContentTypeFromStream(i);
//                java.io.File filename = new java.io.File(uri.getPath());
//                String name = filename.getName();
//
//
//                java.io.File dir = new java.io.File(getCacheDir().getPath() + "/tmp");
//                dir.mkdirs();
//                java.io.File content = new java.io.File(dir + java.io.File.separator + name);
//                OutputStream outputStream = new FileOutputStream(content);
//                outputStream.write(buffer);
//
//
//                FileContent mediaContent = new FileContent(mimeType, content);
//
//                File body = new File();
//                body.setTitle(name);
//
//                File file = mService.files().insert(body, mediaContent).execute();
//
//                System.out.println(file.getId());
//
//                Permission permission = new Permission();
//                permission.setRole("reader");
//                permission.setType("anyone");
//                permission.setWithLink(true);
//                try {
//                    mService.permissions().insert(file.getId(), permission).execute();
//
//                    long date = Calendar.getInstance().getTimeInMillis();
//
//                    HashMap<String, Object> hm = new HashMap<>();
//                    hm.put("id", file.getId());
//                    hm.put("date", date);
//                    hm.put("title", file.getTitle());
//
//                    mDatabase.child("Kurse").child(kursNameRef).child("storagePath").child(file.getId()).setValue(hm);
//                    mDatabase.child("Kurse").child(kursNameRef).child("timestamp").setValue(date);
//                } catch (IOException e) {
//                    System.out.println("An error occurred: " + e);
//                }
//
//
//            }
//        }
//
//        private String readTextFromUri(Uri uri) throws IOException {
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(
//                    inputStream));
//            StringBuilder stringBuilder = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line);
//            }
//            inputStream.close();
//            return stringBuilder.toString();
//        }
//
//
//        @Override
//        protected void onPreExecute() {
//            ProgressBar progressBar = (ProgressBar) findViewById(R.id.kursMediaProgress);
//            progressBar.setIndeterminate(true);
//        }
//
//        @Override
//        protected void onPostExecute(String output) {
//            //FIXME: Fix
//            if (output != null) {
//                ProgressBar progressBar = (ProgressBar) findViewById(R.id.kursMediaProgress);
//                progressBar.setIndeterminate(false);
//                Toast toast = Toast.makeText(KursActivity.this, "Upload abgeschlossen", Toast.LENGTH_SHORT);
//                toast.show();
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
////                mProgress.hide();
//            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    if (cancelledTimes < 1) {
//                        startActivityForResult(
//                                ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                                KursActivity.REQUEST_AUTHORIZATION);
//                    }
//                } else {
//                    Toast toast = Toast.makeText(KursActivity.this, mLastError.getMessage(), Toast.LENGTH_LONG);
//                    toast.show();
//                    //FIXME: fix
////                        mOutputText.setText("The following error occurred:\n"
////                                + mLastError.getMessage());
//                }
//            } else {
//                //FIXME: fix
//                Toast toast = Toast.makeText(KursActivity.this, mLastError.getMessage(), Toast.LENGTH_LONG);
//                toast.show();
////                    mOutputText.setText("Request cancelled.");
//            }
//            cancelledTimes++;
//        }
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
