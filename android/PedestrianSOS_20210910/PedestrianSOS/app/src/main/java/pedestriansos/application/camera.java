package pedestriansos.application;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class camera extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;

    private boolean isRecording = false;

    int cameraFacing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (Build.VERSION.SDK_INT < 16) {
//            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }
//        else    {
//            View decorView = getWindow().getDecorView();
//            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//            decorView.setSystemUiVisibility(uiOptions);
//            //ActionBar actionBar = getActionBar();
//            //actionBar.hide();
//        }
        setContentView(R.layout.camera);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)    {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
                return;
            }
        }

        cameraSetup();
    }

    void cameraSetup()  {
        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        // Add a listener to the Capture button
        ImageButton captureButton = findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                v -> {
                    // get an image from the camera
                    mCamera.takePicture(null, null, mPicture);
                    try {
                        mCamera.startPreview();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
        captureButton.bringToFront();

// Add a listener to the Capture button
        captureButton2 = findViewById(R.id.button_capture2);
        captureButton2.setOnClickListener(
                v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)    {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                            return;
                        }
                    }
                    if (isRecording) {
                        // stop recording and release camera
                        try {
                            mediaRecorder.stop();  // stop the recording

                            uploadFile(videoFile.toString(), URL + "media.php", "file", null);
                        } catch (Exception e) {
                            e.printStackTrace();

                            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
                        }
                        releaseMediaRecorder(); // release the MediaRecorder object
                        mCamera.lock();         // take camera access back from MediaRecorder

                        // inform the user that recording has stopped
                        //setCaptureButtonText("Capture");
                        captureButton2.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_circle_64));
                        isRecording = false;

                        Toast.makeText(getApplicationContext(), videoFile.toString(), Toast.LENGTH_LONG).show();
                    } else {
                        // initialize video camera
                        if (prepareVideoRecorder()) {
                            // Camera is available and unlocked, MediaRecorder is prepared,
                            // now you can start recording
                            mediaRecorder.start();

                            // inform the user that recording has started
                            //setCaptureButtonText("Stop");
                            captureButton2.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_stop_circle_64));
                            isRecording = true;
                        } else {
                            // prepare didn't work, release the camera
                            releaseMediaRecorder();
                            // inform user
                        }
                    }
                }
        );
        captureButton2.bringToFront();

        ImageButton facingButton = findViewById(R.id.facing);
        facingButton.setOnClickListener(v -> {
            if(Camera.getNumberOfCameras() > 1)    {
                mCamera.stopPreview();
                mCamera.release();
                preview.removeView(mPreview);
                if(cameraFacing == 0)    {
                    cameraFacing = 1;
                }
                else    {
                    cameraFacing = 0;
                }
                mCamera = Camera.open(cameraFacing);
                mPreview = new CameraPreview(this, mCamera);
                preview.addView(mPreview);
                captureButton.bringToFront();
                captureButton2.bringToFront();
                facingButton.bringToFront();
            }
        });
        facingButton.bringToFront();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED)    {
            if(requestCode == 0)    {
                cameraSetup();
            }
            else if(requestCode == 1)   {
                finish();
                startActivity(getIntent());
            }
        }
    }

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        // this device has a camera
        // no camera on this device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        }
        else    {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private final Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = null;
            try {
                pictureFile = createFile(".jpg");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            //File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                //Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast.makeText(getApplicationContext(), pictureFile.getPath(), Toast.LENGTH_LONG).show();
                uploadFile(pictureFile.getPath(), URL + "media.php", "file", null);
                //setResult(RESULT_OK);
            } catch (FileNotFoundException e) {
                //Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                //Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    ImageButton captureButton2;

    /*void setCaptureButtonText(String text) {
        captureButton2.setText(text);
    }*/

    MediaRecorder mediaRecorder;

    File videoFile;

    private boolean prepareVideoRecorder(){

        //mCamera = getCameraInstance();
        mediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        //mediaRecorder.setOutputFile(Objects.requireNonNull(getOutputMediaFile(MEDIA_TYPE_VIDEO)).toString());
        try {
            videoFile = createFile(".mp4");
            mediaRecorder.setOutputFile(videoFile.toString());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            //Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            //Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    //public static final int MEDIA_TYPE_IMAGE = 1;
    //public static final int MEDIA_TYPE_VIDEO = 2;

    /* Create a file Uri for saving an image or video */
    /*private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }*/

    /** Create a File for saving an image or video */
    /*private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                //Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }*/

    File createFile(String fileSuffix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "FILE_" + timeStamp + "_";
        File storageDir;
        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                fileName,
                fileSuffix,
                storageDir
        );
    }

    String URL = "http://192.168.0.5/";

    void uploadFile(String filePath, String WEB_URL, String WEB_NAME, InputStream fileStream) {
        int maxBufferSize = 1024 * 1024;
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
            File sourceFile = null;
            if (filePath != null) {
                sourceFile = new File(filePath);
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
                    java.net.URL url = new URL(WEB_URL);
                    conn = (HttpURLConnection) url.openConnection();
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
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
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
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), finalServerResponseString, Toast.LENGTH_LONG).show());
        }).start();
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

}