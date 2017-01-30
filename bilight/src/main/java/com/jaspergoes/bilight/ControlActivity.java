package com.jaspergoes.bilight;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jaspergoes.bilight.helpers.ColorPickerView;
import com.jaspergoes.bilight.helpers.IBoxSettings;
import com.jaspergoes.bilight.helpers.OnColorChangeListener;
import com.jaspergoes.bilight.helpers.OnProgressChangeListener;
import com.jaspergoes.bilight.helpers.ScrollViewPlus;
import com.jaspergoes.bilight.milight.Controller;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.ArrayList;
import java.util.List;

public class ControlActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private LinearLayout mDrawerPanel;

    private IBoxSettings iBoxSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /* Expect this value to be null after some time - Thread will eventually be terminated, of course. Should be done nicer than this, but whatever. */
        if (Controller.milightAddress == null || (Controller.keepAliveTime != 0 && Controller.keepAliveTime < (System.currentTimeMillis() - 6e4))) {
            finish();
            return;
        }

        iBoxSettings = new IBoxSettings(getApplicationContext(), Controller.milightMac);

        setContentView(R.layout.activity_control);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setSubtitle(Controller.milightAddress.getHostAddress() + (Controller.milightPort != Controller.defaultMilightPort ? ":" + Integer.toString(Controller.milightPort) : "") + (Controller.networkInterfaceName.length() > 0 ? " " + getString(R.string.via) + " " + Controller.networkInterfaceName : ""));
        setSupportActionBar(toolbar);

        mDrawerPanel = (LinearLayout) findViewById(R.id.left_drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {

                super.onDrawerOpened(drawerView);

                invalidateOptionsMenu();

            }

            public void onDrawerClosed(View view) {

                super.onDrawerClosed(view);

                invalidateOptionsMenu();

            }

        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setHomeButtonEnabled(true);

        setCheckboxes();

        ((TextView) findViewById(R.id.connect_mac)).setText(Controller.milightMac);

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

                ((ColorPickerView) findViewById(R.id.colorpicker)).invalidate();

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

                    ((ColorPickerView) findViewById(R.id.colorpicker)).invalidate();

                }

            }

        });

    }

    @Override
    protected void onResume() {

        super.onResume();

        /* Expect this value to be null after some time - Thread will eventually be terminated, of course. Should be done nicer than this, but whatever. */
        if (Controller.milightAddress == null || (Controller.keepAliveTime != 0 && Controller.keepAliveTime < (System.currentTimeMillis() - 6e4))) {
            finish();
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerPanel);
        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);

        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle your other action bar items...
        return super.onOptionsItemSelected(item);
    }

    private void setCheckboxes() {

        final AppCompatCheckBox wifi_0 = (AppCompatCheckBox) findViewById(R.id.control_wifi_bridge);
        final AppCompatCheckBox rgb_w0 = (AppCompatCheckBox) findViewById(R.id.control_rgbw);
        final AppCompatCheckBox rgb_ww = (AppCompatCheckBox) findViewById(R.id.control_rgbww);
        final AppCompatCheckBox zone_1 = (AppCompatCheckBox) findViewById(R.id.control_zone_1);
        final AppCompatCheckBox zone_2 = (AppCompatCheckBox) findViewById(R.id.control_zone_2);
        final AppCompatCheckBox zone_3 = (AppCompatCheckBox) findViewById(R.id.control_zone_3);
        final AppCompatCheckBox zone_4 = (AppCompatCheckBox) findViewById(R.id.control_zone_4);

        final AppCompatCheckBox has_iboxl = (AppCompatCheckBox) findViewById(R.id.has_ibox_lamp);
        final AppCompatCheckBox has_rgbw0 = (AppCompatCheckBox) findViewById(R.id.has_rgbw);
        final AppCompatCheckBox has_rgbww = (AppCompatCheckBox) findViewById(R.id.has_rgbww);
        final AppCompatCheckBox has_dualw = (AppCompatCheckBox) findViewById(R.id.has_dualw);

        final DiscreteSeekBar seekbarSatr = (DiscreteSeekBar) findViewById(R.id.seekbar_saturation);
        final DiscreteSeekBar seekbarTemp = (DiscreteSeekBar) findViewById(R.id.seekbar_colortemp);

        /* Set checkboxes, according to values saved in settings */
        has_iboxl.setChecked(iBoxSettings.hasIBoxLamp);
        has_rgbw0.setChecked(iBoxSettings.hasRGBW);
        has_rgbww.setChecked(iBoxSettings.hasRGBWW);
        has_dualw.setChecked(iBoxSettings.hasDualW);

        setElements();

        /* Set checkboxes, according to values currently stored in Controller */
        boolean rgbww_on = false;
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
                    rgbww_on = true;
                    break;

            }

        }

        seekbarSatr.setEnabled(rgbww_on);
        seekbarTemp.setEnabled(rgbww_on);

        for (int x : Controller.controlZones) {

            if (x == -1) {

                break;

            } else if (x == 0) {

                zone_1.setChecked(true);
                zone_2.setChecked(true);
                zone_3.setChecked(true);
                zone_4.setChecked(true);

                break;

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

                if (rgbww_on && any) {

                    deviceList.add(8);

                    seekbarSatr.setEnabled(true);
                    seekbarTemp.setEnabled(true);

                } else {

                    seekbarSatr.setEnabled(false);
                    seekbarTemp.setEnabled(false);

                }

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

        changeListener = new AppCompatCheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                iBoxSettings.hasIBoxLamp = has_iboxl.isChecked();
                iBoxSettings.hasRGBW = has_rgbw0.isChecked();
                iBoxSettings.hasRGBWW = has_rgbww.isChecked();
                iBoxSettings.hasDualW = has_dualw.isChecked();

                setElements();

                List<Integer> deviceList = new ArrayList<Integer>();

                if ((iBoxSettings.hasRGBWW || iBoxSettings.hasDualW) && rgb_ww.isChecked())
                    deviceList.add(8);

                if (iBoxSettings.hasRGBW && rgb_w0.isChecked())
                    deviceList.add(7);

                if (iBoxSettings.hasIBoxLamp && wifi_0.isChecked())
                    deviceList.add(0);

                int ret[] = new int[deviceList.size()];
                for (int i = 0; i < ret.length; i++) ret[i] = deviceList.get(i);

                Controller.controlDevices = ret;

                if (!(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasDualW)) {

                    Controller.controlZones = new int[]{-1};

                }

                iBoxSettings.save(getApplicationContext(), Controller.milightMac);
            }

        };

        has_iboxl.setOnCheckedChangeListener(changeListener);
        has_rgbw0.setOnCheckedChangeListener(changeListener);
        has_rgbww.setOnCheckedChangeListener(changeListener);
        has_dualw.setOnCheckedChangeListener(changeListener);

    }

    private void setElements() {


        if (!(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasIBoxLamp || iBoxSettings.hasDualW)) {

            findViewById(R.id.hasdevice).setVisibility(View.GONE);
            findViewById(R.id.hasnodevice).setVisibility(View.VISIBLE);

        } else {

            findViewById(R.id.hasnodevice).setVisibility(View.GONE);
            findViewById(R.id.hasdevice).setVisibility(View.VISIBLE);

            Controller.hasRGBWW = iBoxSettings.hasRGBWW;

            findViewById(R.id.colorpicker_parent).setVisibility(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasIBoxLamp ? View.VISIBLE : View.GONE);
            findViewById(R.id.zone_parent).setVisibility(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasDualW ? View.VISIBLE : View.GONE);
            findViewById(R.id.control_wifi_bridge).setVisibility(iBoxSettings.hasIBoxLamp ? View.VISIBLE : View.GONE);
            findViewById(R.id.control_rgbw).setVisibility(iBoxSettings.hasRGBW ? View.VISIBLE : View.GONE);
            findViewById(R.id.control_rgbww).setVisibility(iBoxSettings.hasRGBWW || iBoxSettings.hasDualW ? View.VISIBLE : View.GONE);
            findViewById(R.id.sattemp_parent).setVisibility(iBoxSettings.hasRGBWW || iBoxSettings.hasDualW ? View.VISIBLE : View.GONE);
            ((AppCompatCheckBox) findViewById(R.id.control_rgbww)).setText(iBoxSettings.hasRGBWW && iBoxSettings.hasDualW ? getString(R.string.rgbww) : (iBoxSettings.hasRGBWW ? getString(R.string.rgbww) : getString(R.string.dualw)));
        }
    }

}
