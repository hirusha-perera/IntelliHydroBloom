package com.example.intellihydrobloom;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.HashMap;

public class DeviceFragment extends Fragment {

    private static final String USB_PERMISSION = "com.yourapp.USB_PERMISSION";
    private UsbCommunication usbCommunication;
    private UsbManager usbManager;
    private UsbDevice usbDevice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device, container, false);
        usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);

        usbDevice = getUsbDevice();  // Initialize usbDevice

        Button addButton = view.findViewById(R.id.btn_device_save);

        if(usbDevice != null) {
            TextView connectionStatus = view.findViewById(R.id.txt_connection_status);
            connectionStatus.setText("Connected");
            connectionStatus.setTextColor(getResources().getColor(R.color.h_gradient_dark_green));
        }
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUsbData();
            }
        });

        checkUsbPermission();

        return view;
    }

    private UsbDevice getUsbDevice() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        for (UsbDevice device : usbDevices.values()) {
            // Assuming your NodeMCU has a vendor ID of 1234 and a product ID of 5678
            if (device.getVendorId() == 4292  && device.getProductId() == 60000 ) {
                return device;
            }
        }
        return null;
    }

    private void checkUsbPermission() {
        if (usbDevice != null) {
            if (usbManager.hasPermission(usbDevice)) {
                // Permission already granted, initialize UsbCommunication
                usbCommunication = new UsbCommunication(getContext(), usbDevice);
            } else {
                // Request permission
                PendingIntent permissionIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
                usbManager.requestPermission(usbDevice, permissionIntent);
            }
        } else {
            Toast.makeText(getContext(), "USB device not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendUsbData() {
        // Get user input
        EditText deviceNameField = getView().findViewById(R.id.txt_deviceName);
        String deviceName = deviceNameField.getText().toString();

        EditText plantTypeField = getView().findViewById(R.id.txt_plantType);
        String plantType = plantTypeField.getText().toString();

        EditText plantSpeciesField = getView().findViewById(R.id.txt_plantSpecies);
        String plantSpecies = plantSpeciesField.getText().toString();

        EditText wifiSSIDField = getView().findViewById(R.id.txt_wifiSSID);
        String wifiSSID = wifiSSIDField.getText().toString();

        EditText wifiPasswordField = getView().findViewById(R.id.txt_wifiPassword);
        String wifiPassword = wifiPasswordField.getText().toString();

        // Construct data string to send
        String dataString = deviceName + "," + plantType + "," + plantSpecies + "," + wifiSSID + "," + wifiPassword + "\n";
        byte[] data = dataString.getBytes();
        ProgressBar progressBar = getView().findViewById(R.id.data_sending_progressbar);
        progressBar.setVisibility(View.VISIBLE);
        // Send data over USB
        if (usbCommunication != null) {
            int result = usbCommunication.sendData(data);
            if (result >= 0) {
                Toast.makeText(getContext(), "Data sent successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to send data", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "USB not connected", Toast.LENGTH_SHORT).show();
        }
        progressBar.setVisibility(View.GONE);
    }


    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            usbCommunication = new UsbCommunication(getContext(), usbDevice);
                        }
                    } else {
                        Toast.makeText(getContext(), "USB permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();

        IntentFilter permissionFilter = new IntentFilter(USB_PERMISSION);
        permissionFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        permissionFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        getActivity().registerReceiver(usbPermissionReceiver, permissionFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(usbPermissionReceiver);
    }

    @Override
    public void onDestroy() {
        if (usbCommunication != null) {
            usbCommunication.closeConnection();
        }
        super.onDestroy();
    }
}
