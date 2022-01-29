package pedestriansos.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(() -> {
            SharedPreferences sharedPreferences = getSharedPreferences("developersettings", MODE_PRIVATE);
            URL = sharedPreferences.getString("url", DEFAULT_URL);
            runOnUiThread(() -> {
                if(URL.equals("0"))    {
                    AlertDialog.Builder builder000 = new AlertDialog.Builder(this);
                    builder000.setTitle("გაფრთხილება! / WARNING!");
                    builder000.setMessage("აპლიკაციას ამ ეტაპზე არ აქვს ინტერნეტ სერვერი... / The application does not have web server at the time...");
                    builder000.setCancelable(false);
                    builder000.setPositiveButton("გასაგებია / OK", ((dialog000, which000) -> dialog000.cancel()));
                    builder000.show();
                }
            });
            sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
            View takePhotoButton = findViewById(R.id.take_photo);
            View recordVideoButton = findViewById(R.id.record_video);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE)    {
                takePhotoButton.setOnClickListener(v -> takePhoto());
                recordVideoButton.setOnClickListener(v -> recordVideo());
            }
            else    {
                ((ViewGroup)takePhotoButton.getParent()).removeView(takePhotoButton);
                ((ViewGroup)recordVideoButton.getParent()).removeView(recordVideoButton);
            }
            findViewById(R.id.choose_photo).setOnClickListener(v -> choosePhoto());
            findViewById(R.id.choose_video).setOnClickListener(v -> chooseVideo());
            findViewById(R.id.settings).setOnClickListener(v -> openSettings());
            recordSoundIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
            recordSoundAvailable = recordSoundIntent.resolveActivity(getPackageManager()) != null;
            switch (sharedPreferences.getInt("firstscreenis", 0))  {
                case 1:
                    takePhoto();
                    break;
                case 2:
                    recordVideo();
                    break;
                case 3:
                    choosePhoto();
                    break;
                case 4:
                    chooseVideo();
                    break;
            }
            this.registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            findViewById(R.id.statusLayout).setOnClickListener(v -> showLogs(logs));
        }).start();
        //if(!voiceRecordAvailable)
            //((TextView)findViewById(R.id.soundUpload)).setTextColor(Color.argb(128, 255, 255, 255));
    }

    void saveSharedPreferencesBoolean(SharedPreferences sharedPreferences, String key, boolean value)   {
        new Thread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                sharedPreferences.edit().putBoolean(key, value).apply();
            }
            else    {
                sharedPreferences.edit().putBoolean(key, value).commit();
            }
        }).start();
    }

    void addCheckBoxs(String index, int text, int drawable, LinearLayout linearLayout, boolean defaultValue) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(text);
        if(drawable != -1)
            checkBox.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(drawable), null, null, null);
        linearLayout.addView(checkBox);
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> saveSharedPreferencesBoolean(sharedPreferences, getString(text) + index, isChecked));
        if(!sharedPreferences.contains(getString(text) + index))
            saveSharedPreferencesBoolean(sharedPreferences, getString(text) + index, defaultValue);
        checkBox.setChecked(sharedPreferences.getBoolean(getString(text) + index, defaultValue));
    }

    void developerButtonInitialize(TextView textView)    {
        textView.setOnClickListener(v -> {
            if(++developerButtonClicks < 7)
                return;
            developerButtonClicks = 0;
            AlertDialog.Builder builder0 = new AlertDialog.Builder(this);
            builder0.setTitle(R.string.settings);
            builder0.setPositiveButton("✓ " + getString(R.string.open), (dialog0, which0) -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("SET WEB URL");
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                EditText editText = new EditText(this);
                editText.setText(URL);
                Button button = new Button(this);
                button.setText(R.string.setDefaultUrl);
                button.setOnClickListener(v1 -> editText.setText(DEFAULT_URL));
                linearLayout.addView(button);
                linearLayout.addView(editText);
                builder.setView(linearLayout);
                builder.setPositiveButton("✓ " + getString(R.string.save), (dialog, which) -> {
                    URL = editText.getText().toString();
                    SharedPreferences sharedPreferences = getSharedPreferences("developersettings", MODE_PRIVATE);
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putString("url", URL);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                        sharedPreferencesEditor.apply();
                    }
                    else    {
                        sharedPreferencesEditor.commit();
                    }
                });
                builder.setNegativeButton("✕ " + getString(R.string.cancel), (dialog, which) -> dialog.cancel());
                builder.show();
            });
            builder0.setNegativeButton("✕ " + getString(R.string.cancel), (dialog0, which0) -> dialog0.cancel());
            builder0.show();
        });
    }

    void addPhotoVideoCheckBoxLayout(String index, LinearLayout linearLayout, int title, int drawable, boolean takephoto, boolean recordvideo, boolean choosephoto, boolean choosevideo)    {
        LinearLayout linearLayout2 = new LinearLayout(this);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        setStroke(linearLayout2);
        TextView textView = new TextView(this);
        textView.setText(title);
        if(drawable != -1)
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(drawable), null, null, null);
        linearLayout2.addView(textView);
        addCheckBoxs(index, R.string.takephoto, R.drawable.ic_take_photo, linearLayout2, takephoto);
        addCheckBoxs(index, R.string.recordvideo, R.drawable.ic_record_video, linearLayout2, recordvideo);
        addCheckBoxs(index, R.string.choosephoto, R.drawable.ic_choose_photo, linearLayout2, choosephoto);
        addCheckBoxs(index, R.string.choosevideo, R.drawable.ic_choose_video, linearLayout2, choosevideo);
        linearLayout.addView(linearLayout2);
    }

    void resetSettings()    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.reset) + ' ' + getString(R.string.settings) + '?');
        builder.setPositiveButton("✓ " + getString(R.string.reset), (dialog, which) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                getSharedPreferences("settings", MODE_PRIVATE).edit().clear().apply();
            }
            else    {
                getSharedPreferences("settings", MODE_PRIVATE).edit().clear().commit();
            }
            currentSettingsDialog.dismiss();
            openSettings();
        });
        builder.setNegativeButton("✕ " + getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    void clearLogs()    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.clear) + ' ' + getString(R.string.data) + '?');
        builder.setPositiveButton("✓ " + getString(R.string.clear), (dialog, which) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                getSharedPreferences("logs", MODE_PRIVATE).edit().clear().apply();
            }
            else    {
                getSharedPreferences("logs", MODE_PRIVATE).edit().clear().commit();
            }
        });
        builder.setNegativeButton("✕ " + getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    AlertDialog currentSettingsDialog;

    void openSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        title.setText(R.string.settings);
        title.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_settings), null, null, null);
        linearLayout.addView(title);
        LinearLayout logs = new LinearLayout(this);
        logs.setOrientation(LinearLayout.VERTICAL);
        setStroke(logs);
        TextView logsTitle = new TextView(this);
        logsTitle.setText(R.string.data);
        logs.addView(logsTitle);
        Button viewData = new Button(this);
        viewData.setText(R.string.viewData);
        viewData.setOnClickListener(v -> showLogs(getLogs()));
        //if(getSharedPreferences("logs", MODE_PRIVATE).getString("string", "").equals(""))
        boolean isLogsEmpty = getSharedPreferences("logs", MODE_PRIVATE).getAll().isEmpty();
        viewData.setEnabled(!isLogsEmpty);
        logs.addView(viewData);
        //CheckBox keepData = new CheckBox(this);
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        /*keepData.setChecked(sharedPreferences.getBoolean("keepdata", true));
        keepData.setText(R.string.saveData);
        keepData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("keepdata", isChecked);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                editor.apply();
            }
            else    {
                editor.commit();
            }
        });
        logs.addView(keepData);*/
        addCheckBoxs("", R.string.saveData, -1, logs, true);
        Button clearLogs = new Button(this);
        String clearLogsButtonText = getString(R.string.clear) + ' ' + getString(R.string.data);
        clearLogs.setText(clearLogsButtonText);
        clearLogs.setBackgroundColor(0xffff0000);
        clearLogs.setOnClickListener(v -> clearLogs());
        clearLogs.setEnabled(!isLogsEmpty);
        logs.addView(clearLogs);
        linearLayout.addView(logs);
        LinearLayout firstScreenIsLayout = new LinearLayout(this);
        setStroke(firstScreenIsLayout);
        TextView firstScreenIsTitle = new TextView(this);
        firstScreenIsTitle.setText(R.string.firstScreenIs);
        firstScreenIsLayout.addView(firstScreenIsTitle);
        Spinner spinner = new Spinner(this);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    sharedPreferences.edit().putInt("firstscreenis", position).apply();
                }
                else    {
                    sharedPreferences.edit().putInt("firstscreenis", position).commit();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.firstscreenis_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(sharedPreferences.getInt("firstscreenis", 0));
        firstScreenIsLayout.addView(spinner);
        linearLayout.addView(firstScreenIsLayout);
        addPhotoVideoCheckBoxLayout("0", linearLayout, R.string.reOpenScreenTitle, -1, false, false, false, false);
        addPhotoVideoCheckBoxLayout("1", linearLayout, R.string.description, R.drawable.ic_description, true, true, true, true);
        addPhotoVideoCheckBoxLayout("2", linearLayout, R.string.location, R.drawable.ic_location, true, true, false, false);
        Button reset = new Button(this);
        String resetButtonText = getString(R.string.reset) + ' ' + getString(R.string.settings);
        reset.setText(resetButtonText);
        reset.setBackgroundColor(0xffff0000);
        reset.setOnClickListener(v -> resetSettings());
        linearLayout.addView(reset);
        scrollView.addView(linearLayout);
        builder.setView(scrollView);
        developerButtonInitialize(title);
        currentSettingsDialog = builder.show();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)    {
            openSettings();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    short developerButtonClicks;

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(findViewById(R.id.statusLayout).getVisibility() == View.VISIBLE)    {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            Button button = new Button(this);
            button.setText(R.string.exitApp);
            button.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_baseline_exit_to_app_24), null, null, null);
            button.setOnClickListener(v -> finish());
            builder.setView(button);
            builder.show();
        }
        else    {
            //finish();
            super.onBackPressed();
        }
    }

    void setStroke(View view)    {
        ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
        shapeDrawable.getPaint().setColor(Color.WHITE);
        shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
        shapeDrawable.getPaint().setStrokeWidth(1);
        view.setBackgroundDrawable(shapeDrawable);
    }

    String serialize(String[] local_stringArray)  {
        StringBuilder serializedString = new StringBuilder();
        serializedString.append(local_stringArray.length);
        serializedString.append("|");
        for (String s : local_stringArray) {
            if(s == null)
                s = "null";
            serializedString.append("[");
            serializedString.append(s.length());
            serializedString.append("]");
            serializedString.append(s);
        }
        return serializedString.toString();
    }

    String[] unserialize(String local_string)  {
        ArrayList<String> unserializedArray = new ArrayList<>();
        int length;
        int startIndex = 0;
        for (int i = 0; i < Integer.parseInt(local_string.substring(0, local_string.indexOf("|"))); i++) {
            length = Integer.parseInt(local_string.substring(local_string.indexOf("[", startIndex) + 1, local_string.indexOf("]", startIndex)));
            startIndex = local_string.indexOf("]", startIndex) + 1;
            unserializedArray.add(local_string.substring(startIndex, startIndex + length));
            startIndex += length;
        }
        String[] arrayToReturn = new String[unserializedArray.size()];
        for (int i = 0; i < unserializedArray.size(); i++) {
            arrayToReturn[i] = unserializedArray.get(i);
        }
        return arrayToReturn;
    }

    ArrayList<String[]> logs = new ArrayList<>();

    ArrayList<String[]> getLogs()  {
        ArrayList<String[]> logsArrayList = new ArrayList<>();
        SharedPreferences sharedPreferences = getSharedPreferences("logs", MODE_PRIVATE);
        //String logsString = sharedPreferences.getString("string", "");
        Map<String, ?> logsArray = sharedPreferences.getAll();
        if(!logsArray.isEmpty()) {
            //logsString = logsString.replace("\\|\\|\\|\\|", "||||");
            //logsString = logsString.replace("\\*\\*\\*\\*", "****");
            //String[] logsArray = logsString.split("\\*\\*\\*\\*");
            /*for (String s : logsArray) {
                logsArrayList.add(s.split("\\|\\|\\|\\|"));
            }*/
            for (int i = 0; i < logsArray.size(); i++) {
                logsArrayList.add(unserialize((String) Objects.requireNonNull(logsArray.get(String.valueOf(i)))));
            }
        }
        return logsArrayList;
    }

    void saveLogIfEnabled(String[] local_logs) {
        new Thread(() -> {
            if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean(getString(R.string.saveData), true)) {
                SharedPreferences sharedPreferences = getSharedPreferences("logs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                //StringBuilder logsString = new StringBuilder();
            /*for (int i = 0; i < logs.size(); i++) {
                for (int i2 = 0; i2 < logs.get(i).length; i2++) {
                    logsString.append(logs.get(i)[i2]).append("||||");
                }
                logsString.append("****");
            }*/
            /*for (String local_log : local_logs) {
                local_log = local_log.replace("||||", "\\|\\|\\|\\|");
                local_log = local_log.replace("****", "\\*\\*\\*\\*");
                logsString.append(local_log).append("||||");
            }*/
                //logsString.append("****");
            /*for (String local_log : local_logs) {
                logsString.append(local_log.length());
                logsString.append("]");
                logsString.append(local_log);
            }*/
                //logsString.append("}");
                //editor.putString("string", sharedPreferences.getString("string", "") + logsString.toString().toString());
                editor.putString(String.valueOf(sharedPreferences.getAll().size()), serialize(local_logs));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    editor.apply();
                } else {
                    editor.commit();
                }
            }
        }).start();
    }

    void saveLog(String[] local_logArray)   {
        logs.add(local_logArray);
        saveLogIfEnabled(local_logArray);
    }

    void showLogs(ArrayList<String[]> local_logs) {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ArrayList<String[]> arrayList = new ArrayList<>();
        int logsSize = local_logs.size();
        for (int i = 0; i < logsSize; i++) {
            arrayList.add(i, local_logs.get(logsSize - 1 - i));
        }
        //Collections.reverse(arrayList);
        TextView textView;
        String text;
        String[] strings;
        LinearLayout linearLayout1;
        for (int i = 0; i < arrayList.size(); i++) {
            strings = arrayList.get(i);
            //short statusCode = Short.parseShort(strings[2]);
            String statusString = strings[3];
            /*String statusColor = "#40";
            switch (statusCode) {
                case 0:
                    statusString = getString(R.string.uploading);
                    statusColor += "FFFF00";
                    break;
                case 1:
                    statusString = getString(R.string.uploaded);
                    statusColor += "00FF00";
                    break;
                case -1:
                    statusString = getString(R.string.uploadError);
                    statusColor += "FF0000";
                    break;
            }*/
            int textInt = Integer.parseInt(strings[1]);
            text = DateFormat.getDateTimeInstance().format(Long.parseLong(strings[0])) + "\n" + getString(textInt) + ": " + statusString;
            if ((strings.length > 4) && (!strings[4].equals("0"))) {
                text += "\n#" + strings[4];
            }
            if((strings.length > 5) && (strings[2].equals("#40ff0000")))    {
                text += "\nERROR RESPONSE:\n" + strings[5];
            }
            textView = new TextView(this);
            textView.setText(text);
            textView.setBackgroundColor(Color.parseColor(strings[2]));
            int drawable = 0;
            if(textInt == R.string.photo || textInt == R.string.video)    {
                drawable = R.drawable.ic_photovideo;
            }
            else if(textInt == R.string.voice)    {
                drawable = R.drawable.ic_microphone;
            }
            else if(textInt == R.string.description)    {
                drawable = R.drawable.ic_description;
            }
            else if(textInt == R.string.location)    {
                drawable = R.drawable.ic_location;
            }
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(drawable), null, null, null);
            linearLayout1 = new LinearLayout(this);
            linearLayout1.setOrientation(LinearLayout.VERTICAL);
            linearLayout1.setPadding(1, 1, 1, 1);
            setStroke(linearLayout1);
            linearLayout1.addView(textView);
            if((strings.length > 4) && !strings[4].equals("0") && (drawable == R.drawable.ic_photovideo))    {
                String[] finalStrings = strings;
                if(/*(*/strings.length > 6/*) && !submittedDescriptionID.contains(strings[4])*/) {
                    Button button = new Button(this);
                    button.setText(R.string.description);
                    button.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_description), null, getResources().getDrawable(R.drawable.ic_microphone), null);
                    button.setTextColor(Color.WHITE);
                    button.setBackgroundColor(Color.parseColor("#40ffffff"));
                    button.setOnClickListener(v -> description(finalStrings[4], finalStrings[6]));
                    linearLayout1.addView(button);
                }
                Button button = new Button(this);
                button.setText(R.string.share);
                button.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_share), null, null, null);
                button.setTextColor(Color.WHITE);
                button.setBackgroundColor(Color.parseColor("#40ffffff"));
                button.setOnClickListener(v -> shareText(URL + '?' + finalStrings[4]));
                linearLayout1.addView(button);
            }
            linearLayout.addView(linearLayout1);
        }
        scrollView.addView(linearLayout);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(scrollView);
        builder.show();
    }

    ArrayList<Object> uploadFileArray = new ArrayList<>();
    ArrayList<Object> uploadStringArray = new ArrayList<>();
    //ArrayList<Object> uploadVoiceArray0 = new ArrayList<>();
    //ArrayList<Object> uploadVoiceArray1 = new ArrayList<>();
    ArrayList<Object> uploadVoiceArray = new ArrayList<>();

    //boolean networkNotConnected;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //networkNotConnected = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if(!intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)/*networkNotConnected*/)    {
                if(!uploadFileArray.isEmpty())    {
                    ArrayList<Object> arrayList = uploadFileArray;
                    for (int i = 0; i < arrayList.size(); i++) {
                        Object[] object = (Object[]) arrayList.get(i);
                        uploadFile((String) object[0], (InputStream) object[1], (int) object[2], (int) object[3], (int) object[4], null, null, null, (Short) object[5]);
                        uploadFileArray.remove(i--);
                    }
                }
                if(!uploadStringArray.isEmpty())    {
                    for (int i = 0; i < uploadStringArray.size(); i++) {
                        Object[] object = (Object[]) uploadStringArray.get(i);
                        postString((String) object[0], (String[]) object[1], (String[]) object[2], (int) object[3], (int) object[4], (int) object[5]);
                        uploadStringArray.remove(i--);
                    }
                }
                /*if(!uploadVoiceArray0.isEmpty())    {
                    for (int i = 0; i < uploadVoiceArray0.size(); i++) {
                        Object[] object = (Object[]) uploadVoiceArray0.get(i);
                        uploadVoice0((Uri) object[0], (String) object[1], (String) object[2]);
                        uploadVoiceArray0.remove(i--);
                    }
                }
                if(!uploadVoiceArray1.isEmpty())    {
                    for (int i = 0; i < uploadVoiceArray1.size(); i++) {
                        Object[] object = (Object[]) uploadVoiceArray1.get(i);
                        uploadVoice1((Uri) object[0], (String) object[1], (String) object[2]);
                        uploadVoiceArray1.remove(i--);
                    }
                }*/
                if(!uploadVoiceArray.isEmpty())    {
                    for (int i = 0; i < uploadVoiceArray.size(); i++) {
                        Object[] object = (Object[]) uploadVoiceArray.get(i);
                        uploadVoice((Uri) object[0], (String) object[1], (String) object[2]);
                        uploadVoiceArray.remove(i--);
                    }
                }
            }
        }
    };

    /*File createFile(String fileSuffix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "FILE_" + timeStamp + "_";
        File storageDir;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }
        else    {
            String packageName = getApplicationContext().getPackageName();
            File externalPath = Environment.getExternalStorageDirectory();
            storageDir = new File(externalPath.getAbsolutePath() +
                    "/Android/data/" + packageName + "/files");
        }
        return File.createTempFile(
                fileName,
                fileSuffix,
                storageDir
        );
    }

    String cameraIntent(String cameraIntentString, String fileSuffix) {
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
                //cameraActivity.launch(camera_intent);
                startActivityForResult(camera_intent, 0);
            }
        }
        return currentFilePath;
    }*/

    void reopenscreenifenabled()    {
        switch(activityMode)    {
            case 0:
                if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean(getString(R.string.takephoto) + "0", false))
                    takePhoto();
                break;
            case 1:
                if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean(getString(R.string.recordvideo) + "0", false))
                    recordVideo();
                break;
            case 2:
                if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean(getString(R.string.choosephoto) + "0", false))
                    choosePhoto();
                break;
            case 3:
                if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean(getString(R.string.choosevideo) + "0", false))
                    chooseVideo();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            int text;
            if(activityMode == 0 || activityMode == 2)    {
                text = R.string.photo;
            }
            else    {
                text = R.string.video;
            }
            if (requestCode == 0) {
                uploadPhotoVideo(currentPhotoUri, text, activityMode);
                reopenscreenifenabled();
            }
            else if(requestCode == 1)   {
                uploadPhotoVideo(data.getData(), text, activityMode);
                reopenscreenifenabled();
            }
            else if(requestCode == 2)   {
                //uploadVoice0(data.getData(), GLOBAL_ID, GLOBAL_KEY);
                uploadVoice(data.getData(), GLOBAL_ID, GLOBAL_KEY);
                disableVoiceButton();
            }
        }
    }

    //String currentPhotoPath;
    Uri currentPhotoUri;

    Short activityMode;

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= 28) {
            PackageManager packageManager = getPackageManager();
            if (packageManager.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
        }
        //locationUploadEnabled = true;
        //currentPhotoPath = cameraIntent(MediaStore.ACTION_IMAGE_CAPTURE, ".jpg");
        ContentValues contentValues = new ContentValues();
        //contentValues.put(MediaStore.Images.Media.TITLE, "123");
        //contentValues.put(MediaStore.Images.Media.DESCRIPTION, "456");
        currentPhotoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        activityMode = 0;
        startActivityForResult(intent, 0);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    void recordVideo() {
        //locationUploadEnabled = true;
        activityMode = 1;
        startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), 1);
    }

    void choosePhoto()  {
        activityMode = 2;
        chooseFile("image/*");
    }

    void chooseVideo()  {
        activityMode = 3;
        chooseFile("video/*");
    }

    void chooseFile(String fileType)   {
        //locationUploadEnabled = false;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(fileType);
        startActivityForResult(intent, 1);
    }

    void shareText(String text)    {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    String DEFAULT_URL = "0"/*"https://pedestrian-sos.000webhostapp.com/""http://192.168.0.5/"*/;
    String URL;
    String GLOBAL_ID;
    String GLOBAL_KEY;

    /*boolean isNetworkConnected()    {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }*/

    void resetUploadStatuses()  {
        int color = Color.parseColor("#40FFFFFF");
        findViewById(R.id.fileUpload).setBackgroundColor(color);
        findViewById(R.id.soundUpload).setBackgroundColor(color);
        findViewById(R.id.descriptionUpload).setBackgroundColor(color);
        findViewById(R.id.locationUpload).setBackgroundColor(color);
    }

    void postString(String url, String[] POST_PARAMETERS, String[] POST_VALUES/*, int notificationTitle, int notificationIcon*/, int textView, int text, int drawable)   {
        setUploadStatusTextAndIcon(textView, text, drawable);
        View uploadText = findViewById(textView);
        final String[] colorString = {"#40ffff00"};
        uploadText.setBackgroundColor(Color.parseColor(colorString[0]));
        //Toast.makeText(getApplicationContext(), getString(text) + ": " + getString(R.string.uploading), Toast.LENGTH_LONG).show();
        String string = POST_VALUES[0];
        if(textView == R.id.descriptionUpload)
            string += "\n" + POST_VALUES[2];
        //logs.add(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(text), colorString[0], getString(R.string.uploading), string});
        //saveLogsIfEnabled();
        saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(text), colorString[0], getString(R.string.uploading), string});
        new Thread(() -> {
            String serverResponse = postString0(/*url, */POST_PARAMETERS, POST_VALUES);
            runOnUiThread(() -> {
                int contentText;
                //String status;
                if(serverResponse.equals("1"))    {
                    colorString[0] = "#4000ff00";
                    contentText = R.string.uploaded;
                    //status = "1";
                }
                else    {
                    colorString[0] = "#40ff0000";
                    contentText = R.string.uploadError;
                    //status = "-1";
                    //if(networkNotConnected)    {
                        Object[] array = {url, POST_PARAMETERS, POST_VALUES, textView, text, drawable};
                        uploadStringArray.add(array);
                    //}
                }
                //Toast.makeText(getApplicationContext(), getString(text) + ": " + getString(contentText), Toast.LENGTH_LONG).show();
                uploadText.setBackgroundColor(Color.parseColor(colorString[0]));
                //logs.add(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(text), colorString[0], getString(contentText), POST_VALUES[0], serverResponse});
                //saveLogsIfEnabled();
                saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(text), colorString[0], getString(contentText), POST_VALUES[0], serverResponse});
            });
        }).start();
    }

    String postString0(/*String postUrl, */String[] postParameters, String[] postValues) {
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
            URL url = new URL(URL);
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

    String getServerResponse(URLConnection local_httpURLConnection) throws IOException {
        InputStream inputStream = local_httpURLConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null)  {
            result.append(line);
        }
        return result.toString();
    }

    Button voiceButton;

    /*void setVoiceButtonEnabled(boolean enabled)    {
        voiceButton.setEnabled(enabled);
        if(enabled)
            voiceButton.setBackgroundColor(Color.parseColor("#ec0400"));
        else
            voiceButton.setBackgroundColor(Color.parseColor("#80ec0400"));
    }*/

    void disableVoiceButton()   {
        voiceButton.setEnabled(false);
        voiceButton.setBackgroundColor(Color.parseColor("#80ec0400"));
    }

    Intent recordSoundIntent;
    boolean recordSoundAvailable;

    void recordSound()  {
        startActivityForResult(recordSoundIntent, 2);
    }
    /*
    void uploadVoice0(Uri uri, String ID, String KEY)  {
        //setVoiceButtonEnabled(false);
        new Thread(() -> {
            String serverResponse = postString0(URL + "sound_id_and_key.php", new String[]{"n", "key"}, new String[]{ID, KEY});
            if(serverResponse.contains("*"))    {
                String[] responseArray = serverResponse.split("\\*");
                uploadVoice1(uri, responseArray[0], responseArray[1]);
            }
            else    {
                //setVoiceButtonEnabled(true);
                Object[] array = {uri, ID, KEY};
                uploadVoiceArray0.add(array);
            }
        }).start();
    }
    */
    //void uploadVoice1(Uri uri, String id, String key) {
        //runOnUiThread(() -> uploadFileFromURI(uri, URL + "sound_upload.php?id=" + id + "&key=" + key/*, "file"*/, R.id.soundUpload, R.string.voice, R.drawable.ic_microphone, uri, id, key, (short) -1));
    //}

    void uploadVoice(Uri uri, String ID, String KEY)  {
        uploadFileFromURI(uri, "voice", R.id.soundUpload, R.string.voice, R.drawable.ic_microphone, uri, ID, KEY, (short) -1);
    }

    boolean descriptionDialogDisplayed;
    ArrayList<String[]> descriptionDialogStrings = new ArrayList<>();

    void showNextDescriptionDialogIfAvailable()    {
        if(descriptionDialogStrings.isEmpty()) {
            descriptionDialogDisplayed = false;
            return;
        }
        int index = descriptionDialogStrings.size() - 1;
        String[] strings = descriptionDialogStrings.get(index);
        description(strings[0], strings[1]);
        descriptionDialogStrings.remove(index);
    }

    ArrayList<String> submittedDescriptionID = new ArrayList<>();

    void description(String ID, String KEY)  {
        final EditText editText = new EditText(this);
        AlertDialog.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
            editText.setTextColor(Color.WHITE);
            editText.setHintTextColor(Color.argb(128, 255, 255, 255));
            editText.setBackgroundColor(Color.argb(0, 0, 0, 0));
            setStroke(editText);
        }
        else    {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(R.string.description);
        builder.setMessage(getString(R.string.voice) + " & " + getString(R.string.text) + "\n#" + ID);
        //builder.setIcon(R.drawable.ic_baseline_description_24);
        //editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setSingleLine(false);
        editText.setHint(R.string.writeDescription);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if(recordSoundAvailable) {
            Button button = new Button(this);
            button.setText(getString(R.string.voiceRecord));
            button.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_microphone), null, null, null);
            button.setBackgroundColor(Color.parseColor("#ec0400"));
            button.setOnClickListener(v -> {
                GLOBAL_ID = ID;
                GLOBAL_KEY = KEY;
                voiceButton = button;
                recordSound();
            });
            linearLayout.addView(button);
        }
        linearLayout.addView(editText);
        builder.setView(linearLayout);
        builder.setPositiveButton("✓ " + getString(R.string.upload), (dialog, which) -> {
            String[] parametersArray = {"n", "key", "description"/*, "situation"*/};
            String[] valuesArray = {ID, KEY, editText.getText().toString(), ""};
            postString(URL/* + "info.php"*/, parametersArray, valuesArray/*, R.string.descriptionUploadTitle, R.drawable.ic_text_upload*/, R.id.descriptionUpload, R.string.description, R.drawable.ic_description);
            showNextDescriptionDialogIfAvailable();
            submittedDescriptionID.add(ID);
        });
        builder.setNegativeButton("✕ " + getString(R.string.cancel), (dialog, which) -> {
            dialog.cancel();
            showNextDescriptionDialogIfAvailable();
        });
        builder.show();
    }

    //boolean locationUploadEnabled;
    LocationManager locationManager;
    LocationListener locationListener;

    @SuppressLint("MissingPermission")
    boolean requestLocation(String provider/*, long minTime, float minDistance*/) {
        //if (locationManager.isProviderEnabled(provider)) {
        try {
            locationManager.requestLocationUpdates(provider, /*minTime*/60000, /*minDistance*/0, locationListener);
            return true;
        }
        catch (Exception ignored){}
        return false;
    }

    ArrayList<String> LOCATION_ID = new ArrayList<>();
    ArrayList<String> LOCATION_KEY = new ArrayList<>();

    void location() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager packageManager = getPackageManager();
            if (packageManager.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, getPackageName()) != PackageManager.PERMISSION_GRANTED && packageManager.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                return;
            }
        }
        if(locationManager == null) {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        }
        if(locationListener == null) {
            String colorString = "#400000ff";
            locationListener = new LocationListener() {
                @SuppressLint("NewApi")
                @Override
                public void onLocationChanged(Location location) {
                    locationManager.removeUpdates(locationListener);
                    locationListener = null;
                    String altitude;
                    String accuracy;
                    boolean verticalAccuracySupport = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O);
                    String[] locationCoordinates;
                    String[] uploadLocationParameters;
                    String[] uploadLocationValues;
                    for (int i = 0; i < LOCATION_ID.size(); i++) {
                        if(location.hasAltitude())    {
                            altitude = String.valueOf(location.getAltitude());
                        }
                        else {
                            altitude = "-";
                        }
                        if(location.hasAccuracy())    {
                            accuracy = String.valueOf(location.getAccuracy());
                        }
                        else {
                            accuracy = "-";
                        }
                        short valueQuantity;
                        if (verticalAccuracySupport) {
                            String verticalAccuracy;
                            if(location.hasVerticalAccuracy())    {
                                verticalAccuracy = String.valueOf(location.getVerticalAccuracyMeters());
                            }
                            else {
                                verticalAccuracy = "-";
                            }
                            locationCoordinates = new String[]{
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()),
                                    altitude,
                                    accuracy,
                                    verticalAccuracy
                            };
                            uploadLocationParameters = new String[]{
                                    "n",
                                    "key",
                                    "latitude",
                                    "longitude",
                                    "altitude",
                                    "accuracy",
                                    "altitudeAccuracy"
                            };
                            valueQuantity = 7;
                        }
                        else {
                            locationCoordinates = new String[]{
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()),
                                    altitude,
                                    accuracy
                            };
                            uploadLocationParameters = new String[]{
                                    "n",
                                    "key",
                                    "latitude",
                                    "longitude",
                                    "altitude",
                                    "accuracy"
                            };
                            valueQuantity = 6;
                        }
                        uploadLocationValues = new String[valueQuantity];
                        String[] uploadLocationValues0 = {
                                /*ID*/LOCATION_ID.get(i),
                                /*KEY*/LOCATION_KEY.get(i)
                        };
                        for (int i2 = 0; i2 < valueQuantity; i2++) {
                            if(i2 < 2)
                                uploadLocationValues[i2] = uploadLocationValues0[i2];
                            else
                                uploadLocationValues[i2] = locationCoordinates[i2 - 2];
                        }
                        //locationManager.removeUpdates(locationListener);
                        //locationListener = null;
                        //logs.add(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(R.string.location), colorString, Arrays.toString(locationCoordinates)});
                        //saveLogsIfEnabled();
                        saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(R.string.location), colorString, Arrays.toString(locationCoordinates)});
                        postString(URL/* + "location.php"*/, uploadLocationParameters, uploadLocationValues, R.id.locationUpload, R.string.location, R.drawable.ic_location);
                        LOCATION_ID.remove(i);
                        LOCATION_KEY.remove(i);
                        i--;
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {
                    location();
                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
            //Toast.makeText(getApplicationContext(), String.valueOf(locationManager.getAllProviders()), Toast.LENGTH_LONG).show();
            //logs.add(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(R.string.location), colorString, locationManager.getAllProviders().toString()});
            //saveLogsIfEnabled();
            saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(R.string.location), colorString, locationManager.getAllProviders().toString()});
            boolean GPS_locationRequested = requestLocation(LocationManager.GPS_PROVIDER/*, 60000, 0*/);
            boolean NETWORK_locationRequested = requestLocation(LocationManager.NETWORK_PROVIDER/*, 60000, 0*/);
            if(GPS_locationRequested || NETWORK_locationRequested)    {
            //notificationManager2.notify(locationProgressNotificationID, builder2.build());
                /*logs.add*/saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(R.string.location), colorString, "..."});
            }
            else    {
                /*logs.add*/saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(R.string.location), colorString, "✕"});
            }
            //saveLogsIfEnabled();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 0) {
                location();
            } else if (requestCode == 1) {
                takePhoto();
            }
        }
    }

    void setUploadStatusVisibleIfNot()  {
        View uploadStatusLayoutView = findViewById(R.id.statusLayout);
        if(uploadStatusLayoutView.getVisibility() == View.GONE)    {
            ((TextView)findViewById(R.id.statusTextView)).setText(R.string.status);
            setStroke(uploadStatusLayoutView);
            uploadStatusLayoutView.setVisibility(View.VISIBLE);
            findViewById(R.id.after_statusLayout).setVisibility(View.VISIBLE);
        }
    }

    void setUploadStatusTextAndIcon(int view, int text, int drawable)   {
        setUploadStatusVisibleIfNot();
        TextView textView = findViewById(view);
        if(textView.getVisibility() == View.GONE) {
            if(text == R.string.photo || text == R.string.video)    {
                text = R.string.file;
            }
            textView.setText(text);
            textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(drawable), null, null, null);
            textView.setVisibility(View.VISIBLE);
        }
    }

    boolean getDescriptionOrLocationSetting(String index, short local_activityMode) {
        int mode;
        switch(local_activityMode)    {
            case 0:
                mode = R.string.takephoto;
                break;
            case 1:
                mode = R.string.recordvideo;
                break;
            case 2:
                mode = R.string.choosephoto;
                break;
            case 3:
                mode = R.string.choosevideo;
                break;
            default:
                return false;
        }
        return getSharedPreferences("settings", MODE_PRIVATE).getBoolean(getString(mode) + index, false);
    }

    void uploadFile(/*String filePath, *//*String WEB_URL*/String WEB_NAME/*, String WEB_NAME*/, InputStream fileStream/*, int notificationTitle, int notificationIcon*/, int textView, int text, int drawable, Uri VOICE_URI, String VOICE_ID, String VOICE_KEY, short local_activityMode) {
        //String filePath = null;
        //String WEB_NAME = "file";
        /*if(!isNetworkConnected())    {
            Toast.makeText(getApplicationContext(), "!CONNECTION", Toast.LENGTH_LONG).show();
            return;
        }*/
        setUploadStatusTextAndIcon(textView, text, drawable);
        View fileText = findViewById(textView);
        final String[] colorString = {"#40ffff00"};
        fileText.setBackgroundColor(Color.parseColor(colorString[0]));
        //Toast.makeText(getApplicationContext(), getString(text) + ": " + getString(R.string.uploading), Toast.LENGTH_LONG).show();
        /*logs.add*/saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(text), colorString[0], getString(R.string.uploading)});
        //saveLogsIfEnabled();
        //boolean chunkedStream = false;
        int maxBufferSize = 1024 * 1024;
        //int chunkLen = 256 * 1024;
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
            /*long totalSize = 0;
            File sourceFile = null;
            if (filePath != null) {
                sourceFile = new File(filePath);
                //totalSize = sourceFile.length();
            } else {
                try {
                    totalSize = fileStream.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
            if (/*((sourceFile != null) && sourceFile.isFile()) || (*/fileStream != null/*)*/) {
                try {
                    InputStream fileInputStream;
                    /*if(filePath != null) {
                        fileInputStream = new FileInputStream(sourceFile);
                    }
                    else    {*/
                        fileInputStream = fileStream;
                    //}
                    URL url = new URL(/*WEB_URL*/URL);
                    conn = (HttpURLConnection) url.openConnection();
                    //if(chunkedStream)
                        //conn.setChunkedStreamingMode(chunkLen);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);
                    conn.setUseCaches(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty(WEB_NAME, /*filePath*/null);
                    dos = new DataOutputStream(conn.getOutputStream());
                    if(VOICE_ID != null && VOICE_KEY != null)    {
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"n\"" + lineEnd + lineEnd);
                        dos.writeBytes(VOICE_ID + lineEnd);
                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"key\"" + lineEnd + lineEnd);
                        dos.writeBytes(VOICE_KEY + lineEnd);
                    }
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    /*dos.writeBytes("Content-Disposition: form-data; name=\"" + WEB_NAME + "\";filename=\""
                            + filePath + "\"" + lineEnd);*/
                    dos.writeBytes("Content-Disposition: form-data; name=\"" + WEB_NAME + "\";filename=\"0\"" + lineEnd + lineEnd);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    //int progress;
                    //long length = 0;
                    while (bytesRead > 0) {
                        dos.write(buffer, 0, bufferSize);
                        /*if(chunkedStream) {
                            length += bufferSize;
                            progress = (int) (((float) length / (float) totalSize) * 100);
                        }*/
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
                //String status;
                String n = "0";
                String key = "0";
                if ((finalServerResponseString != null) && (((fileText.getId() == R.id.fileUpload) && (finalServerResponseString.charAt(0) == '#')) || (fileText.getId() == R.id.soundUpload) && finalServerResponseString.equals("1"))) {
                    colorString[0] = "#4000ff00";
                    contentText = R.string.uploaded;
                    //status = "1";
                    if((fileText.getId() == R.id.fileUpload)) {
                        String ID = finalServerResponseString.substring(1, finalServerResponseString.indexOf('|'));
                        String KEY = finalServerResponseString.substring(finalServerResponseString.indexOf('|') + 1);
                        //if (locationUploadEnabled) {
                        if(getDescriptionOrLocationSetting("2", local_activityMode)) {
                            LOCATION_ID.add(ID);
                            LOCATION_KEY.add(KEY);
                            location();
                        }
                        //}
                        //recordSound();
                        if(getDescriptionOrLocationSetting("1", local_activityMode)) {
                            if (descriptionDialogDisplayed) {
                                descriptionDialogStrings.add(new String[]{ID, KEY});
                            } else {
                                description(ID, KEY);
                                descriptionDialogDisplayed = true;
                            }
                        }
                        Button shareButton = findViewById(R.id.share_last_uploaded);
                        shareButton.setOnClickListener(v -> shareText(URL + '?' + ID));
                        shareButton.setEnabled(true);
                        String string = getString(R.string.share) + ' ' + getString(R.string.lastUploaded);
                        shareButton.setText(string);
                        shareButton.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.ic_share), null, null);
                        shareButton.setVisibility(View.VISIBLE);
                        findViewById(R.id.layoutAfterShareButton).setVisibility(View.VISIBLE);
                        n = ID;
                        key = KEY;
                    }
                    /*else if(fileText.getId() == R.id.soundUpload)   {
                        ((ViewGroup) voiceButton.getParent()).removeView(voiceButton);
                    }*/
                }
                else {
                    colorString[0] = "#40ff0000";
                    contentText = R.string.uploadError;
                    //status = "-1";
                    //if(networkNotConnected) {
                        if(fileText.getId() == R.id.soundUpload)   {
                            //setVoiceButtonEnabled(true);
                            //uploadVoiceArray1.add(new Object[]{VOICE_URI, VOICE_ID, VOICE_KEY});
                            uploadVoiceArray.add(new Object[]{VOICE_URI, VOICE_ID, VOICE_KEY});
                        }
                        uploadFileArray.add(new Object[]{/*WEB_URL*/WEB_NAME, fileStream, textView, text, drawable, activityMode});
                    //}
                }
                //Toast.makeText(getApplicationContext(), getString(text) + ": " + getString(contentText), Toast.LENGTH_LONG).show();
                fileText.setBackgroundColor(Color.parseColor(colorString[0]));
                /*logs.add*/saveLog(new String[]{String.valueOf(System.currentTimeMillis()), String.valueOf(text), colorString[0], getString(contentText), n, finalServerResponseString, key});
                //saveLogsIfEnabled();
            });
        }).start();
    }

    void uploadFileFromURI(Uri local_uri, /*String web_url*/String web_name/*, String web_name, int notificationTitle, int notificationIcon*/, int textView, int text, int drawable, Uri VOICE_URI, String VOICE_ID, String VOICE_KEY, short local_activityMode)    {
        try {
            uploadFile(/*null, *//*web_url*/web_name/*, web_name"file"*/, getContentResolver().openInputStream(local_uri)/*, notificationTitle, notificationIcon*/, textView, text, drawable, VOICE_URI, VOICE_ID, VOICE_KEY, local_activityMode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    void uploadPhotoVideo(Uri uri, int text, short local_activityMode) {
        resetUploadStatuses();
        uploadFileFromURI(uri, /*URL + "media.php"*/"photovideo"/*, "file"*/, R.id.fileUpload, text, R.drawable.ic_photovideo, null, null, null, local_activityMode);
    }
}