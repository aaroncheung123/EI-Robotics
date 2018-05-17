package com.aaroncheung.prototype4;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class RobotFacade extends ContextWrapper {


    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    public final String TAG = "debug_facade";
    private static RobotFacade sRobotFacadeInstance;

    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    private RobotFacade(Context base) {
        super(base);
        Log.d(TAG, "Constructor has been called 1");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        Log.d(TAG, "Constructor has been called 2");
    }

    public static RobotFacade getInstance(Context context){
        if(sRobotFacadeInstance == null){
            sRobotFacadeInstance = new RobotFacade(context);
        }
        return sRobotFacadeInstance;

    }

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            Log.d(TAG, "onReceivedData 1");
            try {
                data = new String(arg0, "UTF-8");
                Log.d(TAG, "data is " + data);
                //data = data.concat("\n");
                //tvAppend(textView, data);
                //tvAppend(textView, "test1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };


    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcast 1");
            if (intent.getAction() != null && intent.getAction().equals(ACTION_USB_PERMISSION)) {

                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);

                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            //setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Log.d(TAG, "Serial Connection Opened");
                            //tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop();

            }
            Log.d(TAG, "broadcast 2");
        }
    };



    public void onClickStart() {
        Log.d(TAG, "start 1");
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        Log.d(TAG, "start 1.5");
        if (!usbDevices.isEmpty()) {
            Log.d(TAG, "start 2");
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                Log.d(TAG, "start 3");
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    Log.d(TAG, "start 4");
                    PendingIntent pi = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }

        Log.d(TAG, "start 2");
    }

    public void onClickSend(String s) {
        onClickStart();
        Log.d(TAG, "sender was clicked: " + s);
        //Toast.makeText(this, "Message sent: " + s,Toast.LENGTH_SHORT).show();
        serialPort.write(s.getBytes());

        Log.d(TAG, "sender was clicked 1 ");

    }


    public void onClickStop() {
        //setUiEnabled(false);
        serialPort.close();
        Log.d(TAG, "Serial Connection Closed");
        //tvAppend(textView,"\nSerial Connection Closed! \n");
    }

}