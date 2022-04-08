package pedestriansos.application;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class choseFilePreview extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chose_file_preview);
        findViewById(R.id.closeUploadScreen).setOnClickListener(v -> finish());
        if(getSharedPreferences("settings", MODE_PRIVATE).getBoolean("dark_mode", true))
            findViewById(R.id.background).setBackgroundColor(Color.parseColor("#000000"));
        Uri uri = Uri.parse(getIntent().getStringExtra("uri"));
        String fileType = getContentResolver().getType(uri);
        fileType = fileType.substring(0, fileType.indexOf('/'));
        if(fileType.equals("image"))    {
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageURI(uri);
            imageView.setVisibility(View.VISIBLE);
        }
        else if(fileType.equals("video"))   {
            VideoView videoView = findViewById(R.id.videoView);
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);
            videoView.setVideoURI(uri);
            videoView.setVisibility(View.VISIBLE);
        }
        String finalFileType = fileType;
        findViewById(R.id.fileUploadButton).setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("fileType", finalFileType));
            finish();
        });
    }
}
