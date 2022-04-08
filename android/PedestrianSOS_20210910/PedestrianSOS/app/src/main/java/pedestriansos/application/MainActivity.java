package pedestriansos.application;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    boolean darkModeEnabled = true;
    String URL = "https://pedestrian-sos.000webhostapp.com/";

    Uri PS_notificationSoundUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setTheme(R.style.Theme_PedestrianSOS);
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        /*if(sharedPreferences.getBoolean("first_screen", true))    {
            ActivityResultLauncher<Intent> firstScreen = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() == RESULT_OK)    {
                            mainScreenSetup(sharedPreferences);
                        }
                        else if(result.getResultCode() == RESULT_CANCELED)    {
                            finish();
                        }
                    }
            );
            firstScreen.launch(new Intent(this, firstScreen.class));
        }
        else {*/
            mainScreenSetup(sharedPreferences);
        //}
    }

    void mainScreenSetup(SharedPreferences sharedPreferences)  {
        setContentView(R.layout.activity_main);
        findViewById(R.id.takePhotoButton).setOnClickListener(v -> takePhoto());
        findViewById(R.id.recordVideoButton).setOnClickListener(v -> recordVideo());
        findViewById(R.id.choosePhotoButton).setOnClickListener(v -> openFileChooser("image/*"));
        findViewById(R.id.chooseVideoButton).setOnClickListener(v -> openFileChooser("video/*"));
        darkModeEnabled = sharedPreferences.getBoolean("dark_mode", true);
        darkMode(darkModeEnabled);
        SwitchCompat darkModeSwitch = findViewById(R.id.darkModeSwitch);
        darkModeSwitch.setChecked(darkModeEnabled);
        darkModeSwitch.setOnClickListener(v -> darkMode(darkModeSwitch.isChecked()));

        PS_notificationSoundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + '/' + R.raw.psnotification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createNotificationChannel(getString(R.string.app_name), getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT, getString(R.string.notificationchannelid), PS_notificationSoundUri);
            createNotificationChannel(getString(R.string.uploadNotificationChannelName), getString(R.string.uploadNotificationChannelDescription), NotificationManager.IMPORTANCE_LOW, getString(R.string.uploadNotificationChannelID), null);
            createNotificationChannel(getString(R.string.locationNotificationChannelName), getString(R.string.locationNotificationChannelDescription), NotificationManager.IMPORTANCE_LOW, getString(R.string.locationNotificationChannelID), null);
        }

        findViewById(R.id.button).setOnClickListener(v -> {
            sendPedestrianSOSnotification(getString(R.string.app_name));
            Toast.makeText(getApplicationContext(), R.string.app_name, Toast.LENGTH_LONG).show();
            Snackbar.make(findViewById(R.id.background), R.string.app_name, Snackbar.LENGTH_LONG).show();
        });

        findViewById(R.id.button2).setOnClickListener(v -> {
            /*SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putBoolean("first_screen", true);
            sharedPreferencesEditor.apply();*/
            startActivity(new Intent(this, firstScreen.class));
        });

        ActivityResultLauncher<Intent> camera = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    /*if(result.getResultCode() == RESULT_OK) {
                        Toast.makeText(getApplicationContext(), "123", Toast.LENGTH_LONG).show();
                    }*/
                });
        findViewById(R.id.button3).setOnClickListener(v -> {
            //startActivity(new Intent(this, camera.class));
            camera.launch(new Intent(this, camera.class));
        });

        this.registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    volatile boolean networkNotConnected;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            networkNotConnected = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if(networkNotConnected)    {
                //Toast.makeText(getApplicationContext(), "0", Toast.LENGTH_LONG).show();
            }
            else    {
                //Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_LONG).show();
            }
        }
    };

    int notificationID = 0;

    private void sendNotification(int smallIcon, String title, String text, Uri sound) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.notificationchannelid))
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(sound)
                .setColor(Color.parseColor("#028900"));
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notificationID++, builder.build());
    }

    private void sendPedestrianSOSnotification(String text) {
        sendNotification(R.drawable.psnotificationicon, getString(R.string.app_name), text, PS_notificationSoundUri);
    }

    private void createNotificationChannel(String name, String description, int importance, String channelid, Uri sound) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelid, name, importance);
            channel.setDescription(description);
            if(sound != null)
                channel.setSound(sound, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    String currentPhotoPath;

    File createFile(String fileSuffix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "FILE_" + timeStamp + "_";
        File storageDir;
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //}
        /*else    {
            String packageName = getApplicationContext().getPackageName();
            File externalPath = Environment.getExternalStorageDirectory();
            storageDir = new File(externalPath.getAbsolutePath() +
                    "/Android/data/" + packageName + "/files");
        }*/
        return File.createTempFile(
                fileName,
                fileSuffix,
                storageDir
        );
    }

    boolean isNetworkConnected()    {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    boolean locationUploadEnabled;

    void uploadFile(String filePath, String WEB_URL, String WEB_NAME, InputStream fileStream, int notificationTitle, int notificationIcon) {
        if(!isNetworkConnected())    {
            Toast.makeText(getApplicationContext(), "!CONNECTION", Toast.LENGTH_LONG).show();
            return;
        }
        boolean chunkedStream = false;
        int maxBufferSize = 1024 * 1024;
        int chunkLen = 256 * 1024;
        int uploadProgressNotificationID = notificationID++;
        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this, getString(R.string.uploadNotificationChannelID));
        builder2.setContentTitle(getString(notificationTitle))
                .setSmallIcon(notificationIcon)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        int PROGRESS_MAX = 100;
        builder2.setProgress(PROGRESS_MAX, 0, !chunkedStream);
        notificationManager2.notify(uploadProgressNotificationID, builder2.build());
        new Thread(() -> {
            int serverResponseCode;
            String serverResponseString = null;
            HttpURLConnection conn;
            DataOutputStream dos;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            long totalSize = 0;
            File sourceFile = null;
            if (filePath != null) {
                sourceFile = new File(filePath);
                totalSize = sourceFile.length();
            } else {
                try {
                    totalSize = fileStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (((sourceFile != null) && sourceFile.isFile()) || (fileStream != null)) {
                try {
                    InputStream fileInputStream;
                    if(filePath != null) {
                        fileInputStream = new FileInputStream(sourceFile);
                    }
                    else    {
                        fileInputStream = fileStream;
                    }
                    URL url = new URL(WEB_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    if(chunkedStream)
                        conn.setChunkedStreamingMode(chunkLen);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty(WEB_NAME, filePath);
                    dos = new DataOutputStream(conn.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"" + WEB_NAME + "\";filename=\""
                            + filePath + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    int progress;
                    long length = 0;
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        if(chunkedStream) {
                            length += bufferSize;
                            progress = (int) (((float) length / (float) totalSize) * 100);
                            builder2.setContentText(String.valueOf(progress) + '%');
                            builder2.setProgress(PROGRESS_MAX, progress, false);
                            notificationManager2.notify(uploadProgressNotificationID, builder2.build());
                        }
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    serverResponseCode = conn.getResponseCode();
                    if (serverResponseCode == 200) {
                        serverResponseString = getServerResponse(conn);
                    } else {
                        serverResponseString = "HTTP RESPONSE CODE " + serverResponseCode;
                    }
                    fileInputStream.close();
                    dos.flush();
                    dos.close();
                } catch (MalformedURLException ex) {
                    ex.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String finalServerResponseString = serverResponseString;
            runOnUiThread(() -> {
                int contentText;
                if ((finalServerResponseString != null) && (finalServerResponseString.charAt(0) == '#')) {
                    contentText = R.string.uploaded;
                    ID = finalServerResponseString.substring(1, finalServerResponseString.indexOf('|'));
                    KEY = finalServerResponseString.substring(finalServerResponseString.indexOf('|') + 1);
                    if(locationUploadEnabled) {
                        //locationUploadEnabled = false;
                        getLocation();
                    }
                    description();
                }
                else {
                    contentText = R.string.uploadError;
                }
                builder2.setContentText(getString(contentText))
                        .setProgress(0, 0, false);
                notificationManager2.notify(uploadProgressNotificationID, builder2.build());
            });
        }).start();
    }

    String action;

    ActivityResultLauncher<Intent> cameraActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    locationUploadEnabled = true;
                    switch (action) {
                        case "takePhoto":
                            uploadFile(currentPhotoPath, URL + "media.php", "file", null, R.string.photoUploadTitle, R.drawable.ic_image_upload);
                            break;
                        case "recordVideo":
                            assert result.getData() != null;
                            uploadFileFromURI(result.getData().getData(), R.string.videoUploadTitle, R.drawable.ic_video_upload);
                            break;
                    }
                }
            }
    );

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            this::onActivityResult_chooseFile);

    private String cameraIntent(String cameraIntentString, String fileSuffix) {
        String currentFilePath = null;
        Intent camera_intent = new Intent(cameraIntentString);
        if (camera_intent.resolveActivity(getPackageManager()) != null) {
            File file = null;
            try {
                file = createFile(fileSuffix);
                currentFilePath = file.getAbsolutePath();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            if (file != null) {
                Uri URI;
                if (Build.VERSION.SDK_INT >= 23) {
                    URI = FileProvider.getUriForFile(this,
                            "com.example.android.fileprovider",
                            file);
                } else {
                    URI = Uri.fromFile(file);
                }
                camera_intent.putExtra(MediaStore.EXTRA_OUTPUT, URI);
                cameraActivity.launch(camera_intent);
            }
        }
        return currentFilePath;
    }

    void takePhoto() {
        action = "takePhoto";
        currentPhotoPath = cameraIntent(MediaStore.ACTION_IMAGE_CAPTURE, ".jpg");
    }

    void recordVideo() {
        action = "recordVideo";
        cameraActivity.launch(new Intent(MediaStore.ACTION_VIDEO_CAPTURE));
    }

    void openFileChooser(String fileType)  {
        mGetContent.launch(fileType);
    }

    private void darkMode(boolean enabled) {
        View backgroundView = findViewById(R.id.background);
        SwitchCompat darkModeSwitch = findViewById(R.id.darkModeSwitch);
        String foregroundColorString;
        if (enabled) {
            foregroundColorString = "ffffff";
            backgroundView.setBackgroundColor(Color.parseColor("#000000"));
        } else {
            foregroundColorString = "000000";
            backgroundView.setBackgroundColor(Color.parseColor("#ffffff"));
        }
        int foregroundColor = Color.parseColor('#' + foregroundColorString);
        darkModeSwitch.setTextColor(foregroundColor);
        darkModeSwitch.setThumbTintList(ColorStateList.valueOf(foregroundColor));
        darkModeSwitch.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#20" + foregroundColorString)));
        darkModeEnabled = enabled;
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("dark_mode", darkModeEnabled);
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            sharedPreferencesEditor.apply();
        //}
        /*else {
            sharedPreferencesEditor.commit();
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED)    {
            if(requestCode == 0)    {
                getLocation();
            }
        }
    }

    LocationManager locationManager;
    LocationListener locationListener;

    boolean requestLocation(String provider, long minTime, float minDistance) {
        if (locationManager.isProviderEnabled(provider)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                return false;
            }
            locationManager.requestLocationUpdates(provider, minTime, minDistance, locationListener);
            return true;
        }
        return false;
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }
        int locationProgressNotificationID = notificationID++;
        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this, getString(R.string.locationNotificationChannelID));
        builder2.setContentTitle(getString(R.string.locationNotificationChannelDescription))
                .setContentText("...")
                .setSmallIcon(R.drawable.ic_location_searching)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        builder2.setProgress(0, 0, true);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String[] uploadLocationParameters = {
                        "n",
                        "key",
                        "latitude",
                        "longitude",
                        "altitude",
                        "accuracy"
                };
                String[] uploadLocationValues = {ID,
                        KEY,
                        String.valueOf(location.getLatitude()),
                        String.valueOf(location.getLongitude()),
                        String.valueOf(location.getAltitude()),
                        String.valueOf(location.getAccuracy())
                };
                locationManager.removeUpdates(locationListener);
                builder2.setContentText(uploadLocationValues[2] + "; " + uploadLocationValues[3] + "; " + uploadLocationValues[4] + "; " + uploadLocationValues[5])
                        .setSmallIcon(R.drawable.ic_my_location)
                        .setProgress(0, 0, false)
                        .setStyle(new NotificationCompat.BigTextStyle());
                notificationManager2.notify(locationProgressNotificationID, builder2.build());
                postString(URL + "location.php", uploadLocationParameters, uploadLocationValues, R.string.locationUploadTitle, R.drawable.ic_location_upload);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {

            }
        };
        //Toast.makeText(getApplicationContext(), String.valueOf(locationManager.getAllProviders()), Toast.LENGTH_LONG).show();
        boolean GPS_locationRequested = requestLocation(LocationManager.GPS_PROVIDER, 60000, 0);
        boolean NETWORK_locationRequested = requestLocation(LocationManager.NETWORK_PROVIDER, 60000, 0);
        if(
            GPS_locationRequested
                ||
                    NETWORK_locationRequested
        )    {
            notificationManager2.notify(locationProgressNotificationID, builder2.build());
        }
    }

    String ID;
    String KEY;

    void postString(String url, String[] POST_PARAMETERS, String[] POST_VALUES, int notificationTitle, int notificationIcon)   {
        int uploadProgressNotificationID = notificationID++;
        NotificationManagerCompat notificationManager2 = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this, getString(R.string.uploadNotificationChannelID));
        builder2.setContentTitle(getString(notificationTitle))
                .setContentText(getString(R.string.uploading))
                .setSmallIcon(notificationIcon)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        builder2.setProgress(0, 0, true);
        notificationManager2.notify(uploadProgressNotificationID, builder2.build());
        new Thread(() -> {
            String serverResponse = postString0(url, POST_PARAMETERS, POST_VALUES);
            runOnUiThread(() -> {
                int contentText;
                if(serverResponse.equals("1"))    {
                    contentText = R.string.uploaded;
                }
                else    {
                    contentText = R.string.uploadError;
                }
                builder2.setContentText(getString(contentText))
                        .setProgress(0, 0, false);
                notificationManager2.notify(uploadProgressNotificationID, builder2.build());
            });
        }).start();
    }

    String postString0(String postUrl, String[] postParameters, String[] postValues) {
        StringBuilder data = new StringBuilder();
        boolean isFirstParameter = true;

        for(byte i = 0; i < postParameters.length; i++)  {
            if(isFirstParameter)
                isFirstParameter = false;
            else
                data.append('&');
            try {
                data.append(URLEncoder.encode(postParameters[i], "UTF-8")).append('=').append(URLEncoder.encode(postValues[i], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        try
        {
            URL url = new URL(postUrl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data.toString());
            wr.flush();
            return getServerResponse(conn);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
    private String getServerResponse(URLConnection local_httpURLConnection) throws IOException {
        InputStream inputStream = local_httpURLConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null)  {
            result.append(line);
        }
        return result.toString();
    }

    void uploadFileFromURI(Uri local_uri, int notificationTitle, int notificationIcon)    {
        try {
            uploadFile(null, URL + "media.php", "file", getContentResolver().openInputStream(local_uri), notificationTitle, notificationIcon);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    Uri currentFileUri;

    ActivityResultLauncher<Intent> chosenFilePreview = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    int notificationTitle = 0;
                    int notificationIcon = 0;
                    String fileType;
                    assert result.getData() != null;
                    fileType = result.getData().getStringExtra("fileType");
                    if(fileType.equals("image"))    {
                        notificationTitle = R.string.photoUploadTitle;
                        notificationIcon = R.drawable.ic_image_upload;
                    }
                    else if(fileType.equals("video"))   {
                        notificationTitle = R.string.videoUploadTitle;
                        notificationIcon = R.drawable.ic_video_upload;
                    }
                    locationUploadEnabled = false;
                    uploadFileFromURI(currentFileUri, notificationTitle, notificationIcon);
                }
            });

    private void onActivityResult_chooseFile(Uri uri) {
        if(uri == null)
            return;
        currentFileUri = uri;
        Intent intent = new Intent(MainActivity.this, choseFilePreview.class);
        intent.putExtra("uri", uri.toString());
        chosenFilePreview.launch(intent);
    }

    void description()  {
        int alertDialogTheme;
        if(darkModeEnabled)    {
            alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        }
        else    {
            alertDialogTheme = AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, alertDialogTheme);
        builder.setTitle(R.string.description);
        builder.setMessage(R.string.writeDescription);
        builder.setIcon(R.drawable.ic_baseline_description_24);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setSingleLine(false);
        if(darkModeEnabled)    {
            editText.setTextColor(Color.parseColor("#ffffff"));
        }
        builder.setView(editText);
        builder.setPositiveButton("✓ " + getString(R.string.uploadButton), (dialog, which) -> {
            String[] parametersArray = {"n", "key", "description", "situation"};
            String[] valuesArray = {ID, KEY, editText.getText().toString(), ""};
            postString(URL + "info.php", parametersArray, valuesArray, R.string.descriptionUploadTitle, R.drawable.ic_text_upload);
        });
        builder.setNegativeButton("✕ " + getString(R.string.cancelButton), (dialog, which) -> dialog.cancel());
        builder.show();
    }

}

