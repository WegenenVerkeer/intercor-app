package macq.intercor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.ndk.CrashlyticsNdk;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.neurenor.permissions.PermissionCallback;
import com.neurenor.permissions.PermissionsHelper;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import macq.intercor.fragments.IconsActionsFragment;
import macq.intercor.fragments.MapsFragment;
import macq.intercor.helpers.AdminSQLiteOpenHelper;
import macq.intercor.helpers.DriveServiceHelper;
import macq.intercor.helpers.Interval;
import macq.intercor.models.Icon;

public class MainActivity extends AppCompatActivity implements IconsActionsFragment.OnDataPass {

    // Request code to sign in
    private final int RC_SIGN_IN = 1;
    // Path of storage location
    private final String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    // Tag which will appear in logcat
    private static final String TAG = "MainActivity";
    // Helper for the data base in SQLite
    private AdminSQLiteOpenHelper adminSQLiteOpenHelper;
    // Manager of connectivity
    private ConnectivityManager connectivityManager;
    // Service of google drive
    private DriveServiceHelper driveServiceHelper;
    // Authentication of Firebase
    private FirebaseAuth firebaseAuth;
    // Fragment of the icons
    private IconsActionsFragment iconsActionsFragment;
    // Interval of time to send to google drive
    private Interval intervalToSendToDrive;
    // Interval of time to log
    private Interval intervalToLog;
    // Fragment of the map
    private MapsFragment mapsFragment;
    // Information of the device network
    private NetworkInfo networkInfo;
    // Helper of permissions
    private PermissionsHelper permissionsHelper;
    // Name of the application
    private String applicationName = "<-- APP_NAME -->";
    // Name of the internal file (the google drive file will be named equally)
    private String filename;
    // ID of the internal file
    private String fileID;
    // Name of the team set in the settings
    private String teamName;
    // Shared preferences of the user (teamName, time span deselection icons, synchronization)
    private SharedPreferences sharedPreferences;

    /***********************
    ** LIFE CYCLE METHODS **
    ************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set connectivity properties
        this.connectivityManager = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        this.networkInfo = this.connectivityManager.getActiveNetworkInfo();

        // Get preferences previously set by the user
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Get authorization of Firebase
        this.firebaseAuth = FirebaseAuth.getInstance();

        Fabric.with(this, new Crashlytics(), new CrashlyticsNdk());

        // initialize permissions
        this.initializeContentView();
        // initialize map and icons fragments
        this.initializeFragments();
        // initialize permissions
        this.initializePermissions();
        // Initialize Interval of google drive
        this.initializeIntervalSynchronization();
        // Install cache for icons bitmap
        this.installCache();
        // Initialize db SQLite object
        this.adminSQLiteOpenHelper = new AdminSQLiteOpenHelper(this, "iconsDB", null, 1);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.teamName = getTeamName();
        if(this.teamName != null) {
            this.filename = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + '_' + this.teamName;
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .requestScopes(new Scope(DriveScopes.DRIVE))
                    .build();
            Intent signInIntent = GoogleSignIn.getClient(this, gso).getSignInIntent();
            startActivityForResult(signInIntent, this.RC_SIGN_IN);
        }
    }

    @Override
    protected void onStop() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            Log.i(TAG, "CACHED FLUSHED WITH " + cache.size());
            cache.flush();

        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.sendInternalFileToGDrive();
        if (this.intervalToLog != null) this.intervalToLog.stop();
        if (this.intervalToSendToDrive != null) this.intervalToSendToDrive.stop();
        super.onDestroy();
    }

    /*******************
     ** OTHER METHODS **
     *******************/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == this.RC_SIGN_IN) {
            // When it exist internet connection, firebase and google drive tasks done
            if (this.networkInfo != null && this.networkInfo.isConnected()) {
                Log.i(TAG, "Network available, trying google drive and firebase tasks");
                Task<GoogleSignInAccount> googleSignInAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    // Get the google account
                    GoogleSignInAccount account = googleSignInAccountTask.getResult(ApiException.class);
                    Log.i(TAG, "Signed in with Google account: " + account.getEmail());
                    // Authenticate in firebase with the account and fetch icons from dataBase
                    this.handleFirebase(account);
                    // Authenticate in googleDrive with the account and get/create folder and file
                    this.handleGoogleDrive(account);
                } catch(ApiException e) {
                    Log.e(TAG, "Problem with Google Account", e);
                }
            } else {
                // Fetch icons from SQLite
                this.adminSQLiteOpenHelper.getIconsFromDB()
                    .addOnSuccessListener(icons -> {
                        Log.i(TAG, "Icons got from DB");
                        // Create grid with the icons fetched
                        this.iconsActionsFragment.createGrid(icons);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failure when get icons from DB", e));
            }
        }
    }

    @Override
    public void onIconPass(Icon icon) {
        this.writeInFile(icon);
    }

    public void goToSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    public void toggleMaximizationView(View view) {
        try {
            view.setRotation(view.getRotation() + 180f); // rotate button
            Size sizeScreen = this.getSizeScreen();
            FrameLayout iconsView = this.findViewById(R.id.iconsActionsFragment);
            ViewGroup.LayoutParams iconsParams = iconsView.getLayoutParams();
            iconsParams.height = iconsParams.height == sizeScreen.getHeight() ? 0 : sizeScreen.getHeight();
            iconsView.setLayoutParams(iconsParams);
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void handleFirebase(@NonNull GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        this.firebaseAuth.signInWithCredential(credential).
            addOnSuccessListener(r -> {
                Log.i(TAG, "Signed in Firebase with Google account");
                // If the grid is not created, all icons from firebase are fetched and the grid is created
                if (!this.iconsActionsFragment.isGridCreated()) {
                    Log.i(TAG, "Icons Grid not created");
                    FirebaseFirestore fireBaseDB = FirebaseFirestore.getInstance();
                    fireBaseDB.collection("icons").orderBy("position").get()
                        .addOnSuccessListener(result -> {
                            Log.i(TAG, "Icons fetched form Database of Firebase");
                            ArrayList<Icon> icons =  new ArrayList<>(result.getDocuments().size());
                            // An new icon, created from each result, is inserted in the icon array
                            for (DocumentSnapshot document: result.getDocuments()) {
                                Map<String, Object> data = document.getData();
                                icons.add(new Icon(data, document.getId()));
                                Log.i(TAG, "Fetched Icon:" + icons.get(icons.size() - 1).getMessage());
                            }
                            // Create the grid
                            this.iconsActionsFragment.createGrid(icons);
                            // Put the icons in the database
                            this.adminSQLiteOpenHelper.setIconsInDB(icons)
                                .addOnSuccessListener(s -> Log.i(TAG, "New items put in DB"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failure when put items in DB", e));
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Unable fetch icons form Database of Firebase", e));
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to sign in with Google", e);
                Toast.makeText(this, "Unable to connect to Firebase", Toast.LENGTH_SHORT).show();
            });
    }

    private void handleGoogleDrive(GoogleSignInAccount account) {
        // Get credentials and set the account
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE));
        credential.setSelectedAccount(account.getAccount());
        // Build google drive
        Drive driveService = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
        ).setApplicationName(this.applicationName).build();
        // Create the object of DriveServiceHelper
        this.driveServiceHelper = new DriveServiceHelper(driveService, this.teamName);
        // Get and set folder and file in google drive
        this.driveServiceHelper.getFileID(this.teamName, Collections.emptyList())
            .addOnSuccessListener(teamFolderID -> {
                // If a folder does not exist, one is created
                if(teamFolderID.isEmpty()) {
                    this.driveServiceHelper.createFolder(this.teamName)
                        .addOnSuccessListener(newTeamFolderID -> {
                            // The new folder Id is set
                            this.driveServiceHelper.setTeamFolderID(newTeamFolderID);
                            // If the folder creation success, the file is created
                            this.driveServiceHelper.getFileID(this.driveServiceHelper.getFilename(), Arrays.asList(newTeamFolderID))
                                .addOnSuccessListener(fileID -> this.fileID = fileID)
                                .addOnFailureListener(e -> Log.e(TAG, "Problem to get FileID after create folder", e));
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Problem to create team folder", e));
                }
                // If the folder already existed, the file is got
                else {
                    // The existing folder Id is set
                    this.driveServiceHelper.setTeamFolderID(teamFolderID);
                    // the file is created
                    this.driveServiceHelper.getFileID(this.driveServiceHelper.getFilename(), Arrays.asList(teamFolderID))
                        .addOnSuccessListener(fileID -> this.fileID = fileID)
                        .addOnFailureListener(e -> Log.e(TAG, "Problem to get fileID in the existing folder", e));
                }
            })
            .addOnFailureListener(exception -> Log.e(TAG, "Problem to get Team ID Folder", exception));
    }

    private void installCache() {
        try {
            File httpCacheDir = this.getExternalCacheDir();
            long httpCacheSize = 2 * 1024 * 1024; // 2 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
            Log.i(TAG, "HttpResponseCache enabled");
        } catch (IOException e) {
            Log.i(TAG, "HTTP response cache installation failed:" + e);
        }
    }

    private void initializeContentView() {
        this.setContentView(R.layout.main_activity);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
    }

    private void initializeFragments() {
        this.mapsFragment = new MapsFragment();
        this.iconsActionsFragment = new IconsActionsFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.iconsActionsFragment, this.iconsActionsFragment);
        fragmentTransaction.replace(R.id.mapsFragment, this.mapsFragment);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    private void initializeIntervalSynchronization() {
        final int intervalSync = Integer.parseInt(this.sharedPreferences.getString("sync_external", "10"));
        this.intervalToSendToDrive = new Interval(this::sendInternalFileToGDrive, 1000 * intervalSync);
        this.intervalToSendToDrive.start();
    }

    private Size getSizeScreen() {
        Display display = this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return new Size(size.x, size.y);
    }

    private String getTeamName() {
        String teamName = this.sharedPreferences.getString("team_name", "");
        if (teamName == null || teamName.isEmpty()) {
            this.goToSettings(null);
            Toast.makeText(this, "Specify a team name. Go to General > Team name", Toast.LENGTH_LONG).show();
            return null;
        }
        return teamName;
    }

    /************************
     ** PERMISSION METHODS **
     ************************/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        this.permissionsHelper.onRequestPermissionsResult(permissions, grantResults);
    }

    private void checkIfExternalStorageWritingPermission() {
        if (this.permissionsHelper.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            this.launchIntervalLog();
        } else {
            this.permissionsHelper.requestPermissions(
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                this.handlePermissionExternalStorageWritingResult()
            );
        }
    }

    private void checkIfLocationPermission() {
        if (this.permissionsHelper.isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(TAG, "Permission for location granted");
            this.mapsFragment.setLocationPermissionGranted(true);
            this.mapsFragment.updateLocationUI();
        } else {
            Log.i(TAG, "Permission for location not granted");
            this.permissionsHelper.requestPermissions(
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                this.handlePermissionLocationResult()
            );
        }
    }

    public void launchIntervalLog() {
        this.intervalToLog = new Interval(() ->
                this.writeInFile(null),
                Integer.parseInt(this.sharedPreferences.getString("sync_local", "2")) * 1000);
        this.intervalToLog.start();
    }

    private void initializePermissions() {
        this.permissionsHelper = new PermissionsHelper(this);
        this.checkIfLocationPermission();
        this.checkIfExternalStorageWritingPermission();
    }

    private PermissionCallback handlePermissionExternalStorageWritingResult() {
        return (final HashMap<String, PermissionsHelper.PermissionGrant> mapPermissionGranted) -> {
            PermissionsHelper.PermissionGrant permissionGrant = mapPermissionGranted
                    .get(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if(permissionGrant == PermissionsHelper.PermissionGrant.DENIED ||
                permissionGrant == PermissionsHelper.PermissionGrant.NEVERSHOW
            ) {
                Log.i(TAG, "App stopped due to permission of storage");
                this.finish();
            } else {
                this.launchIntervalLog();
            }
        };
    }

    private PermissionCallback handlePermissionLocationResult() {
        return (final HashMap<String, PermissionsHelper.PermissionGrant> mapPermissionGrants) -> {
            PermissionsHelper.PermissionGrant permissionGrant = mapPermissionGrants
                    .get(Manifest.permission.ACCESS_FINE_LOCATION);
            if(permissionGrant == PermissionsHelper.PermissionGrant.GRANTED) {
                this.mapsFragment.setLocationPermissionGranted(true);
                this.mapsFragment.updateLocationUI();
            } else {
                Log.i(TAG, "App stopped due to permission of location");
                this.finish();
            }
        };
    }

    /*****************************
     ** FILE MANAGEMENT METHODS **
     *****************************/

    @NonNull
    private File getLocalFile() {
        String filepath = this.baseDir + File.separator + this.filename + ".csv";
        return new File(filepath);
    }

    private void saveFile() {
        if(this.fileID == null || this.fileID.isEmpty()) {
            Log.i(TAG, "File does not exist when saveFile()");
            this.driveServiceHelper.createFile()
                .addOnSuccessListener(fileId -> {
                    this.fileID = fileId;
                    Log.i(TAG, "File created");
                    this.updateFile();
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Couldn't create the file on Drive", exception));
        } else {
            Log.i(TAG, "File exists when saveFile()");
            this.updateFile();
        }

    }

    private void sendInternalFileToGDrive() {
        // If a local file exist, it is sent to google drive
        if (getLocalFile().exists() && !getLocalFile().isDirectory() && this.driveServiceHelper != null) {
            Log.i(TAG, "Creating a file");
            // If a google drive folder exists, the file is saved in there
            if (this.driveServiceHelper.getTeamFolderID() != null && !this.driveServiceHelper.getTeamFolderID().isEmpty()) {
                this.saveFile();
            }
        }
    }

    private void updateFile() {
        File file = getLocalFile();
        this.driveServiceHelper.updateFile(this.fileID, file)
            .addOnSuccessListener(Void -> Log.i(TAG, "File updated in Google Drive"))
            .addOnFailureListener(e -> Log.e(TAG, "Problem to update file in Google Drive", e));
    }

    private void writeInFile(Icon icon) {
        Log.i(TAG, "Write in local file");
        String todayWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        LatLng latlng = this.mapsFragment.getCoordinates();
        if(latlng != null) {
            try {
                File f = getLocalFile();
                CSVWriter writer;
                if (f.exists() && !f.isDirectory()) {
                    writer = new CSVWriter(new FileWriter(f.getAbsolutePath(), true));
                    String[] data;
                    if(icon != null) {
                        String type = icon.getSelected() ? "MESSAGE_START" : "MESSAGE_STOP";
                        data = new String[]{
                                todayWithTime,
                                type,
                                String.valueOf(latlng.longitude),
                                String.valueOf(latlng.latitude),
                                icon.getMessage(),
                                icon.getType(),
                                icon.getDetail(),
                                this.teamName
                        };
                    } else {
                        data = new String[]{
                                todayWithTime,
                                "GPS",
                                String.valueOf(latlng.longitude),
                                String.valueOf(latlng.latitude),
                                "",
                                "",
                                "",
                                this.teamName
                        };
                    }
                    writer.writeNext(data);
                } else {
                    writer = new CSVWriter(new FileWriter(f.getAbsolutePath()));
                    String[] header = {"Timestamp", "Type", "GPS longitude", "GPS latitude", "Selected Message", "Message type", "detail", "Team"};
                    writer.writeNext(header);
                }
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to write data in CSV file", e);
            }
        }
    }
}
