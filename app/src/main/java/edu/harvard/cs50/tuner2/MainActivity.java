package edu.harvard.cs50.tuner2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static java.lang.Math.signum;

public class MainActivity extends AppCompatActivity {
    public static MainActivity instance;

    Button button;
    Button help;
    Switch auto;
    TextView target_pitch;
    TextView pitchText ;
    TextView noteText ;
    TextView note_sub ;

    public float targetPitchHz = 0;
    boolean isAuto = true;

    // for processPitch()
    public int steps = 5;
    public int count = 0;
    List<Integer> recentHz = new ArrayList<>();

    // list of all the notes , string names and their frequencies
    List<String> notes = Arrays.asList("A","A#","B","C","C#","D","D#","E","F","F#","G","G#");
    List<String> strings = Arrays.asList("E2", "A", "D", "G", "B", "E4");
    float[] numbers = new float[]{(float) 82.4, 110, (float) 146.83, (float) 196, (float) 246.94, (float) 329.63};

    AudioDispatcher dispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        setup();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            start();
        }else{
            help.performClick();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("REC", "STOPPED");
        dispatcher.stop();
    }

    private void setup(){
        // hook all the views , and eventlisteners
        target_pitch = findViewById(R.id.target_pitch);
        button = findViewById(R.id.menu_button);
        help = findViewById(R.id.help);
        auto = findViewById(R.id.switch1);
        pitchText = findViewById(R.id.pitch);
        noteText = findViewById(R.id.note);
        note_sub = findViewById(R.id.note_sub);

        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, IntroActivity.class);
                startActivity(intent);
            }
        });

        auto.setChecked(true);

        auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAuto = isChecked;
                if (!isChecked){
                    button.setText("E");
                    targetPitchHz = (float) 82.4;
                    target_pitch.setText("82.4");
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(MainActivity.this, button);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        isAuto = false;
                        auto.setChecked(false);

                        Toast.makeText(MainActivity.this,"Selected : " + item.getTitle() + " string", Toast.LENGTH_SHORT).show();

                        getStringName(numbers[strings.indexOf(item.getTitle())]);
                        return true;
                    }
                });
                popup.show();//showing popup menu
            }
        });
    }

    public void start(){
        // getting pitch from live audio , and sending it to processPitch()

        int SAMPLE_RATE = 44100;
        int BUFFER_SIZE = 1024 * 4;
        int OVERLAP = 768 * 4;

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, BUFFER_SIZE, OVERLAP);

        PitchDetectionHandler pdh = new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult res, AudioEvent e){
                final float pitchInHz = res.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        processPitch(pitchInHz);
                    }
                });
            }
        };
        AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, SAMPLE_RATE, BUFFER_SIZE, pdh);
        dispatcher.addAudioProcessor(pitchProcessor);

        Thread audioThread = new Thread(dispatcher, "Audio Thread");
        audioThread.start();
    }

    public void processPitch(float pitchInHz) {
        // store pitch 5 times , and then return the most repeated pitch out of it
        // just to reduce randomness , kinda makes it lil bit stable to use ;)
        // steps = 5

        if (count == steps){
            Log.d("recent", recentHz.toString());
            count = 0;
            display(mostCommon(recentHz));
            recentHz.clear();
        }else{
            recentHz.add(round(pitchInHz));
            count++;
        }
    }

    public void display(int pitchInHz){
        // display to user

        String closest_note;
        String sub;
        int i = 0;

        // calculate i , no. of half steps from A4 note (440hz) (https://en.wikipedia.org/wiki/Equal_temperament)
        i = (int) Math.round(12*(Math.log((float) pitchInHz/440) / Math.log(2)));

        // using i , find the index of note played in the "notes" array
        int index = i%12;
        if (index < 0){
            index+=12;
        }

        // if pitch is low causing i = 0 .
        // isAuto refers to state of string selector switch.
        if (i==0){
            closest_note = "-";
            sub = "";

            if (isAuto) {
                button.setText("play or choose");
                button.setTextSize(13);
                target_pitch.setText("auto");
            }

        }else{
            closest_note = notes.get(index);
            sub = Integer.toString((int) (4 + signum(i) * ((9+abs(i))/12)));

            if (isAuto){
                getStringName(pitchInHz);
            }
        }

        noteText.setText(closest_note);
        note_sub.setText(sub);
        pitchText.setText(Integer.toString(pitchInHz));

        if (1.5 < abs(pitchInHz - targetPitchHz)){
            pitchText.setTextColor(getResources().getColor(R.color.colorAccent));
            target_pitch.setTextColor(getResources().getColor(R.color.colorAccent));
        }else{
            pitchText.setTextColor(getResources().getColor(R.color.permission_button));
            target_pitch.setTextColor(getResources().getColor(R.color.permission_button));
        }

    }

    public void getStringName(float pitchInHz){
        // guess the string played using pitch
        // (find the closest frequency)
        button.setTextSize(28);
        float distance = Math.abs(numbers[0] - pitchInHz);
        int idx = 0;
        for(int c = 1; c < numbers.length; c++){
            float cdistance = Math.abs(numbers[c] - pitchInHz);
            if(cdistance < distance){
                idx = c;
                distance = cdistance;
            }
        }
        pitchInHz = numbers[idx];
        target_pitch.setText(Float.toString(pitchInHz));
        button.setText(strings.get(idx));
    }

    // find most repeated frequency out of recentHz .
    public static <T> T mostCommon(List<T> list) {
        Map<T, Integer> map = new HashMap<>();

        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        Map.Entry<T, Integer> max = null;

        for (Map.Entry<T, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }

        return max.getKey();
    }
}
