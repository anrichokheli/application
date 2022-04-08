package pedestriansos.application;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicReference;

public class firstScreen extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_screen);
        animateImageView(findViewById(R.id.icon));
        findViewById(R.id.exit).setOnClickListener(v -> exit());
        findViewById(R.id.skip).setOnClickListener(v -> complete());
        findViewById(R.id.next).setOnClickListener(v -> complete());
        findViewById(R.id.back).setOnClickListener(v -> onBackPressed());
        TextView tv = findViewById(R.id.pedestrian);
        Shader textShader = new LinearGradient(0, 0, tv.getPaint().measureText(tv.getText().toString()), tv.getTextSize(),
                new int[]{Color.rgb(0, 64, 255), Color.rgb(0, 128, 255)},
                new float[]{0, 1}, Shader.TileMode.CLAMP);
        tv.getPaint().setShader(textShader);
        tv = findViewById(R.id.sos);
        textShader = new LinearGradient(0, 0, tv.getPaint().measureText(tv.getText().toString()), tv.getTextSize(),
                new int[]{Color.rgb(255, 0, 0), Color.rgb(255, 32, 0)},
                new float[]{0, 1}, Shader.TileMode.CLAMP);
        tv.getPaint().setShader(textShader);
        LinearLayout scrollView = findViewById(R.id.firstScreenScrollView);
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setText(R.string.welcome);
        textView.setTextSize(32);
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        scrollView.addView(textView);
    }

    void animateImageView(final ImageView v) {
        AtomicReference<Short> redValue = new AtomicReference<>((short) 255);
        AtomicReference<Short> greenValue = new AtomicReference<>((short) 0);
        AtomicReference<Short> blueValue = new AtomicReference<>((short) 0);
        final ValueAnimator colorAnim = ObjectAnimator.ofInt(0);
        colorAnim.addUpdateListener(animation -> {
            if(blueValue.get() == 0 && redValue.get() == 255 && greenValue.get() < 255)   {
                greenValue.getAndSet((short) (greenValue.get() + 1));
            }
            else if(redValue.get() > 0 && greenValue.get() == 255)   {
                redValue.getAndSet((short) (redValue.get() - 1));
            }
            else if(greenValue.get() == 255 && blueValue.get() < 255)   {
                blueValue.getAndSet((short) (blueValue.get() + 1));
            }
            else if(greenValue.get() > 0 && blueValue.get() == 255)   {
                greenValue.getAndSet((short) (greenValue.get() - 1));
            }
            else if(redValue.get() < 255 && blueValue.get() == 255)   {
                redValue.getAndSet((short) (redValue.get() + 1));
            }
            else if(redValue.get() == 255 && blueValue.get() > 0)   {
                blueValue.getAndSet((short) (blueValue.get() - 1));
            }
            v.setColorFilter(Color.rgb(redValue.get(), greenValue.get(), blueValue.get()), PorterDuff.Mode.SRC_ATOP);

        });
        colorAnim.setDuration(1);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.setRepeatCount(-1);
        colorAnim.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    void exit() {
        setResult(RESULT_CANCELED);
        finish();
    }

    void complete() {
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putBoolean("first_screen", false);
        sharedPreferencesEditor.apply();
        setResult(RESULT_OK);
        finish();
    }
}
