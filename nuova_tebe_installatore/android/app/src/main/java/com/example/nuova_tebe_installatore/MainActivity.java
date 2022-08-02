package com.example.nuova_tebe_installatore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattCallback;
import android.os.IBinder;
import android.util.Log;
import android.widget.RadioButton;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends FlutterActivity {
    private final static String TAG = "GATT SERVER";
    private static final String CHANNEL = "com.example.nuova_tebe_installatore/GATTserver";
    private String STATUS = "SEND CMD FLAG";
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;

    private int FwOffset = 0;
    private int FwIDX = 0;
    private int FwPhase = 0;
    private byte mFirmwareData[];
    private int mFirmwareLen = 0;
    private MethodChannel methodChannel;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    private String numInmpianto="25";

    public void setNumeroImpianto(String nImp){
        numInmpianto=nImp;
    }
    public String getNumeroImpianto(){
        return numInmpianto;
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        methodChannel= new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL);
        methodChannel.setMethodCallHandler(
                        (call, result) -> {

                            if (call.method.equals("startGattServer")) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                                }
                                mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
                                BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
                        
                                // Register for system Bluetooth events
                                IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                                Log.w(TAG, "Registro receiver");
                        
                                registerReceiver(mBluetoothReceiver, filter);
                                startServer();
                                result.success("bll");
                            } else if(call.method.equals("setPlant")) {
                                Log.w(TAG, (String) call.arguments);
                                setNumeroImpianto((String) call.arguments);
                                result.success("bll");
                            }
                            else
                            {
                              result.notImplemented();
                            }
                        }
                );
    }










    public String getSTATUS() {
        return STATUS;
    }

    public void setSTATUS(String STATUS) {
        this.STATUS = STATUS;
    }



    private String getProva() {
        int batteryLevel = -1;
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            Intent intent = new ContextWrapper(getApplicationContext()).
                    registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryLevel = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100) /
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        }
        String batteriaProva = "sono una prova del channel con batteria batteryLevel" + batteryLevel;
        return batteriaProva;
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    break;
                default:
                    // Do nothing
            }

        }
    };


    private void startServer() {


        Log.w(TAG, "Start GATT server");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permessi non concessi");
Log.w(TAG, String.valueOf(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)));
          //  return;
        }
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        Log.w(TAG, "Ho aperto il gatto");

        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            //return;
        }

        mBluetoothGattServer.addService(UartProfile.createUartService());
    }

    private void stopServer() {
        if (mBluetoothGattServer == null) return;
        Log.w(TAG, "stop GATT server");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mBluetoothGattServer.close();
    }

//    BluetoothGatt gattMy;


    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);

            }
        }



        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            // Invalid characteristic
            //Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
        }

        public int byteArrayToInt(byte[] b) {
            return b[3] & 0xFF |
                    (b[2] & 0xFF) << 8 |
                    (b[1] & 0xFF) << 16 |
                    (b[0] & 0xFF) << 24;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {


            Log.w(TAG, "SCRIVO su onCharacteristicWriteRequest " + value);



            int chunkdim = 128;
            //int chunkdim = 16;

            //Log.w(TAG, "Characteristic Write: " + characteristic.getUuid());
            String text = new String(value, StandardCharsets.US_ASCII);
            Log.w(TAG, "text from value " + text);

            //Log.w(TAG, text);
            byte[] cmd = null;
            //txtnumImpianto = ((TextView) findViewById(R.id.txtnumImpianto));
            if (text.contentEquals("PING!\0")) {
                Log.w(TAG, "Comunicazione ok");

            } else if (text.contentEquals("C:ACK\0") || text.contentEquals("NACK\0") || (value[0] == (byte) 0x43 && value[1] == (byte) 0x80) || (value[0] == (byte) 0x43 && value[1] == (byte) 0x81)) {

                Log.w(TAG, "ack o nack"+ text);


                if (getSTATUS().equalsIgnoreCase("SEND ENABLE CONF READ")) {

                    cmd = new byte[]{(byte) 0x43, (byte) 0x80};
                    setSTATUS("SEND READ FLAG");
                    methodChannel.invokeMethod("send", cmd);

                } else if (getSTATUS().equalsIgnoreCase("SEND READ FLAG")) {
                    Log.w(TAG, value.toString());
                    //TODO settare la radio button


                    cmd = new byte[]{(byte) 0x43, (byte) 0x81};
                    setSTATUS("SEND READ PLANT");
                    methodChannel.invokeMethod("send", cmd);

                } else if (getSTATUS().equalsIgnoreCase("SEND READ PLANT")) {
                    Log.w(TAG, value.toString());
                    //TODO settare text inpianto

                    byte[] ab = new byte[]{value[2], value[3], value[4], value[5]};
                    byte[] inverted = new byte[4];
                    int index = 0;
                    for (int z = ab.length - 1; z >= 0; z--) {
                        inverted[index] = ab[z];
                        index++;
                    }
                    Integer nii = byteArrayToInt(inverted);
                    String nitext = nii.toString();
                    String numImpiantoInverted = "";
                    for (int i = nitext.length(); i > 0; i--) {
                        numImpiantoInverted += nitext.charAt(i - 1);
                    }


                }

                if (getSTATUS().equalsIgnoreCase("SEND ENABLE CONF")) {


                    String flag = "NFC+125KHz"; ///mettere da chiamata da flutter
                    if (flag.equalsIgnoreCase("NFC+125KHz")) {
                        cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        setSTATUS("SEND CMD FLAG");
                        methodChannel.invokeMethod("send", cmd);


                    } else if (flag.equalsIgnoreCase("NFC+125KHz+13MHz")) {
                        cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                        setSTATUS("SEND CMD FLAG");
                        methodChannel.invokeMethod("send", cmd);


                    }


                } else if (getSTATUS().equalsIgnoreCase("SEND CMD FLAG")) {
                    Log.w(TAG, "SEND CMD FLAG");

                    cmd = new byte[]{(byte) 0x43, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};

                    //Invertire la stringa
                    String numImpiantoInverted = "";
                    for (int i = numInmpianto.length(); i > 0; i--) {
                        numImpiantoInverted += numInmpianto.charAt(i - 1);
                    }

                    if (!numInmpianto.isEmpty()) {
                        Integer ni = Integer.parseInt(numImpiantoInverted);
                        byte[] bytesimp = ByteBuffer.allocate(4).putInt(ni).array();
                        byte[] inverted = new byte[6];

                        int index = 2;
                        inverted[0] = (byte) 0x43;
                        inverted[1] = (byte) 0x01;
                        for (int z = bytesimp.length - 1; z >= 0; z--) {
                            inverted[index] = bytesimp[z];
                            index++;
                        }
                        Log.w(TAG, "ritorno byte impianto" + inverted.toString());

                        setSTATUS("SEND CMD PLANT");

                        Log.w(TAG, "sono diego che scrive");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.w(TAG, "sono nell'ui thread");

                                methodChannel.invokeMethod("send", inverted);
                            }
                        });


                    }
                } else if (getSTATUS().equalsIgnoreCase("SEND CMD PLANT")) {

                }


            } else if (text.contentEquals("U:ACK\0") || text.contentEquals("U:COK\0")) {
              /**  if (getSTATUS().equalsIgnoreCase("SEND FIRMWARE")) {

                    if (FwPhase == 0) {
                        cmd = new byte[]{(byte) 0x55, (byte) 0x3}; // erase signature
                        characteristica.setValue(cmd);
                        if (!gattMy.writeCharacteristic(characteristica)) {
                            Log.w(TAG, "cannot write");
                        } else
                            FwPhase = 1;
                    } else if (FwPhase == 1) {
                        cmd = new byte[]{(byte) 0x55, (byte) 0x4}; // erase flash
                        characteristica.setValue(cmd);
                        if (!gattMy.writeCharacteristic(characteristica)) {
                            Log.w(TAG, "cannot write");
                        } else
                            FwPhase = 2;
                    } else if (FwPhase == 2) {
                        cmd = new byte[]{(byte) 0x55, (byte) 0x2, (byte) 0xa5, (byte) 0x5a};
                        characteristica.setValue(cmd);
                        if (!gattMy.writeCharacteristic(characteristica)) {
                            Log.w(TAG, "cannot write");
                        } else
                            FwPhase = 3;
                    } else if (FwOffset < mFirmwareLen) {
                        if (FwOffset == 0) FwIDX = 0;

                        cmd = new byte[4 + chunkdim];
                        cmd[0] = 0x55;
                        if ((mFirmwareLen - FwOffset) > chunkdim)
                            cmd[1] = 0x00;
                        else
                            cmd[1] = 0x01;  // ultimo pacchetto

                        if ((FwIDX > 0) && ((FwIDX & 0xff) == 0)) FwIDX++;
                        cmd[2] = (byte) (FwIDX & 0xff);
                        cmd[3] = (byte) (FwIDX >> 8);
                        //Log.w(TAG, "ID: L=" + String.format("%02x", cmd[2]) + " H=" + String.format("%02x", cmd[3]));
                        if ((mFirmwareLen - FwOffset) >= chunkdim) {
                            System.arraycopy(mFirmwareData, FwOffset, cmd, 4, chunkdim);
                        } else {
                            System.arraycopy(mFirmwareData, FwOffset, cmd, 4, mFirmwareLen - FwOffset);
                            for (int i = 4 + mFirmwareLen - FwOffset; i < cmd.length; i++)
                                cmd[i] = (byte) 0xff;
                            // L'ultimo pacchetto non restituisce ACK perché il lettore si disconnette
                        }
                        //setSTATUS("SEND FIRMWARE");
                        characteristica.setValue(cmd);
                        if (!gattMy.writeCharacteristic(characteristica)) {
                            Log.w(TAG, "cannot write");
                        } else {
                            FwOffset += chunkdim;
                            FwIDX++;
                        }
                    } else {
                        FwPhase = 0;
                    }

                }*/
            } else {

                if (getSTATUS().equalsIgnoreCase("SEND ENABLE CONF"))
                    Log.w(TAG, "Errore configurazione");
                else if (getSTATUS().equalsIgnoreCase("SEND CMD FLAG"))
                    Log.w(TAG, "Errore salvataggio flag");
                else if (getSTATUS().equalsIgnoreCase("SEND CMD PLANT"))
                    Log.w(TAG, "Errore salvataggio impianto");
                else if (getSTATUS().equalsIgnoreCase("SEND FIRMWARE")) {
                    Log.w(TAG, "Errore aggiornamento firmware");
                    FwPhase = 0;
                } else
                    Log.w(TAG, "Errore di comunicazione");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {

            mBluetoothGattServer.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null);
            //            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {

            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
            //            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopServer();
        unregisterReceiver(mBluetoothReceiver);

    }


}