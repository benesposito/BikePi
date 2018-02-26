package com.example.espo1.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MyActivity";
    private static final UUID uuid = UUID.randomUUID();

    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private BluetoothDevice rpi;
    private static BluetoothChatService bluetooth;

    private Button buttonSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initiateBluetooth();

        buttonSend = findViewById(R.id.buttonSend);

        buttonSend.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                bluetooth.write("Test".getBytes());
            }
        });

    }

    public void initiateBluetooth()
    {
        adapter = BluetoothAdapter.getDefaultAdapter();
        rpi = adapter.getRemoteDevice("B8:27:EB:42:24:F8");

        adapter.cancelDiscovery();

        Log.d(TAG, rpi.getName());

        bluetooth = new BluetoothChatService();
        bluetooth.connect(rpi);

        bluetooth.write("Test".getBytes());
    }
}
