package com.jaspergoes.bilight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jaspergoes.bilight.helpers.Constants;
import com.jaspergoes.bilight.helpers.PreferenceActivityCompat;
import com.jaspergoes.bilight.helpers.PreferenceCategoryCompat;
import com.jaspergoes.bilight.helpers.PreferenceCompat;
import com.jaspergoes.bilight.milight.Controller;
import com.jaspergoes.bilight.milight.objects.Device;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends PreferenceActivityCompat {

    private class LocalDevicePreference extends PreferenceCompat {

        private final int deviceIndex;

        LocalDevicePreference(Context context, int deviceIndex) {

            super(context);

            this.deviceIndex = deviceIndex;

            Device device = Controller.milightDevices.get(deviceIndex);

            String t = findName(device.addrMAC);

            if (!t.equals("")) {
                t += " | ";
            }

            t += device.addrIP;

            this.setTitle(t);
            this.setSummary(device.addrMAC + (device.addrPort != Controller.defaultMilightPort ? " ( " + getString(R.string.port) + ": " + device.addrPort + " )" : ""));

        }

        @Override
        protected void onClick() {

            if (this.isEnabled() && !Controller.isConnecting) connectLocal(deviceIndex);

        }

    }

    private class RemoteDevicePreference extends PreferenceCompat implements View.OnLongClickListener {

        private final int deviceIndex;

        RemoteDevicePreference(Context context, int deviceIndex) {

            super(context);

            this.deviceIndex = deviceIndex;

            Device device = remoteMilightDevices.get(deviceIndex);

            String t = findName(device.addrMAC);

            if (!t.equals("")) {
                t += " | ";
            }

            t += device.addrIP;

            this.setTitle(t);
            this.setSummary(device.addrMAC + (device.addrPort != Controller.defaultMilightPort ? " ( " + getString(R.string.port) + ": " + device.addrPort + " )" : ""));

        }

        @Override
        protected void onClick() {

            if (this.isEnabled() && !Controller.isConnecting) {

                final Device device = remoteMilightDevices.get(deviceIndex);

                /* Prefer local connection if on discovered list */
                int l = Controller.milightDevices.size();
                for (int i = 0; i < l; i++) {

                    if (device.addrMAC.equals(Controller.milightDevices.get(i).addrMAC)) {

                        connectLocal(i);

                        return;

                    }

                }

                Controller.isConnecting = true;

                new Thread(new Runnable() {

                    @Override
                    public void run() {

                        Controller.INSTANCE.setDevice(device.addrIP, device.addrPort, getApplicationContext());

                    }

                }).start();

                updateList(false);

            }

        }

        @Override
        public boolean onLongClick(View view) {

            if (this.isEnabled() && !Controller.isConnecting) {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                try {

                    JSONArray newArray = new JSONArray();
                    JSONArray remoteArray = new JSONArray(prefs.getString("remotes", "[]"));
                    for (int i = 0; i < remoteArray.length(); i++) {
                        if (i != deviceIndex) {
                            newArray.put(remoteArray.getJSONObject(i));
                        }
                    }
                    prefs.edit().putString("remotes", newArray.toString()).apply();

                } catch (JSONException e) {

                    prefs.edit().putString("remotes", "[]").apply();

                }

                updateList(false);

                return true;

            }

            return false;

        }

    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED.equals(action)) {

                updateList(false);

            } else if (Constants.BILIGHT_DEVICE_CONNECTED.equals(action)) {

                startActivity(new Intent(context, ControlActivity.class));

            }

        }

    };

    private static ArrayList<Device> remoteMilightDevices = new ArrayList<Device>();

    private PreferenceCategoryCompat deviceList;
    private PreferenceCategoryCompat remoteList;

    public static boolean isFinishActivitiesOptionEnabled(Context context) {

        int result;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            result = Settings.System.getInt(context.getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
        } else {
            result = Settings.Global.getInt(context.getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0);
        }

        return result == 1;

    }

    private boolean autoConnect = false;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle bundle) {

        super.onCreate(bundle);

        if (Controller.INSTANCE == null) {
            new Controller();
        }

        autoConnect = true;

        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // Local Device Category
        deviceList = new PreferenceCategoryCompat(this);
        root.addPreference(deviceList);

        // Remote Device Category
        remoteList = new PreferenceCategoryCompat(this);
        remoteList.setOrderingAsAdded(true);
        remoteList.setTitle(getString(R.string.saved_devices));
        root.addPreference(remoteList);

        setPreferenceScreen(root);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                ListView listView = (ListView) adapterView;
                ListAdapter listAdapter = listView.getAdapter();
                Object obj = listAdapter.getItem(position);
                if (obj != null && obj instanceof View.OnLongClickListener) {
                    View.OnLongClickListener longListener = (View.OnLongClickListener) obj;
                    return longListener.onLongClick(view);
                }
                return false;
            }
        });

    }

    @Override
    protected void onResume() {

        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BILIGHT_DISCOVERED_DEVICES_CHANGED);
        filter.addAction(Constants.BILIGHT_DEVICE_CONNECTED);
        registerReceiver(receiver, filter);

        updateList(autoConnect && !isFinishActivitiesOptionEnabled(getApplicationContext()));
        autoConnect = false;

    }

    @Override
    protected void onPause() {

        super.onPause();

        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuitem_discover:
                if (!Controller.isConnecting) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            Controller.INSTANCE.discoverNetworks(getApplicationContext());
                        }

                    }).start();
                }
                return true;
            case R.id.menuitem_addremote:
                if (!Controller.isConnecting) {
                    startActivity(new Intent(getApplicationContext(), AddRemoteActivity.class));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateList(boolean connectIfOnlyOneSaved) {

        boolean maySelect = !Controller.isConnecting;
        int i, l;

        // set animation!

        /* Remove all discovered devices from the PreferenceGroup */
        deviceList.removeAll();
        deviceList.setTitle(getString(R.string.discovered_devices)); // + (Controller.networkInterfaceName.length() > 0 ? " - " + Controller.networkInterfaceName : ""));

        /* Re-populate the list with discovered devices */
        if ((l = Controller.milightDevices.size()) > 0) {
            for (i = 0; i < l; i++) {
                LocalDevicePreference pref = new LocalDevicePreference(this, i);
                pref.setIcon(getResources().getDrawable(findBoxResource(Controller.milightDevices.get(i).addrMAC)));
                pref.setEnabled(maySelect);
                deviceList.addPreference(pref);
            }
        } else {
            PreferenceCompat pref = new PreferenceCompat(this);
            pref.setTitle(getString(R.string.no_devices_discovered));
            pref.setIcon(getResources().getDrawable(R.drawable.ic_ibox));
            pref.setEnabled(false);
            deviceList.addPreference(pref);
        }

        /* Remove all remote devices from the PreferenceGroup */
        for (i = remoteList.getPreferenceCount() - 1; i >= 0; i--) {
            remoteList.removePreference(remoteList.getPreference(i));
        }
        remoteMilightDevices.clear();

        /* Re-populate the list of remote devices from sharedpreferences */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean any = false;
        try {
            JSONArray remoteArray = new JSONArray(prefs.getString("remotes", "[]"));
            for (i = 0; i < remoteArray.length(); i++) {
                JSONObject remote = remoteArray.getJSONObject(i);
                remoteMilightDevices.add(new Device(remote.getString("n"), remote.getString("m"), remote.getInt("p")));
                RemoteDevicePreference pref = new RemoteDevicePreference(this, i);
                pref.setIcon(getResources().getDrawable(findBoxResource(remoteMilightDevices.get(i).addrMAC)));
                pref.setEnabled(maySelect);
                remoteList.addPreference(pref);
                any = true;
            }
        } catch (JSONException e) {
            prefs.edit().putString("remotes", "[]").apply();
        }

        if (!any) {
            PreferenceCompat pref = new PreferenceCompat(this);
            pref.setTitle(getString(R.string.no_devices_saved));
            pref.setIcon(getResources().getDrawable(R.drawable.ic_ibox));
            pref.setEnabled(false);
            remoteList.addPreference(pref);
        }

        if (connectIfOnlyOneSaved && remoteMilightDevices.size() == 1) {

            final Device device = remoteMilightDevices.get(0);
            Controller.isConnecting = true;

            new Thread(new Runnable() {

                @Override
                public void run() {

                    Controller.INSTANCE.setDevice(device.addrIP, device.addrPort, getApplicationContext());

                }

            }).start();

            updateList(false);

        }

    }

    private void connectLocal(int deviceIndex) {

        Controller.isConnecting = true;

        Device device = Controller.milightDevices.get(deviceIndex);

        final String addressIP = device.addrIP;
        final int addressPort = device.addrPort;

        new Thread(new Runnable() {

            @Override
            public void run() {

                Controller.INSTANCE.setDevice(addressIP, addressPort, getApplicationContext());

            }

        }).start();

        updateList(false);

    }

    private String findName(String key) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {

            JSONArray remoteArray = new JSONArray(prefs.getString("devices", "[]"));
            for (int i = 0; i < remoteArray.length(); i++) {

                JSONObject device = remoteArray.getJSONObject(i);
                if (device.getString("m").equals(key)) {
                    return device.optString("t", "");
                }
            }

        } catch (JSONException e) {

            prefs.edit().putString("devices", "[]").apply();

        }

        return "";
    }

    private int findBoxResource(String key) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {

            JSONArray remoteArray = new JSONArray(prefs.getString("devices", "[]"));
            for (int i = 0; i < remoteArray.length(); i++) {

                JSONObject device = remoteArray.getJSONObject(i);
                if (device.getString("m").equals(key)) {
                    return device.optBoolean("i", true) ? R.drawable.ic_ibox : R.drawable.ic_ibox2;
                }
            }

        } catch (JSONException e) {

            prefs.edit().putString("devices", "[]").apply();

        }

        return R.drawable.ic_ibox;
    }

}