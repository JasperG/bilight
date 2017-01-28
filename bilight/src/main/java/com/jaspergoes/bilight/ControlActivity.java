package com.jaspergoes.bilight;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

import com.jaspergoes.bilight.helpers.ColorPickerView;
import com.jaspergoes.bilight.helpers.OnColorChangeListener;
import com.jaspergoes.bilight.helpers.OnProgressChangeListener;
import com.jaspergoes.bilight.helpers.ScrollViewPlus;
import com.jaspergoes.bilight.milight.Controller;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.ArrayList;
import java.util.List;

public class ControlActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /* Expect this value to be null after some time - Thread will eventually be terminated, of course. Should be done nicer than this, but whatever. */
        if (Controller.milightAddress == null || (Controller.keepAliveTime != 0 && Controller.keepAliveTime < (System.currentTimeMillis() - 6e4))) {
            finish();
            return;
        }

        setContentView(R.layout.activity_control);

        Toolbar supportActionBar = (Toolbar) findViewById(R.id.toolbar);
        supportActionBar.setSubtitle(Controller.milightAddress.getHostAddress() + (Controller.milightPort != Controller.defaultMilightPort ? ":" + Integer.toString(Controller.milightPort) : "") + (Controller.networkInterfaceName.length() > 0 ? " " + getString(R.string.via) + " " + Controller.networkInterfaceName : ""));
        setSupportActionBar(supportActionBar);

        setCheckboxes();

        /* Switch on */
        ((Button) findViewById(R.id.switchOn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setOnOff(true);

                    }

                }).start();

            }
        });

        /* Switch off */
        ((Button) findViewById(R.id.switchOff)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setOnOff(false);

                    }

                }).start();

            }
        });

        /* White mode */
        ((Button) findViewById(R.id.setWhite)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setWhite();

                    }

                }).start();

            }
        });

        /* Audio analyzer | just trying out some stuff */
        //MicrophoneAnalyzer mic = new MicrophoneAnalyzer();
        //mic.startRecording();

        /* Color */
        TypedArray array = getTheme().obtainStyledAttributes(new int[]{android.R.attr.colorBackground});
        int backgroundColor = array.getColor(0, 0xFFFFFF);
        array.recycle();

        ColorPickerView colorPicker = (ColorPickerView) findViewById(R.id.colorpicker);
        colorPicker.setScrollingParent((ScrollViewPlus) findViewById(R.id.scrollcontent));
        colorPicker.setParentBackground(backgroundColor);
        colorPicker.setOnColorChangeListener(new OnColorChangeListener() {

            @Override
            public void colorChanged(final int color) {

                if (Controller.newColor != (Controller.newColor = (color + 128) % 256)) {

                    synchronized (Controller.INSTANCE) {
                        Controller.INSTANCE.notify();
                    }

                }

            }

            @Override
            public void refresh() {

                Controller.INSTANCE.refresh();

            }

        });

        DiscreteSeekBar seekbar;

        /* Brightness */
        seekbar = (DiscreteSeekBar) findViewById(R.id.seekbar_brightness);
        seekbar.setProgress(Controller.newBrightness == -1 ? 100 : Controller.newBrightness);
        seekbar.setOnProgressChangeListener(new OnProgressChangeListener() {

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, final int value, boolean fromUser) {

                if (fromUser && Controller.newBrightness != (Controller.newBrightness = value)) {

                    synchronized (Controller.INSTANCE) {
                        Controller.INSTANCE.notify();
                    }

                }

            }

        });

        /* Saturation */
        seekbar = (DiscreteSeekBar) findViewById(R.id.seekbar_saturation);
        seekbar.setProgress(Controller.newSaturation == -1 ? 100 : 100 - Controller.newSaturation);
        seekbar.setOnProgressChangeListener(new OnProgressChangeListener() {

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, final int value, boolean fromUser) {

                if (fromUser && Controller.newSaturation != (Controller.newSaturation = 100 - value)) {

                    synchronized (Controller.INSTANCE) {
                        Controller.INSTANCE.notify();
                    }

                }

            }

        });

        /* Temperature */
        seekbar = (DiscreteSeekBar) findViewById(R.id.seekbar_colortemp);
        seekbar.setProgress(Controller.newTemperature == -1 ? 35 : 100 - Controller.newTemperature);
        seekbar.setOnProgressChangeListener(new OnProgressChangeListener() {

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, final int value, boolean fromUser) {

                if (fromUser && Controller.newTemperature != (Controller.newTemperature = 100 - value)) {

                    synchronized (Controller.INSTANCE) {
                        Controller.INSTANCE.notify();
                    }

                }

            }

        });

    }

    private void setCheckboxes() {

        final AppCompatCheckBox wifi_0 = (AppCompatCheckBox) findViewById(R.id.control_wifi_bridge);
        final AppCompatCheckBox rgb_w0 = (AppCompatCheckBox) findViewById(R.id.control_rgbw);
        final AppCompatCheckBox rgb_ww = (AppCompatCheckBox) findViewById(R.id.control_rgbww);
        final AppCompatCheckBox zone_1 = (AppCompatCheckBox) findViewById(R.id.control_zone_1);
        final AppCompatCheckBox zone_2 = (AppCompatCheckBox) findViewById(R.id.control_zone_2);
        final AppCompatCheckBox zone_3 = (AppCompatCheckBox) findViewById(R.id.control_zone_3);
        final AppCompatCheckBox zone_4 = (AppCompatCheckBox) findViewById(R.id.control_zone_4);

         /* Set checkboxes, according to values currently stored in Controller */
        for (int i : Controller.controlDevices) {

            switch (i) {

                case 0:
                    wifi_0.setChecked(true);
                    break;

                case 7:
                    rgb_w0.setChecked(true);
                    break;

                case 8:
                    rgb_ww.setChecked(true);
                    break;

            }

        }

        for (int x : Controller.controlZones) {

            if (x == -1) {

                break;

            } else if (x == 0) {

                zone_1.setChecked(true);
                zone_2.setChecked(true);
                zone_3.setChecked(true);
                zone_4.setChecked(true);

            } else {

                switch (x) {
                    case 1:
                        zone_1.setChecked(true);
                        break;
                    case 2:
                        zone_2.setChecked(true);
                        break;
                    case 3:
                        zone_3.setChecked(true);
                        break;
                    case 4:
                        zone_4.setChecked(true);
                        break;

                }

            }

        }

        AppCompatCheckBox.OnCheckedChangeListener changeListener = new AppCompatCheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                boolean rgbw_on = rgb_w0.isChecked();
                boolean rgbww_on = rgb_ww.isChecked();
                boolean any = zone_1.isChecked() || zone_2.isChecked() || zone_3.isChecked() || zone_4.isChecked();

                List<Integer> deviceList = new ArrayList<Integer>();

                if (rgbww_on && any)
                    deviceList.add(8);

                if (rgbw_on && any)
                    deviceList.add(7);

                if (wifi_0.isChecked())
                    deviceList.add(0);

                int ret[] = new int[deviceList.size()];
                for (int i = 0; i < ret.length; i++) ret[i] = deviceList.get(i);

                Controller.controlDevices = ret;

                if (any && (rgbw_on || rgbww_on)) {

                    if (zone_1.isChecked() && zone_2.isChecked() && zone_3.isChecked() && zone_4.isChecked()) {

                        Controller.controlZones = new int[]{0};

                    } else {

                        List<Integer> zoneList = new ArrayList<Integer>();

                        if (zone_1.isChecked())
                            zoneList.add(1);

                        if (zone_2.isChecked())
                            zoneList.add(2);

                        if (zone_3.isChecked())
                            zoneList.add(3);

                        if (zone_4.isChecked())
                            zoneList.add(4);

                        ret = new int[zoneList.size()];
                        for (int i = 0; i < ret.length; i++) ret[i] = zoneList.get(i);

                        Controller.controlZones = ret;

                    }

                } else {

                    /* None selected, at all */
                    Controller.controlZones = new int[]{-1};

                }

            }

        };

        wifi_0.setOnCheckedChangeListener(changeListener);
        rgb_w0.setOnCheckedChangeListener(changeListener);
        rgb_ww.setOnCheckedChangeListener(changeListener);
        zone_1.setOnCheckedChangeListener(changeListener);
        zone_2.setOnCheckedChangeListener(changeListener);
        zone_3.setOnCheckedChangeListener(changeListener);
        zone_4.setOnCheckedChangeListener(changeListener);

    }

}
