package edu.harvard.cs50.tuner2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import io.github.dreierf.materialintroscreen.MaterialIntroActivity;
import io.github.dreierf.materialintroscreen.SlideFragmentBuilder;
import io.github.dreierf.materialintroscreen.animations.IViewTranslation;

public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        enableLastSlideAlphaExitTransition(true);


        getBackButtonTranslationWrapper()
                .setEnterTranslation(new IViewTranslation() {
                    @Override
                    public void translate(View view, @FloatRange(from = 0, to = 1.0) float percentage) {
                        view.setAlpha(percentage);
                    }
                });

        if (ContextCompat.checkSelfPermission(MainActivity.instance, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            addSlide(new SlideFragmentBuilder()
                    .backgroundColor(R.color.permission)
                    .buttonsColor(R.color.permission_button)
                    .image(R.drawable.round_mic_24)
                    .title("Welcome ! Lets do a quick setup")
                    .description("Grant permission to record audio and press next")
                    .neededPermissions(new String[]{Manifest.permission.RECORD_AUDIO})
                    .build());
        }

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.first_slide_background)
                .buttonsColor(R.color.first_slide_buttons)
                .image(R.drawable.pitch_compare)
                .title("How to tune your instrument ?")
                .description("On left is your current frequency , to tune your instrument , try to match it with standard frequency (right)")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.second_slide_background)
                .buttonsColor(R.color.second_slide_buttons)
                .image(R.drawable.note)
                .title("Strings and notes")
                .description("The note in round button indicates the \"string\" you played , the note above it is your current pitch")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.third_slide_background)
                .buttonsColor(R.color.third_slide_buttons)
                .image(R.drawable.string_button)
                .image(R.drawable.strings)
                .title("Manually choose the string")
                .description("you can manually select a string for tuning, otherwise click \"auto\" switch to get back on auto mode")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.permission)
                .buttonsColor(R.color.permission_button)
                .image(R.drawable.round_check_circle_24)
                .title("That's it")
                .description("you are good to go !")
                .build());
    }

    @Override
    public void onFinish() {
        super.onFinish();
        Toast.makeText(this, "i am akash , and This is cs50 ;)", Toast.LENGTH_SHORT).show();
        MainActivity.instance.start();
    }
}
