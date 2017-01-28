package com.jaspergoes.bilight;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jaspergoes.bilight.milight.Controller;

import java.util.regex.Pattern;

public class AddRemoteActivity extends AppCompatActivity {

    private EditText port;
    private EditText ip;
    private TextView ip_local_warn;
    private Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_remote);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        TextWatcher validate = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                validate();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                validate();
            }
        };

        ip = (EditText) findViewById(R.id.edit_ipaddress);
        ip.addTextChangedListener(validate);

        port = (EditText) findViewById(R.id.edit_port);
        port.setText(Integer.toString(Controller.defaultMilightPort));
        port.addTextChangedListener(validate);

        ip_local_warn = (TextView) findViewById(R.id.ip_local_warn);
        ip_local_warn.setVisibility(View.GONE);

        connect = (Button) findViewById(R.id.connect);
        connect.setEnabled(false);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Controller.INSTANCE.setDevice(ip.getText().toString().trim(), Integer.parseInt(port.getText().toString().trim()), getApplicationContext());
                    }
                }).start();
                finish();
            }
        });

    }

    private void validate() {

        boolean valid = true;
        boolean local = false;
        String t;

        t = ip.getText().toString().trim();
        Pattern ipPattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        if (!ipPattern.matcher(t).matches()) {
            Pattern hostPattern = Pattern.compile("(?=^.{4,253}$)(^((?!-)[a-zA-Z0-9-]{1,63}(?<!-)\\.)+[a-zA-Z]{2,63}$)");
            if (!hostPattern.matcher(t).matches()) {
                valid = false;
            }
        } else {
            local = isLocalV4Address(t);
        }

        t = port.getText().toString().trim();
        try {
            Integer.parseInt(t);
        } catch (NumberFormatException e) {
            valid = false;
        }

        ip_local_warn.setVisibility(local ? View.VISIBLE : View.GONE);

        connect.setEnabled(valid);

    }

    private boolean isLocalV4Address(String ipAddress) {

        /* Fuggggggly function to check whether ip address entered is or might be local */

        boolean isLocal = false;

        if (ipAddress != null && !ipAddress.isEmpty()) {

            String[] ip = ipAddress.split("\\.");

            if (ip.length == 4) {

                try {

                    short[] ipNumber = new short[]{
                            Short.parseShort(ip[0]),
                            Short.parseShort(ip[1]),
                            Short.parseShort(ip[2]),
                            Short.parseShort(ip[3])
                    };

                    if (ipNumber[0] == 10) { // Class A
                        isLocal = true;
                    } else if (ipNumber[0] == 172 && (ipNumber[1] >= 16 && ipNumber[1] <= 31)) { // Class B
                        isLocal = true;
                    } else if (ipNumber[0] == 192 && ipNumber[1] == 168) { // Class C
                        isLocal = true;
                    }

                } catch (NumberFormatException e) {
                }

            }

        }

        return isLocal;

    }

}
