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
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setHomeButtonEnabled(true);
        }

        setCheckboxes();

        ((TextView) findViewById(R.id.connect_mac)).setText(Controller.milightMac);

        /* Switch on */
        findViewById(R.id.switchOn).setOnClickListener(new View.OnClickListener() {
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
        findViewById(R.id.switchOff).setOnClickListener(new View.OnClickListener() {
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
        findViewById(R.id.setWhite).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setWhite();

                    }

                }).start();

                findViewById(R.id.colorpicker).invalidate();

            }
        });

        /* Audio analyzer | just trying out some stuff */
        // MicrophoneAnalyzer mic = new MicrophoneAnalyzer();
        // mic.startRecording();

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

                    findViewById(R.id.colorpicker).invalidate();

                }

            }

        });

        /* Make sure zone and device group names have equal widths */
        ArrayList<View> views = getViewsByTag((ViewGroup) findViewById(R.id.hasdevice), "equal");
        for (View v : views) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    equalWidths();
                }
            });
        }

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

    @Override
    protected void onStop() {

        super.onStop();

        if (isFinishing()) {
            Controller.disconnect();
        }

    }

    private void setCheckboxes() {

        /* Available device groups */
        final AppCompatCheckBox has_iboxl = (AppCompatCheckBox) findViewById(R.id.has_ibox_lamp);
        final AppCompatCheckBox has_rgbw0 = (AppCompatCheckBox) findViewById(R.id.has_rgbw);
        final AppCompatCheckBox has_rgbww = (AppCompatCheckBox) findViewById(R.id.has_rgbww);
        final AppCompatCheckBox has_dualw = (AppCompatCheckBox) findViewById(R.id.has_dualw);

        /* Set checkboxes, according to values saved in settings */
        has_iboxl.setChecked(iBoxSettings.hasIBoxLamp);
        has_rgbw0.setChecked(iBoxSettings.hasRGBW);
        has_rgbww.setChecked(iBoxSettings.hasRGBWW);
        has_dualw.setChecked(iBoxSettings.hasDualW);

        /* Device groups */
        final AppCompatCheckBox wifi_0 = (AppCompatCheckBox) findViewById(R.id.control_wifi_bridge);
        final AppCompatCheckBox rgb_w0 = (AppCompatCheckBox) findViewById(R.id.control_rgbw);
        final AppCompatCheckBox rgb_ww = (AppCompatCheckBox) findViewById(R.id.control_rgbww);
        final AppCompatCheckBox dualww = (AppCompatCheckBox) findViewById(R.id.control_dualw);

        /* Zones */
        final AppCompatCheckBox zone_1 = (AppCompatCheckBox) findViewById(R.id.control_zone_1);
        final AppCompatCheckBox zone_2 = (AppCompatCheckBox) findViewById(R.id.control_zone_2);
        final AppCompatCheckBox zone_3 = (AppCompatCheckBox) findViewById(R.id.control_zone_3);
        final AppCompatCheckBox zone_4 = (AppCompatCheckBox) findViewById(R.id.control_zone_4);

        /* Seekbars */
        final DiscreteSeekBar seekbarSatr = (DiscreteSeekBar) findViewById(R.id.seekbar_saturation);
        final DiscreteSeekBar seekbarTemp = (DiscreteSeekBar) findViewById(R.id.seekbar_colortemp);

        setElements();

        /* Set checkboxes, according to values currently stored in Controller */
        for (int i : Controller.controlDevices) {

            switch (i) {

                case 0:
                    wifi_0.setChecked(true);
                    break;

                case 3:
                    dualww.setChecked(true);
                    break;

                case 7:
                    rgb_w0.setChecked(true);
                    break;

                case 8:
                    rgb_ww.setChecked(true);
                    break;

            }

        }

        boolean any = false;

        for (int x : Controller.controlZones) {

            if (x == -1) {

                break;

            } else if (x == 0) {

                any = true;

                zone_1.setChecked(true);
                zone_2.setChecked(true);
                zone_3.setChecked(true);
                zone_4.setChecked(true);

                break;

            } else {

                switch (x) {
                    case 1:
                        any = true;
                        zone_1.setChecked(true);
                        break;
                    case 2:
                        any = true;
                        zone_2.setChecked(true);
                        break;
                    case 3:
                        any = true;
                        zone_3.setChecked(true);
                        break;
                    case 4:
                        any = true;
                        zone_4.setChecked(true);
                        break;

                }

            }

        }

        seekbarSatr.setEnabled(any && rgb_ww.isChecked());
        seekbarTemp.setEnabled(any && (rgb_ww.isChecked() || dualww.isChecked()));

        AppCompatCheckBox.OnCheckedChangeListener changeListener = new AppCompatCheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                boolean rgbw_on = rgb_w0.isChecked();
                boolean rgbww_on = rgb_ww.isChecked();
                boolean dualw_on = dualww.isChecked();
                boolean any = zone_1.isChecked() || zone_2.isChecked() || zone_3.isChecked() || zone_4.isChecked();

                List<Integer> deviceList = new ArrayList<Integer>();

                seekbarSatr.setEnabled(any && rgbww_on);
                seekbarTemp.setEnabled(any && (rgbww_on || dualw_on));

                if (rgbww_on && any)
                    deviceList.add(8);

                if (rgbw_on && any)
                    deviceList.add(7);

                if (dualw_on && any)
                    deviceList.add(3);

                if (wifi_0.isChecked())
                    deviceList.add(0);

                int ret[] = new int[deviceList.size()];
                for (int i = 0; i < ret.length; i++) ret[i] = deviceList.get(i);

                Controller.controlDevices = ret;

                if (any && (rgbw_on || rgbww_on || dualw_on)) {

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
        dualww.setOnCheckedChangeListener(changeListener);
        zone_1.setOnCheckedChangeListener(changeListener);
        zone_2.setOnCheckedChangeListener(changeListener);
        zone_3.setOnCheckedChangeListener(changeListener);
        zone_4.setOnCheckedChangeListener(changeListener);

        changeListener = new AppCompatCheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if (iBoxSettings.hasIBoxLamp != (iBoxSettings.hasIBoxLamp = has_iboxl.isChecked()) && iBoxSettings.hasIBoxLamp) {
                    wifi_0.setChecked(true);
                }
                if (iBoxSettings.hasRGBW != (iBoxSettings.hasRGBW = has_rgbw0.isChecked()) && iBoxSettings.hasRGBW) {
                    rgb_w0.setChecked(true);
                }
                if (iBoxSettings.hasRGBWW != (iBoxSettings.hasRGBWW = has_rgbww.isChecked()) && iBoxSettings.hasRGBWW) {
                    rgb_ww.setChecked(true);
                }
                if (iBoxSettings.hasDualW != (iBoxSettings.hasDualW = has_dualw.isChecked()) && iBoxSettings.hasDualW) {
                    dualww.setChecked(true);
                }

                setElements();

                List<Integer> deviceList = new ArrayList<Integer>();

                if (iBoxSettings.hasRGBWW && rgb_ww.isChecked())
                    deviceList.add(8);

                if (iBoxSettings.hasRGBW && rgb_w0.isChecked())
                    deviceList.add(7);

                if (iBoxSettings.hasDualW && dualww.isChecked())
                    deviceList.add(3);

                if (iBoxSettings.hasIBoxLamp && wifi_0.isChecked())
                    deviceList.add(0);

                int ret[] = new int[deviceList.size()];
                for (int i = 0; i < ret.length; i++) ret[i] = deviceList.get(i);

                Controller.controlDevices = ret;

                if (!(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasDualW)) {

                    Controller.controlZones = new int[]{-1};

                } else {

                    /* Set zones active */
                    if (zone_1.isChecked() || zone_2.isChecked() || zone_3.isChecked() || zone_4.isChecked()) {

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

                iBoxSettings.save(getApplicationContext(), Controller.milightMac);

            }

        };

        has_iboxl.setOnCheckedChangeListener(changeListener);
        has_rgbw0.setOnCheckedChangeListener(changeListener);
        has_rgbww.setOnCheckedChangeListener(changeListener);
        has_dualw.setOnCheckedChangeListener(changeListener);

    }

    private void setElements() {

        if (!(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasDualW || iBoxSettings.hasIBoxLamp)) {

            findViewById(R.id.hasdevice).setVisibility(View.GONE);
            findViewById(R.id.hasnodevice).setVisibility(View.VISIBLE);

        } else {

            findViewById(R.id.hasnodevice).setVisibility(View.GONE);
            findViewById(R.id.hasdevice).setVisibility(View.VISIBLE);

            findViewById(R.id.colorpicker_parent).setVisibility(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasIBoxLamp ? View.VISIBLE : View.GONE);
            findViewById(R.id.zone_parent).setVisibility(iBoxSettings.hasRGBW || iBoxSettings.hasRGBWW || iBoxSettings.hasDualW ? View.VISIBLE : View.GONE);

            findViewById(R.id.control_wifi_bridge).setVisibility(iBoxSettings.hasIBoxLamp ? View.VISIBLE : View.GONE);
            findViewById(R.id.control_rgbw).setVisibility(iBoxSettings.hasRGBW ? View.VISIBLE : View.GONE);
            findViewById(R.id.control_rgbww).setVisibility(iBoxSettings.hasRGBWW ? View.VISIBLE : View.GONE);
            findViewById(R.id.control_dualw).setVisibility(iBoxSettings.hasDualW ? View.VISIBLE : View.GONE);

            int countGroups = 0;
            if (iBoxSettings.hasIBoxLamp) countGroups++;
            if (iBoxSettings.hasRGBW) countGroups++;
            if (iBoxSettings.hasRGBWW) countGroups++;
            if (iBoxSettings.hasDualW) countGroups++;
            findViewById(R.id.device_parent).setVisibility(countGroups > 1 ? View.VISIBLE : View.GONE);

            ((TextView) findViewById(R.id.warn_rgbww)).setText(getString(R.string.rgbww_dw_only).replace("%s", getString(R.string.rgbww)));
            findViewById(R.id.sat_parent).setVisibility(iBoxSettings.hasRGBWW ? View.VISIBLE : View.GONE);

            ((TextView) findViewById(R.id.warn_rgbww_dualw)).setText(getString(R.string.rgbww_dw_only).replace("%s", iBoxSettings.hasRGBWW && iBoxSettings.hasDualW ? getString(R.string.rgbww) + " / " + getString(R.string.dualw) : getString(iBoxSettings.hasRGBWW ? R.string.rgbww : R.string.dualw)));
            findViewById(R.id.tmp_parent).setVisibility(iBoxSettings.hasRGBWW || iBoxSettings.hasDualW ? View.VISIBLE : View.GONE);

        }

    }

    private void equalWidths() {
        int w = 0;
        ArrayList<View> views = getViewsByTag((ViewGroup) findViewById(R.id.hasdevice), "equal");
        for (View v : views) w = Math.max(v.getMeasuredWidth(), w);
        for (View v : views) {
            if (v.getParent() instanceof LinearLayout) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT);
                v.setLayoutParams(p);
            } else {
                RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(w, RelativeLayout.LayoutParams.WRAP_CONTENT);
                p.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()), 0, 0);
                v.setLayoutParams(p);
            }
        }

    }

    private static ArrayList<View> getViewsByTag(ViewGroup root, String tag) {
        ArrayList<View> views = new ArrayList<View>();
        final int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                views.addAll(getViewsByTag((ViewGroup) child, tag));
            }

            final Object tagObj = child.getTag();
            if (tagObj != null && tagObj.equals(tag)) {
                views.add(child);
            }

        }
        return views;
    }

}
